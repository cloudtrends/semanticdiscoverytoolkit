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


import java.util.ArrayList;
import java.util.List;

/**
 * Basic implementation of concept serialization.
 * <p>
 * @author Spence Koehler
 */
public class BasicConceptSerializer extends AbstractConceptSerializer {

  private StringBuilder builder;
  private List<TypedWord> words;

  public BasicConceptSerializer() {
    this.builder = new StringBuilder();
    this.words = new ArrayList<TypedWord>();
  }

  /**
   * Get the serialized data as a string.
   */
  public String asString() {
    return getBasicConcept();
  }

  public String getBasicConcept() {
    flushWords();
    return builder.toString();
  }

  /**
   * Set the id for the match concept.
   */
  public void setConceptId(int conceptId) {
    builder.append(conceptId);
  }

  /**
   * @return true if there were words to flush.
   */
  protected boolean flushWords() {
    if (words.size() == 0) return false;

    for (TypedWord typedWord : words) {
      builder.append(" W ").append(typedWord.wordType.name()).append(' ').
        append(typedWord.word);
    }

    // clear words for next variant
    words.clear();

    return true;
  }


  protected void startNextForm(boolean flushedWords, Form.Type formType) {
    builder.append(" F ").append(formType.name());
  }

  protected void startNextTerm(boolean flushedWords, Decomp.Type decompType) {
    builder.append(" D ").append(decompType.name());
  }

  protected void startNextSynonym(boolean flushedWords, Synonym.Type synonymType) {
    builder.append(" S ").append(synonymType.name());
  }

  protected void startNextVariant(boolean flushedWords, Variant.Type variantType) {
    builder.append(" V ").append(variantType.name());
  }

  public void addTypedWord(TypedWord typedWord) {
    words.add(typedWord);
  }
}
