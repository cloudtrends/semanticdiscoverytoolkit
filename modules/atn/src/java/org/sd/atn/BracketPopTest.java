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
package org.sd.atn;


import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.sd.token.Token;
import org.sd.util.Usage;
import org.sd.xml.DomElement;
import org.sd.xml.DomNode;
import org.w3c.dom.NodeList;

/**
 * A rule test intended to be used as a popTest for ensuring that a constituent
 * does not end (and/or start) with a bracket unless it starts (and/or ends)
 * with a bracket. This is used to ensure bracketed text is parsed as a
 * separate constituent even when it may otherwise appear to belong with its
 * preceding (or following) text.
 * 
 * @author Spence Koehler
 */
@Usage(notes =
       "An org.sd.atn.AtnRuleStepTest intended to be used as a popTest for\n" +
       "ensuring that a constituent does not end (and/or start) with a\n" +
       "bracket unless it starts (and/or ends) with a bracket. This is used\n" +
       "to ensure bracketed text is parsed as a separate constituent even\n" +
       "when it may otherwise appear to belong with its preceding (or\n" +
       "following) text."
  )
public class BracketPopTest implements AtnRuleStepTest {
  
  private boolean allowBalanced;
  private boolean includeEnds;
  private boolean requireEnds;
  private boolean verbose;

  private List<Bracket> brackets;
  List<Bracket> getBrackets() {
    return brackets;
  }
  boolean hasBrackets() {
    return brackets != null;
  }

  /**
   * 'test' attributes: allowBalanced (default=true), includeEnds (default=false)
   * <ul>
   * <li>allowBalanced accepts the pop if the consistuent contains balanced brackets
   *     within its scope.</li>
   * <li>includeEnds includes the delimiters immediately preceding and following the
   *     constituent in the testing. Note that this option is highly restrictive in
   *     that any constituent immediately preceded or followed by a bracket must span
   *     all tokens to the balanced matching bracket.</li>
   * <li>requireEnds requires (one set of) the delimiters immediately preceding and
   *     following the constituent in the testing.</li>
   * </ul>
   */
  public BracketPopTest(DomNode testNode, ResourceManager resourceManager) {
    this.allowBalanced = testNode.getAttributeBoolean("allowBalanced", true);
    this.includeEnds = testNode.getAttributeBoolean("includeEnds", false);
    this.requireEnds = testNode.getAttributeBoolean("requireEnds", false);
    this.verbose = testNode.getAttributeBoolean("verbose", false);

    //     <brackets>
    //       <!-- -->
    //     </brackets>
    //

    // load brackets
    this.brackets = loadBrackets(testNode);
  }

  protected void setAllowBalanced(boolean allowBalanced) {
    this.allowBalanced = allowBalanced;
  }

  protected boolean allowBalanced() {
    return allowBalanced;
  }

  protected void setIncludeEnds(boolean includeEnds) {
    this.includeEnds = includeEnds;
  }

  protected boolean includeEnds() {
    return includeEnds;
  }

  protected void setRequireEnds(boolean requireEnds) {
    this.requireEnds = requireEnds;
  }

  protected boolean requireEnds() {
    return requireEnds;
  }

  public PassFail accept(Token token, AtnState curState) {
    boolean result = true;

    // only accept constituents that don't have (unbalanced) brackets within

    final LinkedList<AtnState> constituentMatchStates = AtnStateUtil.getConstituentMatchStates(curState);
    if (constituentMatchStates != null && constituentMatchStates.size() > 0) {
      final int tokenStartIndex = token.getStartIndex();
      final Token startToken = constituentMatchStates.get(0).getInputToken();
      final int startTokenIndex = startToken.getStartIndex();

      final int lastTokenStartIndex = constituentMatchStates.getLast().getInputToken().getStartIndex();
      if (lastTokenStartIndex < tokenStartIndex) {
        constituentMatchStates.addLast(curState);
      }

      Bracket endBracket = null;
      Bracket startBracket = null;

      if (result) {
        endBracket = findEndBracket(startToken);

        if (endBracket != null) {
          // there is an end bracket immediately following the constituent start

          if (tokenStartIndex > startTokenIndex) {
            // there are tokens in the constituent following the bracket, so fail
            result = false;
          }
        }
        else {
          final int numCStates = constituentMatchStates.size();

          // if requireEnds, verify that we have brackets around the constituent
          if (requireEnds) {
            result = verifyRequireEnds(constituentMatchStates.get(0), constituentMatchStates.get(numCStates - 1));
          }

          if (result && allowBalanced) {
            // walk forward through constituent tokens seeking start brackets and their
            // balanced ends
            for (int stateNum = includeEnds ? 0 : 1; stateNum < numCStates; ++stateNum) {
              final AtnState cState = constituentMatchStates.get(stateNum);
              final Token curToken = cState.getInputToken();

              if (startBracket == null) {
                startBracket = findStartBracket(curToken);
              }

              if (stateNum == numCStates - 1 && !includeEnds) {
                break;
              }

              if (startBracket != null) {
                if (allowBalanced) {
                  if (startBracket.matchesEnd(curToken)) {
                    // clear balanced case
                    startBracket = null;
                  }
                }
                else {
                  // found an errant bracket
                  result = false;
                  break;
                }
              }
              else {
                // check for wayward end brackets
                endBracket = findEndBracket(curToken);
            
                if (endBracket != null) {
                  result = false;
                  break;
                }
              }
            }

            if (startBracket != null) {
              // unbalanced bracket
              result = false;
            }
          }
        }
      }
    }
    else {
      if (requireEnds) {
        result = verifyRequireEnds(curState, curState);
      }
    }

    if (verbose) {
      System.out.println("***BracketPopTest " + curState.showStateContext() + " result=" + result);
    }

    return PassFail.getInstance(result);
  }

  private final boolean verifyRequireEnds(AtnState preState, AtnState postState) {
    boolean result = true;

    final Token preToken = preState.getInputToken();
    final Bracket preBracket = findStartBracket(preToken);

    if (preBracket == null) {
      result = false;
    }
    else {
      final Token postToken = postState.getInputToken();
      if (!preBracket.matchesEnd(postToken)) {
        result = false;
      }
    }

    return result;
  }

  private List<Bracket> loadBrackets(DomNode grammarNode) {
    List<Bracket> result = null;

    final DomElement bracketsNode = (DomElement)grammarNode.selectSingleNode("brackets");
    if (bracketsNode != null) {
      NodeList bracketNodes = bracketsNode.getChildNodes();
      for (int i = 0; i < bracketNodes.getLength(); ++i) {
        final DomElement bracketElement = (DomElement)bracketNodes.item(i);
        final String bracketClass = bracketElement.getLocalName();
        if ("delim".equals(bracketClass)) {
          final DomElement startElement = (DomElement)bracketElement.selectSingleNode("start");
          final DomElement endElement = (DomElement)bracketElement.selectSingleNode("end");
          
          if (startElement == null || endElement == null) {
            throw new IllegalArgumentException("'delim' bracket must have 'start' and 'end'!");
          }
          final String startData = startElement.getTextContent();
          final String startType = startElement.getAttribute("type");
          final String endData = endElement.getTextContent();
          final String endType = endElement.getAttribute("type");

          final Bracket bracket = new DelimBracket(startData, startType, endData, endType);
          if (result == null) result = new ArrayList<Bracket>();
          result.add(bracket);
        }
      }
    }

    if (result == null) {
      throw new IllegalArgumentException("Malformed 'brackets' under node: " + grammarNode.toString());
    }

    return result;
  }

  Bracket findStartBracket(Token token) {
    Bracket result = null;

    if (brackets != null) {
      for (Bracket bracket : brackets) {
        if (bracket.matchesStart(token)) {
          result = bracket;
          break;
        }
      }
    }

    return result;
  }

  Bracket findEndBracket(Token token) {
    Bracket result = null;

    if (brackets != null) {
      for (Bracket bracket : brackets) {
        if (bracket.matchesEnd(token)) {
          result = bracket;
          break;
        }
      }
    }

    return result;
  }


  static interface Bracket {
    public boolean matchesStart(Token token);
    public boolean matchesEnd(Token token);
  }

  static abstract class BaseBracket implements Bracket {
    private String start;
    private String end;
    
    BaseBracket(String start, String end) {
      this.start = start;
      this.end = end;
    }

    protected String getStart() {
      return start;
    }

    protected String getEnd() {
      return end;
    }
  }

  static final class DelimBracket extends BaseBracket {
    
    private String startType;
    private String endType;

    DelimBracket(String start, String startType, String end, String endType) {
      super(start, end);
      this.startType = startType;
      this.endType = endType;
    }

    public boolean matchesStart(Token token) {
      return matches(getStart(), startType, token.getPreDelim());
    }

    public boolean matchesEnd(Token token) {
      return matches(getEnd(), endType, token.getPostDelim());
    }

    private boolean matches(String value, String type, String data) {
      boolean result = false;

      if ("exact".equals(type)) {
        result = data.equals(value);
      }
      else if ("substr".equals(type)) {
        result = data.indexOf(value) >= 0;
      }

      return result;
    }
  }
}
