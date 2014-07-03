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
package org.sd.token;


/**
 * Auxiliary for testing tokenizers.
 * <p>
 * @author Spence Koehler
 */
public class TokenizeTest {
  
  public final String name;
  public final String text;
  public final String[] expectedPrimaryTokens;
  public final String[][] expectedSecondaryTokens;
  public final String[] expectedPrimaryFeatures;
  public final Tokenizer tokenizer;

  public TokenizeTest(String name, String text, StandardTokenizerOptions options, String[] expectedPrimaryTokens, String[][] expectedSecondaryTokens) {
    this.name = name;
    this.text = text;
    this.expectedPrimaryTokens = expectedPrimaryTokens;
    this.expectedSecondaryTokens = expectedSecondaryTokens;
    this.expectedPrimaryFeatures = null;

    this.tokenizer = StandardTokenizerFactory.getTokenizer(text, options);
  }

  public TokenizeTest(String name, Tokenizer tokenizer, String[] expectedPrimaryTokens, String[][] expectedSecondaryTokens) {
    this(name, tokenizer, expectedPrimaryTokens, expectedSecondaryTokens, null);
  }

  public TokenizeTest(String name, Tokenizer tokenizer, String[] expectedPrimaryTokens, String[][] expectedSecondaryTokens, String[] expectedPrimaryFeatures) {
    this.name = name;
    this.text = tokenizer.getText();
    this.expectedPrimaryTokens = expectedPrimaryTokens;
    this.expectedSecondaryTokens = expectedSecondaryTokens;
    this.expectedPrimaryFeatures = expectedPrimaryFeatures;
    this.tokenizer = tokenizer;
  }

  public boolean runTest() {
    boolean pass = true;

    int numPrimaryTokens = 0;
    int numSecondaryTokens = 0;
    int expectedSecondaryTokenCount = 0;

    for (Token primaryToken = tokenizer.getToken(0); primaryToken != null; primaryToken = tokenizer.getNextToken(primaryToken)) {
      if (expectedPrimaryTokens == null) {
        System.out.println(name + ": Primary token #" + numPrimaryTokens + " = " + primaryToken);
        if (expectedPrimaryFeatures != null && expectedPrimaryFeatures[numPrimaryTokens] != null) {
          System.out.println("\tfeature(" + expectedPrimaryFeatures[numPrimaryTokens] + ")=" + primaryToken.getFeatureValue(expectedPrimaryFeatures[numPrimaryTokens], null));
        }
      }
      else {
        if (!expectedPrimaryTokens[numPrimaryTokens].equals(primaryToken.getText())) {
          System.out.println(name + ": Primary token #" + numPrimaryTokens + " mismatch. Expected=" +
                             expectedPrimaryTokens[numPrimaryTokens] + " got=" + primaryToken.getText());
          pass = false;
        }
        if (expectedPrimaryFeatures != null && expectedPrimaryFeatures[numPrimaryTokens] != null) {
          if (primaryToken.getFeatureValue(expectedPrimaryFeatures[numPrimaryTokens], null) == null) {
            System.out.println(name + ": Primary token #" + numPrimaryTokens + " feature(" + expectedPrimaryFeatures[numPrimaryTokens] + ") missing.");
            pass = false;
          }
        }
        if (!pass) break;
      }

      if (!pass) break;

      int numRevisedTokens = 0;
      for (Token revisedToken = tokenizer.revise(primaryToken); revisedToken != null; revisedToken = tokenizer.revise(revisedToken)) {
        if (expectedSecondaryTokens == null) {
          System.out.println(name + ": Secondary token #" + numPrimaryTokens + "/" + numRevisedTokens + " = " + revisedToken);
          if (revisedToken.hasFeatures()) System.out.println("\t" + revisedToken.getFeatureValue(NamedEntitySegmentFinder.ENTITY_LABEL, null));
        }
        else {
          if (!expectedSecondaryTokens[numPrimaryTokens][numRevisedTokens].equals(revisedToken.getText())) {
            System.out.println(name + ": Secondary token #" + numPrimaryTokens + "/" + numRevisedTokens + " mismatch.");
            pass = false;
            break;
          }
        }
        ++numRevisedTokens;
        ++numSecondaryTokens;
      }

      if (!pass) break;

      if (expectedSecondaryTokens != null && expectedSecondaryTokens.length > numPrimaryTokens) {
        expectedSecondaryTokenCount += expectedSecondaryTokens[numPrimaryTokens].length;
      }

      ++numPrimaryTokens;
    }
      
    if (pass) {
      if (expectedPrimaryTokens != null && expectedPrimaryTokens.length != numPrimaryTokens) {
        System.out.println(name + ": Primary token count mismatch.");
        pass = false;
      }
      if (expectedSecondaryTokens != null && expectedSecondaryTokenCount != numSecondaryTokens) {
        System.out.println(name + ": Secondary token count mismatch.");
        pass = false;
      }
    }

    return pass;
  }
}
