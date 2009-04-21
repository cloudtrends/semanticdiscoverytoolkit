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


import java.util.Set;

/**
 * A lexical entry holds a lexical tokenizer pointer that includes a string
 * with its categories (definition), if any.
 * <p>
 * @author Spence Koehler
 */
public class LexicalEntry {

  private TokenPointer pointer;
  private Categories definition;
  private Categories newDefinition;
  private Categories notDefinition;
  private String[] keys;

  /**
   * Construct a lexical entry for the given tokenizer pointer.
   */
  public LexicalEntry(TokenPointer pointer) {
    this.pointer = pointer;
    this.definition = null;
    this.newDefinition = null;
    this.notDefinition = null;
    this.keys = null;
    
    final Categories categories = pointer.getCategories();
    if (categories != null && !categories.isEmpty()) {
      this.definition = new Categories(categories);

      final Set<Category> types = definition.getTypes();
      this.keys = new String[types.size()];
      int index = 0;
      for (Category type : types) {
        keys[index++] = type.name().toLowerCase();
      }
    }
  }

  /**
   * Get this entry's pointer into the normalized input.
   */
  public TokenPointer getPointer() {
    return pointer;
  }

  /**
   * Get the (start word) position of this entry's token within its string.
   */
  public int getPosition() {
    return pointer.getPosition();
  }

  /**
   * Get this lexical entry's string.
   */
  public String getString() {
    return pointer.getString();
  }

  public String[] getKeys() {
    return keys;
  }

  /**
   * Get this lexical entry's definition.
   * <p>
   * NOTE: This is a combination of original and added definitions.
   *
   * @return this lexical entry's definition or null if undefined.
   */
  public Categories getDefinition() {
    return definition;
  }

  /**
   * Get this lexical entry's original definition.
   *
   * @return this lexical entry's original definition; null if originally undefined.
   */
  public Categories getOriginalDefinition() {
    return pointer.getCategories();
  }

  /**
   * Get this lexical entry's added definition.
   *
   * @return this lexical entry's added definition; null if nothing has been added.
   */
  public Categories getAddedDefinition() {
    return newDefinition;
  }

  /**
   * Get the categories that this entry is presumed NOT to have.
   */
  public Categories getNotDefinition() {
    return notDefinition;
  }

  /**
   * Add the given category to this lexical entry's definition.
   * <p>
   * If the given category already exists, the category will not be consered an addition.
   *
   * @return true if this lexical entry did not already have the category in its definition.
   */
  public boolean addDefinition(Category category) {
    if (definition != null && definition.hasType(category)) return false;

    if (newDefinition == null) newDefinition = new Categories();
    newDefinition.addType(category);

    if (definition == null) definition = new Categories();
    definition.addType(category);

    return true;
  }

  public void addNotDefinition(Category category) {
    if (notDefinition == null) notDefinition = new Categories();
    notDefinition.addType(category);
  }

  public LexicalEntry revise() {
    LexicalEntry result = null;

    final TokenPointer nextPointer = pointer.revise();
    if (nextPointer != null) {
      result = new LexicalEntry(nextPointer);
    }

    return result;
  }

  public LexicalEntry next(boolean allowSkip) {
    LexicalEntry result = null;

    final TokenPointer nextPointer = pointer.next(allowSkip);
    if (nextPointer != null) {
      result = new LexicalEntry(nextPointer);
    }

    return result;
  }

  public boolean isGuessable() {
    return pointer.isGuessable();
  }

  public String toString() {
    return "'" + getString() + "'";
/*
    final StringBuilder result = new StringBuilder();

    result.append('<').append(getString()).append(',').
      append(definition).append('>');

    return result.toString();
*/
  }
}
