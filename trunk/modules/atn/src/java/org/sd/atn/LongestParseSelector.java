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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sd.token.Token;
import org.sd.util.tree.Tree;
import org.sd.xml.DomElement;
import org.sd.xml.DomNode;

/**
 * AtnParseSelector that chooses the longest parse.
 * <p>
 * @author Spence Koehler
 */
public class LongestParseSelector implements AtnParseSelector {
  
  private boolean simplest;
  private boolean onlyfirst;

  /**
   * Attribute 'simplest' (default 'true') accepts only longest parses with
   * the fewest number of nodes in their parse tree.
   * <p>
   * Attribute 'onlyfirst' (default 'false') accepts only the first (longest/
   * simplest) parse.
   */
  public LongestParseSelector(DomNode domNode, ResourceManager resourceManager) {
    final DomElement domElement = (DomElement)domNode;
    this.simplest = domElement.getAttributeBoolean("simplest", true);
    this.onlyfirst = domElement.getAttributeBoolean("onlyfirst", false);
  }

  public List<AtnParse> selectParses(AtnParseResult parseResult) {
    final List<ParseData> parseDatas = new ArrayList<ParseData>();

    for (int parseIndex = 0; parseIndex < parseResult.getNumParses(); ++parseIndex) {
      final AtnParse parse = parseResult.getParse(parseIndex);
      if (!parse.getSelected()) continue;
      parseDatas.add(new ParseData(parse, simplest));
    }

    Collections.sort(parseDatas);

    final Set<Integer> selected = new HashSet<Integer>();

    ParseData lastParseData = null;
    for (ParseData parseData : parseDatas) {
      if (lastParseData == null) {
        selected.add(parseData.getAtnParse().getParseNum());
      }
      else if (parseData.compareTo(lastParseData) != 0) {
        break;
      }
      else {
        selected.add(parseData.getAtnParse().getParseNum());
      }

      lastParseData = parseData;
    }


    final List<AtnParse> result = new ArrayList<AtnParse>();
    String note = "Not longest";

    // collect non-duplicate selected parses
    for (int parseIndex = 0; parseIndex < parseResult.getNumParses(); ++parseIndex) {
      final AtnParse parse = parseResult.getParse(parseIndex);
      boolean select = selected.contains(parse.getParseNum());

      if (select && isDuplicate(parse, result)) {
        note = "Is duplicate";
        select = false;
      }

      if (onlyfirst && select && result.size() > 0) {
        note = "Isn't first";
        select = false;
      }

      parse.setSelected(select);
      if (select) result.add(parse);
      else parse.addNote(note);
    }

    return result;
  }

  private static final boolean isDuplicate(AtnParse parse, List<AtnParse> parses) {
    boolean result = false;

    final Tree<String> parseTree = parse.getParseTree();

    for (AtnParse curParse : parses) {
      final Tree<String> curParseTree = curParse.getParseTree();
      if (parseTree.equals(curParseTree)) {
        result = true;
        break;
      }
    }

    return result;
  }


  private static final class ParseData implements Comparable<ParseData> {
    private AtnParse atnParse;
    private boolean simplest;

    private List<Token> skipTokens;
    private int skipCount;
    private int length;
    private int complexity;  // the lower, the simpler
    
    ParseData(AtnParse atnParse, boolean simplest) {
      this.atnParse = atnParse;
      this.simplest = simplest;

      this.skipTokens = null;
      this.skipCount = 0;
      this.length = 0;
      this.complexity = 0;

      initialize();
    }

    private final void initialize() {
      this.length = atnParse.getEndIndex() - atnParse.getStartIndex();

      for (Tree<AtnState> stateNode = atnParse.getEndState(); stateNode != null; stateNode = stateNode.getParent()) {
        final AtnState pathState = stateNode.getData();
        if (pathState == null) break;

        ++complexity;

        if (pathState.isSkipped()) {
          final Token skipToken = pathState.getInputToken();
          if (skipTokens == null) skipTokens = new ArrayList<Token>();
          skipTokens.add(skipToken);
          skipCount += skipToken.getWordCount();
        }
      }
    }

    public String toString() {
      return atnParse.getParseTree().toString();
    }

    AtnParse getAtnParse() {
      return atnParse;
    }

    public List<Token> getSkipTokens() {
      return skipTokens;
    }

    public int getSkipCount() {
      return skipCount;
    }

    public int getLength() {
      return length;
    }

    public int getComplexity() {
      return complexity;
    }

    public int compareTo(ParseData other) {
      int result = this == other ? 0 : -1;

      if (result != 0) {
        if (length == other.getLength() &&
            (!simplest || complexity == other.getComplexity()) &&
            skipCount == other.getSkipCount()) {
          result = 0;
        }
        else {
          // the parse that skips fewer tokens comes first
          result = compareSkips(other);

          // when still unresolved, the longest parse comes first
          if (result == 0) {
            result = other.getLength() - length;

            // when still unresolved, the minimum complexity comes first
            if (result == 0 && simplest) {
              result = this.complexity - other.getComplexity();
            }
          }
        }
      }

      return result;
    }

    private final int compareSkips(ParseData other) {
      int result = 0;

      if (skipCount > 0 || other.skipCount > 0) {
        if (skipCount > 0 && other.skipCount > 0) {
          final boolean otherReducesThis = reducesSkipCount(other);
          final boolean thisReducesOther = other.reducesSkipCount(this);
          if (otherReducesThis && !thisReducesOther) {
            // prefer other
            result = 1;
          }
          else if (thisReducesOther && !otherReducesThis) {
            // prefer this
            result = -1;
          }
          // else, no preference
        }
        else if (skipCount > 0) {
          // if the other parse reduces this' skip count, other is better (+1)
          if (reducesSkipCount(other)) {
            result = 1;
          }
        }
        else { // other.skipCount > 0
          // if this reduces other's skip count, this is better (-1)
          if (other.reducesSkipCount(this)) {
            result = -1;
          }
        }
      }

      return result;
    }

    private final boolean reducesSkipCount(ParseData other) {
      boolean result = false;

      if (skipCount > 0) {
        for (Token skipToken : skipTokens) {
          final int overlap = other.overlapFlag(skipToken);
          if (overlap != 1) {
            result = true;
            break;
          }
        }
      }

      return result;
    }

    /**
     * Return
     * <ul>
     * <li>0 if token is fully covered by this parse;</li>
     * <li>1 if token is fully outside this parse;</li>
     * <li>-1 if token partially overlaps with this parse</li>
     * </ul>
     */
    private final int overlapFlag(Token token) {
      int result = 0;

      final int myStartIndex = atnParse.getStartIndex();
      final int myEndIndex = atnParse.getEndIndex();
      final int tokenStartIndex = token.getStartIndex();
      final int tokenEndIndex = token.getEndIndex();

      if (Token.encompasses(myStartIndex, myEndIndex, tokenStartIndex, tokenEndIndex)) {
        // count how many of token's words are not skipped by this
        if (skipCount == 0) {
          // nothing is skipped, so all are covered
          result = 0;
        }
        else {
          // if any part of the token is skipped, then not all are covered
          for (Token skipToken : skipTokens) {
            if (skipToken.encompasses(token)) {
              // token is entirely skipped, so not covered
              result = 1;
              break;
            }
            else if (skipToken.overlaps(token)) {
              // token is partially skipped, so not entirely covered
              result = -1;
            }
          }
        }
      }
      else if (Token.overlaps(myStartIndex, myEndIndex, tokenStartIndex, tokenEndIndex)) {
        // token is not entirely covered by this (and not at all if overlap is skipped)
        result = -1;
      }
      else {
        // else no coverage
        result = 1;
      }

      return result;
    }
  }
}
