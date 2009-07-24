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
 * Container interface for various types of extraction data.
 * <p>
 * @author Spence Koehler
 */
public interface ExtractionData {

  public String getExtractedString();

  public ExtractionParseData asParseData();
  public ExtractionMatcherData asMatcherData();
  public ExtractionStringData asStringData();
  public ExtractionStringsData asStringsData();
  public ExtractionXmlData asXmlData();

  public String getString();
  public List<Parser.Parse> getParses();
  public Matcher getMatcher();
  public List<String> getStrings();
  public Tree<XmlLite.Data> getXmlTree();

}
