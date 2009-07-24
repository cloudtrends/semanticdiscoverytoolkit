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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sd.io.FileUtil;

/**
 * Utility methods.
 * <p>
 * @author Spence Koehler
 */
public class WordNetUtils {
  
  public static final String DEFAULT_DICT_PATH_ENV_NAME = "SDWN";
  public static final String DICT_PATH = "resources/data/WordNet";

  public static final File getDefaultDictDir() {

    String dictPath = System.getenv(DEFAULT_DICT_PATH_ENV_NAME);

    if (dictPath == null || "".equals(dictPath)) {
      final String rootDir = getRootDir();
      if (rootDir != null) {
        dictPath = rootDir + DICT_PATH;
      }
    }

    return dictPath == null ? null : FileUtil.getFile(dictPath);
  }

  private static final String getRootDir() {
    final String path = FileUtil.getFilename(WordNetUtils.class, ".");
    final int pos = path.indexOf("/lang/");
    return (pos >= 0) ? path.substring(0, pos + 6) : null;
  }

  /**
   * Close all backing databases, etc.
   */
  public static final void closeAll() {
    WordNetIndex.closeAll();
    SenseIndex.closeAll();
  }

  /**
   * Normalize the input according to the wordnet rules.
   * <p>
   * This lowercases the word and replaces all spaces with an underscore.
   */
  public static final String normalize(String word) {
    return word.replaceAll(" ", "_").toLowerCase();
  }

  public static final long parseLong(String paddedString) {
    final String string = removePadding(paddedString);
    return Long.parseLong(string);
  }

  public static final int parseInt(String paddedString) {
    final String string = removePadding(paddedString);
    return Integer.parseInt(string);
  }

  public static final int parseHex(String hexString) {
    return Integer.parseInt(hexString, 16);
  }

  public static final POS parsePOS(String posChar) {
    return POS.ABBREV_TO_POS.get(posChar);
  }

  public static final Set<String> expand(String word, IrregularInflections ii, boolean needsNormalization, POS[] posHints) {
    final Set<String> result = new LinkedHashSet<String>();

    if (needsNormalization) {
      // replace ' ' with '_'
      word = normalize(word);
    }

    // add the word itself.
    result.add(word);

    // add irregular inflection bases.
    final List<TaggedWord> irregularBases = (ii != null) ? ii.getBases(word) : null;
    if (irregularBases != null) {
      for (TaggedWord base : irregularBases) {
        result.add(base.word);
      }
    }
    else {
      // add regular inflection base.
      final List<RegularConjugations.Word> words = RegularConjugations.getInstance().getPotentialWords(word);
      if (words != null) {
        for (RegularConjugations.Word cword : words) {
          result.add(cword.base);
        }
      }
    }

    return result;
  }

  private static final Map<POS, Set<String>> POS_TO_UNKNOWNS = new HashMap<POS, Set<String>>();

  public static final PointerSymbol getPointerSymbol(POS partOfSpeech, String symbol) {
    PointerSymbol result = PointerSymbol.getPointerSymbol(partOfSpeech, symbol);
    if (result == null) {
      Set<String> unknowns = POS_TO_UNKNOWNS.get(partOfSpeech);
      if (unknowns == null) {
        unknowns = new HashSet<String>();
        POS_TO_UNKNOWNS.put(partOfSpeech, unknowns);
      }
      if (!unknowns.contains(symbol)) {
        unknowns.add(symbol);
        System.err.println("***WARNING: Can't find PointerSymbol for " + partOfSpeech.name() + " symbol='" + symbol + "'!");
      }
      result = PointerSymbol.UNKNOWN;
    }
    return result;
  }

  /**
   * Given an array of parts of speech, trim it down to contain only non-null entries.
   */
  public static final POS[] trim(POS[] partsOfSpeech) {
    if (partsOfSpeech == null) return null;

    POS[] result = null;

    int num = partsOfSpeech.length;
    while (num > 0 && partsOfSpeech[num - 1] == null) --num;

    if (num > 0) {
      result = new POS[num];
      for (int i = 0; i < num; ++i) result[i] = partsOfSpeech[i];
    }
    
    return result;
  }

  private static final String removePadding(String paddedString) {
    final int len = paddedString.length() - 1;
    int startIndex = 0;

    while (startIndex < len && paddedString.charAt(startIndex) == '0') {
      ++startIndex;
    }

    return (startIndex == 0) ? paddedString : paddedString.substring(startIndex);
  }
}
