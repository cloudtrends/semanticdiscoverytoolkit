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
package org.sd.util;


import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Utility to match character subsequences of a source string within a target
 * string. One application of this is for highlighting search hits (targets)
 * based on queries (sources).
 * <p>
 * @author Spence Koehler
 */
public class CharacterMatcher {

  private String source;
  private String target;

  private Set<Match> _matches;
  private List<Substring> _split;
  private Integer _maxMatchLen;

  /**
   * Construct with the given source (characters to find) and target
   * (string in which to find sequences of the characters.)
   */
  public CharacterMatcher(String source, String target) {
    this.source = source;
    this.target = target;

    this._matches = null;
    this._split = null;
    this._maxMatchLen = null;
  }

  /**
   * Get the matches of the source in the target.
   */
  public Set<Match> getMatches() {
    if (_matches == null) {
      _matches = computeMatches();
    }
    return _matches;
  }

  /**
   * Split the target into matching/non-matching substrings.
   */
  public List<Substring> split() {
    if (_split == null) {
      _split = computeSplit();
    }
    return _split;
  }

  /**
   * Get just those substrings that represent matches and have the longest
   * match length found.
   */
  public List<Substring> getLongestMatches() {
    final List<Substring> result = new ArrayList<Substring>();

    final List<Substring> split = split();
    int maxlen = (_maxMatchLen == null) ? 0 : _maxMatchLen;
    for (Substring substring : split) {
      if (!substring.isMatch()) continue;
      final int curlen = substring.length();
      if (curlen > maxlen) {
        result.clear();
        maxlen = curlen;
      }
      if (curlen == maxlen) {
        result.add(substring);
      }
    }
    if (_maxMatchLen == null) _maxMatchLen = maxlen;

    return result;
  }

  /**
   * Get the length of the longest match.
   */
  public int getMaxMatchLength() {
    if (_maxMatchLen == null) {
      final List<Substring> split = split();
      int maxlen = 0;
      for (Substring substring : split) {
        if (!substring.isMatch()) continue;
        final int curlen = substring.length();
        if (curlen > maxlen) maxlen = curlen;
      }
      _maxMatchLen = maxlen;
    }
    return _maxMatchLen;
  }

  private Set<Match> computeMatches() {
    final Set<Match> result = new TreeSet<Match>();

    final int sourceLen= source.length();
    final int targetLen = target.length();

    for (int sourcePos = 0; sourcePos < sourceLen; ++sourcePos) {
      final char c = source.charAt(sourcePos);
      int targetPos = target.indexOf(c);
      while (targetPos >= 0) {
        while (targetPos >= 0 && contains(result, targetPos)) {
          targetPos = target.indexOf(c, targetPos + 1);
        }
        if (targetPos >= 0) {
          int sourceEndPos = sourcePos + 1;
          int targetEndPos = targetPos + 1;

          while (sourceEndPos < sourceLen && targetEndPos < targetLen) {
            if (contains(result, targetEndPos)) break; // avoid overlap
            final char sc = source.charAt(sourceEndPos);
            final char tc = target.charAt(targetEndPos);
            if (sc != tc) break;
            ++sourceEndPos;
            ++targetEndPos;
          }

          result.add(new Match(sourcePos, sourceEndPos, targetPos, targetEndPos));
          targetPos = target.indexOf(c, targetEndPos);
        }
      }
    } 

    return result;
  }

  private final List<Substring> computeSplit() {
    final List<Substring> result = new ArrayList<Substring>();
    final Set<Match> matches = getMatches();

    int lastEndPos = 0;
    for (Match match : matches) {
      if (match.targetStartPos > lastEndPos) {
        result.add(new Substring(lastEndPos, match.targetStartPos));
      }
      result.add(new Substring(match));
      lastEndPos = match.targetEndPos;
    }
    if (target.length() > lastEndPos) {
      result.add(new Substring(lastEndPos, target.length()));
    }

    return result;
  }

  private final boolean contains(Set<Match> matches, int targetPos) {
    boolean result = false;
    for (Match match : matches) {
      if (match.contains(targetPos)) {
        result = true;
        break;
      }
    }
    return result;
  }


  /**
   * Container for match information, correlating source string positions
   * with target string positions for the match.
   * <p>
   * @author Spence Koehler
   */
  public final class Match implements Comparable<Match> {
    public final int sourceStartPos;
    public final int sourceEndPos;
    public final int targetStartPos;
    public final int targetEndPos;

    Match(int sourceStartPos, int sourceEndPos, int targetStartPos, int targetEndPos) {
      this.sourceStartPos = sourceStartPos;
      this.sourceEndPos = sourceEndPos;
      this.targetStartPos = targetStartPos;
      this.targetEndPos = targetEndPos;
    }

    public boolean contains(int targetPos) {
      return targetPos >= targetStartPos && targetPos < targetEndPos;
    }

    public int compareTo(Match other) {
      // sort by earliest occurences in the target
      int result = this.targetStartPos - other.targetStartPos;

      if (result == 0) {
        // break ties by looking at target endpoint
        result = this.targetEndPos - other.targetEndPos;

        if (result == 0) {
          // break ties by looking at source positions
          result = this.sourceStartPos - other.sourceStartPos;

          if (result == 0) {
            // break ties by looking at source endpoint
            result = this.sourceEndPos - other.sourceEndPos;
          }
        }
      }

      return result;
    }
  }

  /**
   * Container for a substring of the target along with its source match
   * information when applicable.
   * <p>
   * Substrings are intended to partition matching from non-matching portions
   * of the target string.
   *
   * @author Spence Koehler
   */
  public final class Substring {
    private Match match;
    private int _startPos;
    private int _endPos;

    Substring(Match match) {
      this.match = match;
    }

    Substring(int startPos, int endPos) {
      this.match = null;
      this._startPos = startPos;
      this._endPos = endPos;
    }

    public boolean isMatch() {
      return match != null;
    }

    public int getStartPos() {
      return (match == null) ? _startPos : match.targetStartPos;
    }

    public int getEndPos() {
      return (match == null) ? _endPos : match.targetEndPos;
    }

    public int length() {
      return getEndPos() - getStartPos();
    }

    public String toString() {
      return target.substring(getStartPos(), getEndPos());
    }
  }
}
