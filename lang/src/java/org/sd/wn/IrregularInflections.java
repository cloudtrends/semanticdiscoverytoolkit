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


import org.sd.io.FileUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class to manage WordNet irregular inflection file data.
 * <p>
 * @author Spence Koehler
 */
public class IrregularInflections {
  
  private static final Map<String, IrregularInflections> dict2ii = new HashMap<String, IrregularInflections>();

  public static final IrregularInflections getInstance(String dictPath) throws IOException {
    return getInstance(FileUtil.getFile(dictPath));
  }

  public static final IrregularInflections getInstance(File dictDir) throws IOException {
    if (dictDir == null) dictDir = WordNetUtils.getDefaultDictDir();

    final String dictPath = dictDir.getAbsolutePath();

    IrregularInflections result = dict2ii.get(dictPath);

    if (result == null) {
      result = new IrregularInflections(dictDir);
      dict2ii.put(dictPath, result);
    }

    return result;
  }

  private File dictDir;
  private Map<String, List<String>> inflect2bases;
  private Map<String, Set<POS>> base2pos;

  private IrregularInflections(File dictDir) throws IOException {
    this.dictDir = dictDir;
    this.inflect2bases = new HashMap<String, List<String>>();
    this.base2pos = new HashMap<String, Set<POS>>();

    loadEntries();
  }

  public File getDictDir() {
    return dictDir;
  }

  /**
   * Given a normalized inflected string, lookup its irregular bases.
   */
  public List<TaggedWord> getBases(String inflectedString) {
    List<TaggedWord> result = null;
    final List<String> bases = inflect2bases.get(inflectedString);

    if (bases != null) {
      result = new ArrayList<TaggedWord>();
      for (String base : bases) {
        final Set<POS> poss = base2pos.get(base);
        result.add(new TaggedWord(base, poss));
      }
    }

    return result;
  }

  private final void loadEntries() throws IOException {
    loadEntries(POS.NOUN);
    loadEntries(POS.VERB);
    loadEntries(POS.ADJ);
    loadEntries(POS.ADV);
  }

  private final void loadEntries(POS partOfSpeech) throws IOException {
    final File excFile = partOfSpeech.getExceptionFile(dictDir);
    if (excFile == null || !excFile.exists()) return;

    final BufferedReader reader = FileUtil.getReader(excFile);

    String line = null;
    while ((line = reader.readLine()) != null) {
      final String[] pieces = line.split(" ");

      List<String> bases = inflect2bases.get(pieces[0]);
      if (bases == null) {
        bases = new ArrayList<String>();
        inflect2bases.put(pieces[0], bases);
      }
      for (int i = 0; i < pieces.length; ++i) {
        final String base = pieces[i];
        bases.add(base);

        Set<POS> poss = base2pos.get(base);
        if (poss == null) {
          poss = new HashSet<POS>();
          base2pos.put(base, poss);
        }
        poss.add(partOfSpeech);
      }
    }

    reader.close();
  }
}
