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


import java.util.LinkedList;
import org.sd.util.range.IntegerRange;

/**
 * Container for an expression used to find a state in the history.
 * <p>
 * @author Spence Koehler
 */
public class StatePath {
  
  private String expr;
  private boolean isRequired;  // if expr startsWith '!'
  private LinkedList<PathComponent> pathComponents;

  public StatePath(String expr) {
    this.expr = expr;
    this.isRequired = false;
    this.pathComponents = new LinkedList<PathComponent>();
    init();
  }

  private final void init() {
    // check for empty expression
    if (expr == null) {
      expr = "";
    }
    else {
      expr = expr.trim();
    }

    if ("".equals(expr)) return;

    // check for "required" flag
    if (expr.charAt(0) == '!') {
      this.isRequired = true;
      expr = expr.substring(1);

      if ("".equals(expr)) return;
    }

    // separate into path components
    PathComponent ppc = null;
    final String[] pieces = expr.split("\\s*\\.\\s*");
    for (String piece : pieces) {
      final PathComponent pc = new PathComponent(piece);
      if (ppc != null && ppc.isDoubleWild() && pc.isDoubleWild()) continue;
      this.pathComponents.add(pc);
      ppc = pc;
    }
  }

  public boolean isRequired() {
    return isRequired;
  }

  /**
   * Align this path against the given state's history.
   */
  public PathAligner getPathAligner(AtnState fromState) {
    return new PathAligner(pathComponents, fromState);
  }      


  private static final class PathComponent {
    private String pcString;
    private IntegerRange repeat;
    private boolean wild;
    private boolean doublewild;
    private boolean toFront;
    private String category;

    public PathComponent(String pcString) {
      this.pcString = pcString;
      this.repeat = null;
      this.wild = false;
      this.doublewild = false;
      this.toFront = false;
      this.category = null;
      init();
    }

    private final void init() {
      int lpos = 0;

      // composition:
      // - * -- wild (matches just one level)
      // - ** -- double wild (matches multiple levels)
      // - category -- category (rule step label) to match
      // - [rangeExpr] -- range for state repeats to match
      // - < -- indicator for falling back to first constituent token (instead of default last)

      // check for empty path component
      if (pcString == null) {
        pcString = "";
      }
      if ("".equals(pcString)) return;

      // check for wild
      if (pcString.charAt(lpos) == '*') {
        this.wild = true;
        ++lpos;
      }
      if (wild && pcString.length() > lpos) {
        if (pcString.charAt(1) == '*') {
          this.doublewild = true;
          ++lpos;
        }
      }

      // check for restricted repeat range (between square brackets)
      final int rbPos = pcString.lastIndexOf(']');
      int lbPos = -1;
      if (rbPos > 0) {
        lbPos = pcString.lastIndexOf('[', rbPos - 1);
      }
      if (lbPos >= 0 && rbPos > lbPos + 1) {
        final String value = pcString.substring(lbPos + 1, rbPos).trim();
        this.repeat = new IntegerRange(value);
      }
      else {
        lbPos = pcString.length();
      }

      // check for "to front" marker (after later of asterisks or brackets)
      final int tfPos = pcString.indexOf('<', Math.max(lpos, rbPos + 1));
      if (tfPos >= 0) {
        this.toFront = true;
        if (tfPos < lbPos) lbPos = tfPos;
      }

      if (wild || doublewild) return;

      // get category
      this.category = pcString.substring(lpos, lbPos);
    }

    public String getPcString() {
      return pcString;
    }

    public IntegerRange getRepeat() {
      return repeat;
    }

    public boolean isWild() {
      return wild;
    }

    public boolean isDoubleWild() {
      return doublewild;
    }

    public boolean toFront() {
      return toFront;
    }

    public String getCategory() {
      return category;
    }

    public boolean matches(AtnState atnState) {
      if (repeat != null) {
        if (!repeat.includes(atnState.getRepeatNum())) {
          return false;  // range doesn't match
        }
      }

      if (wild || doublewild) return true;

      return atnState.getRuleStep().matchesCategory(category);
    }
  }

  public static final class PathAligner {
    private LinkedList<PathComponent> pathComponents;
    private AtnState fromState;
    private AtnState alignedState;
    private boolean aligns;

    private LinkedList<AtnState> _statepath;

    private int pcSize;
    private int pcIdx;
    private PathComponent pc;
    private PathComponent ppc;
    private AtnState curState;

    private PathAligner(LinkedList<PathComponent> pathComponents, AtnState fromState) {
      this.pathComponents = pathComponents;
      this.fromState = fromState;
      this.alignedState = null;
      this.aligns = false;

      this._statepath = null;

      this.pcSize = pathComponents.size();
      this.pcIdx = pcSize - 1;
      this.pc = (pcIdx >= 0) ? pathComponents.get(pcIdx) : null;
      setPPC();

      this.curState = fromState;
      decrementState();

      align();
    }

    public boolean isEmpty() {
      return pcSize == 0;
    }

    public AtnState getFromState() {
      return fromState;
    }

    public boolean aligns() {
      return aligns;
    }

    public AtnState getAlignedState() {
      return aligns ? alignedState : null;
    }

    public LinkedList<AtnState> getStatePath() {
      if (_statepath == null && aligns) {
        _statepath = new LinkedList<AtnState>();
        for (AtnState state = fromState; state != alignedState; state = state.getParentState()) {
          _statepath.add(state);
        }
        _statepath.add(alignedState);
      }
      return _statepath;
    }

    public AtnState getState(int offset) {
      AtnState result = null;

      int idx = getStatePath().size() - offset - 1;
      if (idx >= 0) {
        result = getStatePath().get(idx);
      }

      return result;
    }

    public String getRuleId(int offset) {
      String result = null;

      final AtnState state = getState(offset);
      if (state != null) {
        result = state.getRule().getRuleId();
      }

      return result;
    }


    private final void setPPC() {
      this.ppc = (pcIdx >= 1) ? pathComponents.get(pcIdx - 1) : null;
    }

    private final void decrementState() {
      AtnState result = null;

      AtnState pushState = (curState == null) ? null : curState.getPushState();
      for (AtnState prevState = (curState == null) ? null : curState.getParentState();
           prevState != null;
           prevState = prevState.getParentState()) {

        if (prevState == pushState || prevState.getMatched() || prevState.isPoppedState()) {
          result = prevState;
          break;
        }
      }

      this.curState = result;
    }


    private final void inc() {
      --pcIdx;
      pc = ppc;
      setPPC();
    }

    private final void align() {
      
      while (curState != null && pc != null) {
        boolean matches = false;

        if (pc.isDoubleWild()) {
          matches = (ppc == null) ? true : ppc.matches(curState);
        }
        else {
          matches = pc.matches(curState);
        }

        // increment
        if (matches) {
          boolean atFront = false;
          if (pc.toFront()) {
            // move curState to front of constituent if warranted
            if (!curState.getMatched() && curState.isPoppedState()) {
              curState = curState.getConstituentTop();
              atFront = true;
            }
          }

          if (pcIdx == pcSize - 1) {
            alignedState = curState;
          }

          inc();
          if (pc != null && pc.isDoubleWild()) inc();  // double inc

          if (pc == null) {
            aligns = true;
          }
          else {  // increment state to prior pop state
            // to beginning of cur constit
            if (!atFront) curState = curState.getPushState();
            // to prior constituent's pop
            decrementState();
          }
        }
        else if (pcIdx == pcSize - 1) {
          // haven't hit first match yet, move back to last match
          decrementState();
        }
        else {
          // didn't match after having started to match, doesn't match
          aligns = false;
          break;
        }
      }
    }
  }
}
