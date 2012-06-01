/*
    Copyright 2011 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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
package org.sd.atn.interp;


import java.util.List;
import org.sd.atn.Parse;
import org.sd.atn.ParseInterpretationUtil;
import org.sd.token.CategorizedToken;
import org.sd.util.tree.Tree;
import org.sd.xml.DataProperties;
import org.sd.xml.DomElement;
import org.sd.xml.XmlLite;

/**
 * NodeExtractor for Text.
 * <p>
 * @author Spence Koehler
 */
public class TextNodeExtractor extends AbstractNodeExtractor {
  
  private boolean delims;

  TextNodeExtractor(FieldTemplate fieldTemplate, InnerResources resources, DomElement extractElement) {
    super(fieldTemplate, resources);

    this.delims = extractElement.getAttributeBoolean("delims", false);
  }

  public List<Tree<XmlLite.Data>> extract(Parse parse, Tree<String> parseNode, DataProperties overrides, InterpretationController controller) {
    return super.cleanup(XmlLite.createTextNode(getText(parseNode)), parse, parseNode, true);
  }

  public String extractString(Parse parse, Tree<String> parseNode) {
    return getText(parseNode);
  }

  private String getText(Tree<String> parseNode) {
    String result = null;

    final CategorizedToken cToken = ParseInterpretationUtil.getCategorizedToken(parseNode);
    if (cToken != null) {
      result = delims ? cToken.token.getTextWithDelims() : cToken.token.getText();
    }

    if (result == null) {
      result = parseNode.getLeafText();
    }

    return result;
  }
}
