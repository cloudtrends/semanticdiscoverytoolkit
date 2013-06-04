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
import org.sd.token.Feature;
import org.sd.token.FeatureConstraint;
import org.sd.token.Features;
import org.sd.token.Token;
import org.sd.token.Tokenizer;
import org.sd.util.Usage;
import org.sd.util.tree.Tree;
import org.sd.xml.DomElement;
import org.sd.xml.DomNode;
import org.w3c.dom.NodeList;

/**
 * A rule to test text through RoteList- and/or Regex- Classifiers.
 * <p>
 * Note that this tests the full text of the constituent being considered,
 * not for example the last single token encountered. For testing against the
 * last token, see TokenTest instead.
 *
 * @author Spence Koehler
 */
@Usage(notes =
       "A org.sd.atn.BaseClassifierTest to evaluate text through\n" +
       "org.sd.atn.RoteListClassifier or org.sd.atn.RegexClassifier\n" +
       "instances.\n" +
       "\n" +
       "Note that this tests the full text of the constituent being considered,\n" +
       "not for example the last single token encountered. For testing against the\n" +
       "last token, see TokenTest instead.\n" +
       " \n" +
       "Under the token node, setup allowed and disallowed tokens\n" +
       " \n" +
       " options:\n" +
       " - when reverse='true', fail on match (handled elsewhere)\n" +
       " \n" +
       " <test reverse='true|false'>\n" +
       "   <jclass>org.sd.atn.TextTest</jclass>\n" +
       "   <terms caseSensitive='true|false'>\n" +
       "     <term>...</term>\n" +
       "     ...\n" +
       "   </terms>\n" +
       "   <regexes>      applied to text of constituent\n" +
       "     <regex type='...' groupN='...'>...</regex>\n" +
       "   </regexes>\n" +
       "   <prior-regexes>  // applied to text prior to constituent\n" +
       "     <regex type='...' groupN='...'>...</regex>\n" +
       "   </prior-regexes>\n" +
       "   <post-regexes> applied to text after the constituent\n" +
       "     <regex type='...' groupN='...'>...</regex>\n" +
       "   </post-regexes>\n" +
       " </test>"
  )
public class TextTest extends BaseClassifierTest {
  
  private RegexClassifier priorRegexClassifier;
  private RegexClassifier postRegexClassifier;
  private boolean prevToken;

  public TextTest(DomNode testNode, ResourceManager resourceManager) {
    super(testNode, resourceManager);

    this.priorRegexClassifier = new RegexClassifier((DomElement)testNode, resourceManager, resourceManager.getId2Normalizer(),
                                                    "prior-regexes");
    this.postRegexClassifier = new RegexClassifier((DomElement)testNode, resourceManager, resourceManager.getId2Normalizer(),
                                                   "post-regexes");

    // under token node, setup allowed and disallowed tokens
    //
    // options:
    // - when reverse='true', fail on match (handled elsewhere)
    //
    // <test reverse='true|false'>
    //   <jclass>org.sd.atn.TextTest</jclass>
    //   <terms caseSensitive='true|false'>
    //     <term>...</term>
    //     ...
    //   </terms>
    //   <regexes>        // applied to text of constituent
    //     <regex type='...' groupN='...'>...</regex>
    //   </regexes>
    //   <prior-regexes>  // applied to text prior to constituent
    //     <regex type='...' groupN='...'>...</regex>
    //   </prior-regexes>
    //   <post-regexes>   // applied to text after the constituent
    //     <regex type='...' groupN='...'>...</regex>
    //   </post-regexes>
    // </test>

  }
			
  protected boolean doAccept(Token token, AtnState curState) {
    boolean result = false;

    String text = null;
    Token startToken = token;

    if (verbose) {
      final boolean stopHere = true;
    }

    if (curState.isPoppedState()) {
      final AtnState startState = AtnStateUtil.getConstituentStartState(curState);

      if (token.hasFeatures()) {
        final List<String> constituentCategories = getConstituentCategories(curState, startState);
        // use feature match text from the token, if present.
        text = getFeatureMatchText(token, constituentCategories);
      }

      // otherwise, use the input text spanning the current constituent
      if (text == null) {
        final String fulltext = token.getTokenizer().getText();
        startToken = startState.getInputToken();
        text = fulltext.substring(startToken.getStartIndex(), token.getEndIndex());
      }
    }
    else {
      text = token.getText();
    }

    
    if (!result && roteListClassifier != null) {
      result = (roteListClassifier.doClassify(text) != null);
    }

    if (roteListClassifier == null) {
      result = true;
    }

    if (result && (!priorRegexClassifier.isEmpty() || !postRegexClassifier.isEmpty())) {
      if (result && !priorRegexClassifier.isEmpty()) {
        try {
          final Token priorToken = startToken.getPrevToken();
          if (priorToken != null) {
            result = priorRegexClassifier.doClassify(priorToken, curState);
            if (verbose) System.out.println("TextTest.priorToken(" + priorToken + ")=" + result);
          }
        }
        catch (Exception e) {
          // there is no prior token to test.
        }
      }

      if (result && !postRegexClassifier.isEmpty()) {
        final Token postToken = token.getNextToken();
        if (postToken != null) {
          result = postRegexClassifier.doClassify(postToken, curState);
          if (verbose) System.out.println("TextTest.postToken(" + postToken + ")=" + result);
        }
      }
    }

    return result;
  }

  private final List<String> getConstituentCategories(AtnState curState, AtnState startState) {
    final List<String> result = new ArrayList<String>();

    for (AtnState atnState = curState.getParentState() ;
         atnState != null && atnState != startState ;
         atnState = atnState.getParentState()) {
      if (atnState.isPoppedState() || atnState.getMatched()) {
        final String cat = atnState.getRuleStep().getCategory();
        result.add(cat);
      }
    }

    return result;
  }

  private final String getFeatureMatchText(Token token, List<String> constituentCategories) {
    String result = null;

    // use AtnStateUtil.FEATURE_MATCH features on the token to find interp
    // parse trees as the source for the text.

    for (String cCat : constituentCategories) {
      final List<ParseInterpretation> interps = AtnStateUtil.getFeatureMatchInterps(token, cCat);
      if (interps != null) {
        // keep the longest text from the interp parse tree leaf text
        for (ParseInterpretation interp : interps) {
          final String text = getInterpretationText(interp);
          if (text != null) {
            if (result == null || (text.length() > result.length())) {
              result = text;
            }
          }
        }
      }
    }

    return result;
  }

  private final String getInterpretationText(ParseInterpretation interp) {
    String result = null;

    final AtnParse atnParse = interp.getSourceParse();
    if (atnParse != null && atnParse.getSelected()) {
      final Tree<String> parseTree = atnParse.getParseTree();
      result = parseTree.getLeafText();
    }

    return result;
  }
}
