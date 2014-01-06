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
       "   <halt>\n" +
       "     <test/preDelim/postDelim>...</test/preDelim/postDelim>\n" +
       "     ...\n" +
       "   </halt>\n" +
       " </test>\n" +
       " \n" +
       " The test passes (or fails when reverse=true) when any (or all) categories\n" +
       " are present and/or classifiers match tokens in the current constituent's\n" +
       " (rule's) history."
  )
public class TokenInclusionTest extends BaseClassifierTest {
  
  //
  // Walk back through the states looking for tokens with certain assigned
  // categories and/or applicable classifiers.
  // 
  // <test [include='any|all']>
  //   <jclass>org.sd.atn.TokenInclusionTest</jclass>
  //   <category values="cat1,...,catN" [include='any|all'] />
  //   <classifier values="classifier1,...,classifierN" [include='any|all'] />
  //   <halt>
  //     <test/preDelim/postDelim>...</test/preDelim/postDelim>"
  //     ...
  //   </halt>
  // </test>
  //
  // The test passes (or fails when reverse=true) when any (or all) categories
  // are present and/or classifiers match tokens in the current constituent's
  // (rule's) history before (working backwards) hitting a(n optional) halt
  // condition.
  //
  //NOTE: reverse is applied in the container that runs the tests.

  private boolean includeAll;
  private List<InclusionContainer> containers;
  private StepTestContainer haltContainer;
  private boolean unlimit;
  private boolean haltReverse;
  private boolean applyHaltAfterTest;

  public TokenInclusionTest(DomNode testNode, ResourceManager resourceManager) {
    super(testNode, resourceManager);

    final String include = testNode.getAttributeValue("include", "all");
    this.includeAll = "all".equals(include);
    this.containers = loadContainers(testNode);
    this.haltContainer = loadHaltContainer(testNode, resourceManager);
    this.unlimit = false;
    this.haltReverse = false;
    this.applyHaltAfterTest = false;

    if (haltContainer != null) {
      final DomElement haltElt = haltContainer.getStepElement();
      this.unlimit = haltElt.getAttributeBoolean("unlimit", false);
      this.haltReverse = haltElt.getAttributeBoolean("reverse", false);
      this.applyHaltAfterTest = haltElt.getAttributeBoolean("afterTest", false);
    }
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

  private final StepTestContainer loadHaltContainer(DomNode testNode, ResourceManager resourceManager) {
    StepTestContainer result = null;

    final DomElement haltElement = (DomElement)testNode.selectSingleNode("halt");
    if (haltElement != null) {
      result = new StepTestContainer(haltElement, resourceManager);
    }

    return result;
  }

  /**
   * Determine whether to accept the (matched) state.
   * <p>
   * This is called as a last check on whether a token matches for the current
   * state after its category has been matched to its containing rule step.
   */
  protected boolean doAccept(Token token, AtnState curState) {
    boolean result = false;

    AtnState stopState = (unlimit ? null : curState.getConstituentTop());
    if (stopState != null) stopState = stopState.getParentState();  // back-up one to include constit top itself in search

    if (verbose) {
      System.out.println("\nTokenInclusionTest starting w/token=" + token + ", stopState=" + stopState);
    }

    for (InclusionContainer ic : containers) {
      Token priorToken = token;
      final InclusionContainerContext icContext = new InclusionContainerContext(ic.getTotalCount(), ic.includeAll());
      for (AtnState atnState = curState; atnState != null && atnState != stopState; atnState = atnState.getParentState()) {
        if (haltContainer != null && !applyHaltAfterTest && atnState.getInputToken() != priorToken) {
          priorToken = atnState.getInputToken();
          final boolean haltResult = haltContainer.verify(priorToken, atnState).accept();
          if ((haltResult && !haltReverse) || (!haltResult && haltReverse)) {
            if (verbose) {
              System.out.println("TokenInclusionTest hit (beforeTest) halt (rev=" + haltReverse + ") condition at atnState=" + atnState);
            }
            break;
          }
        }

        final boolean curResult = ic.appliesTo(atnState, icContext);

        if (verbose) {
          final String curCategory = atnState.getRuleStep().getCategory();
          System.out.println("TokenInclusionTest (" + curResult + ") visiting state '" + curCategory + "' " + atnState.getInputToken()); 
        }

        if (icContext.isComplete()) break;

        if (haltContainer != null && applyHaltAfterTest && atnState.getInputToken() != priorToken) {
          priorToken = atnState.getInputToken();
          final boolean haltResult = haltContainer.verify(priorToken, atnState).accept();
          if ((haltResult && !haltReverse) || (!haltResult && haltReverse)) {
            if (verbose) {
              System.out.println("TokenInclusionTest hit (afterTest) halt (rev=" + haltReverse + ") condition at atnState=" + atnState);
            }
            break;
          }
        }
      }

      result = icContext.isComplete();
      if (isFinalResult(result, includeAll)) break;
    }

    if (verbose) {
      System.out.println("\nTokenInclusionTest ending w/result=" + result);
    }

    return result;
  }
  
  private static final boolean isFinalResult(boolean curResult, boolean includeAll) {
    return ((includeAll && !curResult) || (!includeAll && curResult));
  }


  private static final class InclusionContainerContext {

    private int totalCount;
    private boolean includeAll;
    private Set<String> got;

    InclusionContainerContext(int totalCount, boolean includeAll) {
      this.totalCount = totalCount;
      this.includeAll = includeAll;
      this.got = new HashSet<String>();
    }

    void add(String item) {
      got.add(item);
    }

    boolean isComplete() {
      return includeAll ? got.size() == totalCount : got.size() > 0;
    }
  }

  private static abstract class InclusionContainer {
    private boolean includeAll;

    InclusionContainer(DomElement categoryElement, boolean defaultIncludeAll) {
      final String include = categoryElement.getAttributeValue("include", (defaultIncludeAll ? "all" : "any"));
      this.includeAll = "all".equals(include);
    }

    boolean includeAll() {
      return includeAll;
    }

    abstract int getTotalCount();

    abstract boolean appliesTo(AtnState atnState, InclusionContainerContext icContext);
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

    int getTotalCount() {
      return stepRequirements.length;
    }

    boolean appliesTo(AtnState atnState, InclusionContainerContext icContext) {
      final String cat = atnState.getRuleStep().getCategory();
      boolean result = categories.contains(cat);
      if (result) icContext.add(cat);

      if (!result) {
        for (StepRequirement requirement : stepRequirements) {
          result = AtnStateUtil.matchesCategory(atnState, requirement);
          if (result) {
            icContext.add(requirement.getCategory());
            if (icContext.isComplete()) break;
          }
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

    int getTotalCount() {
      return classifierIds.size();
    }

    boolean appliesTo(AtnState atnState, InclusionContainerContext icContext) {
      final String cat = atnState.getRuleStep().getCategory();
      boolean result = classifierIds.contains(cat);
      if (result) icContext.add(cat);

      if (!result) {
        for (String classifierId : classifierIds) {
          final List<AtnStateTokenClassifier> classifiers = atnState.getRule().getGrammar().getClassifiers(classifierId);
          if (classifiers != null) {
            for (AtnStateTokenClassifier classifier : classifiers) {
              result = classifier.classify(atnState.getInputToken(), atnState).matched();
              if (result) {
                icContext.add(classifierId);
                if (icContext.isComplete()) break;
              }
            }
          }
          if (icContext.isComplete()) break;
        }
      }

      return result;
    }
  }
}
