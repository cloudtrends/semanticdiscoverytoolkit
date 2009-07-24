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
import java.util.Iterator;
import java.util.List;

/**
 * Container for a concept's id and forms.
 * <p>
 * @author Spence Koehler
 */
public class ConceptId {

  private Integer id;
  private List<ConceptForm> conceptForms;

  public ConceptId(Integer id) {
    this.id = id;
    this.conceptForms = new ArrayList<ConceptForm>();
  }

  public Integer getId() {
    return id;
  }

  public void add(ConceptForm conceptForm) {
    conceptForms.add(conceptForm);
  }

  public Iterator<ConceptForm> getConceptForms() {
    return conceptForms.iterator();
  }
}
