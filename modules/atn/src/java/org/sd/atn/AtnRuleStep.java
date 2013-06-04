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

/**
 * Container for a rule step within a Rule.
 * <p>
 * @author Spence Koehler
 */
public class AtnRuleStep {
  
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

  public boolean matchesCategory(String cat) {
    boolean result = category.equals(cat);

    if (!result && label != null) {
      result = label.equals(cat);
    }

    return result;
  }

  private String requireString;
  public String getRequireString() {
    return requireString;
  }

  private boolean popStep;
  public boolean isPopStep() {
    return popStep;
  }
  void setPopStep(boolean popStep) {
    this.popStep = popStep;
  }

  private StepRequirement[] require;
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
  public StepRequirement[] getRequire() {
    return require;
  }

  private String unlessString;
  public String getUnlessString() {
    return unlessString;
  }

  private StepRequirement[] unless;
  /**
   * If non-null, this indicates that a rule step only applies if the specified
   * constituent or category has NOT been matched.
   * <p>
   * This allows for a rule to specify a step that only applies when an
   * optional step has not matched.
   */
  public StepRequirement[] getUnless() {
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

  private int repeatLimit;
  public int getRepeatLimit() {
    return repeatLimit;
  }

  private boolean isTerminal;
  public boolean isTerminal() {
    return isTerminal;
  }

  private boolean isNonTerminal;
  public boolean isNonTerminal() {
    return isNonTerminal;
  }

  private StepTestContainer testContainer;
  public StepTestContainer getTestContainer() {
    return testContainer;
  }
  private boolean clusterFlag;  // greedy flag
  boolean getClusterFlag() {
    return clusterFlag;
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

  private boolean verbose;
  public boolean getVerbose() {
    return verbose;
  }


  AtnRuleStep(DomElement stepElement, ResourceManager resourceManager, AtnRule rule) {
    this.rule = rule;
    this.category = stepElement.getLocalName();
    this.label = stepElement.getAttributeValue("label", null);

    this.requireString = stepElement.getAttributeValue("require", null);
    this.require = StepRequirement.buildInstances(requireString);

    this.unlessString = stepElement.getAttributeValue("unless", null);
    this.unless = StepRequirement.buildInstances(unlessString);

    this.isOptional = stepElement.getAttributeBoolean("optional", false);
    this.repeats = stepElement.getAttributeBoolean("repeats", false);
    this.repeatLimit = stepElement.getAttributeInt("repeatLimit", 0);
    this.isTerminal = stepElement.getAttributeBoolean("terminal", false);
    this.isNonTerminal = stepElement.getAttributeBoolean("nonTerminal", false); // force nonTerminal
    this.consumeToken = stepElement.getAttributeBoolean("consumeToken", true);
    this.ignoreToken = stepElement.getAttributeBoolean("ignoreToken", false);
    this.skip = stepElement.getAttributeInt("skip", 0);
    this.verbose = stepElement.getAttributeBoolean("verbose", false);
    this.clusterFlag = stepElement.getAttributeBoolean("cluster", false);

    this.testContainer = new StepTestContainer(stepElement, resourceManager);
  }

  /**
   * Assuming this step's category applies to the token, verify
   * the postDelim and test constraints.
   */
  boolean verify(Token token, AtnState curState) {
    if (isPopStep()) curState.setPopping(true);
    final boolean result = testContainer.verify(token, curState).accept();
    if (isPopStep()) curState.setPopping(false);
    return result;
  }

  void setIsTerminal(boolean isTerminal) {
    if (!isNonTerminal) {
      this.isTerminal = isTerminal;
    }
  }

  public String toString() {
    final StringBuilder result = new StringBuilder();

    result.append(category);
    if (label != null && !label.equals(category)) {
      result.append('[').append(label).append(']');
    }

    if (isOptional && repeats) result.append('*');
    else if (isOptional) result.append('?');
    else if (repeats) result.append('+');

    if (isTerminal) result.append('.');

    return result.toString();
  }
}
