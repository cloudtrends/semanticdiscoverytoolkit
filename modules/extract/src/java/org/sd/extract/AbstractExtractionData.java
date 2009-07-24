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
package org.sd.extract;


import org.sd.nlp.Parser;
import org.sd.util.tree.Tree;
import org.sd.xml.XmlLite;

import java.util.List;
import java.util.regex.Matcher;

/**
 * Abstract extraction data implementation.
 * <p>
 * @author Spence Koehler
 */
public abstract class AbstractExtractionData implements ExtractionData {
  
  public ExtractionParseData asParseData() {
    return null;
  }

  public ExtractionMatcherData asMatcherData() {
    return null;
  }

  public ExtractionStringData asStringData() {
    return null;
  }

  public ExtractionStringsData asStringsData() {
    return null;
  }

  public ExtractionXmlData asXmlData() {
    return null;
  }

  public String getString() {
    return null;
  }

  public List<Parser.Parse> getParses() {
    return null;
  }

  public Matcher getMatcher() {
    return null;
  }

  public List<String> getStrings() {
    return null;
  }

  public Tree<XmlLite.Data> getXmlTree() {
    return null;
  }
}
