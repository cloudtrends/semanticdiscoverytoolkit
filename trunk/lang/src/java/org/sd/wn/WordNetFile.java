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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sd.io.FileUtil;

/**
 * Utility to access data from a WordNet file.
 * <p>
 * @author Spence Koehler
 */
public class WordNetFile {
  
  private static final Map<String, Map<POS, WordNetFile>> dict2pos2file = new HashMap<String, Map<POS, WordNetFile>>();

  public static final WordNetFile getInstance(String dictPath, POS partOfSpeech) throws IOException {
    return getInstance(dictPath == null ? null : FileUtil.getFile(dictPath), partOfSpeech);
  }

  public static final WordNetFile getInstance(File dictDir, POS partOfSpeech) throws IOException {
    if (dictDir == null) dictDir = WordNetUtils.getDefaultDictDir();

    final String dictPath = dictDir.getAbsolutePath();

    WordNetFile result = null;

    Map<POS, WordNetFile> pos2file = dict2pos2file.get(dictPath);
    if (pos2file == null) {
      pos2file = new HashMap<POS, WordNetFile>();
      dict2pos2file.put(dictPath, pos2file);
    }
    result = pos2file.get(partOfSpeech);
    if (result == null) {
      result = new WordNetFile(dictDir, partOfSpeech);
      pos2file.put(partOfSpeech, result);
    }

    return result;
  }

  private File dictDir;
  private POS partOfSpeech;
  private RandomAccessFile dataFile;

  private WordNetIndex _index;

  private WordNetFile(File dictDir, POS partOfSpeech) throws IOException {
    this.dictDir = dictDir;
    this.partOfSpeech = partOfSpeech;

    this.dataFile = new RandomAccessFile(partOfSpeech.getDataFile(dictDir), "r");
    this._index = null;
  }

  public void close() throws IOException {
    this.dataFile.close();
  }

  public List<Entry> getEntries(String word) {
    List<Entry> result = null;

    for (WordForm wordForm = new WordForm(dictDir, word); wordForm != null; wordForm = wordForm.getNext()) {
      if (!wordForm.canBe(partOfSpeech)) continue;

      final WordNetIndex.Entry indexEntry = getIndex().lookup(wordForm.word);

      if (indexEntry != null) {
        if (result == null) result = new ArrayList<Entry>();

        final long[] senseOffsets = indexEntry.getSenseOffsets();
        for (int i = 0; i < senseOffsets.length; ++i) {
          result.add(getEntry(senseOffsets[i]));
        }
      }
    }

    return result;
  }

  public final Entry getEntry(long synsetOffset) {

//todo: use an LRU cache

    Entry result = null;
    String line = null;

    try {
      dataFile.seek(synsetOffset);
      line = dataFile.readLine();
    }
    catch (IOException e) {
    }

    if (line != null) {
      result = new Entry(line, partOfSpeech);
    }

    return result;
  }

  private final WordNetIndex getIndex() {
    if (_index == null) {
      this._index = WordNetIndex.getInstance(dictDir, partOfSpeech);
    }
    return _index;
  }

  public static final class Entry {
    public final long synsetOffset;   // use as check
    public final int lexFilenum;      // lexicographer file number
    public final POS partOfSpeech;    // synset type
    public final Word[] words;        // words in synset
    public final Pointer[] pointers;  // pointers to other synsets
    public final Frame[] frames;      // frames (non-null for verbs only)
    public final String gloss;        // gloss string

    public Entry(String line, POS partOfSpeech) {
      final String[] glossSplit = line.split(" \\| ");
      if (glossSplit.length == 2) {
        this.gloss = glossSplit[1];
      }
      else {
        this.gloss = null;
      }
      final String[] pieces = glossSplit[0].split(" +");

      final int wordCount = WordNetUtils.parseHex(pieces[3]);
      final int pCountInd = 3 + (2 * wordCount) + 1;
      final int pCount = WordNetUtils.parseInt(pieces[pCountInd]);
      final int fCountInd = pCountInd + (4 * pCount) + 1;
      final int fCount = fCountInd < pieces.length ? WordNetUtils.parseHex(pieces[fCountInd]) : 0;

      this.synsetOffset = WordNetUtils.parseLong(pieces[0]);
      this.lexFilenum = WordNetUtils.parseInt(pieces[1]);
      this.partOfSpeech = WordNetUtils.parsePOS(pieces[2]);
      this.words = new Word[wordCount];
      this.pointers = new Pointer[pCount];

      if (fCountInd > 0) {
        this.frames = new Frame[fCount];

        // fill frames
        for (int i = 0; i < fCount; ++i) {
          final int base = fCountInd + (3 * i);
          frames[i] = new Frame(pieces, base);
        }
      }
      else {
        this.frames = null;
      }

      // fill words
      for (int i = 0; i < wordCount; ++i) {
        final int base = 3 + (2 * i);
        words[i] = new Word(pieces[base + 1], WordNetUtils.parseHex(pieces[base + 2]));
      }

      // fill pointers
      for (int i = 0; i < pCount; ++i) {
        final int base = pCountInd + (4 * i);
        pointers[i] = new Pointer(pieces, base, partOfSpeech);

        if (pointers[i].pointerSymbol == null) {
          System.err.println("\tline=" + line + "\n\tbase=" + base + " partOfSpeech=" + partOfSpeech.name() + " i=" + i);
        }
      }
    }

    /**
     * Get the word given the 1-based offset, or the first word if the offset
     * is 0.
     */
    public final Word getWord(int offset) {
      return words[offset == 0 ? offset : offset - 1];
    }

    public final LexName getLexName() {
      return LexName.getLexName(lexFilenum);
    }

    public final String getLexFileName() {
      return getLexName().getName();
    }

    public int hashCode() {
      return (int)(synsetOffset^(synsetOffset>>>32)) * 17 + partOfSpeech.ordinal();
    }

    public boolean equals(Object o) {
      boolean result = (this == o);

      if (!result && o instanceof Entry) {
        final Entry other = (Entry)o;

        result =
          (this.partOfSpeech == other.partOfSpeech) &&
          (this.synsetOffset == other.synsetOffset);
      }

      return result;
    }

    public final String toString() {
      final StringBuilder result = new StringBuilder();

      result.
        append('(').
        append(LexName.getLexName(lexFilenum).fileName).
//        append(partOfSpeech.name().toLowerCase()).append('.').
//        append(LexName.getLexName(lexFilenum).getName()).
        append(')');

      for (int i = 0; i < words.length; ++i) {
        result.append(words[i]);
        if (i + 1 < words.length) result.append(';');
      }

      return result.toString();
    }
  }

  public static final class Word {
    public final String word;
    public final int lexId;

    private String _uniqueId;

    public Word(String word, int lexId) {
      this.word = word;
      this.lexId = lexId;
      this._uniqueId = null;
    }

    public String getUniqueId() {
      if (_uniqueId == null) {
        _uniqueId = word + Integer.toString(lexId);
      }
      return _uniqueId;
    }

    public LexName getLexName() {
      return LexName.getLexName(lexId);
    }

    public String toString() {
      return word;
    }
  }

  public static final class Pointer {
    public final PointerSymbol pointerSymbol;
    public final long synsetOffset;
    public final POS partOfSpeech;
    public final int sourceOffset;
    public final int targetOffset;

    public Pointer(String[] pieces, int base, POS partOfSpeech) {
      this.pointerSymbol = WordNetUtils.getPointerSymbol(partOfSpeech, pieces[base + 1]);
      this.synsetOffset = WordNetUtils.parseLong(pieces[base + 2]);
      this.partOfSpeech = WordNetUtils.parsePOS(pieces[base + 3]);

      final String stString = pieces[base + 4];

      this.sourceOffset = WordNetUtils.parseHex(stString.substring(0, 2));
      this.targetOffset = WordNetUtils.parseHex(stString.substring(2));
    }
  }

  public static final class Frame {
    public final int frameNum;
    public final int wordNum;

//todo: tie to sents.vrb file. see wndb man page "Verb Example Sentences" section.

    public Frame(String[] pieces, int base) {
      this.frameNum = WordNetUtils.parseInt(pieces[base + 2]);
      this.wordNum = WordNetUtils.parseHex(pieces[base + 3]);
    }
  }

//java -Xmx640m org.sd.wn.WordNetFile /usr/local/share/download/wordnet/WordNet-2.1/WordNet-2.1/dict n pump
//java -Xmx640m org.sd.wn.WordNetFile /usr/local/share/download/wordnet/WordNet-2.1/WordNet-2.1/dict v pump
//java -Xmx640m org.sd.wn.WordNetFile /usr/local/share/download/wordnet/WordNet-3.0/dict n "hydraulic pump"
  public static void main(String[] args) throws IOException {
    //arg0: dictDir
    //arg1: partOfSpeech (char)
    //args1+: words to lookup

    try {
      final String dictDir = args[0].length() > 1 ? args[0] : null;
      final String posChar = args[1];
      final int wordsIndex = 2;

      final WordNetFile wordNetFile = WordNetFile.getInstance(dictDir, WordNetUtils.parsePOS(posChar));

      for (int i = wordsIndex; i < args.length; ++i) {
        final List<Entry> entries = wordNetFile.getEntries(args[i]);

        System.out.print(args[i] + " --> ");
        if (entries == null) {
          System.out.print("<no entries>");
        }
        else {
          boolean didFirst = false;
          for (Entry entry : entries) {
            if (didFirst) System.out.print(", ");
            System.out.print(entry);
            didFirst = true;
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
