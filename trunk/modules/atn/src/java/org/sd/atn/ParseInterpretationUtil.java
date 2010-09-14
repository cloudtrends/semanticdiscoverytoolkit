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
import java.util.Map;
import org.sd.token.CategorizedToken;
import org.sd.token.Feature;
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

    if (parseTreeNode.hasAttributes()) {
      final Map<String, Object> parseAttributes = parseTreeNode.getAttributes();
      final CategorizedToken cToken = (CategorizedToken)parseAttributes.get(AtnStateUtil.TOKEN_KEY);
      if (cToken.token.hasFeatures()) {
        final Object featureValue = cToken.token.getFeatureValue(featureKey, null, ParseInterpretation.class);
        if (featureValue != null) {
          result = (ParseInterpretation)featureValue;
        }
      }
    }

    if (result == null && parseTreeNode.numChildren() == 1) {
      result = getFirstInterpretation(context, parseTreeNode.getChildren().get(0), featureKey);
    }

    return result;
  }

  /**
   * Get the first interpretation with the given featureKey from the
   * tokens under the given parseTreeNode or null.
   */
  public static List<ParseInterpretation> getInterpretations(AtnParse context, Tree<String> parseTreeNode, String featureKey) {
    List<ParseInterpretation> result = null;

    if (parseTreeNode.hasAttributes()) {
      final Map<String, Object> parseAttributes = parseTreeNode.getAttributes();
      final CategorizedToken cToken = (CategorizedToken)parseAttributes.get(AtnStateUtil.TOKEN_KEY);
      if (cToken.token.hasFeatures()) {
        final List<Feature> features = cToken.token.getFeatures(featureKey, null, ParseInterpretation.class);
        if (features != null) {
          result = new ArrayList<ParseInterpretation>();
          for (Feature feature : features) {
            final Object value = feature.getValue();
            if (value != null) {
              result.add((ParseInterpretation)value);
            }
          }
        }
      }
    }
          
    if (result == null && parseTreeNode.numChildren() == 1) {
      result = getInterpretations(context, parseTreeNode.getChildren().get(0), featureKey);
    }

    return result;
  }
}
