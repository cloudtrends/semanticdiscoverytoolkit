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

import org.sd.util.tree.Tree;
import org.sd.xml.XmlLite;

/**
 * Lexicon to classify "AM" and "PM" for times.
 * <p>
 * @author Spence Koehler
 */
public class AmPmLexicon extends AbstractLexicon {

  private Category category;

  public AmPmLexicon(Category category) {
    super();
    this.category = category;
    setMaxNumWords(2);
  }

  public AmPmLexicon(Tree<XmlLite.Data> lexiconNode, CategoryFactory categoryFactory, AbstractNormalizer normalizer) {
    super(lexiconNode, categoryFactory, normalizer);

    this.category = getNamedCategory(getAttribute("category"));
    setMaxNumWords(2);
  }

  /**
   * @return this lexicon's category.
   */
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
    final StringWrapper sw = subString.stringWrapper;

    boolean sawFirst = false;
    boolean sawSecond = false;
    for (int index = subString.startPos; index < subString.endPos; ++index) {
      if (!sw.getBreak(index).skip()) {
        final int cp = sw.getCodePoint(index);
        if (sawSecond) return;
        if (sawFirst) {
          if (cp == 'm' || cp == 'M' ||
              cp == 21320) {  // asian's AM/PM marker
            sawSecond = true;
          }
          else return;
        }
        else {
          if (cp == 'A' || cp == 'P' || cp == 'a' || cp == 'p' ||
              cp == 19978 || cp == 19979) {  // asian AM's 'A' and 'P'
            sawFirst = true;
          }
          else return;
        }
      }
    }

    if (sawSecond) {
      subString.addCategory(category);
      subString.setDefinitive(true);   // no other lookups are necessary.
    }
  }
}
