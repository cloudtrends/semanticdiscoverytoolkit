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

import java.util.HashMap;
import java.util.Map;

/**
 * A lexicon to classify a non-delimited sequence of chars that include at least one digit as a number.
 * <p>
 * @author Spence Koehler
 */
public class HasDigitLexicon extends AbstractLexicon {
  
  public static final String CATEGORY_NAME = "NUMBER";
  public static final boolean CAN_GUESS = false;

  private static final Map<CategoryFactory, HasDigitLexicon> INSTANCES = new HashMap<CategoryFactory, HasDigitLexicon>();

  public static final HasDigitLexicon getInstance(CategoryFactory categoryFactory) {
    HasDigitLexicon result = INSTANCES.get(categoryFactory);
    if (result == null) {
      result = new HasDigitLexicon(categoryFactory.getCategory(CATEGORY_NAME));
      INSTANCES.put(categoryFactory, result);
    }
    return result;
  }

  private Category category;

  private HasDigitLexicon(Category category) {
    super();
    this.category = category;
  }

  public Category getCategory() {
    return category;
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

    // has a digit or is a single letter

    if ((StringUtil.hasDigit(string) && string.indexOf(' ') < 0) ||
        (string.length() == 1 && Character.isLetter(string.codePointAt(0)))) {
      subString.addCategory(category);
    }
  }  
}
