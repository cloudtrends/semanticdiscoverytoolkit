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


import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Container class for an alternate concept string and its form type.
 * <p>
 * @author Spence Koehler
 */
public class ConceptForm {

  public final Form.Type formType;
  public final String conceptString;
  public final Map<String, String> properties;

  public ConceptForm(Form.Type formType, String conceptString) {
    this(formType, conceptString, new LinkedHashMap<String, String>());
  }

  public ConceptForm(Form.Type formType, String conceptString, Map<String, String> properties) {
    this.formType = formType;
    this.conceptString = conceptString;
    this.properties = properties;
  }
}
