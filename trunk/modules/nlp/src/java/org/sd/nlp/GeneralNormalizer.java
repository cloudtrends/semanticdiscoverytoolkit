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
package org.sd.nlp;


import java.util.ArrayList;
import java.util.List;

import java.io.IOException;

/**
 * A general normalization implementation suitable for most applications.
 * <ul>
 * <li>All letter, digit, '+', and '&amp;' characters are kept.
 * <li>All letters are lowercased. (optional)</li>
 * <li>All periods are dropped unless between two digits, at the beginning of the string or after a +, -, or space</li>
 * <li>All colons are dropped unless between two digits.</li>
 * <li>All dashes remain, but whitespace before &amp; after is removed.</li>
 * <li>All single-quotes are dropped unless preceded and followed by a letter.</li>
 * <li>All forward slashes preceded or followed by 2 consecutive letters are replaced with a space; others remain.</li>
 * <li>All extra whitespace is removed.</li>
 * <li>Diacritics are not replaced.</li>
 * <li>Asian date markers (year, month, day, hour, minute) are replaced with '/' or ':'</li>
 * <li>All other symbols are dropped.</li>
 * </ul>
 *
 * @author Spence Koehler
 */
public class GeneralNormalizer extends AbstractNormalizer {
  
  private boolean commonCase;

  private static final GeneralNormalizer CASE_SENSITIVE_INSTANCE = new GeneralNormalizer(false);
  private static final GeneralNormalizer CASE_INSENSITIVE_INSTANCE = new GeneralNormalizer(true);

  public static final GeneralNormalizer getCaseSensitiveInstance() {
    return CASE_SENSITIVE_INSTANCE;
  }

  public static final GeneralNormalizer getCaseInsensitiveInstance() {
    return CASE_INSENSITIVE_INSTANCE;
  }

  public GeneralNormalizer(boolean commonCase) {
    super();
    this.commonCase = commonCase;
  }

  /**
   * Normalize the substring's original text.
   */
  public NormalizedString normalize(StringWrapper.SubString subString) {
    if (subString == null) return GeneralNormalizedString.EMPTY;

    final StringBuilder normalized = new StringBuilder();
    final List<Integer> n2oIndexList = new ArrayList<Integer>();

    int prev = 0;
    int needSpace = -1;
    int expectAsianAmPmMarker = -1;
    boolean willNeedSpace = false;

    final StringWrapper sw = subString.stringWrapper;
    final Break[] breaks = sw.getBreaks();

    for (int i = subString.startPos; i < subString.endPos; ++i) {
      int cp = sw.getCodePoint(i);
      if (cp == ' ') {
        if (prev > 0 && prev != '-') {
          prev = cp;
          needSpace = i;
        }
        continue;
      }
      else if (cp == '.') {
        boolean keep = false;
        if ((i + 1) < subString.endPos) {
          int next = sw.getCodePoint(i + 1);
          if (next <= '9' && next >= '0') {
            if (i == subString.startPos) keep = true;
            else {
              keep = (prev == ' ') || (prev == '+') || (prev == '-') || (prev <= '9' && prev >= '0');
            }
          }
        }
        if (!keep) {
          prev = cp;
          continue;
        }
      }
      else if (cp == ':') {
        boolean keep = false;
        if ((prev <= '9' && prev >= '0') && ((i + 1) < subString.endPos) && breaks[i + 1] == Break.NONE) {
          int next = sw.getCodePoint(i + 1);
          keep = (next <= '9' && next >= '0');
        }
        if (!keep) {
          prev = cp;
          continue;
        }
      }
      else if (cp == '-') {
        needSpace = -1;
      }
      else if (cp == '\'') {
        if ((i == subString.startPos) || ((i + 1) == subString.endPos) || (breaks[i - 1] != Break.NONE) || (breaks[i + 1] != Break.NONE) ||
            !Character.isLetter(sw.getCodePoint(i - 1)) || !Character.isLetter(sw.getCodePoint(i + 1))) {
          prev = cp;
          continue;
        }
      }
      else if (cp == '/') {
        if (((i >= subString.startPos + 2) && breaks[i - 1] == Break.NONE && breaks[i - 2] == Break.NONE) ||
            (((i + 2) < subString.endPos) && breaks[i + 1] == Break.NONE && breaks[i + 2] == Break.NONE)) {
          if (prev > 0 && prev != '-') {
            needSpace = i;
            prev = ' ';
          }
          continue;
        }
      }
      else if (cp == '+' || cp == '&') {
        // drop through to keep these symbols
      }
      else if (breaks[i].skip()) {  // is a symbol
        prev = cp;
        needSpace = i;
        continue;  // squash
      }
      else if (cp == 24180 || cp == 26376) {  // asian year, month markers
        cp = '/';
      }
      else if (cp == 26085) {  // asian day marker (squash)
        needSpace = i;
        continue;
      }
      else if ((cp == 19978 || cp == 19979) && (i + 1) < subString.endPos) {  // asian "a" and "p" (semantically) in am/pm
        final int next = sw.getCodePoint(i + 1);
        if (next == 21320) {  // asian am/pm marker
          cp = (cp == 19978) ? 'a' : 'p';
          expectAsianAmPmMarker = i + 1;
        }
      }
      else if (cp == 21320 && (i == expectAsianAmPmMarker)) {
        willNeedSpace = true;
        cp = 'm';
      }
      else if (cp == 26178 || cp == 20998) {  // asian hour, minute marker
        cp = ':';
      }
      else {  // is letter or digit
        if (commonCase) cp = Character.toLowerCase(cp);
      }

      if (needSpace >= 0) {
        if (normalized.length() > 0) {
          normalized.append(' ');
          n2oIndexList.add(needSpace);
        }
        needSpace = -1;
      }
      normalized.appendCodePoint(cp);
      n2oIndexList.add(i);
      prev = cp;

      if (willNeedSpace) {
        needSpace = i + 1;
        willNeedSpace = false;
      }
    }

    final int[] n2oIndexes = new int[n2oIndexList.size()];
    int pos = 0;
    for (Integer n2oIndex : n2oIndexList) {
      n2oIndexes[pos++] = n2oIndex;
    }

    return new GeneralNormalizedString(subString.stringWrapper, normalized.toString(), n2oIndexes, true);
  }

  public boolean equals(Object o) {
    boolean result = (this == o);

    if (!result && o instanceof GeneralNormalizer) {
      final GeneralNormalizer other = (GeneralNormalizer)o;
      result = (this.commonCase == other.commonCase);
    }

    return result;
  }

  public int hashCode() {
    return commonCase ? 1 : 3;
  }

  public static void main(String[] args) throws IOException {
    final GeneralNormalizer normalizer = GeneralNormalizer.getCaseInsensitiveInstance();
    doNormalize(normalizer, args);
  }
}
