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


import java.util.HashMap;
import java.util.Set;
import java.util.Map;

/**
 * Simple implementation of the lexicon interface.
 * <p>
 * @author Spence Koehler
 */
public class LexiconImpl extends AbstractLexicon {
  
  private Map<String, Categories> string2def;

  public LexiconImpl() {
    this(null);
  }

  public LexiconImpl(AbstractNormalizer normalizer) {
    super(normalizer);
    this.string2def = new HashMap<String, Categories>();
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
    return false;  // todo: implement this
  }

  /**
   * Define applicable categories in the subString.
   *
   * @param subString   The substring to define.
   * @param normalizer  The normalizer to use.
   */
  protected void define(StringWrapper.SubString subString, AbstractNormalizer normalizer) {
    final String string = subString.getNormalizedString(normalizer);

    final Categories curDef = string2def.get(string);
    if (curDef != null) {
      subString.addCategories(curDef);
    }
  }

  public void addDefinition(String string, Category category) {
    doAddDefinition(normalize(string), category);
  }

  public void addDefinition(String string, Categories categories) {
    string = normalize(string);
    final Set<Category> defs = categories.getTypes();
    for (Category def : defs) {
      doAddDefinition(string, def);
    }
  }

  // assume string already normalized
  private void doAddDefinition(String string, Category category) {
    Categories curDef = string2def.get(string);
    if (curDef == null) {
      curDef = new Categories();
      string2def.put(string, curDef);
    }
    curDef.addType(category);
  }
}
