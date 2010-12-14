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


import java.util.HashMap;
import java.util.Map;
import org.sd.util.InputContext;

/**
 * A standard English-language tokenizer.
 * <p>
 * @author Spence Koehler
 */
public class StandardTokenizer implements Tokenizer {
  
  protected String text;

  private InputContext inputContext;
  public InputContext getInputContext() {
    return inputContext;
  }
  public void setInputContext(InputContext inputContext) {
    this.inputContext = inputContext;
  }


  /**
   * Maps text positions to breaks. Any unmapped positions is assumed
   * to be a Break.NO_BREAK.
   */
  private Map<Integer, Break> _pos2break;
  private Object pos2breakLock = new Object();
  private boolean pos2breakInit = false;

  private boolean computedWordCount;
  private int _wordCount;

  private StandardTokenizerOptions options;
  public StandardTokenizerOptions getOptions() {
    return options;
  }
  protected void setOptions(StandardTokenizerOptions options) {
    this.options = options;
  }

  /**
   * Construct with the given text and options.
   */
  public StandardTokenizer(String text, StandardTokenizerOptions options) {
    this.text = text;
    this.options = options;
    this._pos2break = null;
    this.computedWordCount = false;
    this._wordCount = 0;
  }


  /**
   * Reset this instance such that breaks will be recomputed.
   */
  protected void reset() {
    this._pos2break = null;
    this.computedWordCount = false;
  }

  /**
   * Default break initialization. Extenders may override.
   */
  protected Map<Integer, Break> createBreaks() {
    final Map<Integer, Break> result = new HashMap<Integer, Break>();

    int increment = 1;
    for (int charPos = 0; charPos < text.length(); charPos += increment) {
      Break curBreak = Break.NO_BREAK;
      increment = 1;  // reset

      final int curChar = text.codePointAt(charPos);
      if (Character.isWhitespace(curChar)) {
        curBreak = options.getWhitespaceBreak();
      }
      else if (Character.isLetterOrDigit(curChar)) {
        if (charPos > 0 && Character.isLetterOrDigit(text.codePointAt(charPos - 1))) {
          // previous char was also a letter or digit
          final int prevChar = text.codePointAt(charPos - 1);

          if (Character.isDigit(curChar)) {
            if (Character.isDigit(prevChar)) {
              // digit digit
              curBreak = Break.NO_BREAK;
            }
            else if (Character.isUpperCase(prevChar)) {
              // upper digit
              curBreak = options.getUpperDigitBreak();
            }
            else {
              // lower digit
              curBreak = options.getLowerDigitBreak();
            }
          }
          else if (Character.isUpperCase(curChar)) {
            if (Character.isDigit(prevChar)) {
              // digit upper
              curBreak = options.getDigitUpperBreak();
            }
            else if (Character.isUpperCase(prevChar)) {
              // upper upper
              curBreak = Break.NO_BREAK;
            }
            else {
              // lower upper
              curBreak = options.getLowerUpperBreak();
            }
          }
          else {  // Character.isLower(curChar)
            if (Character.isDigit(prevChar)) {
              // digit lower
              curBreak = options.getDigitLowerBreak();
            }
            else if (Character.isUpperCase(prevChar)) {
              // upper lower
              curBreak = options.getUpperLowerBreak();
            }
            else {
              // lower lower
              curBreak = Break.NO_BREAK;
            }
          }
        }
        else {
          // first letter or digit is always non-breaking.
          curBreak = Break.NO_BREAK;
        }
      }
      else {

        // char is punctuation or a symbol
        if (curChar == '-') {
          if (charPos + 1 < text.length() && text.codePointAt(charPos + 1) == '-') {
            // there is more than one consecutive dash

            // check for 3+ dashes, treat all as hard breaks
            if (charPos + 2 < text.length() && text.codePointAt(charPos + 2) == '-') {
              while (charPos + increment < text.length() && text.codePointAt(charPos + increment) == '-') {
                setBreak(result, charPos + increment, Break.SINGLE_WIDTH_HARD_BREAK);
                ++increment;
              }
              curBreak = Break.SINGLE_WIDTH_HARD_BREAK;
            }
            else {
              // just 2 dashes
              if ((charPos > 0 && Character.isLetterOrDigit(text.codePointAt(charPos - 1))) && (charPos + 2 < text.length() && Character.isLetterOrDigit(text.codePointAt(charPos + 2)))) {
                // embedded double dash
                curBreak = options.getEmbeddedDoubleDashBreak();
              }
              else {
                // non-embedded double dash
                curBreak = options.getNonEmbeddedDoubleDashBreak();
              }

              // apply the break to the second dash
              final Break secondDashBreak = (curBreak == Break.SINGLE_WIDTH_SOFT_BREAK) ? Break.NO_BREAK : curBreak;
              setBreak(result, charPos + 1, secondDashBreak);

              // skip the second dash
              ++increment;
            }
          }
          else {
            // this is a single dash
            if (charPos > 0 && !Character.isWhitespace(text.codePointAt(charPos - 1))) {
              if (charPos + 1 < text.length() && !Character.isWhitespace(text.codePointAt(charPos + 1))) {
                // embedded dash
                curBreak = options.getEmbeddedDashBreak();
              }
              else {
                // left-bordered dash
                curBreak = options.getLeftBorderedDashBreak();
              }
            }
            else if (charPos + 1 < text.length() && !Character.isWhitespace(text.codePointAt(charPos + 1))) {
              // right-bordered dash
              curBreak = options.getRightBorderedDashBreak();
            }
            else {
              // free-standing dash
              curBreak = options.getFreeStandingDashBreak();
            }
          }
        }
        else if (charPos + 1 < text.length() && Character.isLetterOrDigit(text.codePointAt(charPos + 1))) {
          // symbol immediately precedes a non-white, non-symbol char as part of a token

          if (charPos > 0 && Character.isLetterOrDigit(text.codePointAt(charPos - 1)) && curChar != '/' && curChar != '\\') {
            // symbol is embedded between non-white, non-symbol chars e.g. "don't" or "3.14"
            if (isSymbol(curChar)) {
              curBreak = options.getSymbolBreak();
            }
            else {
              //e.g. punctuation
              curBreak = Break.NO_BREAK;
            }
          }
          else if (curChar == '"' || curChar == '(' || curChar == '[' || curChar == '{' || curChar == '<' || curChar == '\'') {
            // symbol is open quote, paren, or slash
            curBreak = options.getQuoteAndParenBreak();
          }
          else if (curChar == '/' || curChar == '\\') {
            curBreak = options.getSlashBreak();
          }
          else {
            // e.g. "$24.99"
            curBreak = options.getSymbolBreak();
          }
        }
        else if (curChar == '%' && charPos > 0 && Character.isDigit(text.codePointAt(charPos - 1))) {
          // e.g. "99.9%"
          curBreak = Break.NO_BREAK;
        }
        else if (curChar == '/' || curChar == '\\') {
          curBreak = options.getSlashBreak();
        }
        else if (isPunctuation(curChar)) {
          //todo: apply other heuristics for recognizing a punctuation char as a part of a token
          curBreak = Break.SINGLE_WIDTH_HARD_BREAK;
        }
        else if (isSymbol(curChar)) {
          // keep other symbols like copyright, registered trademark, mathematical symbols, etc.
          curBreak = options.getSymbolBreak();
        }
      }

      // set curBreak
      setBreak(result, charPos, curBreak);
    }
//...
    //note: any non-letter-digit-or-white immediately following a break repeats the break


    return result;
  }


  public static final boolean isPunctuation(int codePoint) {
    boolean result = false;

    final int charType = Character.getType(codePoint);
    result = (charType == Character.CONNECTOR_PUNCTUATION ||
              charType == Character.DASH_PUNCTUATION ||
              charType == Character.START_PUNCTUATION ||
              charType == Character.END_PUNCTUATION ||
              charType == Character.INITIAL_QUOTE_PUNCTUATION ||
              charType == Character.FINAL_QUOTE_PUNCTUATION ||
              charType == Character.OTHER_PUNCTUATION);

    return result;
  }


  public static final boolean isSymbol(int codePoint) {
    boolean result = false;

    final int charType = Character.getType(codePoint);
    result = (charType == Character.MATH_SYMBOL ||
              charType == Character.CURRENCY_SYMBOL ||
              charType == Character.MODIFIER_SYMBOL ||
              charType == Character.OTHER_SYMBOL);

    return result;
  }


  protected void setBreak(Map<Integer, Break> pos2break, int pos, Break theBreak) {
    if (theBreak != null && theBreak != Break.NO_BREAK) {
      pos2break.put(pos, theBreak);
    }
    else {
      pos2break.remove(pos);
    }
  }

  public boolean initializing() {
    return pos2breakInit;
  }

  /**
   * Get the pos2break map, initializing through CreateBreaks if necessary.
   */
  protected Map<Integer, Break> getPos2Break() {
    if (pos2breakInit) return null;

    synchronized (pos2breakLock) {
      if (this._pos2break == null) {
        pos2breakInit = true;
        this._pos2break = createBreaks();
        pos2breakInit = false;
      }
    }
    return this._pos2break;
  }


  /**
   * Get the non-null break instance for the text at the given position.
   */
  public Break getBreak(int pos) {
    Break result = null;

    if (pos == text.length()) {
      result = Break.ZERO_WIDTH_HARD_BREAK;
    }
    else {
      final Map<Integer, Break> pos2break = getPos2Break();
      result = pos2break.get(pos);
    }

    return (result == null) ? Break.NO_BREAK : result;
  }

  /**
   * Change the break at the given to position to theBreak.
   * 
   * Note that this method presents a way to change breaks initialized through CreateBreaks
   * and is not to be used by the implementation of CreateBreaks!
   *
   * @returns The previous break at the position.
   */
  public Break changeBreak(int pos, Break theBreak) {
    Break result = getBreak(pos);
    final Map<Integer, Break> pos2break = getPos2Break();

    if (theBreak == null || theBreak == Break.NO_BREAK) {
      pos2break.remove(pos);
    }
    else {
      pos2break.put(pos, theBreak);
    }

    return result;
  }


  public Token getToken(int startPosition) {
    startPosition = skipImmediateBreaks(startPosition);
    return doGetToken(options.getRevisionStrategy(), startPosition, 0);
  }

  public Token getNextSmallestToken(Token token) {
    int startPosition = findEndBreakForward(token.getEndIndex(), false);
    if (startPosition < 0) startPosition = token.getEndIndex();
    return getSmallestToken(startPosition);
  }

  public Token getSmallestToken(int startPosition) {
    return doGetNextToken(startPosition, BreakType.SOFT, options.getRevisionStrategy(), 0, 0);
  }

  public String getPostDelim(Token token) {
    String result = null;

    //collect all break delims after the token
    final int startPos = token.getEndIndex();
    final int endPos = findEndBreakForward(startPos, false);

    if (endPos > startPos) {
      result = text.substring(startPos, endPos);
    }

    return result == null ? "" : result;
  }

  public String getPreDelim(Token token) {
    String result = null;

    //collect all break delims before the token
    final int endPos = token.getStartIndex();
    final int startPos = findEndBreakReverse(endPos);

    if (endPos > startPos && endPos > 0) {
      result = text.substring(Math.max(startPos, 0), endPos);
    }

    return result == null ? "" : result;
  }

  public boolean followsHardBreak(Token token) {
    boolean result = false;

    final int endPos = token.getStartIndex();
    final int startPos = findEndBreakReverse(endPos);

    if (endPos > startPos && startPos >= 0) {
      for (int curPos = startPos; curPos <= startPos; ++curPos) {
        final Break curBreak = getBreak(curPos);
        if (curBreak.isHard()) {
          result = true;
          break;
        }
      }
    }

    return result;
  }

  public Token revise(Token token) {
    if (token == null) return null;

    Token result = null;

    if (token.getRevisionStrategy() != TokenRevisionStrategy.LO && token.getRevisionStrategy() != TokenRevisionStrategy.SO) {
      // revising possible
      int nextRevisionNumber = token.getRevisionNumber() + 1;

      if (token.getRevisionStrategy() == TokenRevisionStrategy.LSL && token.getRevisionNumber() == 0) {
        // get shortest token
        final int endPosition = findNextBreak(token.getStartIndex() + 1, BreakType.SOFT);
        if (endPosition < token.getEndIndex()) {
          result = buildToken(token.getStartIndex(), endPosition, token.getRevisionStrategy(), nextRevisionNumber, token.getSequenceNumber(), 1);
        }
      }
      else if (token.getRevisionStrategy() == TokenRevisionStrategy.LS) {
        // get shorter token
        int endPosition = findPriorBreak(token.getEndIndex());
        if (endPosition > token.getStartIndex() && endPosition < token.getEndIndex()) {
          result = buildToken(token.getStartIndex(), endPosition, token.getRevisionStrategy(), nextRevisionNumber, token.getSequenceNumber(), token.getWordCount() - 1);
        }
      }
      else {
        // get longer token
        final Break curBreak = getBreak(token.getEndIndex());
        if (!curBreak.isHard()) {
          // can go longer as long as we don't revisit the longest when using LSL strategy
          // and as long as there isn't a hard break directly following the soft(s).
          int endBreakPos = findEndBreakForward(token.getEndIndex(), true);
          if (endBreakPos >= 0) {
            if (curBreak.getBWidth() == 0) ++endBreakPos;
            final int endPosition = findNextBreak(endBreakPos, BreakType.SOFT);
            if (!(token.getRevisionStrategy() == TokenRevisionStrategy.LSL && findEndBreakForward(endPosition, true) == -1)) {
              result = buildToken(token.getStartIndex(), endPosition, token.getRevisionStrategy(), nextRevisionNumber, token.getSequenceNumber(), token.getWordCount() + 1);
            }
          }
        }
      }
    }

    // make sure revision didn't generate the input token
    if (result != null && result.getEndIndex() == token.getEndIndex()) {
      result = null;
    }

    return result;
  }

  public Token getNextToken(Token token) {
    int startPosition = findEndBreakForward(token.getEndIndex(), false);
    if (startPosition < 0) startPosition = token.getEndIndex();
    return doGetToken(token.getRevisionStrategy(), startPosition, token.getSequenceNumber() + 1);
  }

  public Token getPriorToken(Token token) {
    throw new UnsupportedOperationException("Implement when needed.");
  }

  public String getText() {
    return text;
  }

  public int getWordCount() {
    if (!computedWordCount) {
      _wordCount = computeWordCount(0, text.length());
      computedWordCount = true;
    }
    return _wordCount;
  }

  public String getNextText(Token token) {
    int startPos = findEndBreakForward(token.getEndIndex(), false);
    if (startPos < 0) startPos = token.getEndIndex();
    return (startPos < text.length()) ? text.substring(startPos) : "";
  }

  public String getPriorText(Token token) {
    final int endPos = findEndBreakReverse(token.getStartIndex());
    return (endPos > 0) ? text.substring(0, endPos) : "";
  }

  public Token buildToken(int startPosition, int endPosition) {
    Token result = null;

    if (startPosition >= 0 && endPosition <= text.length()) {
      result = buildToken(startPosition, endPosition, options.getRevisionStrategy(),
                          0, -1, computeWordCount(startPosition, endPosition));
    }

    return result;
  }


  private int computeWordCount(int startIndex, int endIndex) {
    int result = 0;

    int priorBreakPos = startIndex;
    int breakPos = findNextBreak(startIndex, BreakType.SOFT);
    while (breakPos <= endIndex) {
      if (breakPos > priorBreakPos) ++result;

      if (breakPos >= endIndex) break;

      breakPos = findEndBreakForward(breakPos, false);
      priorBreakPos = breakPos;
      breakPos = findNextBreak(breakPos + 1, BreakType.SOFT);
    }

    return result;
  }

  private Token doGetToken(TokenRevisionStrategy revisionStrategy, int startPosition, int sequenceNumber) {
    final BreakType breakToFind = getBreakToFind(revisionStrategy);
    return doGetNextToken(startPosition, breakToFind, revisionStrategy, 0, sequenceNumber);
  }

  private BreakType getBreakToFind(TokenRevisionStrategy revisionStrategy) {
    BreakType breakToFind = BreakType.HARD;
    switch (revisionStrategy) {
      case LSL:
      case LO:
      case LS:
        breakToFind = BreakType.HARD;
        break;
      case SO:
      case SL:
        breakToFind = BreakType.SOFT;
        break;
    }
    return breakToFind;
  }

  private Token doGetNextToken(int startPosition, BreakType breakToFind, TokenRevisionStrategy revisionStrategy, int revisionNumber, int sequenceNumber) {
    Token result = null;

    int endPosition = findNextBreak(startPosition + 1, breakToFind);

    // verify that if breakToFind is hard, we back up over any immediate soft breaks
    if (breakToFind == BreakType.HARD) {
      endPosition = findEndBreakReverse(endPosition);
    }

    result = buildToken(startPosition, endPosition, revisionStrategy, revisionNumber, sequenceNumber, computeWordCount(startPosition, endPosition));

    return result;
  }

  private Token buildToken(int startPosition, int endPosition, TokenRevisionStrategy revisionStrategy, int revisionNumber, int sequenceNumber, int wordCount) {
    Token result = null;

    if (endPosition > startPosition) {
      final String tokenText = text.substring(startPosition, endPosition);
      result = new Token(this, tokenText, startPosition, revisionStrategy, revisionNumber, sequenceNumber, wordCount);
    }

    return result;
  }

  /**
   * Find the next break position at or after startPosition that agrees with
   * the given breakToFind.
   */
  private int findNextBreak(int startPosition, BreakType breakTypeToFind) {
    int result = text.length();

    final Map<Integer, Break> pos2break = getPos2Break();
    for (int pos = startPosition; pos < text.length(); ++pos) {
      final Break posBreak = pos2break.get(pos);
      if (posBreak != null) {
        if (posBreak.agreesWith(breakTypeToFind)) {
          result = pos;
          break;
        }
      }
    }

    return result;
  }

  private int findPriorBreak(int endPosition) {
    int result = -1;

    final Map<Integer, Break> pos2break = getPos2Break();
    for (int pos = endPosition - 1; pos >= 0; --pos) {
      final Break posBreak = pos2break.get(pos);
      if (posBreak != null) {
        if (posBreak.breaks()) {
          result = findEndBreakReverse(pos);
          break;
        }
      }
    }

    return result;
  }

  /**
   * Find the first non-break position from startPosition (inclusive).
   */
  private int skipImmediateBreaks(int startPosition) {
    int result = startPosition;
    final Map<Integer, Break> pos2break = getPos2Break();

    while (result < text.length()) {
      final Break posBreak = pos2break.get(result);

      if (posBreak != null && posBreak.breaks() && posBreak.getBWidth() > 0) {
        result += posBreak.getBWidth();
      }
      else break;
    }
    
    return result;
  }

  /**
   * Find the position after all breaks starting at startPosition.
   * 
   * If softOnly and a Hard Break is found, then return -1 to
   * indicate that the sequence of breaks should be considered as
   * hard.
   */
  private int findEndBreakForward(int startPosition, boolean softOnly) {
    int result = startPosition;
    final Map<Integer, Break> pos2break = getPos2Break();
    if (pos2break == null) return result;  // still initializing

    while (result < text.length()) {
      final Break posBreak = pos2break.get(result);

      if (posBreak != null) {
        if (posBreak.breaks()) {
          if (softOnly && posBreak.isHard()) {
            result = -1;
            break;
          }
          else if (posBreak.getBWidth() > 0) {
            result += posBreak.getBWidth();
          }
          else break;
        }
        else break;
      }
      else break;
    }


    return result;
  }

  /**
   * Find the position of the start of breaks ending before startPosition.
   */
  private int findEndBreakReverse(int startPosition) {
    int result = startPosition;
    final Map<Integer, Break> pos2break = getPos2Break();
    if (pos2break == null) return result;  // still initializing

    if (!pos2break.containsKey(startPosition)) --result;

    while (result >= 0) {
      final Break posBreak = pos2break.get(result);
      if (posBreak != null) {
        if (posBreak.breaks() && posBreak.getBWidth() == 0) {
          // time to stop
          break;
        }
        else {
          if (posBreak.breaks() && posBreak.getBWidth() > 0) {
            --result;
          }
          else break;
        }
      }
      else {
        // no longer at a breaking character. move forward to the break.
        if (result < startPosition)	++result;
        break;
      }
    }

    return result;
  }
}
