/*
    Copyright 2011 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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


import java.util.Map;
import org.sd.atn.ResourceManager;
import org.sd.token.Normalizer;
import org.sd.token.Token;
import org.sd.util.Usage;
import org.sd.xml.DomElement;

/**
 * Classifier for assigning a classification to every token encountered.
 * <p>
 * This is useful, for example, for picking up all remaining tokens, regardless
 * of their content, at the end of a parse or for explicitly allowing "unknown"
 * tokens.
 *
 * @author Spence Koehler
 */
@Usage(notes =
       "org.sd.atn.RoteListClassifier for classifying any token\n" +
       "(as a grab bag or explicit unknown, for example).\n" +
       "\n" +
       "Attributes:\n" +
       "\n" +
       "  feature -- (optional, default=<null>) specifies name of feature holding\n" +
       "             the token text.\n" +
       "  minLength -- (optional, default=0 [unbounded]) specifies the minimum\n" +
       "               acceptable input text length (e.g. at least 2 digits).\n" +
       "  maxLength -- (optional, default=0 [unbounded]) specifies the maximum\n" +
       "               acceptable input text length (e.g. at most 10 digits)." +
       "<tokenType feature='optionalFeatureOnMatch' minLength='A' maxLength='B'>\n" +
       "  <jclass>org.sd.atn.AnyTokenClassifier</jclass>\n" +
       "  <stopwords>\n" +
       "    <terms.../>\n" +
       "    ...\n" +
       "    <textfile.../>\n" +
       "    ...\n" +
       "    <regexes.../>\n" +
       "    ...\n" +
       "    <classifiers>\n" +
       "      <classifier>...</classifier>\n" +
       "      ...\n" +
       "    </classifiers>\n" +
       "  </stopwords>\n" +
       "</tokenType>"
  )
public class AnyTokenClassifier extends RoteListClassifier {
  
  private String featureName;
  private int minLength;
  private int maxLength;

  public AnyTokenClassifier(DomElement classifierIdElement, ResourceManager resourceManager, Map<String, Normalizer> id2Normalizer) {
    super(classifierIdElement, resourceManager, id2Normalizer);

    // ignore any maxWordCount specified by the element and set to 1
    getTokenClassifierHelper().setMaxWordCount(1);

    this.featureName = classifierIdElement.getAttributeValue("feature", null);

    this.minLength = classifierIdElement.getAttributeInt("minLength", 0);
    this.maxLength = classifierIdElement.getAttributeInt("maxLength", 0);
  }

  public boolean doClassify(Token token, AtnState atnState) {
    boolean result = true;

    if (minLength > 0 || maxLength > 0) {
      final String text = token.getText();
      final int textLen = text.length();

      if ((minLength > 0 && textLen < minLength) ||
          (maxLength > 0 && textLen > maxLength)) {
        result = false;
      }
    }

    if (result && doClassifyStopword(token, atnState)) {
      result = false;
    }
    
    if (result && featureName != null && !"".equals(featureName)) {
      token.setFeature(featureName, token.getText(), this);
    }

    return result;
  }
}
