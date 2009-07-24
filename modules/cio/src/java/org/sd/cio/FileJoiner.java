/*
    Copyright 2009 Semantic Discovery, Inc. (www.semanticdiscovery.com)

    This file is part of the Semantic Discovery Toolkit.

    The Semantic Discovery Toolkit is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    The Semantic Discovery Toolkit is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with The Semantic Discovery Toolkit.  If not, see <http://www.gnu.org/licenses/>.
*/
package org.sd.cio;


import org.sd.io.FileUtil;
import org.sd.util.LineBuilder;
import org.sd.util.PropertiesParser;
import org.sd.util.ReflectUtil;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Utility to join data from one file with the data from another.
 * <p>
 * @author Spence Koehler
 */
public class FileJoiner {
  
  public static interface KeyGenerator {
    public String generateKey(String fieldData);
  }

  public static final class DefaultKeyGenerator implements KeyGenerator {
    public String generateKey(String fieldData) {return fieldData;}
  }

  public static final DefaultKeyGenerator DEFAULT_KEY_GENERATOR = new DefaultKeyGenerator();


  private File[] inputFiles;
  private FieldRef[] fieldRefs;
  private int[] joinCols;
  private boolean[] keepEmpties;
  private boolean subtract;
  private KeyGenerator[] keyGenerators;
  private boolean[] skipFirstLines;
  private File outputFile;
  private boolean verbose;

  /**
   * Properties:
   *
   *  fileA -- The first file to pull information from
   *  fileB -- The second file to pull information from
   *
   *  outputCols -- output columns with comma-delimited entries  of the
   *                form X.n where X is "A" or "B" for file A or file B
   *                and n is the (0-based) column number from its file.
   *
   *  joinColA -- the (0-based) column number from A to use for joining.
   *  joinColB -- the (0-based) column number from B to use for joining.
   *
   *  keepAllA -- "true" (default="false") to keep all A keys, even if empty.
   *  keepAllB -- "true" (default="false") to keep all B keys, even if empty.
   *
   *  subtract -- "true" (default="false") to suppress joined lines. Note
   *              that one would set keepAllX to "true" where X represents the
   *              set to keep after subtraction.
   *
   *  keyGeneratorA -- classpath for the "A" KeyGenerator to use to align the
   *                   join columns.
   *  keyGeneratorB -- classpath for the "B" KeyGenerator to use to align the
   *                   join columns.
   *
   *  skipFirstLineA -- "true" (default="false") to skip file A's first (i.e.
   *                     header) line
   *  skipFirstLineB -- "true" (default="false") to skip file B's first (i.e.
   *                     header) line
   *
   *  outputFile -- path to the output file.
   *
   *  verbose -- "true" (default="true") to print debug info to stdout.
   */
  public FileJoiner(Properties properties) {
    this.inputFiles = new File[2];
    this.joinCols = new int[2];
    this.keepEmpties = new boolean[2];
    this.keyGenerators = new KeyGenerator[2];
    this.skipFirstLines = new boolean[2];

    this.inputFiles[0] = FileUtil.getFile(properties.getProperty("fileA"));
    this.inputFiles[1] = FileUtil.getFile(properties.getProperty("fileB"));

    final String[] outputCols = properties.getProperty("outputCols").split("\\s*,\\s*");
    this.fieldRefs = buildFieldRefs(outputCols);

    this.joinCols[0] = Integer.parseInt(properties.getProperty("joinColA"));
    this.joinCols[1] = Integer.parseInt(properties.getProperty("joinColB"));
    this.keepEmpties[0] = "true".equals(properties.getProperty("keepAllA", "false"));
    this.keepEmpties[1] = "true".equals(properties.getProperty("keepAllB", "false"));

    this.subtract = "true".equals(properties.getProperty("subtract", "false"));

    final String keyGeneratorNameA = properties.getProperty("keyGeneratorA");
    final String keyGeneratorNameB = properties.getProperty("keyGeneratorB");

    this.keyGenerators[0] = keyGeneratorNameA != null ?
      (KeyGenerator)ReflectUtil.buildInstance(keyGeneratorNameA, properties) :
      DEFAULT_KEY_GENERATOR;

    this.keyGenerators[1] = keyGeneratorNameB != null ?
      (KeyGenerator)ReflectUtil.buildInstance(keyGeneratorNameB, properties) :
      DEFAULT_KEY_GENERATOR;

    this.skipFirstLines[0] = "true".equals(properties.getProperty("skipFirstLineA", "false"));
    this.skipFirstLines[1] = "true".equals(properties.getProperty("skipFirstLineB", "false"));

    this.outputFile = new File(properties.getProperty("outputFile"));
    this.verbose = "true".equals(properties.getProperty("verbose", "true"));

    if (verbose) {
      for (int i = 0; i < 2; ++i) {
        final char c = (char)('A' + i);
        System.out.println(c + ": file=" + inputFiles[i] +
                           " joinCol=" + joinCols[i] +
                           " keepEmpties=" + keepEmpties[i] +
                           " keyGenerator=" + keyGenerators[i] +
                           " skipFirstLine=" + skipFirstLines[i]);
      }
      System.out.println("outputFile=" + outputFile);
      System.out.println("subtract=" + subtract);
    }
  }

  public void doJoin() throws IOException {

    final long lenA = inputFiles[0].length();
    final long lenB = inputFiles[1].length();
    final int loadIndex = (lenB < lenA) ? 1 : 0;  // load the smaller file
    final int iterIndex = (loadIndex + 1) % 2;    // iterate over the other

    if (verbose) {
      System.out.println("loadIndex=" + Character.toString((char)('A' + loadIndex)));
      System.out.println("iterIndex=" + Character.toString((char)('A' + iterIndex)));
    }

    // load one file
    final Map<String, List<Record>> map = loadFile(loadIndex);
    final File loadFile = inputFiles[loadIndex];
    final int loadJoinCol = joinCols[loadIndex];
    final boolean keepLoadEmpties = keepEmpties[loadIndex];
    final Set<String> loadKeys = keepLoadEmpties ? new HashSet<String>(map.keySet()) : null;

    if (verbose) {
      System.out.println("keyCount=" + map.size());
    }

    // iterate over the other
    int iterCount = 0;
    int joinCount = 0;
    int writeCount = 0;

    final File iterFile = inputFiles[iterIndex];
    final int iterJoinCol = joinCols[iterIndex];
    final boolean keepIterEmpties = keepEmpties[iterIndex];
    final KeyGenerator iterKeyGenerator = keyGenerators[iterIndex];

    final BufferedReader reader = FileUtil.getReader(inputFiles[iterIndex]);
    final BufferedWriter writer = FileUtil.getWriter(outputFile);
    String line = skipFirstLines[iterIndex] ? reader.readLine() : null;
    while ((line = reader.readLine()) != null) {
      if ("".equals(line) || line.charAt(0) == '#') continue;
      ++iterCount;

      final Record iterRecord = new Record(line);
      final String key = iterKeyGenerator.generateKey(iterRecord.getField(iterJoinCol));
      if (key == null || "".equals(key)) continue;

      if (verbose && iterCount < 10) {
        System.out.println("iterKey #" + iterCount + ": " + key);
      }

      final List<Record> loadRecords = map.get(key);
      if (loadRecords != null) {
        ++joinCount;
        if (loadKeys != null) loadKeys.remove(key);

        // join and write joined records
        if (!subtract) {
          final List<Record> joinedRecords = doJoin(iterIndex, iterRecord, loadIndex, loadRecords);
          for (Record record : joinedRecords) {
            ++writeCount;
            writer.write(record.toString());
            writer.newLine();
            writer.flush();
          }
        }
      }
      else if (keepIterEmpties) {
        // write an empty iter record
        final Record emptyRecord = getEmptyRecord(iterIndex, iterRecord);
        ++writeCount;
        writer.write(emptyRecord.toString());
        writer.newLine();
        writer.flush();
      }
    }
    reader.close();

    if (loadKeys != null && loadKeys.size() > 0) {
      // write empty load records
      for (String loadKey : loadKeys) {
        final List<Record> loadRecords = map.get(loadKey);
        for (Record loadRecord : loadRecords) {
          ++writeCount;
          final Record emptyRecord = getEmptyRecord(loadIndex, loadRecord);
          writer.write(emptyRecord.toString());
          writer.newLine();
          writer.flush();
        }
      }
    }

    writer.close();

    if (verbose) {
      System.out.println("iterCount=" + iterCount);
      System.out.println("joinCount=" + joinCount);
      System.out.println("writeCount=" + writeCount);
    }
  }

  private final List<Record> doJoin(int iterIndex, Record iterRecord, int loadIndex, List<Record> loadRecords) {
    final List<Record> result = new ArrayList<Record>();

    for (Record loadRecord : loadRecords) {
      final Record joinedRecord = doJoin(iterIndex, iterRecord, loadIndex, loadRecord);
      if (joinedRecord != null) {
        result.add(joinedRecord);
      }
    }

    return result;
  }

  private final Record doJoin(int iterIndex, Record iterRecord, int loadIndex, Record loadRecord) {
    final Record result = new Record();

    int index = 0;
    for (FieldRef fieldRef : fieldRefs) {
      final String fieldValue = fieldRef.getFieldValue(iterIndex, iterRecord, loadIndex, loadRecord);
      result.setField(index++, fieldValue);
    }

    return result;
  }

  private Record getEmptyRecord(int typeIndex, Record record) {
    final Record result = new Record();

    int index = 0;
    for (FieldRef fieldRef : fieldRefs) {
      final String fieldValue = fieldRef.getFieldValue(typeIndex, record);
      result.setField(index++, fieldValue);
    }

    return result;
  }

  private final Map<String, List<Record>> loadFile(int loadIndex) throws IOException {
    final File file = inputFiles[loadIndex];
    final int joinCol = joinCols[loadIndex];
    final KeyGenerator keyGenerator = keyGenerators[loadIndex];

    final Map<String, List<Record>> result = new HashMap<String, List<Record>>();

    int loadCount = 0;
    final BufferedReader reader = FileUtil.getReader(file);
    String line = skipFirstLines[loadIndex] ? reader.readLine() : null;
    while ((line = reader.readLine()) != null) {
      if ("".equals(line) || line.charAt(0) == '#') continue;
      ++loadCount;

      final Record record = new Record(line);
      final String key = keyGenerator.generateKey(record.getField(joinCol));

      if (key != null && !"".equals(key)) {
        if (verbose && loadCount < 10) {
          System.out.println("loadKey #" + loadCount + ": " + key);
        }

        List<Record> records = result.get(key);
        if (records == null) {
          records = new ArrayList<Record>();
          result.put(key, records);
        }
        records.add(record);
      }
    }
    reader.close();

    if (verbose) {
      System.out.println("loadCount=" + loadCount);
    }

    return result;
  }


  private static final FieldRef[] buildFieldRefs(String[] outputCols) {
    final FieldRef[] result = new FieldRef[outputCols.length];

    for (int i = 0; i < outputCols.length; ++i) {
      result[i] = new FieldRef(outputCols[i]);
    }

    return result;
  }

  public static final class FieldRef {

    private String fieldRefString;
    private final int[] typeIndexes;
    private final int[] fieldNums;

    /**
     * Create a field reference given a string of the form X.n
     * where X is "A" or "B" and n is a 0-based index.
     */
    public FieldRef(String fieldRefString) {
      final String[] parts = fieldRefString.split("/");

      this.typeIndexes = new int[parts.length];
      this.fieldNums = new int[parts.length];

      int i = 0;
      for (String part : parts) {
        final String[] pieces = part.split("\\.");
        typeIndexes[i] = (pieces[0].charAt(0) - 'A');
        fieldNums[i] = Integer.parseInt(pieces[1]);
        ++i;
      }
    }

    public String getFieldValue(int typeIndex1, Record record1, int typeIndex2, Record record2) {
      final int typeIndex = typeIndexes[0];
      final int fieldNum = fieldNums[0];

      return (typeIndex == typeIndex1) ?
        record1.getField(fieldNum) :
        record2.getField(fieldNum);
    }

    public String getFieldValue(int typeIndex, Record record) {
      String result = null;

      for (int i = 0; i < typeIndexes.length; ++i) {
        if (typeIndex == typeIndexes[i]) {
          result = record.getField(fieldNums[i]);
          break;
        }
      }

      return result;
    }

    public String toString() {
      return fieldRefString;
    }
  }

  public static final class Record {

    private String[] pieces;
    private String line;

    public Record() {
      this.pieces = null;
      this.line = null;
    }

    public Record(String line) {
      this.pieces = line.split("\\s*\\|\\s*");
      this.line = line;
    }

    public Record(String[] pieces) {
      this.pieces = pieces;
      this.line = null;
    }

    public String getField(int index) {
      return (index < pieces.length && index >= 0) ? pieces[index] : "";
    }

    public void setField(int index, String fieldValue) {
      this.line = null;
      if (pieces == null || index >= pieces.length) {
        final String[] newPieces = new String[index + 1];
        if (pieces != null) {
          for (int i = 0; i < pieces.length; ++i) {
            newPieces[i] = pieces[i];
          }
        }
        this.pieces = newPieces;
      }
      pieces[index] = fieldValue;
    }

    public String toString() {
      if (line == null) {
        final LineBuilder builder = new LineBuilder();

        for (String piece : pieces) {
          builder.append(piece);
        }

        line = builder.toString();
      }
      return line;
    }
  }

  //
  // examples:
  //
  // - remove elements of fileB from fileA:
  //   - java org.sd.io.FileJoiner fileA=/home/sbk/tmp/jointest/fileA.txt fileB=/home/sbk/tmp/jointest/fileB.txt outputCols="A.0/B.0" joinColA=0 joinColB=0 keepAllA=true keepAllB=false subtract=true outputFile=/home/sbk/tmp/jointest/fileA-B.txt
  //
  // - filter certain elements (by generating non-null key for the elements to filter) from a file
  //   - java org.sd.io.FileJoiner fileA=/home/sbk/tmp/jointest/fileA.txt fileB=/home/sbk/tmp/jointest/fileA.txt outputCols="A.0/B.0" joinColA=0 joinColB=0 keepAllA=true keepAllB=false subtract=true keyGeneratorB=org.sd.io.TestFileJoiner$VowelFilterGenerator outputFile=/home/sbk/tmp/jointest/file.filtered.txt
  //
  // ...

  public static final void main(String[] args) throws IOException {
    // Properties:
    //
    //  fileA -- The first file to pull information from
    //  fileB -- The second file to pull information from
    //
    //  outputCols -- output columns of the form X.n where X is "A" or "B"
    //                for file A or file B and n is the (0-based) column
    //                number from its file.
    //
    //  joinColA -- the (0-based) column number from A to use for joining.
    //  joinColB -- the (0-based) column number from B to use for joining.
    //
    //  keepAllA -- "true" (default="false") to keep all A keys, even if empty.
    //  keepAllB -- "true" (default="false") to keep all B keys, even if empty.
    //
    //  subtract -- "true" (default="false") to suppress joined lines. Note
    //              that one would set keepAllX to "true" where X represents the
    //              set to keep after subtraction.
    //
    //  keyGeneratorA -- classpath for the "A" KeyGenerator to use to align the
    //                   join columns.
    //  keyGeneratorB -- classpath for the "B" KeyGenerator to use to align the
    //                   join columns.
    //
    //  outputFile -- path to the output file.
    //
    //  verbose -- "true" (default="true") to print debug info to stdout.
    //
    final PropertiesParser pp = new PropertiesParser(args);
    final Properties properties = pp.getProperties();

    final FileJoiner fileJoiner = new FileJoiner(properties);
    fileJoiner.doJoin();
  }
}
