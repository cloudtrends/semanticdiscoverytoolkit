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
package org.sd.nlp;


/**
 * A lexicon that is a pipeline of lexicons that execute serially.
 * <p>
 * @author Spence Koehler
 */
public class LexiconPipeline implements Lexicon {

  /** The lexicons to execute in order. */
  private Lexicon[] lexicons;
  
  private int maxNumWords;

  /** Construct with the lexicons to execute in order. */
  public LexiconPipeline(Lexicon[] lexicons) {
    this.lexicons = lexicons;

    maxNumWords = 0;
    if (lexicons != null) {
      for (int i = 0; i < lexicons.length; ++i) {
        final int curMaxNumWords = lexicons[i].maxNumWords();
        if (curMaxNumWords == 0) {
          maxNumWords = 0;
          break;
        }
        if (curMaxNumWords > maxNumWords) maxNumWords = curMaxNumWords;
      }
    }
  }

  /**
   * Get the normalizer that applies to this lexicon.
   *
   * @return this lexicon's normalizer. null when no normalization occurs.
   */
  public Normalizer getNormalizer() {
    return null;
  }

  /**
   * Get the maximum number of words in terms defined by this lexicon.
   *
   * @return the maximum number of words or 0 for unlimited.
   */
  public int maxNumWords() {
    return maxNumWords;
  }

  /**
   * Look up the categories for a subString, adding definition that apply.
   *
   * @param subString  The subString to lookup categories for.
   */
  public void lookup(StringWrapper.SubString subString) {
    if (lexicons != null) {
      for (Lexicon lexicon : lexicons) {
        final int numCategoriesBeforeLookup = subString.getNumCategories();
        lexicon.lookup(subString);
        final int numCategoriesAfterLookup = subString.getNumCategories();

        if (false/*verbose*/ && numCategoriesAfterLookup > numCategoriesBeforeLookup) {
          System.out.println(subString + " -" + lexicon + "-> " + subString.getCategories());
        }

        if (subString.hasDefinitiveDefinition()) break;  // can stop trying to define.
      }
    }
  }
}
