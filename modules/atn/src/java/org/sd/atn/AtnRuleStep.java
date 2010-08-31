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

/**
 * Container for a rule step within a Rule.
 * <p>
 * @author Spence Koehler
 */
public class AtnRuleStep {
  
  private static final ClusteringTest CLUSTER_TEST = new ClusteringTest();


  private String category;
  public String getCategory() {
    return category;
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


  AtnRuleStep(DomElement stepElement, ResourceManager resourceManager) {
    this.category = stepElement.getLocalName();
    this.isOptional = stepElement.getAttributeBoolean("optional", false);
    this.repeats = stepElement.getAttributeBoolean("repeats", false);
    this.isTerminal = stepElement.getAttributeBoolean("terminal", false);
    this.consumeToken = stepElement.getAttributeBoolean("consumeToken", true);
    this.ignoreToken = stepElement.getAttributeBoolean("ignoreToken", false);
    final boolean clusterFlag = stepElement.getAttributeBoolean("cluster", false);

    final DomElement postDelimElement = (DomElement)stepElement.selectSingleNode("postdelim");
    this.postDelim = (postDelimElement != null) ? new DelimTest(false, postDelimElement) : null;

    final DomElement preDelimElement = (DomElement)stepElement.selectSingleNode("predelim");
    this.preDelim = (preDelimElement != null) ? new DelimTest(true, preDelimElement) : null;

    this.clusterTest = clusterFlag ? CLUSTER_TEST : null;

    final DomElement testElement = (DomElement)stepElement.selectSingleNode("test");
    this.test = (testElement != null) ? (AtnRuleStepTest)resourceManager.getResource(testElement) : null;
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
