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
package org.sd.atn;


import org.sd.token.Token;
import org.sd.xml.DomElement;

/**
 * A wrapper for a rule step test with its condition.
 * <p>
 * @author Spence Koehler
 */
public class StepTestWrapper {
  
  private AtnRuleStepTest test;
  private StepTestContainer conditions;
  private String verboseTag;
  private boolean empty;
  private boolean reverse;

  public StepTestWrapper(DomElement testElement, ResourceManager resourceManager) {
    this.test = buildTest(testElement, resourceManager);
    this.conditions = loadCondition(testElement, resourceManager);
    this.verboseTag = buildVerboseTag(testElement);
    this.empty = (test == null && conditions == null);
  }

  public boolean isEmpty() {
    return empty;
  }

  public String getVerboseTag() {
    return verboseTag;
  }

  public boolean meetsCondition(Token token, AtnState curState) {
    boolean result = true;

    if (conditions != null) {
      if (verboseTag != null) {
        System.out.println("***StepTestWrapper.condition(" + curState.showStateContext() + "): " + verboseTag + " START");
      }

      result = conditions.verify(token, curState).conditionalAccept();

      // condition not met 
      if (verboseTag != null) {
        System.out.println("***StepTestWrapper.condition(" + curState.showStateContext() + "): " + verboseTag + " END (result=" +
                           (result ? "SUCCESS" : "FAILED") + ")");
      }
    }

    return result;
  }

  public PassFail verify(Token token, AtnState curState) {
    PassFail result = PassFail.PASS;

    if (test != null) {
      if (verboseTag != null) {
        System.out.println("***StepTestWrapper.verify(" + curState.showStateContext() + "): " + verboseTag + " START");
      }

      result = test.accept(token, curState);

      if (verboseTag != null) {
        System.out.println("***StepTestWrapper.verify(" + curState.showStateContext() + "): " + verboseTag + " END (result=" +
                           result + ")");
      }
    }

    return result;
  }

  private final AtnRuleStepTest buildTest(DomElement testElement, ResourceManager resourceManager) {
    AtnRuleStepTest result = buildBaseTest(testElement, resourceManager);

    if (result != null) {
      // reverse test if necessary
      this.reverse = testElement.getAttributeBoolean("reverse", false);
      if (reverse) {
        result = new ReversedAtnRuleStepTest(result);
      }
    }

    return result;
  }

  private final AtnRuleStepTest buildBaseTest(DomElement testElement, ResourceManager resourceManager) {
    AtnRuleStepTest result = null;

    final String eltName = testElement.getLocalName().toLowerCase();

    if ("predelim".equals(eltName)) {
      result = new DelimTest(true, testElement, resourceManager);
    }
    else if ("postdelim".equals(eltName)) {
      result = new DelimTest(false, testElement, resourceManager);
    }
    else if ("test".equals(eltName)) {  // test
      result = (AtnRuleStepTest)resourceManager.getResource(testElement);
    }

    return result;
  }

  private final StepTestContainer loadCondition(DomElement testElement, ResourceManager resourceManager) {
    StepTestContainer result = null;

    final DomElement conditionElement = (DomElement)testElement.selectSingleNode("condition");

    if (conditionElement != null) {
      result = new StepTestContainer(conditionElement, resourceManager);
      if (result.isEmpty()) result = null;
      else {
        final String condTypeString = conditionElement.getAttributeValue("type", "all").toLowerCase();
        final StepTestContainer.ConditionType condType =
          "any".equals(condTypeString) ?
          StepTestContainer.ConditionType.ANY :
          StepTestContainer.ConditionType.ALL;
        result.setConditionType(condType);
      }
    }

    return result;
  }

  private final String buildVerboseTag(DomElement testElement) {
    if (!testElement.getAttributeBoolean("verbose", false)) return null;

    final StringBuilder result = new StringBuilder();

    for (DomElement parentElt = testElement;
         parentElt != null && !"rules".equals(parentElt.getLocalName());
         parentElt = (DomElement)parentElt.getParentNode()) {
      if (result.length() > 0) result.insert(0, ".");
      result.insert(0, parentElt.getLocalName());
    }

    if (reverse) {
      result.append("(rev)");
    }

    return result.toString();
  }
}
