/*
    Copyright 2013 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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
import org.sd.token.Token;
import org.sd.token.TokenInfo;
import org.sd.util.InputContext;

/**
 * A ParseInputContext derived from an XmlInputDecoder.Paragraph.
 * <p>
 * @author Spence Koehler
 */
public class XmlParseInputContext implements ParseInputContext {
  
  private XmlInputDecoder.Paragraph paragraph;
  private int id;

  private List<TokenInfo> _tokenInfos;
  private List<TokenInfo> _hardBreaks;

  public XmlParseInputContext(XmlInputDecoder.Paragraph paragraph, int id) {
    this.paragraph = paragraph;
    this.id = id;
  }

  
  /**
   * Get this context's text.
   */
  public String getText() {
    return paragraph.getText();
  }

  /**
   * Get an ID that identifies this InputContext within its container.
   */
  public int getId() {
    return id;
  }

  /**
   * Get the character startPosition of the other context's text within
   * this context or return false if the other context is not contained
   * within this context.
   *
   * @param other  The other input context
   * @param startPosition a single element array holding the return value
   *        of the start position -- only set when returning 'true'.
   *
   * @result true and startPosition[0] holds the value or false.
   */
  public boolean getPosition(InputContext other, int[] startPosition) {
    boolean result = false;

    if (other == this) {
      result = true;
      startPosition[0] = 0;
    }

    return result;
  }

  /**
   * Get this context's ultimate container or itself if it is not contained
   * by anything.
   */
  public InputContext getContextRoot() {
    return this;
  }

  /**
   * Get the tokenInfo instances for building an AtnParseBasedTokenizer for
   * this input, if available.
   *
   * @return the tokenInfo instances or null.
   */
  public List<TokenInfo> getTokenInfos() {
    if (_tokenInfos == null && paragraph.hasTokens()) {
      _tokenInfos = buildTokenInfos();
    }
    return _tokenInfos;
  }

  /**
   * Get the spans of tokens that are designated as hard breaks.
   */
  public List<TokenInfo> getHardBreaks() {
    if (_hardBreaks == null && paragraph.hasBreaks()) {
      _hardBreaks = buildHardBreaks();
    }
    return _hardBreaks;
  }


  private final List<TokenInfo> buildTokenInfos() {
    final List<TokenInfo> result = new ArrayList<TokenInfo>();

    //NOTE: only get here if we have tokens
    for (XmlInputDecoder.MarkerInfo tokenStart : paragraph.getTokenStarts()) {
      final TokenInfo tokenInfo =
        new ParseTokenInfo(tokenStart.getPos(), tokenStart.getOtherInfo().getPos(),
                           tokenStart.getCategory(), tokenStart.getAttributes());
      result.add(tokenInfo);
    }

    return result;
  }

  private final List<TokenInfo> buildHardBreaks() {
    final List<TokenInfo> result = new ArrayList<TokenInfo>();

    //NOTE: only get here if we have breaks
    for (XmlInputDecoder.MarkerInfo breakMarker : paragraph.getBreakMarkers()) {
      final TokenInfo tokenInfo =
        new ParseTokenInfo(breakMarker.getPos(), breakMarker.getEndPos(),
                           breakMarker.getBreakType(), null);
      result.add(tokenInfo);
    }

    return result;
  }


  public static final class ParseTokenInfo extends TokenInfo {
    private Map<String, String> attributes;

    public ParseTokenInfo(int tokenStart, int tokenEnd, String category, Map<String, String> attributes) {
      super(tokenStart, tokenEnd, category);
      this.attributes = attributes;
    }

    public void addTokenFeatures(Token token, Object source) {

      // Add the matched grammar rule's category as a token feature
      // (NOTE: this feature is used by AtnState.tokenMatchesStepCategory to
      //        identify a token match and allows pre-defined tokens to
      //        behave as though they matched a grammar rule)
      final String category = getCategory();
      if (category != null && !"".equals(category)) {
        token.setFeature(category, new Boolean(true), source);
      }

      if (attributes != null) {
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
          token.setFeature(entry.getKey(), entry.getValue(), source);
        }
      }
    }
  }
}
