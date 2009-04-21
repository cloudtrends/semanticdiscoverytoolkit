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
package org.sd.text.align;


import org.sd.nlp.NormalizedString;
import org.sd.util.StringUtil;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * Utility class for aligning two phrases.
 * <p>
 * @author Spence Koehler
 */
public class PhraseAligner extends AbstractAligner<String> {
  
  public static final PhraseAligner getInstance(String phrase1, String phrase2) {
    final NormalizedString nphrase1 = AlignmentNormalizer.getInstance().normalize(phrase1);
    final NormalizedString nphrase2 = AlignmentNormalizer.getInstance().normalize(phrase2);
    final String[] flat1 = getFlatTokens(nphrase1);
    final String[] flat2 = getFlatTokens(nphrase2);

    return new PhraseAligner(phrase1, phrase2, nphrase1, nphrase2, flat1, flat2);
  }

  private String phrase1;
  private String phrase2;
  private NormalizedString nphrase1;
  private NormalizedString nphrase2;
  private String[] flat1;
  private String[] flat2;

  private Map<Character, Map<String, LinkedList<Integer>>> c2skipped2poss = null;
  private Map<Character, Map<String, LinkedList<Integer>>> c2missed2poss = null;

  private PhraseAligner(String phrase1, String phrase2,
                        NormalizedString nphrase1, NormalizedString nphrase2,
                        String[] flat1, String[] flat2) {
    super(flat1, flat2);

    this.phrase1 = phrase1;
    this.phrase2 = phrase2;
    this.nphrase1 = nphrase1;
    this.nphrase2 = nphrase2;
    this.flat1 = flat1;
    this.flat2 = flat2;
  }

  //todo: add methods for scoring taking flat matches into account

  private static final String[] getFlatTokens(NormalizedString nstring) {
    final String[] result = nstring.split();
    for (int i = 0; i < result.length; ++i) {
      result[i] = flatten(result[i]);
    }
    return result;
  }

  public static final String flatten(String string) {
    // take off a trailing 's' for strings longer than 3 chars and flatten
    int len = string.length();
    if (len > 3 && string.charAt(len - 1) == 's') --len;
    return StringUtil.flatten(string, 0, len);
  }


  protected void markSkipped(String e2, int pos2) {
    if (c2skipped2poss == null) c2skipped2poss = new HashMap<Character, Map<String, LinkedList<Integer>>>();
    mark(c2skipped2poss, e2, pos2);
  }

  protected void markMissed(String e1, int pos1) {
    if (c2missed2poss == null) c2missed2poss = new HashMap<Character, Map<String, LinkedList<Integer>>>();
    mark(c2missed2poss, e1, pos1);
  }

  protected int getMissedPenalty(String e1, int pos1) {
    return getPenalty(c2missed2poss, e1, pos1);
  }

  protected int getSkippedPenalty(String e2, int pos2) {
    return getPenalty(c2skipped2poss, e2, pos2);
  }

  private final void mark(Map<Character, Map<String, LinkedList<Integer>>> c2elt2poss, String e, int pos) {
    if (e != null && e.length() > 0) {
      final Character c = e.charAt(0);
      Map<String, LinkedList<Integer>> elt2poss = c2elt2poss.get(c);
      if (elt2poss == null) {
        elt2poss = new HashMap<String, LinkedList<Integer>>();
        c2elt2poss.put(c, elt2poss);
      }
      mark(elt2poss, e, pos);
    }
  }

  private final int getPenalty(Map<Character, Map<String, LinkedList<Integer>>> c2elt2poss, String e, int pos) {
    int result = 0;

    if (c2elt2poss != null && e.length() > 0) {
      final Character c = e.charAt(0);
      final Map<String, LinkedList<Integer>> elt2poss = c2elt2poss.get(c);
      if (elt2poss != null) {

        //NOTE: hafta iterate through because equals and hashCode don't apply to fuzzy matches
        for (Map.Entry<String, LinkedList<Integer>> entry : elt2poss.entrySet()) {
          if (aligns(e, entry.getKey())) {  // found a match!
            final LinkedList<Integer> poss = entry.getValue();
            if (poss != null && poss.size() > 0) {
              final int pos1 = poss.removeFirst();
              result = pos - pos1;
            }
            break;
          }
        }
      }
    }

    return result;
  }

  private final boolean aligns(String string1, String string2) {
    //todo: use a cache here?
    final WordAligner wordAligner = new WordAligner(string1, string2);
    return wordAligner.isFuzzyMatch();
  }

  public static void main(String[] args) {
    // arg1: phrase1
    // arg2: phrase2

    final PhraseAligner aligner = PhraseAligner.getInstance(args[0], args[1]);
    final org.sd.util.LineBuilder builder = new org.sd.util.LineBuilder();

    builder.append(args[0]).append(args[1]).
      append("matchScore=" + aligner.getMatchScore()).
      append("unmatchedCount=" + aligner.getUnmatchedCount()).
      append("extraCount=" + aligner.getExtraCount()).
      append("exactMatch=" + aligner.isExactMatch()).
      append("fuzzyMatch=" + aligner.isFuzzyMatch());

    System.out.println(builder.toString());
  }
}
