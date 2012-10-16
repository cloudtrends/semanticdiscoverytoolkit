/*
    Copyright 2010 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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


import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sd.cio.MessageHelper;
import org.sd.io.Publishable;

/**
 * A literal (or hardwired) tokenizer.
 * <p>
 * @author Spence Koehler
 */
public class LiteralTokenizer extends StandardTokenizer implements Publishable {
  
  private static final StandardTokenizerOptions OPTIONS = new StandardTokenizerOptions();
  static {
    OPTIONS.setRevisionStrategy(TokenRevisionStrategy.SO);
  }


  private List<TokenData> tokenDatas;
  private transient Map<Integer, TokenData> tokens;


  /**
   * Empty constructor for publishable reconstruction.
   */
  public LiteralTokenizer() {
    super(null, OPTIONS);
    this.tokens = null;
  }

  /**
   * Construct with the given text.
   */
  public LiteralTokenizer(String text) {
    this(text, null);
  }

  /**
   * Construct with the given (consecutive) tokens, resetting their offsets
   * such that the first token's start offset is zero.
   */
  public LiteralTokenizer(String text, List<Token> tokens) {
    super(text, OPTIONS);

    if (tokens != null && tokens.size() > 0) {
      Integer zeroIndex = null;

      this.tokenDatas = new ArrayList<TokenData>();
      int sequenceNumber = 0;
      for (Token token : tokens) {
        if (zeroIndex == null) zeroIndex = token.getStartIndex();
        this.tokenDatas.add(new TokenData(zeroIndex, token, sequenceNumber++));
      }
    }

    // force initialization
    getPos2Break();
  }

  protected Map<Integer, Break> createBreaks() {
    final Map<Integer, Break> result = new HashMap<Integer, Break>();

    this.tokens = new HashMap<Integer, TokenData>();

    if (tokenDatas != null) {
      int lastEnd = -1;
      for (TokenData tokenData : tokenDatas) {
        final Token token = tokenData.asToken(this);

        final int tokenStart = token.getStartIndex();
        final int tokenEnd = token.getEndIndex();

        if (tokenStart > 0) {
          for (int i = lastEnd + 1; i < tokenStart; ++i) {
            setBreak(result, i, Break.SINGLE_WIDTH_HARD_BREAK);
          }

          final int leftOffset = tokenData.getLeftOffset();
          setBreak(result, leftOffset, leftOffset == 0 ? Break.ZERO_WIDTH_HARD_BREAK : Break.SINGLE_WIDTH_HARD_BREAK);
        }

        final int rightBreakWidth = tokenData.getRightBreakWidth();
        setBreak(result, tokenEnd, rightBreakWidth == 0 ? Break.ZERO_WIDTH_HARD_BREAK : Break.SINGLE_WIDTH_HARD_BREAK);

        this.tokens.put(tokenStart, tokenData);

        lastEnd = tokenEnd;
      }
    }

    return result;
  }

  public Token getToken(int startPosition) {
    Token result = doGetToken(startPosition);

    if (result == null) {
      result = super.getToken(startPosition);
    }

    return result;
  }

  public Token getSmallestToken(int startPosition) {
    return getToken(startPosition);
  }

  public Token revise(Token token) {
    return null;
  }

  public Token getNextToken(Token token) {
    Token result = null;

    if (doGetToken(token.getStartIndex()) == token) {
      int tokenIndex = token.getSequenceNumber();

      if (tokenIndex < tokens.size()) {
        result = doGetToken(tokenIndex + 1);
      }
    }
    else {
      result = super.getNextToken(token);
    }

    return result;
  }

  public Token getPriorToken(Token token) {
    Token result = null;

    if (doGetToken(token.getStartIndex()) == token) {
      int tokenIndex = token.getSequenceNumber();

      if (tokenIndex > 0) {
        result = doGetToken(tokenIndex - 1);
      }
    }
    else {
      result = super.getPriorToken(token);
    }

    return result;
  }

  public Token buildToken(int startPosition, int endPosition) {
    Token result = null;

    final Token token = doGetToken(startPosition);
    if (token != null && token.getEndIndex() == endPosition) {
      result = token;
    }
    else {
      result = super.buildToken(startPosition, endPosition);
    }

    return result;
  }

  public String getPostDelim(Token token) {
    String result = "";
    final TokenData tokenData = this.tokens.get(token.getStartIndex());

    if (tokenData != null && tokenData.length() == token.getLength()) {
      result = tokenData.getPostDelim();
    }
    else {
      final Token nextToken = getNextToken(token);
      if (nextToken != null) {
        result = nextToken.getPreDelim();
      }
    }

    return result;
  }

  public String getPreDelim(Token token) {
    String result = "";
    final TokenData tokenData = this.tokens.get(token.getStartIndex());

    if (tokenData != null) {
      result = tokenData.getPreDelim();
    }

    return result;
  }


  /**
   * Write this message to the dataOutput stream such that this message
   * can be completely reconstructed through this.read(dataInput).
   *
   * @param dataOutput  the data output to write to.
   */
  public void write(DataOutput dataOutput) throws IOException {
    MessageHelper.writeString(dataOutput, text);
    if (tokenDatas == null) dataOutput.writeInt(0);
    else {
      dataOutput.writeInt(tokenDatas.size());

      for (TokenData tokenData : tokenDatas) {
        tokenData.write(dataOutput);
      }
    }
  }

  /**
   * Read this message's contents from the dataInput stream that was written by
   * this.write(dataOutput).
   * <p>
   * NOTE: this requires all implementing classes to have a default constructor
   *       with no args.
   *
   * @param dataInput  the data output to write to.
   */
  public void read(DataInput dataInput) throws IOException {
    this.text = MessageHelper.readString(dataInput);
    final int numTokens = dataInput.readInt();
    if (numTokens > 0) {
      this.tokenDatas = new ArrayList<TokenData>();
      for (int i = 0; i < numTokens; ++i) {
        final TokenData tokenData = new TokenData();
        tokenData.read(dataInput);
        this.tokenDatas.add(tokenData);
      }
    }

    // force initialization
    getPos2Break();
  }


  private final Token doGetToken(int startPosition) {
    Token result = null;
    final TokenData tokenData = this.tokens.get(startPosition);

    if (tokenData != null) {
      result = tokenData.getToken();
    }

    return result;
  }


  public static final class TokenData implements Publishable {
    private String text;
    private int startIndex;
    private int revisionNumber;
    private int sequenceNumber;
    private int wordCount;
    private int leftOffset;
    private int rightBreakWidth;
    private String preDelim;
    private String postDelim;

    private Token _token;

    //NOTE: We could hold on to features here, writing and reading them, but
    //      we only hold the "low level" tokens here. Any manually built tokens
    //      (for example those covering multiple low-level tokens) aren't
    //      preserved so their features won't be preserved. Any consumer
    //      needing preserved features would then need to handle persisting
    //      and reading the "high-level" features while either duplicating
    //      the low-level features or using those persisted here. In the end,
    //      it is more simple for consumers to always handle persisting
    //      features when necessary.

    /**
     * Empty constructor for publishable reconstruction.
     */
    public TokenData() {
    }

    public TokenData(int zeroIndex, Token token, int sequenceNumber) {
      final int tokenStart = token.getStartIndex();
      final int tokenEnd = token.getEndIndex();

      this.text = token.getText();
      this.startIndex = tokenStart - zeroIndex;
      this.revisionNumber = 0;
      this.sequenceNumber = sequenceNumber;
      this.wordCount = token.getWordCount();

      this.leftOffset = 0;
      this.rightBreakWidth = 1;

      this.preDelim = token.getPreDelim();
      this.postDelim = token.getPostDelim();
      this._token = null;  //NOTE: initialized when "asToken" is called

      final StandardTokenizer tokenizer = (StandardTokenizer)token.getTokenizer();

      this.leftOffset = 0;
      final Break preBreak = tokenizer.getBreak(tokenStart);
      if (preBreak == null && tokenStart > 0) leftOffset = -1;
      final Break postBreak = tokenizer.getBreak(tokenEnd);
      if (postBreak != null) this.rightBreakWidth = postBreak.getBWidth();
    }

    public int length() {
      return text.length();
    }

    public Token asToken(Tokenizer tokenizer) {
      final Token result = new Token(tokenizer, text, startIndex, TokenRevisionStrategy.SO, revisionNumber, sequenceNumber, wordCount, 1);
      this._token = result;
      return result;
    }

    public boolean hasToken() {
      return _token != null;
    }

    public Token getToken() {
      return _token;
    }

    public String getPreDelim() {
      return preDelim;
    }

    public String getPostDelim() {
      return postDelim;
    }

    public int getLeftOffset() {
      return leftOffset;
    }

    public int getRightBreakWidth() {
      return rightBreakWidth;
    }

    /**
     * Write this message to the dataOutput stream such that this message
     * can be completely reconstructed through this.read(dataInput).
     *
     * @param dataOutput  the data output to write to.
     */
    public void write(DataOutput dataOutput) throws IOException {
      MessageHelper.writeString(dataOutput, text);
      dataOutput.writeInt(startIndex);
      dataOutput.writeInt(revisionNumber);
      dataOutput.writeInt(sequenceNumber);
      dataOutput.writeInt(wordCount);
      dataOutput.writeInt(leftOffset);
      dataOutput.writeInt(rightBreakWidth);
      MessageHelper.writeString(dataOutput, preDelim);
      MessageHelper.writeString(dataOutput, postDelim);
    }

    /**
     * Read this message's contents from the dataInput stream that was written by
     * this.write(dataOutput).
     * <p>
     * NOTE: this requires all implementing classes to have a default constructor
     *       with no args.
     *
     * @param dataInput  the data output to write to.
     */
    public void read(DataInput dataInput) throws IOException {
      this.text = MessageHelper.readString(dataInput);
      this.startIndex = dataInput.readInt();
      this.revisionNumber = dataInput.readInt();
      this.sequenceNumber = dataInput.readInt();
      this.wordCount = dataInput.readInt();
      this.leftOffset = dataInput.readInt();
      this.rightBreakWidth = dataInput.readInt();
      this.preDelim = MessageHelper.readString(dataInput);
      this.postDelim = MessageHelper.readString(dataInput);
    }
  }
}
