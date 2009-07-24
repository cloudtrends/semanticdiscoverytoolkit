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
import java.util.HashMap;
import java.util.Map;

/**
 * Class to hold a WordNet index in memory.
 * <p>
 * @author Spence Koehler
 */
public class WordNetIndex {

  private static final Map<String, Map<POS, WordNetIndex>> dict2pos2index = new HashMap<String, Map<POS, WordNetIndex>>();

  public static final WordNetIndex getInstance(String dictPath, POS partOfSpeech) {
    return getInstance(dictPath != null ? FileUtil.getFile(dictPath) : null, partOfSpeech);
  }

  public static final WordNetIndex getInstance(File dictDir, POS partOfSpeech) {
    if (dictDir == null) dictDir = WordNetUtils.getDefaultDictDir();

    final String dictPath = dictDir.getAbsolutePath();

    WordNetIndex result = null;

    Map<POS, WordNetIndex> pos2index = dict2pos2index.get(dictPath);
    if (pos2index == null) {
      pos2index = new HashMap<POS, WordNetIndex>();
      dict2pos2index.put(dictPath, pos2index);
    }
    result = pos2index.get(partOfSpeech);
    if (result == null) {
      result = new WordNetIndex(dictDir, partOfSpeech);
      pos2index.put(partOfSpeech, result);
    }

    return result;
  }

  public static final void closeAll() {
    for (Map<POS, WordNetIndex> pos2index : dict2pos2index.values()) {
      for (WordNetIndex index : pos2index.values()) {
        index.close();
      }
    }
  }


  private final File dictDir;
  private final POS partOfSpeech;
  private final DbMap word2entry;

  private WordNetIndex(File dictDir, POS partOfSpeech) {
    this.dictDir = dictDir;
    this.partOfSpeech = partOfSpeech;
    this.word2entry = buildDbMap(dictDir, partOfSpeech);
  }

  public Entry lookup(String normalizedWord) {
    //NOTE: it is assumed that callers use all WordForm instances to attempt to
    //      lookup a word. Therefore, computing bases from inflections is NOT
    //      done here.

    return getEntry(normalizedWord);
  }

  public void close() {
    word2entry.close();
  }

  private final Entry getEntry(String word) {
    Entry result = null;
    final DbValue dbValue = word2entry.get(word);

    if (dbValue != null) {
      result = (Entry)dbValue.getPublishable();
    }

    return result;
  }

  private final DbMap buildDbMap(final File dictDir, final POS partOfSpeech) {
    return DbMap.createInstance(dictDir, "index." + partOfSpeech.name, new DbMap.Loader() {
        public boolean load(DbMap dbMap) {
          try {
            final BufferedReader reader = FileUtil.getReader(partOfSpeech.getIndexFile(dictDir));

            String line = null;
            while ((line = reader.readLine()) != null) {
              if (line.length() == 0 || line.charAt(0) == ' ') continue;

              final Entry entry = new Entry(line, partOfSpeech);
              dbMap.put(entry.getLemma(), new DbValue(entry));
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

  /**
   * Container class for index entry data.
   */
  public static final class Entry implements Publishable {

    private String line;
    private POS partOfSpeech;

    private String _posLabel;
    private String[] _pieces;
    private String _lemma;
    private PointerSymbol[] _pointerSymbols;
    private long[] _senseOffsets;
    private Integer _tagsenseCount;

    public Entry() {
    }

    public Entry(String line, POS partOfSpeech) {
      this.line = line;
      this.partOfSpeech = partOfSpeech;
    }

    private final String[] getPieces() {
      if (_pieces == null) {
        _pieces = line.split(" ");
      }
      return _pieces;
    }

    public final POS getPartOfSpeech() {
      POS result = partOfSpeech;

      if (partOfSpeech == null) {
        if (_posLabel != null) {
          partOfSpeech = Enum.valueOf(POS.class, _posLabel);
          result = partOfSpeech;
        }
      }

      return result;
    }

    private final String getPosLabel() {
      if (_posLabel == null) {
        if (partOfSpeech != null) {
          _posLabel = partOfSpeech.name();
        }
      }
      return _posLabel;
    }

    public final String getLemma() {
      if (_lemma == null) {
        final String[] pieces = getPieces();
        _lemma = pieces[0];
      }
      return _lemma;
    }

    public final PointerSymbol[] getPointerSymbols() {
      if (_pointerSymbols == null) {
        final String[] pieces = getPieces();
        final int pointerCount = Integer.parseInt(pieces[3]);
        this._pointerSymbols = new PointerSymbol[pointerCount];
        for (int i = 0; i < pointerCount; ++i) {
          _pointerSymbols[i] = WordNetUtils.getPointerSymbol(partOfSpeech, pieces[4 + i]);
//        if (_pointerSymbols[i] == PointerSymbol.UNKNOWN) {
//          System.err.println("\tsymbol #" + i + ", POS=" + partOfSpeech + ", line=" + line);
//        }
        }
      }
      return _pointerSymbols;
    }

    public final long[] getSenseOffsets() {
      if (_senseOffsets == null) {
        final String[] pieces = getPieces();
        final int synsetCount = Integer.parseInt(pieces[2]);
        final int pointerCount = Integer.parseInt(pieces[3]);
        this._senseOffsets = new long[synsetCount];
        for (int i = 0; i < synsetCount; ++i) {
          _senseOffsets[i] = WordNetUtils.parseLong(pieces[3 + pointerCount + 3 + i]);
        }
      }
      return _senseOffsets;
    }

    public final int getTagsenseCount() {
      if (_tagsenseCount == null) {
        final String[] pieces = getPieces();
        final int pointerCount = Integer.parseInt(pieces[3]);
        _tagsenseCount = new Integer(pieces[3 + pointerCount + 2]);
      }
      return _tagsenseCount;
    }

    /**
     * Write thie message to the dataOutput stream such that this message
     * can be completely reconstructed through this.read(dataInput).
     *
     * @param dataOutput  the data output to write to.
     */
    public final void write(DataOutput dataOutput) throws IOException {
      MessageHelper.writeString(dataOutput, line);
      MessageHelper.writeString(dataOutput, getPosLabel());
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
    public final void read(DataInput dataInput) throws IOException {
      this.line = MessageHelper.readString(dataInput);
      this._posLabel = MessageHelper.readString(dataInput);
    }
  }
}
