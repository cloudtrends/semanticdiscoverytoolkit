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
import org.sd.token.CategorizedToken;
import org.sd.util.tree.Tree;

/**
 * Utilities for working with parse interpretations.
 * <p>
 * @author Spence Koehler
 */
public class ParseInterpretationUtil {
  
  /**
   * Get the first interpretation with the given featureKey from the
   * tokens under the given parseTreeNode or null.
   */
  public static ParseInterpretation getFirstInterpretation(AtnParse context, Tree<String> parseTreeNode, String featureKey) {
    ParseInterpretation result = null;

    final List<CategorizedToken> cTokens = context.getCategorizedTokens(parseTreeNode);
    for (CategorizedToken cToken : cTokens) {
      final Object featureValue = cToken.token.getFeatureValue(featureKey, null, ParseInterpretation.class);
      if (featureValue != null) {
        result = (ParseInterpretation)featureValue;
        break;
      }
    }

    return result;
  }

  /**
   * Get the first interpretation with the given featureKey from the
   * tokens under the given parseTreeNode or null.
   */
  public static List<ParseInterpretation> getInterpretations(AtnParse context, Tree<String> parseTreeNode, String featureKey) {
    List<ParseInterpretation> result = null;

    final List<CategorizedToken> cTokens = context.getCategorizedTokens(parseTreeNode);
    for (CategorizedToken cToken : cTokens) {
      final Object featureValue = cToken.token.getFeatureValue(featureKey, null, ParseInterpretation.class);
      if (featureValue != null) {
        if (result == null) result = new ArrayList<ParseInterpretation>();
        result.add((ParseInterpretation)featureValue);
      }
    }

    return result;
  }
}
