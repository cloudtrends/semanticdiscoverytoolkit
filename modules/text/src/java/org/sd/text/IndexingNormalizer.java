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


import org.sd.nlp.AbstractNormalizer;
import org.sd.nlp.GeneralNormalizedString;
import org.sd.nlp.NormalizedString;
import org.sd.nlp.StringWrapper;
import org.sd.util.StringSplitter;
import org.sd.util.StringUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.io.IOException;


/**
 * A general normalization implementation suitable for most indexing applications.
 * <ul>
 * <li>Keeps email addresses and urls intact, but lowercases the domain.</li>
 * <li>Diacritics are replaced.</li>
 * <li>All letters are lowercased and kept.</li>
 * <li>All digits are kept.</li>
 * <li>All '-'s are replaced by white space, unless there's a number in the token, in which case the '-' is kept. This accounts for real numbers and product numbers. Note that splitting should ignore '-'.</li>
 * <li>Any '.' not in a kept '-'s token and not preceding a letter or digit is replaced by white space.</li> 
 * <li>All punctuation characters are replaced by white space.</li>
 * <li>All extra whitespace is removed.</li>
 * </ul>
 *
 * @author Spence Koehler
 */
public class IndexingNormalizer extends AbstractNormalizer {
  
  // OPTIONS: (bitmask)

  public static final int SPLIT_ON_CAMEL_CASE           = 0x01;
  public static final int REPLACE_DIACRITICS            = 0x02;
  public static final int LOWERCASE_URLS                = 0x04;
  public static final int KEEP_URL_AS_SINGLE_TOKEN      = 0x08;
  public static final int KEEP_EMAIL_AS_SINGLE_TOKEN    = 0x10;
  public static final int KEEP_ASIAN_CHARS              = 0x20;
  public static final int KEEP_ACRONYM_AS_SINGLE_TOKEN  = 0x40;
  public static final int STRIP_URL_TLD                 = 0x80;
  public static final int ORDINAL_AS_INDEX              = 0x100;

  /**
   * Default indexing options: 
   *   don't split on camel case,
   *   do replace diacritics,
   *   do lowercase urls,
   *   do keep url as single token,
   *   do keep email as single token,
   *   don't keep asian chars.
   */
  public static final int DEFAULT_NON_ASIAN_INDEXING_OPTIONS =
    REPLACE_DIACRITICS | LOWERCASE_URLS | KEEP_URL_AS_SINGLE_TOKEN | KEEP_EMAIL_AS_SINGLE_TOKEN | KEEP_ACRONYM_AS_SINGLE_TOKEN;

  /**
   * Default indexing options: 
   *   don't split on camel case,
   *   do replace diacritics,
   *   do lowercase urls,
   *   do keep url as single token,
   *   do keep email as single token,
   *   do keep asian chars.
   */
  public static final int DEFAULT_INDEXING_OPTIONS =
    DEFAULT_NON_ASIAN_INDEXING_OPTIONS | KEEP_ASIAN_CHARS;

  /**
   * Default normalization options: 
   *   do split on camel case,
   *   do replace diacritics,
   *   don't lowercase urls,
   *   do keep url as single token,
   *   do keep email as single token,
   *   do keep asian chars.
   */
  public static final int DEFAULT_NORMALIZATION_OPTIONS =
    SPLIT_ON_CAMEL_CASE | REPLACE_DIACRITICS | KEEP_URL_AS_SINGLE_TOKEN | KEEP_EMAIL_AS_SINGLE_TOKEN | KEEP_ASIAN_CHARS | KEEP_ACRONYM_AS_SINGLE_TOKEN;

  /**
   * Default web options: 
   *   do split on camel case,
   *   don't replace diacritics (there is no need),
   *   do lowercase urls,
   *   don't keep url as single token,
   *   don't keep email as single token,
   *   don't keep asian chars.
   */
  public static final int DEFAULT_WEB_OPTIONS =
    SPLIT_ON_CAMEL_CASE | LOWERCASE_URLS;

  /**
   * Convenience variable with all option flags set.
   */
  public static final int ALL_OPTIONS =
    SPLIT_ON_CAMEL_CASE | REPLACE_DIACRITICS | LOWERCASE_URLS | KEEP_URL_AS_SINGLE_TOKEN | KEEP_EMAIL_AS_SINGLE_TOKEN | KEEP_ASIAN_CHARS | KEEP_ACRONYM_AS_SINGLE_TOKEN;



  private static final int START_POS = 0;
  private static final int END_POS   = 1;
  private static final int SYM_POS   = 2;  // non-char, non-digit, non-white
  private static final int DASH_POS  = 3;
  private static final int DOT_POS   = 4;
  private static final int DIGIT_POS = 5;
  private static final int ATSET_POS = 6;
  private static final int COLON_POS = 7;
  private static final int POS_COUNT = 8;


  private static final Map<Integer, IndexingNormalizer> opts2instance = new HashMap<Integer, IndexingNormalizer>();

  public static final IndexingNormalizer getInstance(int options) {
    IndexingNormalizer result = null;

    synchronized (opts2instance) {
      result = opts2instance.get(options);
      if (result == null) {
        result = new IndexingNormalizer(options);
        opts2instance.put(options, result);
      }
    }

    return result;
  }


  private boolean splitOnCamelCase;
  private boolean replaceDiacritics;
  private boolean lowercaseUrls;
  private boolean keepUrlAsSingleToken;
  private boolean keepEmailAsSingleToken;
  private boolean keepAsianChars;
  private boolean keepAcronymAsSingleToken;
  private boolean stripUrlTld;
  private boolean ordinalAsIndex;

  private IndexingNormalizer(int options) {
    this.splitOnCamelCase = (options & SPLIT_ON_CAMEL_CASE) != 0;
    this.replaceDiacritics = (options & REPLACE_DIACRITICS) != 0;
    this.lowercaseUrls = (options & LOWERCASE_URLS) != 0;
    this.keepUrlAsSingleToken = (options & KEEP_URL_AS_SINGLE_TOKEN) != 0;
    this.keepEmailAsSingleToken = (options & KEEP_EMAIL_AS_SINGLE_TOKEN) != 0;
    this.keepAsianChars = (options & KEEP_ASIAN_CHARS) != 0;
    this.keepAcronymAsSingleToken = (options & KEEP_ACRONYM_AS_SINGLE_TOKEN) != 0;
    this.stripUrlTld = (options & STRIP_URL_TLD) != 0;
    this.ordinalAsIndex = (options & ORDINAL_AS_INDEX) != 0;
  }

  public boolean splitOnCamelCase() {
    return splitOnCamelCase;
  }

  public boolean replaceDiacritics() {
    return replaceDiacritics;
  }

  public boolean lowercaseUrls() {
    return lowercaseUrls;
  }

  public boolean keepUrlAsSingleToken() {
    return keepUrlAsSingleToken;
  }

  public boolean keepEmailAsSingleToken() {
    return keepEmailAsSingleToken;
  }

  public boolean keepAsianChars() {
    return keepAsianChars;
  }

  public boolean keepAcronymAsSingleToken() {
    return keepAcronymAsSingleToken;
  }

  public boolean stripUrlTld() {
    return stripUrlTld;
  }

  public boolean ordinalAsIndex() {
    return ordinalAsIndex;
  }


  public final NormalizedString normalize(StringWrapper.SubString subString) {

    final StringWrapper stringWrapper = subString.stringWrapper;
    final int[] codePoints = stringWrapper.getCodePoints();
    int startPos = subString.startPos;
    final int endPos = subString.endPos;
    
    // normalized output container
    final StringBuilder normalized = new StringBuilder();
    final List<Integer> n2oIndexList = new ArrayList<Integer>();

    // wordStart (incl), wordEnd (excl), nonCharDigPos, dashPos, digPos, atPos, colonPos
    int[] s2e = new int[POS_COUNT];

    while (startPos < endPos) {
      fullWordBoundaries(s2e, codePoints, startPos, endPos);

      // initialize for this go'round
      boolean keepDashes = false;
      boolean disableBreaks = false;
      boolean didWord = false;

      final boolean hasSymbol = s2e[SYM_POS] >= 0;
      final int wordEndPos = s2e[END_POS];

      // do we have symbols?
      if (hasSymbol) {
    
        // are we dealing with a url?
        if (!didWord &&
            s2e[COLON_POS] > 0 &&
            s2e[COLON_POS] < wordEndPos - 2 &&
            codePoints[s2e[COLON_POS] + 1] == '/' &&
            codePoints[s2e[COLON_POS] + 2] == '/') {

          // treat full substring as a url, normalizing appropriately
          didWord = normalizeUrl(normalized, n2oIndexList, codePoints, s2e, keepUrlAsSingleToken, lowercaseUrls);
        }

        final int atsetPos = s2e[ATSET_POS];

        // are we dealing with an email address?
        if (!didWord &&
            atsetPos > 0 &&
            atsetPos < (wordEndPos - 1) &&
            isValidLastLocalChar(codePoints[atsetPos - 1]) &&
            isValidFirstDomainChar(codePoints[atsetPos + 1])) {

          // lowercase from start to end, keeping all symbols
          // NOTE: technically, the localName part of an email address is case-sensitive,
          //       but most mail servers treat them case-insensitively; hence, so will we.
          didWord = normalizeEmailAddress(normalized, n2oIndexList, codePoints, s2e, keepEmailAsSingleToken);
        }
        
        final int dotPos = s2e[DOT_POS];

        // are we dealing with a domain?
        if (!didWord && 
            stripUrlTld && 
            dotPos > 0 &&
            dotPos < (wordEndPos - 1)){
          didWord = normalizeDomain(normalized, n2oIndexList, codePoints, s2e, lowercaseUrls);
        }
        // are we dealing with an acronym?
        else if (!didWord && keepAcronymAsSingleToken && dotPos > 0 && dotPos < wordEndPos - 1) {
          didWord = normalizeAcronym(normalized, n2oIndexList, codePoints, s2e, dotPos, wordEndPos);
        }

        if (!didWord) {
          // keep dashes if there are digits in the number
          keepDashes = s2e[DASH_POS] >= 0 && s2e[DIGIT_POS] >= 0;

          // disable breaking within the word if we have digits and a dash or dot
          disableBreaks = s2e[DIGIT_POS] >= 0 && (s2e[DASH_POS] >= 0 || dotPos >= 0);
        }
      }

      if (!didWord) {
        // normalize word(s)
        final int wordStartPos = s2e[START_POS];
        int n2oIndex = wordStartPos;
        boolean n2oStabilized = false;

        boolean needSpace = normalized.length() > 0;

        // add 'word's from startPos to endPos
        for (int i = wordStartPos; i < wordEndPos; ++i) {
          final int cp = Character.toLowerCase(codePoints[i]);

          boolean keepChar = false;
          String diacriticReplacement = null;

          // keep dots if before a letter (or if keeping dashes)
          if (cp == '.') {
            keepChar = (keepDashes ||
                        (((i + 1) < wordEndPos) &&
                         Character.isLetterOrDigit(codePoints[i + 1])));
          }
          else if (cp == '-') {
            keepChar = keepDashes;
          }
          else {
            keepChar = !hasSymbol || Character.isLetterOrDigit(cp);

            if (keepChar) {
              if (replaceDiacritics) {
                diacriticReplacement = StringSplitter.getDiacriticReplacement(cp);
              }
            }
            else if (keepAsianChars) {
              keepChar = StringUtil.isAsianCodePoint(cp);
            }
          }

          if (keepChar) {
            if (diacriticReplacement != null) {
              // replace diacritics
              if (needSpace) {
                normalized.append(' ');
                n2oIndexList.add(i - 1);  // just call prev char the space's original pos
                n2oIndex = i;
                needSpace = false;
              }
              normalized.append(diacriticReplacement);
              final int num = diacriticReplacement.length();
              for (int j = 0; j < num; ++j) {
                n2oIndexList.add(n2oIndex);  // add 'i' 'j' times.
              }
            }
            else {
              if (needSpace) {
                normalized.append(' ');
                n2oIndexList.add(i - 1);  // just call prev char the space's original pos
                n2oIndex = i;
                needSpace = false;
              }
              normalized.appendCodePoint(cp);
              n2oIndexList.add(n2oIndex);
            }
          }
          else {
            // replace punctuation chars by white space, removing extra whitespace
            needSpace = true;
          }

          if (disableBreaks) {
            // increment until a letter or digit. then hold constant to "spoof" splitting in NormalizedString.
            if (!n2oStabilized) {
              if (Character.isLetterOrDigit(cp)) {
                n2oStabilized = true;
              }
              else {
                ++n2oIndex;
              }
            }
          }
          else {
            ++n2oIndex;
          }
        }
      }

      // reset for next go'round
      startPos = s2e[END_POS];
    }

    return new GeneralNormalizedString(stringWrapper, normalized.toString(), n2oIndexList, splitOnCamelCase);
  }

  private static final boolean isRomanDigit(int codePoint) {
    return codePoint <= '9' && codePoint >= '0';
  }

  /**
   * Where an email address is of the form localPart @ domainName,
   * determine whether the given char is a valid character for the
   * last letter of the localPart.
   */
  private static final boolean isValidLastLocalChar(int cp) {
    // ascii letter, digit, ! # $ % * / ? | ^ { } ` ~ & ' + - = _, double quote
    return (cp < 127 &&
            (cp >= 'a' ||
             cp == '_' ||
             (cp <= 'Z' && cp > ' ' &&
              !(cp == '(' || cp == ')' || cp == ',' || cp == '.'))));
  }

  /**
   * Where an email address is of the form localPart @ domainName,
   * determine whether the given char is a valid character for the
   * first letter of the domanName.
   */
  private static final boolean isValidFirstDomainChar(int cp) {
    // ascii letter, digit, hyphen
    return (cp <= 'z' &&
            (cp >= 'a' ||
             (cp <= 'Z' && cp >= 'A') ||
             cp == '-'));
  }


  private static final void fullWordBoundaries(int[] result, int[] codePoints, int startPos, int endPos) {
    int curPos = startPos;

    // initialize
    for (int i = 0; i < result.length; ++i) result[i] = -1;

    // move forward until not white
    for (; curPos < endPos; ++curPos) {
      final int cp = codePoints[curPos];
      if (!Character.isWhitespace(cp)) break;
    }
    result[START_POS] = curPos;

    for (; curPos < endPos; ++curPos) {
      final int cp = codePoints[curPos];
      if (Character.isWhitespace(cp)) {
        break;
      }
      else if (cp == '-') {
        if (result[DASH_POS] < 0) result[DASH_POS] = curPos;
        if (result[SYM_POS] < 0) result[SYM_POS] = curPos;
      }
      else if (cp == '@') {
        if (result[ATSET_POS] < 0) result[ATSET_POS] = curPos;
        if (result[SYM_POS] < 0) result[SYM_POS] = curPos;
      }
      else if (cp == ':') {
        if (result[COLON_POS] < 0) result[COLON_POS] = curPos;
        if (result[SYM_POS] < 0) result[SYM_POS] = curPos;
      }
      else if (cp == '.') {
        if (result[DOT_POS] < 0) result[DOT_POS] = curPos;
        if (result[SYM_POS] < 0) result[SYM_POS] = curPos;
      }
      else if (isRomanDigit(cp)) {
        if (result[DIGIT_POS] < 0) result[DIGIT_POS] = curPos;
      }
      else if (result[SYM_POS] < 0 && !Character.isLetterOrDigit(cp)) {
        if (result[SYM_POS] < 0) result[SYM_POS] = curPos;
      }
    }

    result[END_POS] = curPos;
  }

  private static final boolean normalizeEmailAddress(StringBuilder normalized, List<Integer> n2oIndexList, int[] codePoints, int[] s2e, boolean keepEmailAsSingleToken) {
    // lowercase from start to end, keeping all symbols
    // NOTE: technically, the localName part of an email address is case-sensitive,
    //       but most mail servers treat them case-insensitively; hence, so will we.

    final int startIndex = s2e[START_POS];
    final int endIndex = s2e[END_POS];

    if (normalized.length() > 0) {
      normalized.append(' ');
      n2oIndexList.add(startIndex - 1);
    }

    for (int i = startIndex; i < endIndex; ++i) {
      normalized.appendCodePoint(Character.toLowerCase(codePoints[i]));
      n2oIndexList.add(keepEmailAsSingleToken ? startIndex : i);

      //note: keeping as single token is accomplished by "spoofing" NormalizedString
      //      into not detecting changes in this range. We simply point to the first
      //      char for all chars in the range.
    }

//todo: if it becomes important to do so, verify we are indeed dealing with an email address.
    return true;
  }

  private static final boolean normalizeUrl(StringBuilder normalized, List<Integer> n2oIndexList, int[] codePoints, int[] s2e, boolean keepUrlAsSingleToken, boolean lowercaseUrls) {
    final StringBuilder urlString = new StringBuilder();
    final int startIndex = s2e[START_POS];
    final int endIndex = s2e[END_POS];
    for (int i = startIndex; i < endIndex; ++i) {
      urlString.appendCodePoint(codePoints[i]);
    }

    final DetailedUrl dUrl = new DetailedUrl(urlString.toString());
    String normalizedUrl = dUrl.getAbbreviatedNormalizedUrl();

    if (lowercaseUrls) normalizedUrl = normalizedUrl.toLowerCase();

    if (normalized.length() > 0) {
      normalized.append(' ');
      n2oIndexList.add(startIndex - 1);
    }
    normalized.append(normalizedUrl);

    final int hostStartIndex = dUrl.getPostPrefixHostIndex();
    int origIndex = startIndex + hostStartIndex;
    if (origIndex >= endIndex) origIndex = endIndex - 1;

    for (int i = 0; i < normalizedUrl.length(); ++i) {
      n2oIndexList.add(origIndex);
      if (!keepUrlAsSingleToken) {
        ++origIndex;
        if (origIndex >= endIndex) origIndex = endIndex - 1;
      }

      //note: keeping as single token is accomplished by "spoofing" NormalizedString
      //      into not detecting changes in this range. We simply point to the first
      //      char for all chars in the range.
    }

    return true;
  }


  private static final boolean normalizeDomain(StringBuilder normalized, List<Integer> n2oIndexList, int[] codePoints, int[] s2e, boolean lowercaseUrls) {
    final StringBuilder urlString = new StringBuilder();
    final int startIndex = s2e[START_POS];
    final int endIndex = s2e[END_POS];
    for (int i = startIndex; i < endIndex; ++i) {
      urlString.appendCodePoint(codePoints[i]);
    }

    final DetailedUrl dUrl = new DetailedUrl(urlString.toString());
    String normalizedUrl = dUrl.doHostExtensionsSplit(dUrl.getHost(true, false, false))[0];

    if (lowercaseUrls) normalizedUrl = normalizedUrl.toLowerCase();

    if (normalized.length() > 0) {
      normalized.append(' ');
      n2oIndexList.add(startIndex - 1);
    }
    normalized.append(normalizedUrl);

    final int hostStartIndex = dUrl.getPostPrefixHostIndex();
    int origIndex = startIndex + hostStartIndex;
    if (origIndex >= endIndex) origIndex = endIndex - 1;

    for (int i = 0; i < normalizedUrl.length(); ++i) {
      n2oIndexList.add(origIndex);
    }

    return true;
  }

  private static final boolean normalizeAcronym(StringBuilder normalized, List<Integer> n2oIndexList, int[] codePoints, int[] s2e, int dotPos, int endPos) {
    boolean result = false;

    // only classify as an acronym if the '.' is before a letter which is before another dot or end of word.
    final int afterCP = codePoints[dotPos + 1];
    if (((afterCP <= 'Z' && afterCP >= 'A') || (afterCP <= 'z' && afterCP >= 'a')) &&
        ((endPos == dotPos + 2) || (codePoints[dotPos + 2] == '.'))) {
      result = true;

      final StringBuilder aString = new StringBuilder();
      final int startIndex = s2e[START_POS];
      int charIndex = -1;
      int lastDotPos = -1;
      boolean foundSymbol = false;

      for (int i = startIndex; i < endPos; ++i) {
        final int cp = codePoints[i];

        // fail if we see anything other than letters and dots, a dot at the beginning, or consecutive dots
        if (cp != '.') {
          if (!Character.isLetter(cp)) {
            // can see symbols at the end of the acronym
            foundSymbol = true;
          }
          else {
            if (foundSymbol) {
              // but can't accept any more chars after seeing a symbol
              result = false;
              break;
            }
            aString.appendCodePoint(Character.toLowerCase(cp));
            if (charIndex < 0) charIndex = i;
          }
        }
        else {
          if (i == lastDotPos + 1) {
            result = false;
            break;
          }
          lastDotPos = i;
        }
      }
      
      if (result && charIndex < 0) result = false;

      if (result) {
        if (normalized.length() > 0) {
          normalized.append(' ');
          n2oIndexList.add(startIndex - 1);
        }
        normalized.append(aString);

        final int len = aString.length();
        for (int i = 0; i < len; ++i) {
          n2oIndexList.add(charIndex);
        }

        //note: keeping as single token is accomplished by "spoofing" NormalizedString
        //      into not detecting changes in this range. We simply point to the first
        //      char for all chars in the range.
      }
    }

    return result;
  }


  public static void main(String[] args) throws IOException {
    final IndexingNormalizer normalizer = IndexingNormalizer.getInstance(DEFAULT_NORMALIZATION_OPTIONS/*DEFAULT_WEB_OPTIONS*//*DEFAULT_INDEXING_OPTIONS*/);
    doNormalize(normalizer, args);
  }
}
