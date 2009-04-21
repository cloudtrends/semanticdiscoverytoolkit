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
package org.sd.cio;


import org.sd.util.NameGenerator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A NameGenerator to use with FilenameGenerator for generating a padded numeric
 * sequence of filenames.
 * <p>
 * @author Spence Koehler
 */
public class NumericFilenameGenerator implements NameGenerator {
  
  public final Pattern filenamePattern;
  public final String firstName;
  public final char padChar;
  public final int keyLen;
  public final String postFix;


  /**
   * Construct a default instance.
   * <p>
   * Generated filenames will be of the form of keyLen zero padded digits
   * with a decimal number and the given extension appended; starting with
   * the (padded) startNum.
   */
  public NumericFilenameGenerator(String ext, int startNum, int keyLen) {
    this(Pattern.compile("^0*([0-9]+)" + ext + "$"),
         leftPad(Integer.toString(startNum), keyLen, '0') + ext,
         '0', keyLen, ext);
  }

  /**
   * Construct with the given params.
   *
   * @param filenamePattern  The pattern to recognize generated filenames, where group 1 identifies the (unpadded) key (generated) portion of the filename.
   * @param firstName  The name of the first file to generate in the directory (including its postFix).
   * @param padChar  The character to prepend to the generated portion of a local name to keep names of length keyLen.
   * @param keyLen  The total/constant length of generated keys with padding.
   * @param postFix  The filename postFix to attach after a generated key (i.e. ".html.gz")
   */
  public NumericFilenameGenerator(Pattern filenamePattern, String firstName,
                                  char padChar, int keyLen, String postFix) {
    this.filenamePattern = filenamePattern;
    this.firstName = firstName;
    this.padChar = padChar;
    this.keyLen = keyLen;
    this.postFix = postFix;
  }

  /**
   * Pad the given string (to the left) so that it is at least the given size
   * using the given pad char.
   */
  public static final String leftPad(String string, int size, char padChar) {
    final StringBuilder result = new StringBuilder();

    result.append(string);
    for (int i = 0; i < size - string.length(); ++i) result.insert(0, padChar);

    return result.toString();
  }

  /**
   * Generate the next name after the given name.
   *
   * @param name preceding the next name; if null, generate the first name.
   *
   * @return the next name.
   */
  public String getNextName(String name) {
    String result = null;

    if (name == null) {
      result = firstName;
    }
    else {
      final Matcher m = filenamePattern.matcher(name);
      if (m.matches()) {
        String num = m.group(1);
        int npos = 0;
        for (; npos < num.length(); ++npos) {
          if (num.charAt(npos) != '0') break;
        }
        final int nextValue = Integer.parseInt(num.substring(npos)) + 1;
        num = Integer.toString(nextValue);

        result = leftPad(num, keyLen, padChar) + postFix;
      }
    }

    return result;
  }

  /**
   * Determine whether the given name is valid for this generator.
   */
  public boolean isValidName(String name) {
    final Matcher m = this.filenamePattern.matcher(name);
    return m.matches();
  }
}
