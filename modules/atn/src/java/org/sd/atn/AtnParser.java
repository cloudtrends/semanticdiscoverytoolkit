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
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.sd.token.Token;
import org.sd.token.Tokenizer;
import org.sd.util.Usage;
import org.sd.xml.DataProperties;
import org.sd.xml.DomElement;

/**
 * Controlling class for parsing input tokenizer text according to a grammar.
 * <p>
 * This is a Phrase Parser implementation using principles of an Augmented
 * Transition Network (ATN). It is capable of computing all potential parses
 * of input according to the grammar.
 *
 * @author Spence Koehler
 */
@Usage(notes =
       "Controlling class for parsing input tokenizer text according to a grammar.\n" +
       "\n" +
       "This is a Phrase Parser implementation using principles of an Augmented\n" +
       "Transition Network (ATN). It is capable of computing all potential parses\n" +
       "of input according to the grammar."
  )
public class AtnParser {
  
  private AtnGrammar grammar;
  public AtnGrammar getGrammar() {
    return grammar;
  }

  /**
   * Construct a parser for the given grammar.
   */
  public AtnParser(DomElement grammar, ResourceManager resourceManager) {
    this.grammar = new AtnGrammar(grammar, resourceManager);
  }

  /**
   * Construct a parser for the given grammar.
   */
  AtnParser(AtnGrammar grammar) {
    this.grammar = grammar;
  }

  /**
   * Parse the from the tokenizer's first token according to the options.
   */
  public AtnParseResult parse(Tokenizer tokenizer, AtnParseOptions options, Set<Integer> stopList,
                              DataProperties overrides, AtomicBoolean die) {
    return parse(tokenizer.getToken(0), options, stopList, overrides, die);
  }

  /**
   * Parse starting with the given token and options.
   */
  public AtnParseResult parse(Token firstToken, AtnParseOptions options, Set<Integer> stopList,
                              DataProperties overrides, AtomicBoolean die) {
    final AtnParseResult result = buildParseResult(firstToken, firstToken.getStartIndex(), options, stopList, overrides, die);

    // Compute at least the first parse now.
    result.continueParsing();

    return result;
  }

  /**
   * Seek a valid parse starting with the first token.
   * 
   * If starting with the token fails to find a valid full parse, then
   * skip a word (smallest token) and try again until a valid full parse
   * is found or the input is exhausted.
   *
   * @returns The first valid full parse or null
   */
  public AtnParseResult seekParse(Tokenizer tokenizer, AtnParseOptions options, Set<Integer> stopList,
                                  DataProperties overrides, AtomicBoolean die) {
    return seekParse(tokenizer.getToken(0), options, stopList, overrides, die);
  }

  /**
   * Seek a valid parse starting with the first token.
   * 
   * If starting with the token fails to find a valid full parse, then
   * skip a word (smallest token) and try again until a valid full parse
   * is found or the input is exhausted.
   *
   * @returns The first valid full parse or null
   */
  public AtnParseResult seekParse(Token firstToken, AtnParseOptions options, Set<Integer> stopList,
                                  DataProperties overrides, AtomicBoolean die) {
    if (firstToken == null) return null;

    // //NOTE: when seeking, must be able to leave unconsumed text.
    // if (options.getConsumeAllText()) {
    //   // make a copy w/out consume all text
    //   options = new AtnParseOptions(options);
    //   options.setConsumeAllText(false);
    // }

    if (AtnState.getTraceFlow()) {
      System.out.println("\tAtnParser seeking from firstToken=" + firstToken);
    }

    final int seekStartIndex = firstToken.getStartIndex();
    AtnParseResult result = buildParseResult(firstToken, seekStartIndex, options, stopList, overrides, die);
    result.continueParsing();

    while (result.getNumParses() == 0 && !options.getConsumeAllText()) {
      firstToken = getSmallestToken(firstToken).getNextToken();
      if (firstToken == null) break;

      if (AtnState.getTraceFlow()) {
        System.out.println("\tAtnParser re-seeking from firstToken=" + firstToken);
      }

      result = buildParseResult(firstToken, seekStartIndex, options, stopList, overrides, die);
      result.continueParsing();
    }

    return result.getNumParses() > 0 ? result : null;
  }

  /**
   * Seek the next parse after the given parse.
   */
  public AtnParseResult seekNextParse(AtnParse lastParse, AtnParseOptions options, Set<Integer> stopList,
                                      DataProperties overrides, AtomicBoolean die) {
    return
      (!options.getConsumeAllText() && lastParse.getNextToken() != null) ?
      seekParse(lastParse.getNextToken(), options, stopList, overrides, die) :
      null;
  }

  /**
   * Seek all (first) parses from the tokenizer's text.
   */
  public List<AtnParseResult> seekAll(Tokenizer tokenizer, AtnParseOptions options, Set<Integer> stopList,
                                      DataProperties overrides, AtomicBoolean die) {
    final List<AtnParseResult> result = new ArrayList<AtnParseResult>();

    AtnParse parse = null;
    for (AtnParseResult parseResult = seekParse(tokenizer, options, stopList, overrides, die);
         parseResult != null;
         parseResult = doSeekNextParse(parse, options, stopList, overrides, die)) {
      int numParses = parseResult.getNumParses();
      if (numParses > 0) {
        int numSelectedParses = 0;

        // don't add completely subsumed parses
        //
        // NOTE: this happens when we've incremented over tokens after a
        //       successful parse and we get a parse similar to a prior where
        //       the initial tokens were optional or skipped, so the new parse
        //       is a subset of prior parses and is ignored.

        for (int parseNum = 0; parseNum < numParses; ++parseNum) {
          parse = parseResult.getParse(parseNum);
          boolean keeper = false;
          if (parse != null && parse.getSelected()) {
            keeper = true;
            for (AtnParseResult existingResult : result) {
              final int[] parsedRange = existingResult.getParsedRange();
              if (parsedRange != null) {
                if (Token.encompasses(parsedRange[0], parsedRange[1], parse.getStartIndex(), parse.getEndIndex())) {
                  parse.setSelected(false);
                  keeper = false;
                  break;
                }
              }
            }
            if (keeper) ++numSelectedParses;
          }
        }

        if (numSelectedParses > 0) {
          result.add(parseResult);
        }
      }
    }

    return result;
  }

  /**
   * Wrapper for calling seekNextParse based on prior parse (success).
   */
  private final AtnParseResult doSeekNextParse(AtnParse parse, AtnParseOptions options, Set<Integer> stopList,
                                               DataProperties overrides, AtomicBoolean die) {
    AtnParseResult result = null;

    if (parse != null) {
      result = seekNextParse(parse, options, stopList, overrides, die);
    }

    return result;
  }

  private final AtnParseResult buildParseResult(Token firstToken, int seekStartIndex, AtnParseOptions options,
                                                Set<Integer> stopList, DataProperties overrides, AtomicBoolean die) {
    return new AtnParseResult(grammar, firstToken, seekStartIndex, options, stopList, overrides, die);
  }

  private final Token getSmallestToken(Token firstToken) {
    Token result = null;

    if (grammar.isImmutable(firstToken)) {
      result = firstToken;
    }
    else {
      result = firstToken.getTokenizer().getSmallestToken(firstToken.getStartIndex());
    }

    return result;
  }
}
