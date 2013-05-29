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


import org.sd.token.Token;
import org.sd.util.Usage;
import org.sd.xml.DomNode;

/**
 * A rule to test whether there is another token in the input.
 *
 * @author Spence Koehler
 */
@Usage(notes =
       "An org.sd.atn.BaseClassifierTest implementation that succeeds\n" +
       "for a token when it has any non-white delim before or after."
  )
public class HasAnyDelimTest extends BaseClassifierTest {
  
  private boolean checkPre;
  private boolean checkPost;
  private boolean requirePre;
  private boolean requirePost;

  public HasAnyDelimTest(DomNode testNode, ResourceManager resourceManager) {
    super(testNode, resourceManager);

    this.checkPre = testNode.getAttributeBoolean("checkPre", true);
    this.checkPost = testNode.getAttributeBoolean("checkPost", true);
    this.requirePre = testNode.getAttributeBoolean("requirePre", false);
    this.requirePost = testNode.getAttributeBoolean("requirePost", false);
  }
			
  protected boolean doAccept(Token token, AtnState curState) {
    boolean result = true;

    boolean hasPre = !(checkPre || requirePre) ? true : false;
    boolean hasPost = !(checkPost || requirePost) ? true : false;

    if (checkPre || requirePre) {
      hasPre = !"".equals(token.getPreDelim().trim());
    }

    if (checkPost || requirePost) {
      hasPost = !"".equals(token.getPostDelim().trim());
    }

    if (requirePre && !hasPre) {
      result = false;
    }
    else if (requirePost && !hasPost) {
      result = false;
    }
    else if (!hasPre && !hasPost) {
      result = false;
    }

    return result;
  }
}
