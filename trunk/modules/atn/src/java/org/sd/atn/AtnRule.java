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


import java.util.LinkedList;
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
  AtnGrammar getGrammar() {
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
    //   <ruleStep require='' optional='' repeats='' terminal='' skip=''>
    //     <postdelim><disallowall|allowall|disallow|allow /></postdelim>
    //     <predelim><disallowall|allowall|disallow|allow /></predelim>
    //     <test>
    //       <jclass>RuleStepTest-class</jclass>
    //     </test>
    //   </ruleStep>
    //   ...next rule step...
    // </ruleName>
    //

    final NodeList childNodes = ruleElement.getChildNodes();
    for (int i = 0; i < childNodes.getLength(); ++i) {
      final Node curNode = childNodes.item(i);

      if (curNode.getNodeType() != DomElement.ELEMENT_NODE) continue;

      final DomElement stepElement = (DomElement)curNode;
      AtnRuleStep step = new AtnRuleStep(stepElement, resourceManager);
      this.steps.addLast(step);

      if (this.steps.size() == childNodes.getLength()) step.setIsTerminal(true);

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
}
