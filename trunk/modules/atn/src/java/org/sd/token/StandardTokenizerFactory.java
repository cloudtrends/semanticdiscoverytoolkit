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


import java.util.ArrayList;
import java.util.List;
import org.sd.util.tree.Tree;

/**
 * A factory for StandardTokenizer instances and tokens.
 * <p>
 * @author Spence Koehler
 */
public class StandardTokenizerFactory {
  
  /**
   * Default StandardTokenizerOptions.
   */
  public static final StandardTokenizerOptions DEFAULT_OPTIONS = new StandardTokenizerOptions();

  /**
   * Get a StandardTokenizer using the Default StandardTokenizerOptions.
   */
  public static StandardTokenizer getTokenizer(String text) {
    return getTokenizer(text, DEFAULT_OPTIONS);
  }

  /**
   * Get a StandardTokenizer for the given text using the given options.
   */
  public static StandardTokenizer getTokenizer(String text, StandardTokenizerOptions options) {
    return new StandardTokenizer(text, options);
  }

  /**
   * Get the first token in the text using Default StandardTokenizerOptions.
   * 
   * Note that this token will contain the newly built tokenizer instance for
   * further tokenization of the text.
   */
  public static Token getFirstToken(String text) {
    return getFirstToken(text, DEFAULT_OPTIONS);
  }

  /**
   * Get the first token in the text using the given options.
   * 
   * Note that this token will contain the newly built tokenizer instance for
   * further tokenization of the text.
   */
  public static Token getFirstToken(String text, StandardTokenizerOptions options) {
    StandardTokenizer tokenizer = getTokenizer(text, options);
    return tokenizer.getToken(0);
  }

  /**
   * Get the primary token strings from the text using Default StandardTokenizerOptions.
   */
  public static String[] tokenize(String text) {
    return tokenize(text, DEFAULT_OPTIONS);
  }

  /**
   * Get the primary token strings from the text using the given options.
   */
  public static String[] tokenize(String text, StandardTokenizerOptions options) {
    final List<String> result = new ArrayList<String>();

    for (Token token = getFirstToken(text, options); token != null; token = token.getNextToken()) {
      result.add(token.getText());
    }

    return result.toArray(new String[result.size()]);
  }

  /**
   * Create a tree of tokens where the root contains the full input text (as
   * a token), the second level contains primary tokenizations (according to
   * Default StandardTokenizerOptions,) and the third level contains the
   * token revisions for each of their (parent) primary tokenizations.
   */
  public static Tree<Token> fullTokenization(String text) {
    return fullTokenization(text, DEFAULT_OPTIONS);
  }

  /**
   * Create a tree of tokens where the root contains the full input text (as
   * a token), the second level contains primary tokenizations (according to
   * the given options,) and the third level contains the token revisions for
   * each of their (parent) primary tokenizations.
   */
  public static Tree<Token> fullTokenization(String text, StandardTokenizerOptions options) {
    final StandardTokenizer tokenizer = getTokenizer(text, options);

    final Tree<Token> result = new Tree<Token>(new Token(tokenizer, text, 0, options.getRevisionStrategy(), 0, 0, tokenizer.getWordCount(), -1));

    for (Token primaryToken = tokenizer.getToken(0); primaryToken != null; primaryToken = tokenizer.getNextToken(primaryToken)) {
      final Tree<Token> primaryTokenNode = result.addChild(primaryToken);

      for (Token revisedToken = tokenizer.revise(primaryToken); revisedToken != null; revisedToken = tokenizer.revise(revisedToken)) {
        primaryTokenNode.addChild(revisedToken);
      }
    }

    return result;
  }

  /**
   * Create a tree of token strings where the root contains the full input
   * text, the second level contains primary tokenizations (according to
   * the default options,) and the third level contains the token revision
   * texts for each of their (parent) primary tokenizations.
   */
  public static Tree<String> fullyTokenize(String text) {
    return fullyTokenize(text, DEFAULT_OPTIONS);
  }

  /**
   * Create a tree of token strings where the root contains the full input
   * text, the second level contains primary tokenizations (according to
   * the given options,) and the third level contains the token revision
   * texts for each of their (parent) primary tokenizations.
   */
  public static Tree<String> fullyTokenize(String text, StandardTokenizerOptions options) {
    final Tree<String> result = new Tree<String>(text);

    for (Token primaryToken = getFirstToken(text, options); primaryToken != null; primaryToken = primaryToken.getNextToken()) {
      final Tree<String> primaryTokenNode = result.addChild(primaryToken.getText());

      for (Token revisedToken = primaryToken.getRevisedToken(); revisedToken != null; revisedToken = revisedToken.getRevisedToken()) {
        primaryTokenNode.addChild(revisedToken.getText());
      }
    }

    return result;
  }

  /**
   * Get all of the revision texts of the given token, including the token
   * itself.
   */
  public static List<String> fullyRevise(Token token) {
    final List<String> result = new ArrayList<String>();

    while (token != null) {
      result.add(token.getText());
      token = token.getRevisedToken();
    }

    return result;
  }
}
