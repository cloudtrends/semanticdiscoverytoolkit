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
package org.sd.token;


import java.util.Iterator;
import org.sd.xml.DataProperties;
import org.sd.util.tree.Tree;

/**
 * Utilities for working with tokens.
 * <p>
 * @author Spence Koehler
 */
public class TokenUtil {
  
  /**
   * Compute the minimum number of tokens to be generated through the tokenizer.
   */
  public static int minTokenCount(Tokenizer tokenizer) {
    return minTokenCount(tokenizer, 0);
  }

  /**
   * Compute the minimum number of tokens to be generated through the tokenizer,
   * but don't keep counting beyond maxCap if maxCap > 0.
   */
  public static int minTokenCount(Tokenizer tokenizer, int maxCap) {
    int result = 0;

    for (Token token = tokenizer.getToken(0); token != null; token = token.getNextToken()) {
      token = largestRevisedToken(token);
      ++result;

      if (maxCap > 0 && result >= maxCap) break;
    }

    return result;
  }

  /**
   * Compute the maximum number of tokens to be generated through the tokenizer.
   */
  public static int maxTokenCount(Tokenizer tokenizer) {
    return maxTokenCount(tokenizer, 0);
  }

  /**
   * Compute the maximum number of tokens to be generated through the tokenizer,
   * but don't keep counting beyond maxCap if maxCap > 0.
   */
  public static int maxTokenCount(Tokenizer tokenizer, int maxCap) {
    int result = 0;

    for (Token token = tokenizer.getToken(0); token != null; token = token.getNextToken()) {
      token = smallestRevisedToken(token);
      ++result;

      if (maxCap > 0 && result >= maxCap) break;
    }

    return result;
  }

  /**
   * Get the largest revision of the given token (including the token itself).
   */
  public static Token largestRevisedToken(Token token) {
    Token result = token;
    int maxLen = token.getLength();

    for (token = token.getRevisedToken(); token != null; token = token.getRevisedToken()) {
      int curLen = token.getLength();
      if (curLen > maxLen) {
        result = token;
        maxLen = curLen;
      }
    }

    return result;
  }

  /**
   * Get the largest revision of the given token (including the token itself).
   */
  public static Token smallestRevisedToken(Token token) {
    Token result = token;
    int minLen = token.getLength();

    for (token = token.getRevisedToken(); token != null; token = token.getRevisedToken()) {
      int curLen = token.getLength();
      if (curLen < minLen) {
        result = token;
        minLen = curLen;
      }
    }

    return result;
  }


  public static void main(String[] args) {
    final DataProperties dataProperties = new DataProperties(args);
    args = dataProperties.getRemainingArgs();

    final StandardTokenizerOptions tokenizerOptions = new StandardTokenizerOptions(dataProperties);

    for (String arg : args) {
      final Tree<Token> tokens = StandardTokenizerFactory.fullTokenization(arg, tokenizerOptions);

      System.out.println("\nFull Tokenization of '" + arg + "':\n");
      for (Iterator<Tree<Token>> iter = tokens.iterator(Tree.Traversal.DEPTH_FIRST); iter.hasNext(); ) {
        final Tree<Token> curNode = iter.next();
        for (int indentPos = 0; indentPos < curNode.depth(); ++indentPos) {
          System.out.print(' ');
        }
        System.out.println(curNode.getData().toString());
      }
    }
  }
}
