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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Utilities for splitting text based on different criteria.
 * <p>
 * @author Spence Koehler
 */
public class SplitUtils {
  
  public static final char[] DEFAULT_MULTI_DELIMS_TO_EXCLUDE = new char[] {
    '(', ')', '/', '\\', '&', '\'', '"', '%', '#', '$', '{', '}', '[', ']',
  };
  public static final Set<Character> DEFAULT_MULTI_DELIMS_TO_EXCLUDE_SET = new HashSet<Character>();
  static {
    for (char c : DEFAULT_MULTI_DELIMS_TO_EXCLUDE) {
      DEFAULT_MULTI_DELIMS_TO_EXCLUDE_SET.add(c);
    }
  }

  public static final char[] DEFAULT_SINGLE_DELIMS_TO_EXCLUDE = new char[] {
    '.', '(', ')', '/', '\\', '&', '\'', '"', '%', '#', '$', '{', '}', '[', ']',
  };
  public static final Set<Character> DEFAULT_SINGLE_DELIMS_TO_EXCLUDE_SET = new HashSet<Character>();
  static {
    for (char c : DEFAULT_SINGLE_DELIMS_TO_EXCLUDE) {
      DEFAULT_SINGLE_DELIMS_TO_EXCLUDE_SET.add(c);
    }
  }

  /**
   * Split the string on multiple consecutive non-white/non-letter-or-digit delimiters.
   * In identifying a delimiter, exclude a default set of delims.
   */
  public static final String[] multiDelimSplit(String string) {
    return nDelimSplit(2, string, DEFAULT_MULTI_DELIMS_TO_EXCLUDE_SET);
  }

  /**
   * Split the string on multiple consecutive non-white/non-letter-or-digit delimiters.
   * In identifying a delimiter, exclude a default set of delims.
   */
  public static final String[] singleDelimSplit(String string) {
    return nDelimSplit(1, string, DEFAULT_SINGLE_DELIMS_TO_EXCLUDE_SET);
  }

  /**
   * Split the string on multiple consecutive non-white/non-letter delimiters.
   * In identifying a delimiter, also exclude the delims to exclude.
   * <p>
   * Also, don't consider a '-' as a delimter if it has a letter immediately before it.
   */
  public static final String[] nDelimSplit(int numDelims, String string, Set<Character> delimsToExclude) {
    final List<String> result = new ArrayList<String>();
    final StringBuilder resultBuffer = new StringBuilder();
    final StringBuilder delimBuffer = new StringBuilder();
    final int len = string.length();

    int seenDelims = 0;
    boolean readyToSplit = false;
    boolean sawSpace = false;

    for (int i = 0; i < len; ++i) {
      final char c = string.charAt(i);
      if (Character.isLetterOrDigit(c) || delimsToExclude.contains(c) ||  // character for resultBuffer
          (c == '-' && !sawSpace && seenDelims == 0)) {

        if (readyToSplit) {
          result.add(resultBuffer.toString().trim());
          resultBuffer.setLength(0);
        }
        else if (delimBuffer.length() > 0) {
          resultBuffer.append(delimBuffer);  // need to put delims back. it wasn't time to split yet.
        }
        seenDelims = 0;
        sawSpace = false;
        readyToSplit = false;
        delimBuffer.setLength(0);

        // record this char.
        resultBuffer.append(c);
      }
      else if (c == ' ') {  // whitespace can go either way
        if (delimBuffer.length() > 0) {
          delimBuffer.append(c);  // could go either way
        }
        else {
          resultBuffer.append(c);  // belongs in a result string
        }
        sawSpace = true;
      }
      else {  // character for delimBuffer
        ++seenDelims;

        if (seenDelims >= numDelims) {
          readyToSplit = true;
        }

        delimBuffer.append(c);
      }
    }

    if (resultBuffer.length() > 0) {
      final String lastString = resultBuffer.toString().trim();
      if (lastString.length() > 0) {
        result.add(lastString);
      }
    }

    return result.toArray(new String[result.size()]);
  }


  public static final void main(String[] args) {
    for (int i = 0; i < args.length; ++i) {
      final String string = args[i];
      System.out.println("'" + string + "'");
      final String[] msplit = multiDelimSplit(args[i]);
      for (int j = 0; j < msplit.length; ++j) {
        System.out.println("  --> '" + msplit[j] + "'");

        final String[] ssplit = singleDelimSplit(msplit[j]);
        for (int k = 0; k < ssplit.length; ++k) {
          System.out.println("    --> '" + ssplit[k] + "'");
        }
      }
    }
  }
}
