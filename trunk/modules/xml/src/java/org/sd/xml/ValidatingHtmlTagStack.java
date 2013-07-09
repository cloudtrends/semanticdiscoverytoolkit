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
package org.sd.xml;


/**
 * Implementation of the TagStack interface for html which attempts
 * to validate the tag stack is compliant with HTML tag rules
 * <p>
 * @author Abe Sanderson
 */
public class ValidatingHtmlTagStack extends HtmlTagStack 
{
  private boolean valid = true;
  //private HtmlTagStack validStack;
  public boolean isValid() { return valid; }
  //public HtmlTagStack getValidTagStack() { return validStack; }

  public ValidatingHtmlTagStack() {
    this(false);
  }
  public ValidatingHtmlTagStack(boolean useTagEquivalents) {
    super(useTagEquivalents);
  }

  /**
   * Push the given tag onto this stack.
   */
  public void pushTag(XmlLite.Tag tag) 
  {
    // check prior tag
    // get last tag on the stack
    //  check if nesting is allowed on this 
    XmlLite.Tag parent = getTag(depth() - 1);
    while(!HtmlHelper.isNestingAllowed(parent, tag))
    {
      super.popTag(parent.name);
      valid = false;
      parent = getTag(depth() - 1);
    }
    //validStack.pushTag(tag);
    super.pushTag(tag);

    //System.out.println("tag stack tags: "+getTags());
    //System.out.println("valid tag stack tags: "+validStack.getTags());
  }
}
