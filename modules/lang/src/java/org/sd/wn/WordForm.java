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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Structure to hold a word form.
 * <p>
 * @author Spence Koehler
 */
public class WordForm {

  private static final RegularConjugations REGULAR_CONJUGATIONS = RegularConjugations.getInstance();


  public enum Form {BASE, REGULAR, IRREGULAR};

  public final String word;
  public final File dictDir;
  public final String input;  // original input
  public final Form form;     // form
  public final POS[] limits;  // pos limitations
  
  private IrregularInflections _ii;

  private List<TaggedWord> nextWords;
  private int nextWordsIndex;

  public WordForm(File dictDir, String input) {
    this.word = WordNetUtils.normalize(input);

    this.dictDir = dictDir;
    this.input = input;
    this.form = Form.BASE;
    this.limits = null;
    this.nextWords = null;
    this.nextWordsIndex = -1;
    this._ii = null;
  }

  private WordForm(File dictDir, String input, Form form, List<TaggedWord> nextWords, int nextWordsIndex) {
    final TaggedWord taggedWord = nextWords.get(nextWordsIndex);

    this.word = taggedWord.word;
    this.limits = WordNetUtils.trim(taggedWord.partsOfSpeech);

    this.dictDir = dictDir;
    this.input = input;
    this.form = form;
    this.nextWords = nextWords;
    this.nextWordsIndex = nextWordsIndex;
  }

  /**
   * Get the next word form.
   */
  public WordForm getNext() {
    WordForm result = null;

    if (hasNextWord()) {
      result = popNextWord(form);
    }
    else {
      if (form == Form.BASE) {
        // try as irregular inflection
        List<TaggedWord> bases = lookupIrregularBases(word);
        if (bases != null) {
          this.nextWords = bases;
          result = popNextWord(Form.IRREGULAR);
        }
        // else, try regular when didn't find any irregulars
        else {
          bases = computeRegularBases(word);
          if (bases != null) {
            this.nextWords = bases;
            result = popNextWord(Form.REGULAR);
          }
        }
      }
    }

    return result;
  }

  public boolean canBe(POS partOfSpeech) {
    boolean result = true;

    if (limits != null) {
      result = false;
      for (POS limit : limits) {
        if (limit == partOfSpeech) {
          result = true;
          break;
        }
      }
    }
    return result;
  }

  private final boolean hasNextWord() {
    return nextWords != null && nextWordsIndex + 1 < nextWords.size();
  }

  private final WordForm popNextWord(Form form) {
    return new WordForm(dictDir, input, form, nextWords, nextWordsIndex + 1);
  }

  private final IrregularInflections getIrregularInflections() {
    if (_ii == null) {
      try {
        _ii = IrregularInflections.getInstance(dictDir);
      }
      catch (IOException e) {
        throw new IllegalStateException("Can't load exc files!", e);
      }
    }
    return _ii;
  }

  private final List<TaggedWord> lookupIrregularBases(String word) {
    final IrregularInflections ii = getIrregularInflections();
    return ii.getBases(word);
  }

  private final List<TaggedWord> computeRegularBases(String word) {
    List<TaggedWord> result = null;

    final List<RegularConjugations.Word> words = REGULAR_CONJUGATIONS.getPotentialWords(word);
    if (words != null) {
      result = new ArrayList<TaggedWord>();

      final Map<String, Set<POS>> base2partsOfSpeech = new HashMap<String, Set<POS>>();
      for (RegularConjugations.Word cword : words) {
        Set<POS> poss = base2partsOfSpeech.get(cword.base);
        if (poss == null) {
          poss = new HashSet<POS>();
          base2partsOfSpeech.put(cword.base, poss);
        }
        poss.add(cword.basePos); // the base's part of speech, not the actual word's part of speech
      }

      for (Map.Entry<String, Set<POS>> entry : base2partsOfSpeech.entrySet()) {
        result.add(new TaggedWord(entry.getKey(), entry.getValue()));
      }
    }

    return result;
  }
}
