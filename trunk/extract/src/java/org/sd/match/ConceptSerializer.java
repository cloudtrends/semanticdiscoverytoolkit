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
 * Interface for serializing concept data.
 * <p>
 * @author Spence Koehler
 */
public interface ConceptSerializer {

  /**
   * Get the serialized data as a string.
   */
  public String asString();

  /**
   * Set the id for the match concept.
   */
  public void setConceptId(int conceptId);

  /**
   * Add the next concept form for the match concept.
   * 
   * @param formType the form type to add.
   */
  public void addConceptForm(Form.Type formType);

  /**
   * Add the next concept term's decomp type for the current concept form.
   * 
   * @param decompType the concept term's decomp type to add.
   */
  public void addConceptTerm(Decomp.Type decompType);

  /**
   * Add the next synonym type for the current concept term.
   * 
   * @param synonymType the synonym type to add.
   */
  public void addTermSynonym(Synonym.Type synonymType);

  /**
   * Add the next variant type for the current synonym.
   * 
   * @param variantType the variant type to add.
   */
  public void addVariantType(Variant.Type variantType);

  /**
   * Add the next typed word for the current variant.
   * 
   * @param typedWord the typedWord to add for the current variant.
   */
  public void addTypedWord(TypedWord typedWord);
  
}
