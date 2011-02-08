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
import org.sd.xml.DomElement;
import org.w3c.dom.NodeList;

/**
 * Container for a rule step within a Rule.
 * <p>
 * @author Spence Koehler
 */
public class AtnRuleStep {
  
  private static final ClusteringTest CLUSTER_TEST = new ClusteringTest();


  private AtnRule rule;
  public AtnRule getRule() {
    return rule;
  }

  private String category;
  public String getCategory() {
    return category;
  }

  private String label;
  public String getLabel() {
    return label == null ? category : label;
  }

  private String require;
  /**
   * If non-null, this indicates that a rule step only applies if the specified
   * constituent or category has been matched.
   * <p>
   * This allows for a rule to specify a step that only applies when an
   * optional step has matched.
   * <p>
   * Also, this aids in disabling superfluous extraneous parses when the step's
   * category could have been matched in a prior constituent followed by a
   * missing optional constituent, which leads to this step. Specifying the
   * optional constituent as required for this step ensures that the extraneous
   * parse with this step's category is not redundantly generated when the
   * optional constituent is missing.
   */
  public String getRequire() {
    return require;
  }

  private String unless;
  /**
   * If non-null, this indicates that a rule step only applies if the specified
   * constituent or category has NOT been matched.
   * <p>
   * This allows for a rule to specify a step that only applies when an
   * optional step has not matched.
   */
  public String getUnless() {
    return unless;
  }

  private boolean isOptional;
  public boolean isOptional() {
    return isOptional;
  }

  private boolean repeats;
  public boolean repeats() {
    return repeats;
  }

  private boolean isTerminal;
  public boolean isTerminal() {
    return isTerminal;
  }

  private DelimTest postDelim;
  DelimTest getPostDelim() {
    return postDelim;
  }

  private DelimTest preDelim;
  DelimTest getPreDelim() {
    return preDelim;
  }

  private ClusteringTest clusterTest;
  ClusteringTest getClusterTest() {
    return clusterTest;
  }

  private AtnRuleStepTest test;
  AtnRuleStepTest getTest() {
    return test;
  }

  private boolean consumeToken;
  public boolean consumeToken() {
    return consumeToken;
  }

  private boolean ignoreToken;
  public boolean getIgnoreToken() {
    return ignoreToken;
  }

  private int skip;
  public int getSkip() {
    return skip;
  }


  AtnRuleStep(DomElement stepElement, ResourceManager resourceManager, AtnRule rule) {
    this.rule = rule;
    this.category = stepElement.getLocalName();
    this.label = stepElement.getAttributeValue("label", null);
    this.require = stepElement.getAttributeValue("require", null);
    this.unless = stepElement.getAttributeValue("unless", null);
    this.isOptional = stepElement.getAttributeBoolean("optional", false);
    this.repeats = stepElement.getAttributeBoolean("repeats", false);
    this.isTerminal = stepElement.getAttributeBoolean("terminal", false);
    this.consumeToken = stepElement.getAttributeBoolean("consumeToken", true);
    this.ignoreToken = stepElement.getAttributeBoolean("ignoreToken", false);
    this.skip = stepElement.getAttributeInt("skip", 0);
    final boolean clusterFlag = stepElement.getAttributeBoolean("cluster", false);

    final DomElement postDelimElement = (DomElement)stepElement.selectSingleNode("postdelim");
    this.postDelim = (postDelimElement != null) ? new DelimTest(false, postDelimElement) : null;

    final DomElement preDelimElement = (DomElement)stepElement.selectSingleNode("predelim");
    this.preDelim = (preDelimElement != null) ? new DelimTest(true, preDelimElement) : null;

    this.clusterTest = clusterFlag ? CLUSTER_TEST : null;

    // load test(s)
    this.test = null;
    final NodeList testNodes = stepElement.selectNodes("test");
    if (testNodes.getLength() > 0) {
      if (testNodes.getLength() == 1) {
        final DomElement testElement = (DomElement)testNodes.item(0);
        this.test = buildTest(testElement, resourceManager);
      }
      else {
        final AtnRuleStepTestPipeline pipeline = new AtnRuleStepTestPipeline();
        for (int nodeNum = 0; nodeNum < testNodes.getLength(); ++nodeNum) {
          final DomElement testElement = (DomElement)testNodes.item(nodeNum);
          pipeline.add(buildTest(testElement, resourceManager));
        }
        this.test = pipeline;
      }
    }
  }

  private final AtnRuleStepTest buildTest(DomElement testElement, ResourceManager resourceManager) {
    AtnRuleStepTest result = null;

    final boolean reverse = testElement.getAttributeBoolean("reverse", false);
    result = (AtnRuleStepTest)resourceManager.getResource(testElement);
    if (reverse) {
      result = new ReversedAtnRuleStepTest(result);
    }

    return result;
  }

  /**
   * Assuming this step's category applies to the token, verify
   * the postDelim and test constraints.
   */
  boolean verify(Token token, AtnState curState) {
    boolean result = true;

    if (result && postDelim != null) {
      result = postDelim.accept(token, curState);
    }

    if (result && preDelim != null) {
      result = preDelim.accept(token, curState);
    }

    if (result && clusterTest != null) {
      result = clusterTest.accept(token, curState);
    }

    if (result && test != null) {
      result = test.accept(token, curState);
    }

    return result;
  }

  void setIsTerminal(boolean isTerminal) {
    this.isTerminal = isTerminal;
  }

  public String toString() {
    final StringBuilder result = new StringBuilder();

    result.append(category);

    if (isOptional && repeats) result.append('*');
    else if (isOptional) result.append('?');
    else if (repeats) result.append('+');

    if (isTerminal) result.append('.');

    return result.toString();
  }
}
