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


import org.sd.util.StringUtil;

/**
 * A lexicon that defines any undefined sequence of non-digit characters as a word.
 * <p>
 * @author Spence Koehler
 */
public class WordLexicon extends AbstractLexicon {
  
  private Category category;
  private boolean acceptDigits;

  public WordLexicon(CategoryFactory categoryFactory, boolean acceptDigits) {
    super();

    this.category = categoryFactory.getCategory("WORD");
    this.acceptDigits = acceptDigits;
  }

  /**
   * Determine whether the categories container already has category type(s)
   * that this lexicon would add.
   * <p>
   * NOTE: this is used to avoid calling "define" when categories already exist
   *       for the substring.
   *
   * @return true if this lexicon's category type(s) are already present.
   */
  protected boolean alreadyHasTypes(Categories categories) {
    return categories.hasType(category);
  }

  /**
   * Define applicable categories in the subString.
   *
   * @param subString   The substring to define.
   * @param normalizer  The normalizer to use.
   */
  protected void define(StringWrapper.SubString subString, AbstractNormalizer normalizer) {
    final String string = subString.getNormalizedString(normalizer);

    // First, make sure it is not already defined as something else (like prep or det.)
    // If it is only 1 word, has no digits, then call it a word.

    final Categories current = subString.getCategories();
    if (current != null && !current.isEmpty()) return;  // don't guess if already def'd as smthng else.

    final int len = string.length();
    boolean okay = len > 0 &&        // at least one char
      string.indexOf(' ') < 0 &&     // only one word
      (acceptDigits || !StringUtil.hasDigit(string));  // with no digits

    if (okay) {
      subString.addCategory(category);
    }
  }  
}
