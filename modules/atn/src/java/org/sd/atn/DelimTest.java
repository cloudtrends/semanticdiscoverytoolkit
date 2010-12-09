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
package org.sd.atn;


import java.util.ArrayList;
import java.util.List;
import org.sd.token.Token;
import org.sd.xml.DomElement;
import org.sd.xml.DomNode;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A rule test for pre- or post- delims.
 * <p>
 * @author Spence Koehler
 */
public class DelimTest implements AtnRuleStepTest {
  
  private boolean isPre;
  boolean isPre() {
    return isPre;
  }

  boolean isPost() {
    return !isPre;
  }

  private boolean allowAll;
  boolean allowAll() {
    return allowAll;
  }

  private boolean disallowAll;
  boolean disallowAll() {
    return disallowAll;
  }

  private List<DelimString> delimStrings;
  List<DelimString> getDelimStrings() {
    return delimStrings;
  }

  private List<DelimString> requiredDelimStrings;
  List<DelimString> getRequiredDelimStrings() {
    return requiredDelimStrings;
  }


  DelimTest(boolean isPre, DomNode delimNode) {
    this.isPre = isPre;
    this.allowAll = false;
    this.disallowAll = false;
    this.delimStrings = new ArrayList<DelimString>();
    this.requiredDelimStrings = null;

    // under delim node, setup allowed and disallowed delims
    //
    // <pre-or-post-delim>
    //   <disallowall />
    //   <allowall />
    //   <allow type='substr|exact'>delims-to-allow</allow>
    //   <disallow type='substr|exact'>delims-to-disallow</disallow>
    //   <require type='substr|exact'>delims-to-require</require>
    // </pre-and-or-post-delim>

    if (delimNode.hasChildNodes()) {
      final NodeList childNodes = delimNode.getChildNodes();
      for (int childIndex = 0; childIndex < childNodes.getLength(); ++childIndex) {
        final Node curNode = childNodes.item(childIndex);
        if (curNode.getNodeType() != DomNode.ELEMENT_NODE) continue;

        final DomElement childNode = (DomElement)curNode;
        final String childName = childNode.getLocalName();
        final boolean exact = "exact".equalsIgnoreCase(childNode.getAttributeValue("type", "substr"));
        
        if ("disallowall".equalsIgnoreCase(childName)) {
          disallowAll = true;
          allowAll = false;
          delimStrings.clear();
        }
        else if ("allowall".equalsIgnoreCase(childName)) {
          allowAll = true;
          disallowAll = false;
          delimStrings.clear();
        }
        else if ("allow".equalsIgnoreCase(childName)) {
          if (!disallowAll && !allowAll) disallowAll = true;
          delimStrings.add(new DelimString(true, exact, childNode.getTextContent()));
        }
        else if ("disallow".equalsIgnoreCase(childName)) {
          if (!disallowAll && !allowAll) allowAll = true;
          delimStrings.add(new DelimString(false, exact, childNode.getTextContent()));
        }
        else if ("require".equalsIgnoreCase(childName)) {
          if (requiredDelimStrings == null) requiredDelimStrings = new ArrayList<DelimString>();
          requiredDelimStrings.add(new DelimString(true, exact, childNode.getTextContent()));
        }
      }
    }
  }
			
  public boolean accept(Token token, AtnState curState) {
    final String delim = getDelim(token);

    if (!meetsRequiredConstraints(delim)) return false;
    else if (delimStrings.size() == 0) return true;

    // ignore (always accept) purely whitespace (unless there are requiredDelimStrings)
    if ("".equals(delim.trim())) return true;

    boolean result = allowAll;

    boolean foundMatch = false;
    for (DelimString delimString : delimStrings) {
      if (delimString.matches(delim)) {
        if ((allowAll && !delimString.isAllowed) || (disallowAll && delimString.isAllowed)) {
          foundMatch = true;
        }

        //if definitive, break
        if (delimString.exact) break;
      }
      else if (delimString.exact && delimString.isInexactMatch(delim)) {
        // exact delimString only matched inexactly
        foundMatch = false;
        break;
      }
    }

    if (foundMatch) {
      result = !result;
    }

    return result;
  }

  private boolean meetsRequiredConstraints(String delim) {
    boolean result = true;

    if (requiredDelimStrings != null) {
      for (DelimString delimString : requiredDelimStrings) {
        if (!delimString.matches(delim)) {
          result = false;
          break;
        }
      }
    }

    return result;
  }

  private String getDelim(Token token) {
    String delim = null;

    if (isPre) {
      delim = token.getPreDelim();
    }
    else {
      delim = token.getPostDelim();
    }

    return delim;
  }

  class DelimString {

    boolean isAllowed;
    boolean exact;
    String delim;

    DelimString(boolean isAllowed, boolean exact, String delim) {
      this.isAllowed = isAllowed;
      this.exact = exact;
      this.delim = delim;
    }

    boolean matches(String delim) {
      return exact ? this.delim.equals(delim) : isInexactMatch(delim);
    }

    boolean isInexactMatch(String delim) {
      return delim.indexOf(this.delim) != -1;
    }

    public String toString() {
      final StringBuilder result = new StringBuilder();

      result.append(isAllowed ? '+' : '-');
      result.append(exact ? '=' : '~');
      result.append(delim);

      return result.toString();
    }
  }
}
