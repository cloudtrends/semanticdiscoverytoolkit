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


import org.sd.nlp.AbstractNormalizer;
import org.sd.nlp.GeneralNormalizedString;
import org.sd.nlp.NormalizedString;
import org.sd.nlp.StringWrapper;
import org.sd.util.StringSplitter;
import org.sd.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * A normalizer for tokenizing words in phrases for alignment.
 * <p><ul>
 * <li>keep all letters, digits, percent signs</li>
 * <li>lowercase</li>
 * <li>replace diacritics</li>
 * <li>remove parens, adding pre '(' or post ')' space if necessary</li>
 * <li>keep periods before a digit</li>
 * <li>squash a single quote preceded and followed by a letter</li>
 * <li>(disabled) squash all symbols that are preceded or followed by 2 or 1 consecutive letters</li>
 * <li>leave all other symbols</li>
 * </ul>
 *
 * @author Spence Koehler
 */
public class AlignmentNormalizer extends AbstractNormalizer {
  
  private static final AlignmentNormalizer INSTANCE = new AlignmentNormalizer();

  public static final AlignmentNormalizer getInstance() {
    return INSTANCE;
  }

  private AlignmentNormalizer() {
  }

  public final NormalizedString normalize(StringWrapper.SubString subString) {
    final StringBuilder normalized = new StringBuilder();

    final String inputString = subString.originalSubString;
    final List<Integer> n2oIndexList = new ArrayList<Integer>();
    
    int wasWhite = -1;
    boolean sawLetter = false;
    for (StringUtil.StringContext context = new StringUtil.StringContext(3, inputString.trim()); context.hasNext(); ) {
      final int codePoint = context.next();
      int pos = context.getPosition();

      if (isNormal(codePoint)) {
        if (wasWhite >= 0) {
          normalized.append(' ');
          n2oIndexList.add(wasWhite);
        }
        wasWhite = -1;
        int num = StringSplitter.appendReplacementDiacritic(getProperCase(codePoint), normalized);
        for (int i = 0; i < num; ++i) n2oIndexList.add(pos + i);

        // spin to end of consecutive letters.
        while (context.hasNext() && isNormal(context.getCodePoint(1))) {
          num = StringSplitter.appendReplacementDiacritic(getProperCase(context.next()), normalized);
          pos = context.getPosition();
          for (int i = 0; i < num; ++i) n2oIndexList.add(pos + i);
        }
        sawLetter = true;
      }
      else if (StringUtil.isWhite(codePoint)) {
        wasWhite = sawLetter ? pos : -1;
        // spin to end of consecutive whitespace.
        while (context.hasNext() && StringUtil.isWhite(context.getCodePoint(1))) context.next();
      }
      else if (codePoint == '(' || codePoint == ')' ||
               codePoint == '{' || codePoint == '}' ||
               codePoint == '[' || codePoint == ']') {
        // pretend like there was whitespace
        wasWhite = sawLetter ? pos : -1;
      }
      else if (codePoint == '.' && Character.isDigit(context.getCodePoint(1))) {
        // if preceding a digit, keep.
        normalized.appendCodePoint(codePoint);
        n2oIndexList.add(pos);
      }
      else if (codePoint == '\'' && Character.isLetter(context.getCodePoint(1)) && Character.isLetter(context.getCodePoint(-1))) {
        // squash == do nothing
      }
//      else if (hasOneOrTwoLettersAfter(context) || hasOneOrTwoLettersBefore(context)) {
//        // squash symbols before or after 1 or 2 letters ==> do nothing.
//      }
      else {
        // keep all other symbols.
        normalized.appendCodePoint(codePoint);
        n2oIndexList.add(pos);
      }

    }

    return new GeneralNormalizedString(subString.stringWrapper, normalized.toString(), n2oIndexList);
  }

  private final boolean isNormal(int codePoint) {
    // let letters, digits, and '%' through as normal characters.
    return Character.isLetterOrDigit(codePoint) || codePoint == '%';
  }

  private final int getProperCase(int codePoint) {
    return Character.toLowerCase(codePoint);
  }

//   private final boolean hasOneOrTwoLettersAfter(StringUtil.StringContext context) {
//     boolean result = false;

//     if (Character.isLetter(context.getCodePoint(1))) {
//       result = true;
//       if (Character.isLetter(context.getCodePoint(2))) {
//         result = !Character.isLetter(context.getCodePoint(3));
//       }
//     }

//     return result;
//   }

//   private final boolean hasOneOrTwoLettersBefore(StringUtil.StringContext context) {
//     boolean result = false;

//     if (Character.isLetter(context.getCodePoint(-11))) {
//       result = true;
//       if (Character.isLetter(context.getCodePoint(-2))) {
//         result = !Character.isLetter(context.getCodePoint(-3));
//       }
//     }

//     return result;
//   }

  public static void main(String[] args) {
    final AlignmentNormalizer normalizer = AlignmentNormalizer.getInstance();

    for (int i = 0; i < args.length; ++i) {
      final NormalizedString nstring = normalizer.normalize(args[i]);
      final String[] tokens = nstring.split();

      final org.sd.util.LineBuilder builder = new org.sd.util.LineBuilder();
      builder.append(args[i]).append(nstring.toString());
      for (String token : tokens) {
        builder.append(token).append(PhraseAligner.flatten(token));
      }
      System.out.println(builder.toString());
    }
  }
}
