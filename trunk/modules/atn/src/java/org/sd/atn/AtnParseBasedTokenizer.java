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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.sd.token.Break;
import org.sd.token.FeatureConstraint;
import org.sd.token.CategorizedToken;
import org.sd.token.Token;
import org.sd.token.TokenInfo;
import org.sd.token.TokenInfoContainer;
import org.sd.token.StandardTokenizer;
import org.sd.token.StandardTokenizerOptions;
import org.sd.util.InputContext;
import org.sd.util.range.IntegerRange;
import org.sd.util.Usage;
import org.sd.xml.DomElement;
import org.sd.xml.DomNode;
import org.sd.xml.DomUtil;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Primary ATN Parser tokenizer that wraps prior parses as tokens.
 * <p>
 * @author Spence Koehler
 */
@Usage(notes = "Primary org.sd.AtnParser tokenizer that wraps prior parses as tokens.")
public class AtnParseBasedTokenizer extends StandardTokenizer {
  
  public static final String SOURCE_PARSE = "_sourceParse";

  private TreeMap<Integer, TokenInfoContainer<MyTokenInfo>> pos2tokenInfoContainer;
  private IntegerRange _parseSpans;
  private List<TokenInfo> hardBreaks;
  private boolean retainEndBreaks = true;

  public AtnParseBasedTokenizer(InputContext inputContext, StandardTokenizerOptions tokenizerOptions) {
    this(null, null, null, inputContext, tokenizerOptions);
  }

  public AtnParseBasedTokenizer(List<AtnParseResult> parseResults, InputContext inputContext, StandardTokenizerOptions tokenizerOptions) {
    this(null, null, parseResults, inputContext, tokenizerOptions);
  }

  public AtnParseBasedTokenizer(ResourceManager resourceManager, DomElement tokenizerConfig, List<AtnParseResult> parseResults, InputContext inputContext, StandardTokenizerOptions tokenizerOptions) {
    super(inputContext.getText(), tokenizerOptions);
    this.hardBreaks = null;

    NodeList tokenNodes = null;
    if (tokenizerConfig != null) {
      tokenNodes = tokenizerConfig.selectNodes("tokens/token");
    }

    init(resourceManager, inputContext, parseResults, tokenNodes);
  }

  private void init(ResourceManager resourceManager, InputContext inputContext, List<AtnParseResult> parseResults, NodeList tokenNodes) {
    super.setInputContext(inputContext);
    this.pos2tokenInfoContainer = new TreeMap<Integer, TokenInfoContainer<MyTokenInfo>>();

    add(parseResults);
    add(tokenNodes);

    if (inputContext instanceof ParseInputContext) {
      // augment tokenizer based on parse input data
      final ParseInputContext parseInput = (ParseInputContext)inputContext;
      addOtherTokenInfos(parseInput.getTokenInfos());
      this.hardBreaks = parseInput.getHardBreaks();
    }
  }

  public void setRetainEndBreaks(boolean retainEndBreaks) {
    if (retainEndBreaks != this.retainEndBreaks) {
      this.retainEndBreaks = retainEndBreaks;
      this.reset();
    }
  }

  /**
   * Broaden the token's start position to an already established token with
   * the same end but an earlier start, if possible.
   * <p>
   * NOTE: For the AtnParseBasedTokenizer, this finds broader tokens resulting
   *       from a prior parse.
   */
  @Override
  public Token broadenStart(Token token) {
    Token result = null;

    for (Map.Entry<Integer, TokenInfoContainer<MyTokenInfo>> priorEntry = pos2tokenInfoContainer.floorEntry(token.getStartIndex() - 1);
         priorEntry != null;
         priorEntry = pos2tokenInfoContainer.floorEntry(priorEntry.getKey() - 1)) {
      final TokenInfoContainer<MyTokenInfo> priorInfoContainer = priorEntry.getValue();
      if (priorInfoContainer.getTokenInfoList().containsKey(token.getEndIndex())) {
        result = buildToken(priorEntry.getKey(), token.getEndIndex());
        if (result != null) {
          addTokenFeatures(result);
          break;
        }
      }
    }

    return result;
  }

  public final void add(List<AtnParseResult> parseResults) {
    if (parseResults != null && parseResults.size() > 0) {
      boolean changed = false;

      for (AtnParseResult parseResult : parseResults) {
        changed |= add(parseResult);
      }

      if (changed) super.reset();
    }
  }

  private final boolean add(AtnParseResult parseResult) {
    boolean changed = false;

    final InputContext parseInputContext = parseResult.getInputContext();

    final int[] startPosition = new int[]{0};
    if (super.getInputContext().getPosition(parseInputContext, startPosition)) {
      changed = true;
      addParseResult(parseResult, startPosition[0]);
    }

    return changed;
  }

  private final void addParseResult(AtnParseResult parseResult, int startPosition) {
    for (int parseNum = 0; parseNum < parseResult.getNumParses(); ++parseNum) {
      final AtnParse parse = parseResult.getParse(parseNum);

      if (parse.getSelected()) {
        final MyTokenInfo tokenInfo = new MyTokenInfo(parse);
        addTokenInfo(tokenInfo, startPosition);
      }
    }
  }

  private final void add(NodeList tokenNodes) {
    if (tokenNodes != null) {
      for (int i = 0; i < tokenNodes.getLength(); ++i) {
        final DomNode tokenNode = (DomNode)tokenNodes.item(i);
        MyTokenInfo tokenInfo = new MyTokenInfo(tokenNode);
        addTokenInfo(tokenInfo, 0);
      }
    }
  }


  private final void addTokenInfo(MyTokenInfo tokenInfo, int startPosition) {
    final int pos = startPosition + tokenInfo.getTokenStart();

    TokenInfoContainer<MyTokenInfo> tokenInfoContainer = pos2tokenInfoContainer.get(pos);

    if (tokenInfoContainer == null) {
      tokenInfoContainer = new TokenInfoContainer<MyTokenInfo>();
      pos2tokenInfoContainer.put(pos, tokenInfoContainer);
      _parseSpans = null; // force recalculate
    }

    tokenInfoContainer.add(tokenInfo, startPosition);
  }


  private final void addOtherTokenInfos(List<TokenInfo> tokenInfos) {
    if (tokenInfos != null) {
      for (TokenInfo tokenInfo : tokenInfos) {
        addTokenInfo(new MyTokenInfo(tokenInfo), 0);
      }

      // changed
      super.reset();
    }
  }

  /**
   * Access this data for results analysis.
   */
  public Map<Integer, TokenInfoContainer<MyTokenInfo>> getPos2tokenInfoContainer() {
    return pos2tokenInfoContainer;
  }

  /**
   * Scan this tokenizer's token's for Parses, collecting those that are most
   * inclusive.
   */
  public List<AtnParse> getParses() {
    final List<AtnParse> result = new ArrayList<AtnParse>();

    final int textlen = text.length();
    for (int i = 0; i < textlen; ++i) {
      final TokenInfoContainer<MyTokenInfo> tic = pos2tokenInfoContainer.get(i);
      if (tic != null) {
        // get the longest parse's entry
        final Map.Entry<Integer, List<MyTokenInfo>> lastEntry = tic.getTokenInfoList().lastEntry();

        // keep the most "complex" parse
        MyTokenInfo ti = null;
        AtnParse parse = null;
        int complexity = 0;
        for (MyTokenInfo curti : lastEntry.getValue()) {
          final AtnParse curParse = curti.getParse();
          if (curParse != null) {
            final int curComplexity = curParse.getParseTree().countNodes();
            if (ti == null || curComplexity > complexity) {
              ti = curti;
              complexity = curComplexity;
            }
          }
        }
        if (ti != null) {
          result.add(ti.getParse());
          i = ti.getTokenEnd();
        }
      }
    }

    return result;
  }

  public void setTokenizerOptions(StandardTokenizerOptions tokenizerOptions) {
    super.setOptions(tokenizerOptions);
  }

  private IntegerRange getParseSpans() {
    if (_parseSpans == null) {
      _parseSpans = new IntegerRange();
      for (Map.Entry<Integer, TokenInfoContainer<MyTokenInfo>> mapEntry : pos2tokenInfoContainer.entrySet()) {
        final int pos = mapEntry.getKey();
        final TokenInfoContainer<MyTokenInfo> tic = mapEntry.getValue();
        _parseSpans.add(pos + 1, tic.getTokenInfoList().lastKey() - 1, true);
      }
    }
    return _parseSpans;
  }

  private final Set<Integer> getTokenEnds() {
    final Set<Integer> result = new HashSet<Integer>();

    for (TokenInfoContainer<MyTokenInfo> tic : pos2tokenInfoContainer.values()) {
      result.addAll(tic.getTokenInfoList().keySet());
    }

    return result;
  }

  protected Map<Integer, Break> createBreaks() {
    final Map<Integer, Break> result = super.createBreaks();
    final Set<Integer> tokenEnds = getTokenEnds();

    // set hard breaks, if any
    if (hardBreaks != null) {
      for (TokenInfo hardBreak : hardBreaks) {
        final int startPos = hardBreak.getTokenStart();
        final int endPos = hardBreak.getTokenEnd();
        final String breakType = hardBreak.getCategory();  // null or "h" for hard, "s" for soft, "n" for none
        if (endPos > startPos) {
          final Break theBreak =
            "s".equals(breakType) ? Break.SINGLE_WIDTH_SOFT_BREAK :
            "n".equals(breakType) ? Break.NO_BREAK :
            Break.SINGLE_WIDTH_HARD_BREAK;

          for (int pos = startPos; pos < endPos; ++pos) {
            result.put(pos, theBreak);
          }
        }
        else {
          final Break theBreak =
            "s".equals(breakType) ? Break.ZERO_WIDTH_SOFT_BREAK :
            "n".equals(breakType) ? null :
            Break.ZERO_WIDTH_HARD_BREAK;

          if (theBreak != null) {
            result.put(startPos, theBreak);
          }
        }
      }
    }

    // get the ranges covered by parses
    final IntegerRange parseSpans = getParseSpans();

    final Map<Integer, Break> standardBreaks = retainEndBreaks ? super.createBreaks() : null;

    // turn boundaries between parses into hard breaks; within parse alternatives as soft breaks; clearing other breaks
    for (Map.Entry<Integer, TokenInfoContainer<MyTokenInfo>> mapEntry : pos2tokenInfoContainer.entrySet()) {
      int pos = mapEntry.getKey();
      final TokenInfoContainer<MyTokenInfo> tic = mapEntry.getValue();
      Map<Integer, Integer> ticEndAdjustments = null;

      // Set LHS break as Hard (or soft if contained w/in another parse)
      final boolean isHard = !parseSpans.includes(pos);
      setBreak(result, pos, true, isHard);

      // Set parse boundaries as Soft
      int tokenInfoListIndex = 0;
      final int tokenInfoListIndexMax = tic.getTokenInfoList().size() - 1;
      int lastEndPos = -1;
      for (Integer endPos : tic.getTokenInfoList().keySet()) {

        final Integer origEndPos = endPos;
        if (standardBreaks != null) {
          // adjust endPos back over breaks
          for (int fallbackEndPos = endPos - 1; fallbackEndPos >= pos; --fallbackEndPos) {
            final Break standardBreak = standardBreaks.get(fallbackEndPos);
            if (standardBreak != null && standardBreak.breaks() && standardBreak.getBWidth() > 0) {
              --endPos;
            }
            else break;
          }
        }

        if (!origEndPos.equals(endPos)) {
          if (ticEndAdjustments == null) ticEndAdjustments = new HashMap<Integer, Integer>();
          ticEndAdjustments.put(origEndPos, endPos);
        }

        if (tokenInfoListIndex >= tokenInfoListIndexMax) {
          lastEndPos = endPos;
          break;
        }

        doClearBreaks(result, pos + 1, endPos, tokenEnds);
        setBreak(result, endPos, false, false);
        pos = endPos;

        ++tokenInfoListIndex;
      }

      // Set RHS break as Hard (or soft if contained w/in another parse)
      doClearBreaks(result, pos + 1, lastEndPos, tokenEnds);
      setBreak(result, lastEndPos, false, !parseSpans.includes(lastEndPos));

      if (ticEndAdjustments != null) {
        for (Map.Entry<Integer, Integer> entry : ticEndAdjustments.entrySet()) {
          tic.adjustEnd(entry.getKey(), entry.getValue());
        }
      }
    }

    return result;
  }

  /**
   * Clear the breaks in the range, but preserve "tokenEnds" as soft.
   */
  private final void doClearBreaks(Map<Integer, Break> result, int startPos, int endPos, Set<Integer> tokenEnds) {
    for (int breakIndex = startPos; breakIndex < endPos; ++breakIndex) {
      if (tokenEnds.contains(breakIndex)) {
        // keep tokenEnd break(s), but flip from hard to soft
        int bWidth = 1;
        if (result.containsKey(breakIndex)) {
          bWidth = result.get(breakIndex).getBWidth();
        }
        result.put(breakIndex, bWidth == 0 ? Break.ZERO_WIDTH_SOFT_BREAK : Break.SINGLE_WIDTH_SOFT_BREAK);
      }
      else {
        result.remove(breakIndex);
      }
    }
  }

  protected boolean hitsTokenBreakLimit(int startIdx, int breakIdx, int curBreakCount) {
    boolean result = super.hitsTokenBreakLimit(startIdx, breakIdx, curBreakCount);

    if (result) {
      final IntegerRange parseSpans = getParseSpans();
      if (parseSpans.includes(breakIdx)) {
        result = false;
      }
    }

    return result;
  }

  // /**
  //  * A feature constraint for locating parse category features on tokens
  //  * <p>
  //  * Note that values of features found through this constraint will be Parse
  //  * instances.
  //  */
  // public static final FeatureConstraint createParseFeatureConstraint(String category) {
  //   final FeatureConstraint result = new FeatureConstraint();
  //   result.setType(category);
  //   result.setClassType(AtnParseBasedTokenizer.class);
  //   result.setFeatureValueType(Parse.class);
  //   return result;
  // }

  /**
   * A feature constraint for locating parse interpretation features on tokens
   * <p>
   * Note that values of features found through this constraint will be
   * ParseInterpretation instances.
   */
  public static final FeatureConstraint createParseInterpretationFeatureConstraint(String category) {
    final FeatureConstraint result = new FeatureConstraint();
    result.setType(category);
    result.setClassType(AtnParseBasedTokenizer.class);
    result.setFeatureValueType(ParseInterpretation.class);
    return result;
  }

  // adds parse category features to token
  protected void addTokenFeatures(Token token) {
    if (token != null) {
      final TokenInfoContainer<MyTokenInfo> tic = pos2tokenInfoContainer.get(token.getStartIndex());
      if (tic != null) {
        final MyTokenInfo first = tic.getFirst(token.getEndIndex());

        final List<MyTokenInfo> tokenInfos = tic.getAll(token.getEndIndex());
        if (tokenInfos != null) {
          for (MyTokenInfo tokenInfo : tokenInfos) {
            tokenInfo.addTokenFeatures(token, this);
          }
        }
      }
    }
  }

  public static class MyTokenInfo extends TokenInfo {

    private AtnParse parse;
    private TokenInfo wrappedTokenInfo;

    MyTokenInfo(DomNode tokenNode) {
      super(
        tokenNode.getAttributeInt("start"),
        tokenNode.getAttributeInt("end"),
        tokenNode.getTextContent());
      this.parse = null;
      this.wrappedTokenInfo = null;
    }

    MyTokenInfo(AtnParse parse) {
      super(
        parse.getStartIndex(),
        parse.getEndIndex(),
        parse.getCategory());
      this.parse = parse;
      this.wrappedTokenInfo = null;
    }

    MyTokenInfo(TokenInfo wrappedTokenInfo) {
      super(
        wrappedTokenInfo.getTokenStart(),
        wrappedTokenInfo.getTokenEnd(),
        wrappedTokenInfo.getCategory());
      this.parse = null;
      this.wrappedTokenInfo = wrappedTokenInfo;
    }

    public AtnParse getParse() {
      return parse;
    }

    public TokenInfo getWrappedTokenInfo() {
      return wrappedTokenInfo;
    }

    @Override
    public void setTokenStart(int tokenStart) {
      if (wrappedTokenInfo != null) {
        wrappedTokenInfo.setTokenStart(tokenStart);
      }
      super.setTokenStart(tokenStart);
    }

    @Override
    public void setTokenEnd(int tokenEnd) {
      if (wrappedTokenInfo != null) {
        wrappedTokenInfo.setTokenEnd(tokenEnd);
      }
      super.setTokenEnd(tokenEnd);
    }

    @Override
    public void setCategory(String category) {
      if (wrappedTokenInfo != null) {
        wrappedTokenInfo.setCategory(category);
      }
      super.setCategory(category);
    }

    public void addTokenFeatures(Token token, Object source) {

      // Add the matched grammar rule's category as a token feature
      // (NOTE: this feature is used by AtnState.tokenMatchesStepCategory to
      //        identify a token match and needs to be present whether we
      //        have a parse or not.)
      final String category = getCategory();
      if (category != null && !"".equals(category)) {
        token.setFeature(category, new Boolean(true), source);
      }


      if (wrappedTokenInfo != null) {
        wrappedTokenInfo.addTokenFeatures(token, source);
      }

      if (parse != null) {

        // Add the Parse as a (_sourceParse) feature on the token
        final Parse sourceParse = parse.getParse();
        if (sourceParse != null) {
          token.setFeature(AtnParseBasedTokenizer.SOURCE_PARSE, sourceParse, source);
        }

        // Add the interpretation classifications as token features
        final List<ParseInterpretation> interpretations = parse.getParseInterpretations();
        if (interpretations != null) {
          for (ParseInterpretation interpretation : interpretations) {
            if (interpretation.getClassification() != null) {
              token.setFeature(interpretation.getClassification(), interpretation, source);
            }
          }
        }

        // Add the parse's token's features as features on this token
        final List<CategorizedToken> parseCTokens = parse.getTokens();
        if (parseCTokens != null) {
          final String tokenText = token.getText();
          for (CategorizedToken cToken : parseCTokens) {
            if (tokenText.equals(cToken.token.getText())) {
              // add a feature for the category
              token.setFeature(cToken.category, new Boolean(true), source);

              // add the token's features
              if (cToken.token.hasFeatures()) {
                token.addFeatures(cToken.token.getFeatures());
              }
            }
          }
        }
      }
    }
  }
}
