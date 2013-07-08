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
package org.sd.text;


import org.sd.nlp.AbstractLexicon;
import org.sd.nlp.AbstractNormalizer;
import org.sd.nlp.Categories;
import org.sd.nlp.Category;
import org.sd.nlp.StringWrapper;

/**
 * A lexicon to classify complete words in a trie.
 * <p>
 * @author Spence Koehler
 */
public class TrieLexicon extends AbstractLexicon {

  private Category category;
  private Trie trie;

  public TrieLexicon(AbstractNormalizer normalizer, Category category, Trie trie) {
    super(normalizer);
    this.category = category;
    this.trie = trie;
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

    if (trie.contains(string)) {
      subString.addCategory(category);
    }
  }  

}
