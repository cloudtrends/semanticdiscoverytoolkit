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
 * A wrapper for a state selection test (wrapping a StepTestWrapper).
 * <p>
 * @author Spence Koehler
 */
public class StateTestWrapper {
  
  private StateTest.StateType stateType;
  private StepTestWrapper stepTestWrapper;
  private boolean scanType;  // scan -vs- final type of test
  private StateTestContainer.Directive ifTrue;
  private StateTestContainer.Directive ifFalse;

  public StateTestWrapper(DomElement testElement, ResourceManager resourceManager) {
    this.stateType = StateTest.StateType.valueOf(testElement.getAttributeValue("stateType", "any").toUpperCase());
    try {
      this.stepTestWrapper = new StepTestWrapper(testElement, resourceManager);
    }
    catch (IllegalArgumentException e) {
      // test is empty, so should always succeed
      this.stepTestWrapper = null;
    }
    this.scanType = testElement.getAttributeValue("type", "final").equalsIgnoreCase("scan");

    this.ifTrue = loadDirective(testElement, "ifTrue", scanType ? "continue" : "succeed");
    this.ifFalse = loadDirective(testElement, "ifFalse", "fail");
  }

  public boolean isEmpty() {
    return false;
  }

  public boolean isScanType() {
    return scanType;
  }

  public StateTestContainer.Directive verify(Token token, AtnState curState) {
    boolean verified = true;

    if (stepTestWrapper != null && StateTest.meetsStateTypeConstraint(curState, stateType)) {
      verified = stepTestWrapper.verify(token, curState).accept();
    }

    return verified ? ifTrue : ifFalse;
  }

  private final StateTestContainer.Directive loadDirective(DomElement testElement, String attribute, String defaultValue) {
    final String directiveString = testElement.getAttributeValue(attribute, defaultValue).toUpperCase();
    final StateTestContainer.Directive result = StateTestContainer.Directive.valueOf(directiveString);
    return result;
  }
}
