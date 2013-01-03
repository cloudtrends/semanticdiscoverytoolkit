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
import org.sd.util.Usage;
import org.sd.util.range.IntegerRange;
import org.sd.xml.DomElement;
import org.sd.xml.DomNode;
import org.w3c.dom.NodeList;

/**
 * A rule to test state attributes.
 * <p>
 * @author Spence Koehler
 */
@Usage(notes =
       "A org.sd.atn.BaseClassifierTest to evaluate state attributes.\n" +
       "\n" +
       " options:\n" +
       " <test reverse='true|false' stateType='push|pop|match|skip|other|any' repeat='<valid-repeat-range>'>\n" +
       "   <jclass>org.sd.atn.StateTest</jclass>\n" +
       "   <category ascend='true|false' cat='X' />\n" +
       " </test>"
  )
public class StateTest extends BaseClassifierTest {
  
  public enum StateType {PUSH, POP, MATCH, SKIP, OTHER, ANY};


  //NOTE: reverse is handled in BaseClassifierTest
  private StateType stateType;
  private IntegerRange repeatRange;
  private List<CategoryWrapper> categories;

  public StateTest(DomNode testNode, ResourceManager resourceManager) {
    super(testNode, resourceManager);

    this.stateType = StateType.valueOf(testNode.getAttributeValue("stateType", "any").toUpperCase());

    this.repeatRange = null;
    final String repeatRangeString = testNode.getAttributeValue("repeat", null);
    if (repeatRangeString != null) {
      this.repeatRange = new IntegerRange(repeatRangeString);
    }

    this.categories = null;
    final NodeList categoryNodes = testNode.selectNodes("category");
    if (categoryNodes != null && categoryNodes.getLength() > 0) {
      this.categories = new ArrayList<CategoryWrapper>();
      for (int nodeNum = 0; nodeNum < categoryNodes.getLength(); ++nodeNum) {
        final DomElement categoryElt = (DomElement)categoryNodes.item(nodeNum);
        final CategoryWrapper catWrapper = new CategoryWrapper(categoryElt);
        this.categories.add(catWrapper);
      }
    }

    // options:
    // - when reverse='true', fail on match (handled elsewhere)
    //
    // <test reverse='true|false' stateType='push|pop|match|skip|other|any' repeat='<valid-repeat-range>'>\n" +
    //   <jclass>org.sd.atn.StateTest</jclass>\n" +
    //   <category ascend='true|false' cat='X' />\n" +
    // </test>"
  }
			
  protected boolean doAccept(Token token, AtnState curState) {
    boolean result = meetsStateTypeConstraint(curState, stateType);

    if (result && repeatRange != null) {
      result = repeatRange.includes(curState.getRepeatNum());
    }

    if (result && categories != null) {
      result = false;

      for (CategoryWrapper catWrapper : categories) {
        final boolean hasCat = catWrapper.matches(curState);
        if (hasCat) {
          result = true;
          break;
        }
      }
    }

    return result;
  }

  public static final boolean meetsStateTypeConstraint(AtnState curState, StateType stateType) {
    boolean result = true;  // default to "any"

    switch (stateType) {
      case PUSH :
        result = curState.isRuleStart();
        break;
      case POP :
        result = curState.isPoppedState();
        break;
      case MATCH :
        result = curState.getMatched();
        break;
      case SKIP :
        result = curState.isSkipped();
        break;
      case OTHER :
        result = !curState.isRuleStart() && !curState.isPoppedState() && !curState.getMatched() && !curState.isSkipped();
        break;
    }

    return result;
  }


  private static final class CategoryWrapper {

    private String category;

    public CategoryWrapper(DomElement catElt) {
      this.category = catElt.hasAttributes() ? catElt.getAttributeValue("cat", null) : null;
      if (this.category == null) {
        this.category = catElt.getTextContent().trim();
      }
    }

    public boolean matches(AtnState curState) {
      boolean result = curState.getRuleStep().getLabel().equals(category);

      for (AtnState theState = curState.getPushState(); !result && theState != null; theState = theState.getPushState()) {
        final AtnRule rule = theState.getRule();
        final String ruleName = rule.getRuleName();
        final String ruleId = rule.getRuleId();

        result = ruleName.equals(category);
        if (!result && ruleId != null) {
          result = ruleId.equals(category);
        }
      }

      return result;
    }
  }
}
