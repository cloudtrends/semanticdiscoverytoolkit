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
package org.sd.wn;


import org.sd.bdb.DbMap;
import org.sd.bdb.DbValue;
import org.sd.cio.MessageHelper;
import org.sd.io.FileUtil;
import org.sd.io.Publishable;

import java.io.BufferedReader;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class to hold a WordNet sense index in memory.
 * <p>
 * @author Spence Koehler
 */
public class SenseIndex {
  
  private static final String SENSE_INDEX = "index.sense";
  private static final Map<String, SenseIndex> dict2index = new HashMap<String, SenseIndex>();

  public static final SenseIndex getInstance(String dictPath) throws IOException {
    return getInstance(dictPath != null ? FileUtil.getFile(dictPath) : null);
  }

  public static final SenseIndex getInstance(File dictDir) throws IOException {
    if (dictDir == null) dictDir = WordNetUtils.getDefaultDictDir();

    final String dictPath = dictDir.getAbsolutePath();

    SenseIndex result = dict2index.get(dictPath);

    if (result == null) {
      result = new SenseIndex(dictDir);
      dict2index.put(dictPath, result);
    }

    return result;
  }

  public static final void closeAll() {
    for (SenseIndex index : dict2index.values()) {
      index.close();
    }
  }


  private final File dictDir;
  private final DbMap word2entries;

  private SenseIndex(File dictDir) throws IOException {
    this.dictDir = dictDir;
    this.word2entries = buildDbMap(dictDir);
  }

  public List<WordSense> getWordSenses(String word) {
    List<WordSense> result = null;

    for (WordForm wordForm = new WordForm(dictDir, word); wordForm != null; wordForm = wordForm.getNext()) {
      final List<WordSense> entries = getWordSenses(wordForm);
      if (entries != null) {
        if (result == null) result = new ArrayList<WordSense>();
        result.addAll(entries);
      }
    }

    return result;
  }

  public List<WordSense> getWordSenses(WordForm wordForm) {
    List<WordSense> result = null;

    final DbValue dbValue = word2entries.get(wordForm.word);
    if (dbValue != null) {
      final Entries entries = (Entries)dbValue.getPublishable();
      if (result ==  null) result = new ArrayList<WordSense>();
      for (Entry entry : entries.getEntries()) {
        if (wordForm.canBe(entry.getPartOfSpeech())) {
          result.add(new WordSense(wordForm, entry));
        }
      }
    }

    return result;
  }

  public void close() {
    word2entries.close();
  }

  private final DbMap buildDbMap(final File dictDir) {
    return DbMap.createInstance(dictDir, SENSE_INDEX, new DbMap.Loader() {
        public boolean load(DbMap dbMap) {
          try {
            final BufferedReader reader = FileUtil.getReader(new File(dictDir, SENSE_INDEX));

            String line = null;
            while ((line = reader.readLine()) != null) {
              final Entry entry = new Entry(line);

              final String word = entry.getLemma();

              Entries entries = null;
              final DbValue dbValue = dbMap.get(word);
              if (dbValue == null) {
                entries = new Entries();
                dbMap.put(word, new DbValue(entries));
              }
              else {
                entries = (Entries)dbValue.getPublishable();
              }
              entries.add(entry);
            }

            reader.close();
          }
          catch (IOException e) {
            throw new IllegalStateException(e);
          }
          return true;
        }
      });
  }

  public static final class Entries implements Publishable {
    private List<Entry> entries;

    public Entries() {
      this.entries = new ArrayList<Entry>();
    }

    public void add(Entry entry) {
      entries.add(entry);
    }

    public List<Entry> getEntries() {
      return entries;
    }

    /**
     * Write thie message to the dataOutput stream such that this message
     * can be completely reconstructed through this.read(dataInput).
     *
     * @param dataOutput  the data output to write to.
     */
    public void write(DataOutput dataOutput) throws IOException {
      dataOutput.writeInt(entries.size());
      for (Entry entry : entries) {
        MessageHelper.writePublishable(dataOutput, entry);
      }
    }

    /**
     * Read this message's contents from the dataInput stream that was written by
     * this.write(dataOutput).
     * <p>
     * NOTE: this requires all implementing classes to have a default constructor
     *       with no args.
     *
     * @param dataInput  the data output to write to.
     */
    public void read(DataInput dataInput) throws IOException {
      final int numEntries = dataInput.readInt();
      for (int i = 0; i < numEntries; ++i) {
        add((Entry)MessageHelper.readPublishable(dataInput));
      }
    }
  }

  public static final class Entry implements Publishable {
    private String line;

    private String[] _pieces;
    private String[] _skPieces;
    private String[] _lsPieces;

    private String _lemma;
    private POS _partOfSpeech;
    private Integer _lexFileNum;
    private Integer _lexId;
    private String _headWord;
    private Integer _headId;
    private Long _synsetOffset;
    private Integer _senseNumber;
    private Integer _tagCount;

    public Entry() {
    }

    public Entry(String line) {
      this.line = line;
    }

    private final String[] getPieces() {
      if (_pieces == null) {
        _pieces = line.split(" ");
      }
      return _pieces;
    }

    private final String[] getSkPieces() {
      if (_skPieces == null) {
        final String[] pieces = getPieces();
        final String senseKey = pieces[0];
        _skPieces = senseKey.split("%");
      }
      return _skPieces;
    }

    private final String[] getLsPieces() {
      if (_lsPieces == null) {
        final String[] skPieces = getSkPieces();
        final String lexSense = skPieces[1];
        _lsPieces = lexSense.split(":");
      }
      return _lsPieces;
    }

    public final String getLemma() {
      if (_lemma == null) {
        final String[] skPieces = getSkPieces();
        _lemma = skPieces[0];
      }
      return _lemma;
    }

    public final POS getPartOfSpeech() {
      if (_partOfSpeech == null) {
        final String[] lsPieces = getLsPieces();
        _partOfSpeech = WordNetUtils.parsePOS(lsPieces[0]);
      }
      return _partOfSpeech;
    }

    public final int getLexFileNum() {
      if (_lexFileNum == null) {
        final String[] lsPieces = getLsPieces();
        _lexFileNum = WordNetUtils.parseInt(lsPieces[1]);
      }
      return _lexFileNum;
    }

    public final int getLexId() {
      if (_lexId == null) {
        final String[] lsPieces = getLsPieces();
        _lexId = WordNetUtils.parseInt(lsPieces[2]);
      }
      return _lexId;
    }

    public final String getHeadWord() {
      if (_headWord == null) {
        final String[] lsPieces = getLsPieces();
        _headWord = (lsPieces.length > 3) ? lsPieces[3] : null;
      }
      return _headWord;
    }

    public final int getHeadId() {
      if (_headId == null) {
        final String[] lsPieces = getLsPieces();
        _headId = (lsPieces.length > 4) ? WordNetUtils.parseInt(lsPieces[4]) : -1;
      }
      return _headId;
    }

    public final Long getSynsetOffset() {
      if (_synsetOffset == null) {
        final String[] pieces = getPieces();
        _synsetOffset = WordNetUtils.parseLong(pieces[1]);
      }
      return _synsetOffset;
    }

    public final int getSenseNumber() {
      if (_senseNumber == null) {
        final String[] pieces = getPieces();
        _senseNumber = WordNetUtils.parseInt(pieces[2]);
      }
      return _senseNumber;
    }

    public final int getTagCount() {
      if (_tagCount == null) {
        final String[] pieces = getPieces();
        _tagCount = WordNetUtils.parseInt(pieces[3]);
      }
      return _tagCount;
    }

    /**
     * Write thie message to the dataOutput stream such that this message
     * can be completely reconstructed through this.read(dataInput).
     *
     * @param dataOutput  the data output to write to.
     */
    public void write(DataOutput dataOutput) throws IOException {
      MessageHelper.writeString(dataOutput, line);
    }

    /**
     * Read this message's contents from the dataInput stream that was written by
     * this.write(dataOutput).
     * <p>
     * NOTE: this requires all implementing classes to have a default constructor
     *       with no args.
     *
     * @param dataInput  the data output to write to.
     */
    public void read(DataInput dataInput) throws IOException {
      this.line = MessageHelper.readString(dataInput);
    }
  }


  //java -Xmx640m org.sd.wn.SenseIndex /usr/local/share/download/wordnet/WordNet-3.0/dict "butterfly valve"
  public static void main(String[] args) throws IOException {
    //arg0: dictDir
    //args1+: words to lookup

    try {
      final String dictDir = args[0].length() > 1 ? args[0] : null;
      final int wordsIndex = 1;

      final SenseIndex senseIndex = SenseIndex.getInstance(dictDir);

      for (int i = wordsIndex; i < args.length; ++i) {
        final List<WordSense> senses = senseIndex.getWordSenses(args[i]);

        System.out.println(args[i] + " -->");
        if (senses == null) {
          System.out.print("<no senses>");
        }
        else {
          for (WordSense sense : senses) {
            System.out.println("\t" + sense);
          }
        }
        System.out.println();
      }
    }
    finally {
      WordNetUtils.closeAll();
    }
  }
}
