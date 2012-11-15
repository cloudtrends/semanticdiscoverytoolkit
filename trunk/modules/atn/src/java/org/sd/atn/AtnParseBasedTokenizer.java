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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.sd.token.Break;
import org.sd.token.FeatureConstraint;
import org.sd.token.CategorizedToken;
import org.sd.token.Token;
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

  private Map<Integer, TokenInfoContainer> pos2tokenInfoContainer;
  private IntegerRange _parseSpans;

  public AtnParseBasedTokenizer(InputContext inputContext, StandardTokenizerOptions tokenizerOptions) {
    this(null, null, null, inputContext, tokenizerOptions);
  }

  public AtnParseBasedTokenizer(List<AtnParseResult> parseResults, InputContext inputContext, StandardTokenizerOptions tokenizerOptions) {
    this(null, null, parseResults, inputContext, tokenizerOptions);
  }

  public AtnParseBasedTokenizer(ResourceManager resourceManager, DomElement tokenizerConfig, List<AtnParseResult> parseResults, InputContext inputContext, StandardTokenizerOptions tokenizerOptions) {
    super(inputContext.getText(), tokenizerOptions);

    NodeList tokenNodes = null;
    if (tokenizerConfig != null) {
      tokenNodes = tokenizerConfig.selectNodes("tokens/token");
    }

    init(resourceManager, inputContext, parseResults, tokenNodes);
  }

  private void init(ResourceManager resourceManager, InputContext inputContext, List<AtnParseResult> parseResults, NodeList tokenNodes) {
    super.setInputContext(inputContext);
    this.pos2tokenInfoContainer = new TreeMap<Integer, TokenInfoContainer>();

    add(parseResults);
    add(tokenNodes);
  }

  public void add(List<AtnParseResult> parseResults) {
    if (parseResults != null && parseResults.size() > 0) {
      boolean changed = false;

      for (AtnParseResult parseResult : parseResults) {
        changed = add(parseResult);
      }

      if (changed) super.reset();
    }
  }

  public boolean add(AtnParseResult parseResult) {
    boolean changed = false;

    final InputContext parseInputContext = parseResult.getInputContext();

    final int[] startPosition = new int[]{0};
    if (super.getInputContext().getPosition(parseInputContext, startPosition)) {
      changed = true;
      addParseResult(parseResult, startPosition[0]);
    }

    return changed;
  }

  private void addParseResult(AtnParseResult parseResult, int startPosition) {
    for (int parseNum = 0; parseNum < parseResult.getNumParses(); ++parseNum) {
      final AtnParse parse = parseResult.getParse(parseNum);

      if (parse.getSelected()) {
        final TokenInfo tokenInfo = new TokenInfo(parse);
        addTokenInfo(tokenInfo, startPosition);
      }
    }
  }

  private void add(NodeList tokenNodes) {
    if (tokenNodes != null) {
      for (int i = 0; i < tokenNodes.getLength(); ++i) {
        final DomNode tokenNode = (DomNode)tokenNodes.item(i);
        TokenInfo tokenInfo = new TokenInfo(tokenNode);
        addTokenInfo(tokenInfo, 0);
      }
    }
  }


  private void addTokenInfo(TokenInfo tokenInfo, int startPosition) {
    final int pos = startPosition + tokenInfo.getTokenStart();

    TokenInfoContainer tokenInfoContainer = pos2tokenInfoContainer.get(pos);

    if (tokenInfoContainer == null) {
      tokenInfoContainer = new TokenInfoContainer();
      pos2tokenInfoContainer.put(pos, tokenInfoContainer);
      _parseSpans = null; // force recalculate
    }

    tokenInfoContainer.add(tokenInfo, startPosition);
  }

  /**
   * Access this data for results analysis.
   */
  public Map<Integer, TokenInfoContainer> getPos2tokenInfoContainer() {
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
      final TokenInfoContainer tic = pos2tokenInfoContainer.get(i);
      if (tic != null) {
        // get the longest parse's entry
        final Map.Entry<Integer, List<TokenInfo>> lastEntry = tic.getTokenInfoList().lastEntry();

        // keep the most "complex" parse
        TokenInfo ti = null;
        AtnParse parse = null;
        int complexity = 0;
        for (TokenInfo curti : lastEntry.getValue()) {
          final AtnParse curParse = curti.getParse();
          final int curComplexity = curParse.getParseTree().countNodes();
          if (ti == null || curComplexity > complexity) {
            ti = curti;
            complexity = curComplexity;
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
      for (Map.Entry<Integer, TokenInfoContainer> mapEntry : pos2tokenInfoContainer.entrySet()) {
        final int pos = mapEntry.getKey();
        final TokenInfoContainer tic = mapEntry.getValue();
        _parseSpans.add(pos + 1, tic.getTokenInfoList().lastKey() - 1, true);
      }
    }
    return _parseSpans;
  }

  private final Set<Integer> getTokenEnds() {
    final Set<Integer> result = new HashSet<Integer>();

    for (TokenInfoContainer tic : pos2tokenInfoContainer.values()) {
      result.addAll(tic.getTokenInfoList().keySet());
    }

    return result;
  }

  protected Map<Integer, Break> createBreaks() {
    final Map<Integer, Break> result = super.createBreaks();
    final Set<Integer> tokenEnds = getTokenEnds();

    // get the ranges covered by parses
    final IntegerRange parseSpans = getParseSpans();

    // turn boundaries between parses into hard breaks; within parse alternatives as soft breaks; clearing other breaks
    for (Map.Entry<Integer, TokenInfoContainer> mapEntry : pos2tokenInfoContainer.entrySet()) {
      int pos = mapEntry.getKey();
      final TokenInfoContainer tic = mapEntry.getValue();

      // Set LHS break as Hard (or soft if contained w/in another parse)
      final boolean isHard = !parseSpans.includes(pos);
      setBreak(result, pos, true, isHard);

      // Set parse boundaries as Soft
      int tokenInfoListIndex = 0;
      final int tokenInfoListIndexMax = tic.getTokenInfoList().size() - 1;
      int lastEndPos = -1;
      for (Integer endPos : tic.getTokenInfoList().keySet()) {
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
      final TokenInfoContainer tic = pos2tokenInfoContainer.get(token.getStartIndex());
      if (tic != null) {
        final TokenInfo first = tic.getFirst(token.getEndIndex());

        final List<TokenInfo> tokenInfos = tic.getAll(token.getEndIndex());
        if (tokenInfos != null) {
          for (TokenInfo tokenInfo : tokenInfos) {

            // Add the matched grammar rule's category as a token feature
            // (NOTE: this feature is used by AtnState.tokenMatchesStepCategory to
            //        identify a token match and needs to be present whether we
            //        have a parse or not.)
            token.setFeature(tokenInfo.getCategory(), new Boolean(true), this);

            final AtnParse atnParse = tokenInfo.getParse();

            if (atnParse != null) {

              // Add the Parse as a (_sourceParse) feature on the token
              final Parse sourceParse = atnParse.getParse();
              if (sourceParse != null) {
                token.setFeature(AtnParseBasedTokenizer.SOURCE_PARSE, sourceParse, this);
              }

              // Add the interpretation classifications as token features
              final List<ParseInterpretation> interpretations = atnParse.getParseInterpretations();
              if (interpretations != null) {
                for (ParseInterpretation interpretation : interpretations) {
                  if (interpretation.getClassification() != null) {
                    token.setFeature(interpretation.getClassification(), interpretation, this);
                  }
                }
              }

              // Add the parse's token's features as features on this token
              final List<CategorizedToken> parseCTokens = atnParse.getTokens();
              if (parseCTokens != null) {
                for (CategorizedToken cToken : parseCTokens) {
                  // add a feature for the category
                  token.setFeature(cToken.category, new Boolean(true), this);

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
  }

  public class TokenInfo {

    private int tokenStart;
    public int getTokenStart() {
      return tokenStart;
    }

    private int tokenEnd;
    public int getTokenEnd() {
      return tokenEnd;
    }

    private String category;
    public String getCategory() {
      return category;
    }

    private AtnParse parse;
    public AtnParse getParse() {
      return parse;
    }


    TokenInfo(DomNode tokenNode) {
      this.tokenStart = tokenNode.getAttributeInt("start");
      this.tokenEnd = tokenNode.getAttributeInt("end");
      this.category = tokenNode.getTextContent();
      this.parse = null;
    }

    TokenInfo(AtnParse parse) {
      this.tokenStart = parse.getStartIndex();
      this.tokenEnd = parse.getEndIndex();
      this.category = parse.getCategory();
      this.parse = parse;
    }
  }

  public class TokenInfoContainer {

    private TreeMap<Integer, List<TokenInfo>> tokenInfoList;
    public TreeMap<Integer, List<TokenInfo>> getTokenInfoList() {
      return tokenInfoList;
    }

    TokenInfoContainer() {
      this.tokenInfoList = new TreeMap<Integer, List<TokenInfo>>();
    }

    void add(TokenInfo tokenInfo, int offset) {
      final int endIndex = offset + tokenInfo.getTokenEnd();
      List<TokenInfo> tokenInfos = tokenInfoList.get(endIndex);
      if (tokenInfos == null) {
        tokenInfos = new ArrayList<TokenInfo>();
        tokenInfoList.put(endIndex, tokenInfos);
      }
      tokenInfos.add(tokenInfo);
    }

    TokenInfo getFirst(int endPos) {
      TokenInfo result = null;

      final List<TokenInfo> tokenInfos = tokenInfoList.get(endPos);
      if (tokenInfos != null && tokenInfos.size() > 0) {
        result = tokenInfos.get(0);
      }

      return result;
    }

    List<TokenInfo> getAll(int endPos) {
      return tokenInfoList.get(endPos);
    }
  }
}
