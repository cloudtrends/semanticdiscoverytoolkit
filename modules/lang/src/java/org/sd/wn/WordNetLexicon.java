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


import org.sd.nlp.AbstractLexicon;
import org.sd.nlp.AbstractNormalizer;
import org.sd.nlp.Categories;
import org.sd.nlp.Category;
import org.sd.nlp.GeneralNormalizer;
import org.sd.nlp.StringWrapper;
import org.sd.wn.POS;

import java.io.File;
import java.io.IOException;

/**
 * Lexicon interface wrapper around word net lexicons.
 * <p>
 * @author Spence Koehler
 */
public class WordNetLexicon extends AbstractLexicon {
  
  private WordNetIndex nounIndex;
  private Category category;

  public WordNetLexicon(File wnDictDir, POS partOfSpeech, Category category, AbstractNormalizer normalizer) throws IOException {
    super(normalizer == null ? GeneralNormalizer.getCaseInsensitiveInstance() : normalizer);

    this.nounIndex = WordNetIndex.getInstance(wnDictDir, partOfSpeech);
    this.category = category;
  }

  /**
   * Define applicable categories in the subString.
   *
   * @param subString   The substring to define.
   * @param normalizer  The normalizer to use.
   */
  protected void define(StringWrapper.SubString subString, AbstractNormalizer normalizer) {
    final String string = subString.getNormalizedString(normalizer);
    
    final WordNetIndex.Entry entry = nounIndex.lookup(string);
    if (entry != null) {
      subString.addCategory(category);
      //todo: set attributes on subString from entry
      //todo: subString.setDefinitive if warranted
    }
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

  public String toString() {
    final StringBuilder result = new StringBuilder();

    result.append("WordNetLexicon[").append(category).append(']');

    return result.toString();
  }
}
