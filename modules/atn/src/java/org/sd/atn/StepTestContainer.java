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


import java.util.ArrayList;
import java.util.List;
import org.sd.token.Token;
import org.sd.xml.DomElement;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A container for rule step test wrappers.
 * <p>
 * @author Spence Koehler
 */
public class StepTestContainer {
  
  public enum ConditionType { ANY, ALL };


  private DomElement stepElement;
  private List<StepTestWrapper> testWrappers;
  private ConditionType conditionType;
  private boolean empty;

  public StepTestContainer() {
    this.testWrappers = null;
    this.conditionType = null;
    this.empty = true;
  }

  public StepTestContainer(DomElement stepElement, ResourceManager resourceManager) {
    this.stepElement = stepElement;
    this.testWrappers = null;

    final NodeList childNodes = stepElement.getChildNodes();
    final int numChildNodes = childNodes.getLength();
    for (int childIdx = 0; childIdx < numChildNodes; ++childIdx) {
      final Node childNode = childNodes.item(childIdx);
      if (childNode.getNodeType() != Node.ELEMENT_NODE) continue;
      final DomElement childElt = (DomElement)childNode;

      final StepTestWrapper testWrapper = new StepTestWrapper(childElt, resourceManager);
      if (!testWrapper.isEmpty()) {
        if (this.testWrappers == null) this.testWrappers = new ArrayList<StepTestWrapper>();
        this.testWrappers.add(testWrapper);
      }
    }

    this.empty = (testWrappers == null);
  }

  public DomElement getStepElement() {
    return stepElement;
  }

  public final synchronized boolean addTestWrapper(StepTestWrapper testWrapper) {
    boolean result = false;

    if (testWrapper != null && !testWrapper.isEmpty()) {
      if (this.testWrappers == null) this.testWrappers = new ArrayList<StepTestWrapper>();
      this.testWrappers.add(testWrapper);
      result = true;
      this.empty = false;
    }

    return result;
  }

  public boolean isCondition() {
    return conditionType != null;
  }

  public void setConditionType(ConditionType conditionType) {
    this.conditionType = conditionType;
  }

  public ConditionType getConditionType() {
    return conditionType;
  }

  /**
   * Determine whether this container is empty.
   */
  public boolean isEmpty() {
    return empty;
  }

  /**
   * Verify the post-, pre-Delim and test constraints.
   */
  public PassFail verify(Token token, AtnState curState) {
    if (empty) return PassFail.getInstance(!isCondition());

    // if this container holds a condition, then failure means condition not
    // met so "parent" container test "succeeds" without applying its test

    boolean result = !isCondition();

    if (isCondition()) {
      for (StepTestWrapper testWrapper : testWrappers) {
        final boolean condResult = testWrapper.meetsCondition(token, curState);
        if (condResult) {
          // condition's condition is met, so verify test

          // NOTE: NOT_APPLICABLE counted as "FAIL" for conditions
          result = testWrapper.verify(token, curState).conditionalAccept();

          if (result) {
            if (conditionType == ConditionType.ANY) {
              // condition's test passed, so condition(s) apply
              break;
            }
            // else continue checking for all tests to apply
          }
          else {
            // condition's test failed, so this condition doesn't apply
            if (conditionType == ConditionType.ALL) {
              // not all conditions can apply
              break;
            }
            // else keep looping to see if "any" condition test applies
          }
        }
        else {
          if (conditionType == ConditionType.ALL) break;
        }
      }
    }
    else {
      // this isn't a condition; check all tests
      for (StepTestWrapper testWrapper : testWrappers) {
        final boolean condResult = testWrapper.meetsCondition(token, curState);
        if (condResult) {
          result = testWrapper.verify(token, curState).accept();
          if (!result) {
            break;
          }
        }
      }
    }

    return PassFail.getInstance(result);
  }

  public List<StepTestWrapper> getTestWrappers() {
    return testWrappers;
  }
}
