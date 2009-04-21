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
import java.util.HashMap;
import java.util.Map;

/**
 * Structure to hold a word sense.
 * <p>
 * @author Spence Koehler
 */
public final class WordSense {
  
  private File dictDir;
  private POS partOfSpeech;
  private long synsetOffset;

  private WordForm wordForm;
  private SenseIndex.Entry indexEntry;

  private WordNetFile.Entry fileEntry;
  private Map<POS, Map<Long, WordSense>> pos2offset2sense;

  WordSense(WordForm wordForm, SenseIndex.Entry indexEntry) {
    this.dictDir = wordForm.dictDir;
    this.partOfSpeech = indexEntry.getPartOfSpeech();
    this.synsetOffset = indexEntry.getSynsetOffset();

    this.wordForm = wordForm;
    this.indexEntry = indexEntry;
    this.fileEntry = null;
    this.pos2offset2sense = new HashMap<POS, Map<Long, WordSense>>();

    cacheThis();
  }

  private WordSense(File dictDir, WordNetFile.Entry fileEntry, Map<POS, Map<Long, WordSense>> pos2offset2sense) {
    this.dictDir = dictDir;
    this.partOfSpeech = fileEntry.partOfSpeech;
    this.synsetOffset = fileEntry.synsetOffset;

    this.wordForm = null;
    this.indexEntry = null;
    this.fileEntry = fileEntry;
    this.pos2offset2sense = pos2offset2sense;

    cacheThis();
  }

  public File getDictDir() {
    return dictDir;
  }

  public POS getPartOfSpeech() {
    return partOfSpeech;
  }

  public long getSynsetOffset() {
    return synsetOffset;
  }

  public WordForm getWordForm() {
    return wordForm;
  }

  public SenseIndex.Entry getIndexEntry() {
    return indexEntry;
  }

  public WordNetFile.Entry getFileEntry() {
    if (fileEntry == null) {
      final WordNetFile wnFile = getWordNetFile(dictDir, partOfSpeech);
      this.fileEntry = wnFile.getEntry(indexEntry.getSynsetOffset());
    }
    return fileEntry;
  }

  public WordSense getInstance(POS partOfSpeech, long synsetOffset) {
    WordSense result = null;

    final Map<Long, WordSense> offset2sense = pos2offset2sense.get(partOfSpeech);
    if (offset2sense != null) {
      result = offset2sense.get(synsetOffset);
    }

    if (result == null) {
      // Create an instance from the available information.
      final WordNetFile wnFile = getWordNetFile(dictDir, partOfSpeech);
      final WordNetFile.Entry newFileEntry = wnFile.getEntry(synsetOffset);
      result = new WordSense(dictDir, newFileEntry, pos2offset2sense);
    }

    return result;
  }

  private final void cacheThis() {
    Map<Long, WordSense> offset2sense = pos2offset2sense.get(partOfSpeech);
    if (offset2sense == null) {
      offset2sense = new HashMap<Long, WordSense>();
      pos2offset2sense.put(partOfSpeech, offset2sense);
    }
    offset2sense.put(synsetOffset, this);
  }

  private final WordNetFile getWordNetFile(File dictDir, POS partOfSpeech) {
    WordNetFile result = null;

    try {
      result = WordNetFile.getInstance(dictDir, partOfSpeech);
    }
    catch (IOException e) {
      throw new IllegalStateException(e);
    }

    return result;
  }

  public int hashCode() {
    return (int)(synsetOffset^(synsetOffset>>>32)) * 17 + partOfSpeech.ordinal();
  }

  public boolean equals(Object o) {
    boolean result = (this == o);

    if (!result && o instanceof WordSense) {
      final WordSense other = (WordSense)o;

      result =
        (this.partOfSpeech == other.partOfSpeech) &&
        (this.synsetOffset == other.synsetOffset);
    }

    return result;
  }

  public String toString() {
    final StringBuilder result = new StringBuilder();

    final WordNetFile.Entry entry = getFileEntry();
    result.append(entry).append(" -- ").append(entry.gloss);

    return result.toString();
  }
}
