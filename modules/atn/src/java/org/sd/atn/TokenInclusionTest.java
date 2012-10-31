/*
    Copyright 2010 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sd.token.Token;
import org.sd.util.Usage;
import org.sd.xml.DomElement;
import org.sd.xml.DomNode;
import org.w3c.dom.NodeList;

/**
 * An AtnRuleStep test that checks tokens in states leading up to the current
 * state for inclusion of certain categories and/or text content.
 * <p>
 * @author Spence Koehler
 */
@Usage(notes =
       "An org.sd.atn.AtnRuleStep test that checks tokens in states\n" +
       "leading up to the current state for inclusion of certain\n" +
       "categories and/or text content.\n" +
       " \n" +
       " Walk back through the states looking for tokens with certain assigned\n" +
       " categories and/or applicable classifiers.\n" +
       " \n" +
       " <test [include='any|all']>\n" +
       "   <jclass>org.sd.atn.TokenInclusionTest</jclass>\n" +
       "   <category values=\"cat1,...,catN\" [include='any|all'] />\n" +
       "   <classifier values=\"classifier1,...,classifierN\" [include='any|all'] />\n" +
       " </test>\n" +
       " \n" +
       " The test passes (or fails when reverse=true) when any (or all) categories\n" +
       " are present and/or classifiers match tokens in the current constituent's\n" +
       " (rule's) history."
  )
public class TokenInclusionTest implements AtnRuleStepTest {
  
  //
  // Walk back through the states looking for tokens with certain assigned
  // categories and/or applicable classifiers.
  // 
  // <test [include='any|all']>
  //   <jclass>org.sd.atn.TokenInclusionTest</jclass>
  //   <category values="cat1,...,catN" [include='any|all'] />
  //   <classifier values="classifier1,...,classifierN" [include='any|all'] />
  // </test>
  //
  // The test passes (or fails when reverse=true) when any (or all) categories
  // are present and/or classifiers match tokens in the current constituent's
  // (rule's) history.
  //


  private boolean verbose;
  private boolean includeAll;
  private List<InclusionContainer> containers;

  public TokenInclusionTest(DomNode testNode, ResourceManager resourceManager) {
    this.verbose = testNode.getAttributeBoolean("verbose", false);
    final String include = testNode.getAttributeValue("include", "all");
    this.includeAll = "all".equals(include);
    this.containers = loadContainers(testNode);
  }

  private final List<InclusionContainer> loadContainers(DomNode testNode) {
    final List<InclusionContainer> result = new ArrayList<InclusionContainer>();

    final NodeList categoryNodes = testNode.selectNodes("category");
    if (categoryNodes != null) {
      final int numCategoryNodes = categoryNodes.getLength();
      for (int nodeNum = 0; nodeNum < numCategoryNodes; ++nodeNum) {
        final DomElement categoryElement = (DomElement)categoryNodes.item(nodeNum);
        final InclusionContainer ic = new CategoryContainer(categoryElement, includeAll);
        result.add(ic);
      }
    }

    final NodeList classifierNodes = testNode.selectNodes("classifier");
    if (classifierNodes != null) {
      final int numClassifierNodes = classifierNodes.getLength();
      for (int nodeNum = 0; nodeNum < numClassifierNodes; ++nodeNum) {
        final DomElement classifierElement = (DomElement)classifierNodes.item(nodeNum);
        final InclusionContainer ic = new ClassifierContainer(classifierElement, includeAll);
        result.add(ic);
      }
    }

    return result;
  }

  /**
   * Determine whether to accept the (matched) state.
   * <p>
   * This is called as a last check on whether a token matches for the current
   * state after its category has been matched to its containing rule step.
   */
  public PassFail accept(Token token, AtnState curState) {
    boolean result = false;

    final AtnState stopState = curState.getPushState();

    if (verbose) {
      System.out.println("\nTokenInclusionTest starting w/token=" + token + ", stopState=" + stopState);
    }

    for (InclusionContainer ic : containers) {
      for (AtnState atnState = curState; atnState != null && atnState != stopState; atnState = atnState.getParentState()) {
        result = ic.appliesTo(atnState);

        if (verbose) {
          final String curCategory = atnState.getRuleStep().getCategory();
          System.out.println("TokenInclusionTest (" + result + ") visiting state '" + curCategory + "' " + atnState.getInputToken()); 
        }

        if (result) break;
      }

      if (isFinalResult(result, includeAll)) break;
    }

    if (verbose) {
      System.out.println("\nTokenInclusionTest ending w/result=" + result);
    }

    return PassFail.getInstance(result);
  }
  
  private static final boolean isFinalResult(boolean curResult, boolean includeAll) {
    return ((includeAll && !curResult) || (!includeAll && curResult));
  }

  private static abstract class InclusionContainer {
    private boolean includeAll;

    InclusionContainer(DomElement categoryElement, boolean defaultIncludeAll) {
      final String include = categoryElement.getAttributeValue("include", (defaultIncludeAll ? "all" : "any"));
      this.includeAll = "all".equals(include);
    }

    boolean scanIsComplete(boolean curResult) {
      return isFinalResult(curResult, includeAll);
    }

    abstract boolean appliesTo(AtnState atnState);
  }

  // <category values="cat1,...,catN" [include='any|all'] />
  private static final class CategoryContainer extends InclusionContainer {
    private Set<String> categories;
    private StepRequirement[] stepRequirements;

    CategoryContainer(DomElement categoryElement, boolean defaultIncludeAll) {
      super(categoryElement, defaultIncludeAll);
      this.categories = new HashSet<String>();

      final String[] values = categoryElement.getAttributeValue("values", "").split("\\s*,\\s*");
      this.stepRequirements = new StepRequirement[values.length];
      for (int i = 0; i < values.length; ++i) {
        final String value = values[i];
        categories.add(value);
        stepRequirements[i] = new StepRequirement(value, null);
      }
    }

    boolean appliesTo(AtnState atnState) {
      boolean result = categories.contains(atnState.getRuleStep().getCategory());

      if (!result) {
        for (StepRequirement requirement : stepRequirements) {
          result = AtnStateUtil.matchesCategory(atnState, requirement);
          if (scanIsComplete(result)) break;
        }
      }

      return result;
    }
  }

  // <classifier values="classifier1,...,classifierN" [include='any|all'] />
  private static final class ClassifierContainer extends InclusionContainer {
    private Set<String> classifierIds;

    ClassifierContainer(DomElement classifierElement, boolean defaultIncludeAll) {
      super(classifierElement, defaultIncludeAll);
      this.classifierIds = new HashSet<String>();

      final String[] values = classifierElement.getAttributeValue("values", "").split("\\s*,\\s*");
      for (String value : values) classifierIds.add(value);
    }

    boolean appliesTo(AtnState atnState) {
      boolean result = classifierIds.contains(atnState.getRuleStep().getCategory());

      if (!result) {
        for (String classifierId : classifierIds) {
          final List<AtnStateTokenClassifier> classifiers = atnState.getRule().getGrammar().getClassifiers(classifierId);
          if (classifiers != null) {
            for (AtnStateTokenClassifier classifier : classifiers) {
              result = classifier.classify(atnState.getInputToken(), atnState).matched();
              if (result) break;
            }
          }
          if (scanIsComplete(result)) break;
        }
      }

      return result;
    }
  }
}
