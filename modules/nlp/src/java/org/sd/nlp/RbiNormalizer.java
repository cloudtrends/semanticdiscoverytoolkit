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


import org.sd.nlp.AbstractNormalizer;
import org.sd.nlp.NormalizedString;
import org.sd.nlp.StringWrapper;
import org.sd.util.StringSplitter;
import org.sd.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Normalizer for rbi text.
 * <p><ul>
 * <li>keep all letters, digits, percent signs</li>
 * <li>lowercase</li>
 * <li>replace diacritics</li>
 * <li>replace slash with whitespace if preceded or followed by 2 consecutive letters; otherwise, leave slash.</li>
 * <li>remove parens, adding pre '(' or post ')' space if necessary
 * <li>remove periods not before a digit, adding post space if necessary
 * <li>leave all dashes but remove all whitespace before & after</li>
 * <li>replace underscore with whitespace
 * <li>drop all other symbols (like single-quotes, etc.)</li>
 * </ul>
 * @author Spence Koehler
 */
public class RbiNormalizer extends AbstractNormalizer {
  
  private static final RbiNormalizer INSTANCE = new RbiNormalizer(false);
  private static final RbiNormalizer CS_INSTANCE = new RbiNormalizer(true);

  public static final RbiNormalizer getInstance() {
    return INSTANCE;
  }

  public static final RbiNormalizer getCaseSensitiveInstance() {
    return CS_INSTANCE;
  }

  private boolean caseSensitive;

  private RbiNormalizer(boolean caseSensitive) {
    this.caseSensitive = caseSensitive;
  }

  public final NormalizedString normalize(StringWrapper.SubString subString) {
    final StringBuilder normalized = new StringBuilder();

    final String inputString = subString.originalSubString;
    final List<Integer> n2oIndexList = new ArrayList<Integer>();
    
    boolean keepCaps = false;
    if (!caseSensitive) {
      // determine whether to keep all-caps segments (i.e. acronyms) as all-caps
      // only do this if the string is a single word or is not all-caps
      boolean foundLower = false;
      boolean foundSpace = false;
      final char[] chars = subString.getOriginalChars();
      for (char c : chars) {
        if (c == ' ' || c == '.') foundSpace = true;  // treat '.' like a space so all-caps domains don't remain all-caps
        if (Character.isLowerCase(c)) foundLower = true;
        if (foundSpace && foundLower) break;
      }
      keepCaps = (foundSpace && foundLower) || (!foundSpace && !foundLower);
    }
    final StringBuilder buffer = keepCaps ? new StringBuilder() : null;

    int wasWhite = -1;
    int wasDash = -1;
    boolean sawLetter = false;
    for (StringUtil.StringContext context = new StringUtil.StringContext(2, inputString.trim()); context.hasNext(); ) {
      final int codePoint = context.next();
      int pos = context.getPosition();

      if (isNormal(codePoint)) {
        StringBuilder theBuffer = normalized;
        boolean altBuffer = false;
        boolean toUpper = false;
        if (keepCaps && Character.isUpperCase(codePoint)) {
          // make sure we're not after a dot
          if (context.getPrevCodePoint(1) != '.') {
            theBuffer = buffer;
            altBuffer = true;
            toUpper = true;
          }
        }

        if ((wasWhite >= 0) && (wasDash < 0)) {
          normalized.append(' ');
          n2oIndexList.add(wasWhite);
        }
        wasWhite = -1;
        if (wasDash >= 0) {
          normalized.append('-');
          
          n2oIndexList.add(wasDash);
          wasDash = -1;
        }

        int num = StringSplitter.appendReplacementDiacritic(getProperCase(codePoint), theBuffer);
        for (int i = 0; i < num; ++i) n2oIndexList.add(pos + i);

        // spin to end of consecutive letters.
        while (context.hasNext() && isNormal(context.getCodePoint(1))) {
          final int curCodePoint = context.next();
          num = StringSplitter.appendReplacementDiacritic(getProperCase(curCodePoint), theBuffer);
          pos = context.getPosition();
          if (toUpper) toUpper = Character.isUpperCase(curCodePoint);
          for (int i = 0; i < num; ++i) n2oIndexList.add(pos + i);
        }
        sawLetter = true;

        if (altBuffer) {
          normalized.append(toUpper ? theBuffer.toString().toUpperCase() : theBuffer);
          buffer.setLength(0);
        }
      }
      else if (StringUtil.isWhite(codePoint)) {
        wasWhite = ((wasDash < 0) && sawLetter) ? pos : -1;
        // spin to end of consecutive whitespace.
        while (context.hasNext() && StringUtil.isWhite(context.getCodePoint(1))) context.next();
      }
      else if (codePoint == '-') {
        // if preceded and followed by whitespace, treat as whitespace (let wasWhite "ride")
        if (wasWhite < 0 || !StringUtil.isWhite(context.getNextCodePoint(1))) {
          // otherwise, treat as dash
          wasWhite = -1;
          wasDash = pos;
        }
      }
      else if (codePoint == '.') {
        // if preceded by a letter and followed by non-white (i.e. digit or letter), treat as whitespace
        if (!StringUtil.isWhite(context.getNextCodePoint(1)) && Character.isLetter(context.getPrevCodePoint(1))) {
          wasWhite = pos;
        }
        else if (Character.isDigit(context.getNextCodePoint(1))) {
          // leave period before a digit
          normalized.appendCodePoint(codePoint);
          n2oIndexList.add(pos);
        }

        // otherwise, ignore as if not present
      }
      else if (codePoint == '/' || codePoint == '\\') {
        // replace slash with whitespace if preceded or followed by whitespace or 2 consecutive letters
        // and not followed by a digit.
        if (!(Character.isDigit(context.getNextCodePoint(1))) &&
            ((StringUtil.isWhite(context.getPrevCodePoint(1)) || StringUtil.isWhite(context.getNextCodePoint(1))) ||
             (Character.isLetter(context.getNextCodePoint(1)) && Character.isLetter(context.getNextCodePoint(2))) ||
             (Character.isLetter(context.getPrevCodePoint(1)) && Character.isLetter(context.getPrevCodePoint(2))))) {
          wasWhite = ((wasDash < 0) && sawLetter) ? pos : -1; // pretend like there was whitespace
        }
        else {
          // leave slash
          normalized.appendCodePoint(codePoint);
          n2oIndexList.add(pos);
        }
      }
      else if (codePoint == '_') {
        // replace underscore with whitespace
        wasWhite = ((wasDash < 0) && sawLetter) ? pos : -1; // pretend like there was whitespace
      }
      else if (codePoint == '(' || codePoint == ')' ||
               codePoint == '{' || codePoint == '}' ||
               codePoint == '[' || codePoint == ']') {
        // pretend like there was whitespace
        wasWhite = sawLetter ? pos : -1;
      }
      else if (codePoint == '"') {
        // drop unless preceded by a digit
        if (Character.isDigit(context.getPrevCodePoint(1))) {
          normalized.appendCodePoint(codePoint);
          n2oIndexList.add(pos);
        }
      }
      //else: drop all other symbols
    }

    return new GeneralNormalizedString(subString.stringWrapper, normalized.toString(), n2oIndexList, false);
  }

  private final boolean isNormal(int codePoint) {
    // let letters, digits, and '%' through as normal characters.
    return Character.isLetterOrDigit(codePoint) || codePoint == '%';
  }

  private final int getProperCase(int codePoint) {
    return caseSensitive ? codePoint : Character.toLowerCase(codePoint);
  }

  public static void main(String[] args) throws Exception {
//    final RbiNormalizer normalizer = RbiNormalizer.getCaseSensitiveInstance();
    final RbiNormalizer normalizer = RbiNormalizer.getInstance();
    doNormalize(normalizer, args);
  }
}
