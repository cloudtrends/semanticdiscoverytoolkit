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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.sd.token.CategorizedToken;
import org.sd.token.Feature;
import org.sd.token.FeatureConstraint;
import org.sd.token.Token;
import org.sd.token.Tokenizer;
import org.sd.util.tree.Tree;

/**
 * Utilities for working with parse interpretations.
 * <p>
 * @author Spence Koehler
 */
public class ParseInterpretationUtil {
  
  public static CategorizedToken getCategorizedToken(Tree<String> parseTreeNode) {
    return getCategorizedToken(parseTreeNode, true);
  }

  public static CategorizedToken getCategorizedToken(Tree<String> parseTreeNode, boolean createToken) {
    // first look for the cached token
    CategorizedToken result = getCategorizedToken(parseTreeNode, true, true);

    // create and cache if necessary
    if (createToken && result == null) {
      final Tree<String> firstLeaf = parseTreeNode.getFirstLeaf();
      final Tree<String> lastLeaf = parseTreeNode.getLastLeaf();

      final CategorizedToken firstToken = AtnStateUtil.getCategorizedToken(firstLeaf);
      if (firstToken != null) {
        if (firstLeaf == lastLeaf) {
          result = new CategorizedToken(firstToken.token, parseTreeNode.getData());
        }
        else {
          final CategorizedToken lastToken = AtnStateUtil.getCategorizedToken(lastLeaf);
          final Token newToken = firstToken.token.getTokenizer().buildToken(firstToken.token.getStartIndex(), lastToken.token.getEndIndex());
          result = new CategorizedToken(newToken, parseTreeNode.getData());
        }
      }
      
      parseTreeNode.getAttributes().put(AtnStateUtil.TOKEN_KEY, result);
    }

    return result;
  }

  private static CategorizedToken getCategorizedToken(Tree<String> parseTreeNode, boolean descend, boolean ascend) {
    CategorizedToken result = null;

    if (parseTreeNode.hasAttributes()) {
      final Map<String, Object> parseAttributes = parseTreeNode.getAttributes();
      result = (CategorizedToken)parseAttributes.get(AtnStateUtil.TOKEN_KEY);    
    }

    if (descend && result == null && parseTreeNode.numChildren() == 1) {
      result = getCategorizedToken(parseTreeNode.getChildren().get(0), true, false);
    }

    if (ascend && result == null && parseTreeNode.getNumSiblings() == 1) {
      result = getCategorizedToken(parseTreeNode.getParent(), false, true);
    }

    return result;
  }

  /**
   * Get the first matching token feature.
   * <p>
   * NOTE: Non-null result's value will be of type featureValueClass.
   */
  public static Feature getFirstTokenFeature(Tree<String> parseTreeNode, String featureKey, Class featureValueClass) {
    Feature result = null;

    final CategorizedToken cToken = getCategorizedToken(parseTreeNode, false);
    if (cToken != null && cToken.token.hasFeatures()) {
      result = cToken.token.getFeature(featureKey, null, featureValueClass);
    }

    return result;
  }

  public static List<Feature> getAllTokenFeatures(Tree<String> parseTreeNode, String featureKey, Class featureValueClass) {
    List<Feature> result = null;

    final CategorizedToken cToken = getCategorizedToken(parseTreeNode, false);
    if (cToken != null && cToken.token.hasFeatures()) {
      result = cToken.token.getFeatures(featureKey, null, featureValueClass);
    }

    return result;
  }

  /**
   * Get the first interpretation with the given featureKey from the
   * tokens under the given parseTreeNode or null.
   */
  public static ParseInterpretation getFirstInterpretation(Tree<String> parseTreeNode, String featureKey) {
    ParseInterpretation result = null;

    final Feature feature = getFirstTokenFeature(parseTreeNode, featureKey, ParseInterpretation.class);
    if (feature != null) {
      result = (ParseInterpretation)feature.getValue();
    }

    return result;
  }

  /**
   * Get all interpretations with the given featureKey from the
   * tokens under the given parseTreeNode or null.
   */
  public static List<ParseInterpretation> getInterpretations(Tree<String> parseTreeNode, String featureKey) {
    List<ParseInterpretation> result = null;

    final List<Feature> features = getAllTokenFeatures(parseTreeNode, featureKey, ParseInterpretation.class);
    if (features != null) {
      result = new ArrayList<ParseInterpretation>();
      for (Feature feature : features) {
        result.add((ParseInterpretation)feature.getValue());
      }
    }

    return result;
  }

  /**
   * Resolve the interpretations with the given featureKey from the
   * tokens under the given parseTreeNode to the given interpretation.
   */
  public static void resolveInterpretation(Tree<String> parseTreeNode, String featureKey, ParseInterpretation resolved) {

    boolean sawResolved = false;

    final CategorizedToken cToken = getCategorizedToken(parseTreeNode, false);
    if (cToken != null && cToken.token.hasFeatures()) {
      final List<Feature> features = cToken.token.getFeatures().getFeatures();
      final FeatureConstraint constraint = FeatureConstraint.getInstance(featureKey, null, ParseInterpretation.class);
      for (Iterator<Feature> iter = features.iterator(); iter.hasNext(); ) {
        final Feature feature = iter.next();
        if (!constraint.includes(feature)) continue;
        if (resolved == feature.getValue()) {
          sawResolved = true;
          continue;
        }
        final ParseInterpretation interp = (ParseInterpretation)feature.getValue();
        final Parse parse = interp.getParse();
        if (parse != null) {
          final Tree<String> parseTree = parse.getParseTree();
          parseTree.prune(true, true);
        }
        
        iter.remove();
      }
    }

    if (cToken != null && !sawResolved) {
      // set the feature on the token
      cToken.token.setFeature(resolved.getClassification(), resolved, null);
    }
  }

  private enum CharType {
    UPPER('A'), LOWER('a'), DIGIT('x'), OTHER('-'), WHITE('.'), NONE('*');

    private char c;
    private CharType(char c) {
      this.c = c;
    }
    char getChar() { return c; }
  };

  public static final String getTextPattern(ParseInterpretation interp) {
    String text = null;

    final Parse parse = interp.getParse();
    if (parse != null) {
      final Tokenizer tokenizer = parse.getTokenizer();
      if (tokenizer != null) {
        text = tokenizer.getText();
      }
      else {
        final Tree<String> parseTree = parse.getParseTree();
        if (parseTree != null) {
          text = parseTree.getLeafText();
        }
      }
    }
    else {
      text = interp.toString();
    }

    return getTextPattern(text);
  }

  public static final String getTextPattern(String text) {
    final StringBuilder result = new StringBuilder();

    final int len = text.length();

    CharType lastCharType = CharType.NONE;

    for (int cPos = 0; cPos < len; ++cPos) {
      final char c = text.charAt(cPos);
      
      final CharType curCharType = getCharType(c);

      if (!curCharType.equals(lastCharType)) {
        result.append(curCharType.getChar());
      }

      lastCharType = curCharType;
    }

    return result.toString();
  }

  private static final CharType getCharType(char c) {
    CharType result = null;

    if (Character.isWhitespace(c) || c == '.') {
      result = CharType.WHITE;
    }
    else if (Character.isDigit(c)) {
      result = CharType.DIGIT;
    }
    else if (Character.isUpperCase(c)) {
      result = CharType.UPPER;
    }
    else if (Character.isLowerCase(c)) {
      result = CharType.LOWER;
    }
    else {
      result = CharType.OTHER;
    }

    return result;
  }
}
