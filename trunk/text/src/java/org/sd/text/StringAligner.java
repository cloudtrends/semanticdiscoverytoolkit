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
package org.sd.text;


import org.sd.nlp.Break;
import org.sd.nlp.BreakStrategy;
import org.sd.nlp.GeneralBreakStrategy;
import org.sd.nlp.StringWrapper;
import org.sd.util.StringUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Utility to align two strings if possible.
 * <p>
 * @author Spence Koehler
 */
public class StringAligner {

  private String base;
  private String cand;

  private int[] cpBase;
  private int[] cpCand;

  private Alignment alignment;

  public StringAligner(String base, String candidate) {
    this.base = base;
    this.cand = candidate;

    this.cpBase = StringUtil.toCodePoints(base.toLowerCase());
    this.cpCand = StringUtil.toCodePoints(cand.toLowerCase());

    this.alignment = computeBestAlignment();
  }

  /**
   * Get whether the base aligns with the candidate.
   * <p>
   * Alignment means that all letters from base or candidate are matched
   * in order to the other; potentially with skipped letters (see penalty).
   */
  public boolean aligns() {
    return alignment != null;
  }

  /**
   * Get the alignment or null.
   */
  public Alignment getAlignment() {
    return alignment;
  }

  /**
   * Get the alignment penalty.
   * <p>
   * 0 means perfect alignment (essentially equals);
   * -1 means no alignment;
   * 1+ counts the number of "skipped" chars while aligning.
   */
  public int getPenalty() {
    int result = -1;

    if (alignment != null) {
      result = alignment.penalty;
    }

    return result;
  }

  /**
   * Get the portion of the candidate string that matched the base.
   */
  public String getMatchedCandidateString() {
    String result = null;

    if (alignment != null) {
      result = cand.substring(alignment.candStartInd, alignment.candEndInd);
    }

    return result;
  }

  /**
   * Get the portion of the candidate string after the match to the base.
   */
  public String getPostMatchedCandidateString() {
    String result = null;

    if (alignment != null) {
      result = cand.substring(alignment.candEndInd).trim();
    }

    return result;
  }

  /**
   * Get the portion of the candidate string before the match to the base.
   */
  public String getPreMatchedCandidateString() {
    String result = null;

    if (alignment != null) {
      result = cand.substring(0, alignment.candStartInd).trim();
    }

    return result;
  }

  /**
   * Get the segment of the candidate string starting from where the
   * base starts matching and ending at the first HARD break, but only if
   * the segment starts after a break.
   *
   * @param breakStrategy  The breakStrategy to use; if null, a GeneralBreakStrategy will be used.
   */
  public String getMatchedSegment(BreakStrategy breakStrategy) {
    String result = null;

    if (alignment != null) {
      if (breakStrategy == null) breakStrategy = GeneralBreakStrategy.getInstance();
      final String string = cand.substring(alignment.candStartInd);
      final StringWrapper wrapper = new StringWrapper(cand, breakStrategy);

      final int startInd = alignment.candStartInd;
      if (startInd == 0 ||
          wrapper.getBreak(startInd) == Break.SOFT_SPLIT ||
          (wrapper.getBreak(startInd - 1) == Break.SOFT_FULL || wrapper.getBreak(startInd - 1) == Break.HARD)) {
        // starts at a breaking point

        for (int i = startInd + alignment.candEndInd; i < wrapper.length(); ++i) {
          if (wrapper.getBreak(i) == Break.HARD) {
            result = wrapper.string.substring(startInd, i).trim();
            break;
          }
        }

        if (result == null) {
          result = wrapper.string.substring(startInd).trim();
        }
      }
    }

    return result;
  }

   private final Alignment computeBestAlignment() {
     final Alignment result = new Alignment(incToNextLetter(cpBase, -1));
     return result.aligns ? result : null;
   }

//for very relaxed alignment... need to set limits for how short a base to accept...
//   private final Alignment computeBestAlignment() {
//     Alignment a = null;

//     for (int i = 0; i < cpBase.length; ++i) {
//       if (!Character.isLetterOrDigit(cpBase[i])) continue;  // skip non letters/digits

//       final Alignment b = new Alignment(i);
//       if (b.aligns) {
//         a = b;
//         break;
//       }
//     }

//     return a;
//   }

  /**
   * Scan cp from index until we find c.
   *
   * @return the match index or -1.
   */
  private final int scanToChar(int[] cp, int fromIndex, int c) {
    for (int i = fromIndex; i < cp.length; ++i) {
      if (cp[i] == c) return i;
    }

    return -1;
  }

  private final int countPenalty(int[] cp, int fromIndex, int toIndex) {
    int result = 0;

    for (int i = fromIndex; i < toIndex; ++i) {
      final int c = cp[i];
      if (Character.isLetterOrDigit(c)) {
        ++result;
      }
    }

    return result;
  }

  private final int computePenalty(int[] cp, int[] path) {
    int result = 0;

    int lastIndex = path[0];
    for (int i = 1; i < path.length; ++i) {
      result += countPenalty(cp, lastIndex + 1, path[i]);
      lastIndex = path[i];
    }

    return result;
  }

  private final int incToNextLetter(int[] cp, int fromIndex) {
    for (fromIndex = fromIndex + 1; fromIndex < cp.length; ++fromIndex) {
      if (Character.isLetterOrDigit(cp[fromIndex])) break;
    }
    return fromIndex;
  }

  public final class Alignment {
    int penalty = 0;
    int baseStart = 0;
    MultiIndex candStart = null;
    int candStartInd = 0;
    int baseEnd = 0;
    int candEndInd = 0;
    boolean aligns = false;

    Alignment(int baseStart) {
      this.baseStart = baseStart;
      if (baseStart >= 0 && baseStart < cpBase.length) {
        this.candStart = new MultiIndex(cpCand, 0, cpBase[baseStart]);
        if (candStart.indeces != null) {
          this.aligns = align(baseStart, candStart);
        }
      }
    }

    public int getBaseStart() {
      return baseStart;
    }

    public int getCandStart() {
      return candStartInd;
    }

    public int getBaseEnd() {
      return baseEnd;
    }

    public int getCandEnd() {
      return candEndInd;
    }

    private final boolean align(int baseIndex, MultiIndex candIndex) {
      boolean result = false;

      baseIndex = incToNextLetter(cpBase, baseIndex);

      if (baseIndex == cpBase.length) {
        // reached end w/successful alignment
        final int[] bestCandIndeces = candIndex.getBestIndeces(); //find best 'path'
        if (bestCandIndeces != null) {
          this.penalty = computePenalty(cpCand, bestCandIndeces);
          this.candStartInd = bestCandIndeces[0];
          this.candEndInd = bestCandIndeces[bestCandIndeces.length - 1] + 1;
//          this.candEndInd = incToNextLetter(cpCand, bestCandIndeces[bestCandIndeces.length - 1]);

          result = true;
        }
      }
      else {
        final MultiIndex nextCandIndex = new MultiIndex(candIndex, cpBase[baseIndex]);
        if (nextCandIndex.indeces == null) {
          // failed to align.
          result = false;
        }
        else {
          result = align(baseIndex, nextCandIndex);
        }
      }

      return result;
    }
  }

  private final class MultiIndex {
    private int[] cp;         // codePoints
    private int[] indeces;    // indeces
    private MultiIndex next;  // next
    private MultiIndex prev;

    public MultiIndex(int[] cp, int fromIndex, int c) {
      this.cp = cp;
      this.indeces = findIndeces(cp, fromIndex, c);
      this.next = null;
      this.prev = null;
    }

    private final int[] findIndeces(int[] cp, int fromIndex, int c) {
      final List<Integer> found = new ArrayList<Integer>();
      while (fromIndex < cp.length && fromIndex >= 0) {
        fromIndex = scanToChar(cp, fromIndex, c);
        if (fromIndex >= 0) {
          found.add(fromIndex);
          fromIndex = incToNextLetter(cp, fromIndex);
        }
      }

      return convertFound(found);
    }

    private final int[] convertFound(Collection<Integer> found) {
      int[] result = null;

      final int numFound = found.size();
      if (numFound > 0) {
        result = new int[numFound];
        int index = 0;
        for (Integer f : found) {
          result[index++] = f;
        }
      }

      return result;
    }

    public MultiIndex(MultiIndex prior, int c) {
      this.cp = prior.cp;
      this.indeces = findIndeces(prior, c);
      prior.next = this;
      this.prev = prior;
    }

    private final int[] findIndeces(MultiIndex prior, int c) {
      if (prior.indeces == null) return null;

      final Set<Integer> found = new LinkedHashSet<Integer>();
      for (int i = 0; i < prior.indeces.length; ++i) {
        final int priorIndex = incToNextLetter(prior.cp, prior.indeces[i]);
        if (priorIndex < prior.cp.length) {
          final int rIndex = scanToChar(prior.cp, priorIndex, c);
          if (rIndex >= 0) {
            found.add(rIndex);
          }
        }
      }

      return convertFound(found);
    }

    public MultiIndex getLast() {
      MultiIndex result = this;
      while (result.next != null) {
        result = result.next;
      }
      return result;
    }

    public MultiIndex getFirst() {
      MultiIndex result = this;
      while (result.prev != null) {
        result = result.prev;
      }
      return result;
    }

    public int getPathLength() {
      int result = 1;
      MultiIndex mi = this;
      while (mi.next != null) {
        mi = mi.next;
        ++result;
      }
      return result;
    }

    private final List<int[]> getMatchPaths() {
      if (indeces == null) return null;

      final List<int[]> result = new ArrayList<int[]>();

      final MultiIndex multiIndex = getFirst();
      for (int i = 0; i < multiIndex.indeces.length; ++i) {
        final int[] matchPath = multiIndex.getMatchPath(multiIndex.indeces[i]);
        if (matchPath != null) result.add(matchPath);
      }

      return result;
    }

    private final int[] getMatchPath(int index) {
      int[] result = new int[getPathLength()];

      int pos = 0;
      result[pos++] = index;
      MultiIndex mi = this;

      while (mi.next != null) {
        mi = mi.next;
        final int nextIndex = mi.getBestIndex(index);

        if (nextIndex < 0) {
          // failed to build a path.
          result = null;
          break;
        }

        result[pos++] = nextIndex;
        index = nextIndex;
      }

      return result;
    }

    private final int getBestIndex(int index) {
      // get the indeces[k] greater than and closest to index
      int result = -1;
      int min = -1;

      if (indeces != null) {
        for (int nextIndex : indeces) {
          if (nextIndex > index) {
            if (min < 0 || nextIndex - index < min) {
              min = nextIndex - index;
              result = nextIndex;
            }
          }
        }
      }

      return result;
    }

    public int[] getBestIndeces() {
      final List<int[]> matchPaths = getMatchPaths();
      if (matchPaths == null) return null;

      // pick the best path.
      int[] bestPath = null;
      int minPenalty = -1;
      for (int[] matchPath : matchPaths) {
        final int curPenalty = computePenalty(cp, matchPath);
        if (minPenalty < 0 || curPenalty < minPenalty) {
          bestPath = matchPath;
          minPenalty = curPenalty;
        }
      }

      return bestPath;
    }
  }

  public static void main(String[] args) {
    final StringAligner aligner = new StringAligner(args[0], args[1]);

    System.out.println("aligns('" + args[0] + "', '" + args[1] + "')=" + aligner.aligns() + " penalty=" + aligner.getPenalty());
  }
}
