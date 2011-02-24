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
import java.util.LinkedList;
import java.util.List;
import org.sd.token.Token;
import org.sd.util.tree.Tree;
import org.sd.xml.DomElement;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Class to encapsulate a grammar rule.
 * <p>
 * @author Spence Koehler
 */
public class AtnRule {
  
  private AtnGrammar grammar;
  public AtnGrammar getGrammar() {
    return grammar;
  }

  private String ruleName;
  public String getRuleName() {
    return ruleName;
  }

  private String ruleId;
  public String getRuleId() {
    return ruleId;
  }

  private LinkedList<AtnRuleStep> steps;
  LinkedList<AtnRuleStep> getSteps() {
    return steps;
  }

  private boolean isStart;
  boolean isStart() {
    return isStart;
  }

  private String tokenFilterId;
  String getTokenFilterId() {
    return tokenFilterId;
  }

  private List<AtnRuleStep> popSteps;
  List<AtnRuleStep> getPopSteps() {
    return popSteps;
  }
  private void addPopStep(AtnRuleStep popStep) {
    if (popSteps == null) popSteps = new ArrayList<AtnRuleStep>();
    popSteps.add(popStep);
  }


  AtnRule(AtnGrammar grammar, DomElement ruleElement, ResourceManager resourceManager) {
    this.grammar = grammar;
    this.ruleName = ruleElement.getLocalName();
    this.ruleId = ruleElement.getAttributeValue("id", null);
    this.steps = new LinkedList<AtnRuleStep>();
    this.isStart = ruleElement.getAttributeBoolean("start", false);
    this.tokenFilterId = ruleElement.getAttributeValue("tokenFilter", null);

    //
    // RuleElement is of the form:
    //
    // <ruleName start='' tokenFilter='tokenFilterId' id='ruleId'>
    //   <ruleStep require='' unless='' optional='' repeats='' terminal='' skip=''>
    //     <postdelim><disallowall|allowall|disallow|allow /></postdelim>
    //     <predelim><disallowall|allowall|disallow|allow /></predelim>
    //     <test>
    //       <jclass>RuleStepTest-class</jclass>
    //     </test>
    //   </ruleStep>
    //   ...next rule step...
    // </ruleName>
    //

    final List<DomElement> childNodes = collectChildElements(ruleElement);
    final int numChildNodes = childNodes.size();
    for (int i = 0; i < numChildNodes; ++i) {
      final DomElement stepElement = childNodes.get(i);
      AtnRuleStep step = new AtnRuleStep(stepElement, resourceManager, this);

      if (stepElement.getAttributeBoolean("popTest", false)) {
        addPopStep(step);

        if (i + 1 == numChildNodes && steps.size() > 0) {
          step = steps.get(steps.size() - 1);
          step.setIsTerminal(true);
        }
      }
      else {
        this.steps.addLast(step);
        if (i + 1 == numChildNodes) step.setIsTerminal(true);
      }


      // mark prior steps as terminal while terminals are optional
      if (step.isTerminal()) {
        for (int stepPos = steps.size() - 2; step.isOptional() && stepPos >= 0; --stepPos) {
          final AtnRuleStep prevStep = this.steps.get(stepPos);
          if (prevStep.isTerminal()) break;
          prevStep.setIsTerminal(true);
          step = prevStep;
        }
      }
    }
  }

  boolean isLast(int stepNum) {
    return stepNum == this.steps.size() - 1;
  }

  boolean isTerminal(int stepNum) {
    return isLast(stepNum) || steps.get(stepNum).isTerminal();
  }

  AtnRuleStep getStep(int stepNum) {
    AtnRuleStep result = null;

    if (stepNum >= 0 && stepNum < steps.size()) {
      return steps.get(stepNum);
    }

    return result;
  }

  boolean verifyPop(Token token, AtnState curState) {
    boolean result = true;

    if (popSteps != null) {
      for (AtnRuleStep popStep : popSteps) {
        result = popStep.verify(token, curState);
        if (!result) {
          break;
        }
      }
    }

    return result;
  }

  private final List<DomElement> collectChildElements(DomElement parentElement) {
    final List<DomElement> result = new ArrayList<DomElement>();

    final NodeList childNodes = parentElement.getChildNodes();
    for (int i = 0; i < childNodes.getLength(); ++i) {
      final Node curNode = childNodes.item(i);
      if (curNode.getNodeType() != DomElement.ELEMENT_NODE) continue;
      final DomElement childElement = (DomElement)curNode;
      result.add(childElement);
    }

    return result;
  }
}
