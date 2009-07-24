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


import org.sd.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utility to compute the edit distance between two strings.
 * <p>
 * @author Spence Koehler
 */
public class EditDistance {

  /**
   * Compute levenshtein edit distance between the two strings.
   * <p>
   * Operates in O(n-squared) time.
   */
  public static final int lev(String a, String b) {
    return lev(a.toCharArray(), b.toCharArray());
  }

  /**
   * Compute levenshtein edit distance between the two strings.
   * <p>
   * Operates in O(n-squared) time.
   */
  public static final int lev(char[] a, char[] b) {
    final int alen = a.length;
    final int blen = b.length;

    final int arr[][] = new int[blen + 1][alen + 1];

    for (int i = 0; i <= alen; ++i) {
      arr[0][i] = i;
    }
    for (int j = 0; j <= blen; ++j) {
      arr[j][0] = j;
    }

    int add = 0;
    int l = 0;
    int m = 0;
    int n = 0;

    for (int j = 1; j <= blen; ++j) {
      for (int i = 1; i <= alen; ++i) {
        add = (a[i - 1] == b[j - 1]) ? 0 : 1;
        m = 1 + arr[j - 1][i];
        l = 1 + arr[j][i - 1];
        n = add + arr[j - 1][i - 1];
        arr[j][i] = (m < l ? (m < n ? m : n) : (l < n ? l : n));
      }
    }

    return arr[blen][alen];
  }

  /**
   * Compute an edit distance between two arrays of strings.
   */
  public static final int lev(String[] a, String[] b) {
    final int alen = a.length;
    final int blen = b.length;

    final int arr[][] = new int[blen + 1][alen + 1];

    for (int i = 0; i <= alen; ++i) {
      arr[0][i] = i;
    }
    for (int j = 0; j <= blen; ++j) {
      arr[j][0] = j;
    }

    int add = 0;
    int l = 0;
    int m = 0;
    int n = 0;

    for (int j = 1; j <= blen; ++j) {
      for (int i = 1; i <= alen; ++i) {
        add = a[i - 1].equals(b[j - 1]) ? 0 : 1;

        m = 1 + arr[j - 1][i];
        l = 1 + arr[j][i - 1];
        n = add + arr[j - 1][i - 1];
        arr[j][i] = (m < l ? (m < n ? m : n) : (l < n ? l : n));
      }
    }

    return arr[blen][alen];
  }

  /**
   * Compute levenshtein edit distance between the two strings.
   * <p>
   * Operates in O(n-squared) time.
   */
  public static final int lev(int[] a, int[] b) {
    final int alen = a.length;
    final int blen = b.length;

    final int arr[][] = new int[blen + 1][alen + 1];

    for (int i = 0; i <= alen; ++i) {
      arr[0][i] = i;
    }
    for (int j = 0; j <= blen; ++j) {
      arr[j][0] = j;
    }

    int add = 0;
    int l = 0;
    int m = 0;
    int n = 0;

    for (int j = 1; j <= blen; ++j) {
      for (int i = 1; i <= alen; ++i) {
        add = (a[i - 1] == b[j - 1]) ? 0 : 1;
        m = 1 + arr[j - 1][i];
        l = 1 + arr[j][i - 1];
        n = add + arr[j - 1][i - 1];
        arr[j][i] = (m < l ? (m < n ? m : n) : (l < n ? l : n));
      }
    }

    return arr[blen][alen];
  }

  /**
   * Modified algorithm to not penalize for char additions from abbrev to full.
   * <p>
   * Algorithm: if abbrev is longer than full, compute normal lev;
   *            if full starts with abbrev (ignoring any trailing . on abbrev),
   *            then the score is 0. Otherwise, the score is computed by levVowel.
   */
  public static int levAbbrev(String abbrev, String full) {
    return levAbbrev(abbrev.toCharArray(), full.toCharArray());
  }

  /**
   * Modified algorithm to not penalize for char additions from abbrev to full.
   * <p>
   * Algorithm: if abbrev is longer than full or the first chars do not agree,
   *              return -1;
   *            otherwise, count chars in abbrev that are not in full (in sequence).
   *            If abbrev has a period, inc to next word in full to keep matching.
   */
  public static final int levAbbrev(char[] a, char[] b) {
    int alen = a.length;
    final int blen = b.length;

    if (alen > 0 && a[alen - 1] == '.') {
      --alen;
    }

    if (alen >= blen || a[0] != b[0]) {
      return -1;
    }

    int ai = 1;
    int bi = 1;

    while (ai < alen && bi < blen) {
      if (a[ai] == b[bi]) {
        ++ai;
        ++bi;
      }
      else {
        // if a[ai] == '.', inc b to 1st letter beyond a space or a dot; inc a until find a letter.
        if (a[ai] == '.') {
          ai++;
          while (ai < alen && !Character.isLetter(a[ai])) {
            ++ai;
          }
          while (bi < blen && b[bi] != ' ') {
            ++bi;
          }
          while (bi < blen && !Character.isLetter(b[bi])) {
            ++bi;
          }
        }
        else {
          ++bi;
        }
      }
    }

    return alen - ai;
  }

  public static final int computeUnorderedDistance(String s1, String s2) {
    final int[] cp1 = toSortedCodePoints(s1);
    final int[] cp2 = toSortedCodePoints(s2);
    
    return lev(cp1, cp2);
  }

  /**
   * Convert the string to sorted code points of lower-cased letters and digits.
   */
  public static final int[] toSortedCodePoints(String string) {
    final List<Integer> integers = new ArrayList<Integer>();

    for (StringUtil.StringIterator iter = new StringUtil.StringIterator(string); iter.hasNext(); ) {
      final StringUtil.StringPointer pointer = iter.next();
      final int cp = pointer.codePoint;
      if (Character.isLetterOrDigit(cp)) {
        integers.add(Character.toLowerCase(cp));
      }
    }

    final int[] result = new int[integers.size()];

    int index = 0;
    for (Integer integer : integers) {
      result[index++] = integer;
    }

    Arrays.sort(result);

    return result;
  }


  public static void main(String[] args) {
    String ai = args[0];
    String bi = args[1];

    ai = "smith john";
//    ai = "john carl smith";
    bi = "john smith";

    String[] a = ai.split(" ");
    String[] b = bi.split(" ");

    System.out.print("input: '" + ai + "' '" + bi + "'");
    System.out.println();
    System.out.print("a: ");
    for (int i = 0; i < a.length; ++i) {
      System.out.print("'" + a[i] + "' ");
    }
    System.out.println();
    System.out.print("b: ");
    for (int i = 0; i < b.length; ++i) {
      System.out.print("'" + b[i] + "' ");
    }
    System.out.println();

    System.out.println("lev1=" + lev(ai, bi));
    System.out.println("lev2=" + lev(a, b));

    System.out.println("lev1(ab,bc)=" + lev("ab", "bc"));
    System.out.println("lev1(ab,ba)=" + lev("ab", "ba"));
    System.out.println("lev1(carl,eric)=" + lev("carl", "eric"));
  }
}
