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
 * Strategy interface for building concept models.
 * <p>
 * Implementations keep track of state and respond appropriately to the
 * requests for information, returning null when a part is finished.
 *
 * @author Spence Koehler
 */
public interface ConceptModelBuilderStrategy {

  /**
   * Get the conceptId for the next match concept.
   * <p>
   * Note that state will be reset for all lower types.
   * 
   * @return the conceptId or null if there are no more match concepts.
   */
  public Integer nextMatchConcept();

  /**
   * Get the current match concept's info object.
   * <p>
   * NOTE: the current match concept is that whose conceptId was last
   *       returned from nextMatchConcept.
   */
  public Object getCurrentInfoObject();

  /**
   * Get the next concept form for the current match concept.
   * <p>
   * Note that state will be reset for all lower types.
   * 
   * @return the form type or null if there are no more concept forms
   *         for the current match concept.
   */
  public Form.Type nextConceptForm();

  /**
   * Get the next concept term's decomp type for the current concept form.
   * <p>
   * Note that state will be reset for all lower types.
   * 
   * @return the concept term's decomp type or null if there are no more
   *         concept terms for the current concept form.
   */
  public Decomp.Type nextConceptTerm();

  /**
   * Get the next synonym type for the current concept term.
   * <p>
   * Note that state will be reset for all lower types.
   * 
   * @return the synonym type or null if there are no more synonyms for the
   *         current concept term.
   */
  public Synonym.Type nextTermSynonym();

  /**
   * Get the next variant type for the current synonym.
   * <p>
   * Note that state will be reset for all lower types.
   * 
   * @return the variant type or null if there are no more variants for the
   *         current synonym
   */
  public Variant.Type nextVariantType();

  /**
   * Get the next typed word for the current variant.
   * 
   * @return the typedWord or null if there are no more words for the current
   *         variant.
   */
  public TypedWord nextTypedWord();

}
