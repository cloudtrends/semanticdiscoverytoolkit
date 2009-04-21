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
package org.sd.match;


/**
 * Wrapper for a typed word with a number designating its original order.
 * <p>
 * If words have been sorted, this will allow the original ordering to
 * be recovered.
 *
 * @author Spence Koehler
 */
public class NumberedWord implements Comparable<NumberedWord> {

  public int number;
  public TypedWord word;

  private Form.Type formType;
  private Decomp.Type termType;
  private Synonym.Type synType;
  private Variant.Type varType;

  NumberedWord(int number, TypedWord word) {
    this.number = number;
    this.word = word;
  }

  NumberedWord(int number, TypedWord word, Form.Type formType, Decomp.Type termType, Synonym.Type synType, Variant.Type varType) {
    this.number = number;
    this.word = word;

    this.formType = formType;
    this.termType = termType;
    this.synType = synType;
    this.varType = varType;
  }

  /**
   * Default comparator places words in alphabetical order and preserves
   * numbered ordering with equal strings.
   */
  public int compareTo(NumberedWord other) {
    // sort by string
    int result = word.word.compareTo(other.word.word);
    if (result == 0) {
      // secondarily sort by number.
      result = this.number - other.number;
    }
    return result;
  }

  /**
   * If a new form starts at the pointer's current position, get its type;
   * otherwise, return null.
   */
  public Form.Type getFormType() {
    return formType;
  }

  /**
   * If a new term starts at the pointer's current position, get its type;
   * otherwise, return null.
   */
  public Decomp.Type getTermType() {
    return termType;
  }

  /**
   * If a new synonym starts at the pointer's current position, get its type;
   * otherwise, return null.
   */
  public Synonym.Type getSynonymType() {
    return synType;
  }

  /**
   * If a new variant starts at the pointer's current position, get its type;
   * otherwise, return null.
   */
  public Variant.Type getVariantType() {
    return varType;
  }

  /**
   * Get this numbered word's word type.
   */
  public Word.Type getWordType() {
    return word.wordType;
  }

  /**
   * Get this number word's word string.
   */
  public String getWord() {
    return word.word;
  }
}
