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


import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class to test regex patterns against some text.
 * <p>
 * @author Spence Koehler
 */
public class Regex {

  /**
   * Usage: java org.....Regex [-F|-M|-L] pattern input
   * i.e. java -ea Regex "(.+?)\s+(.+)" "this is a test"
   *      java -ea Regex "(.+)\s+(.+)" "this is a test"
   */
  public static void main(String[] args) {
    char mode = 'M';
    int argNum = 0;

    if (args[argNum].charAt(0) == '-') {
      mode = args[argNum].charAt(1);
      ++argNum;
    }

    final String patternString = args[argNum++];
    final Pattern p = Pattern.compile(patternString);

    System.out.println("Mode=" + mode + ", Pattern=" + patternString);

    for (; argNum < args.length; ++argNum) {
      final Matcher m = p.matcher(args[argNum]);

      switch (mode) {
        case 'M' :
        case 'm' :

          if (m.matches()) {
            System.out.println("string \"" + args[argNum] + "\" matches pattern: \"" + patternString + "\":");
            for (int i = 0; i <= m.groupCount(); i++) {
              System.out.println("group[" + i + "]='" + m.group(i) + "'");
            }
          }
          else {
            System.out.println("string \"" + args[argNum] + "\" does not match pattern: \"" + patternString + '"');
          }
          
          break;

        case 'L' :
        case 'l' :

          if (m.lookingAt()) {
            System.out.println("string \"" + args[argNum] + "\" matches pattern: \"" + patternString + "\":");
            for (int i = 0; i <= m.groupCount(); i++) {
              System.out.println("group[" + i + "]='" + m.group(i) + "'");
            }
          }
          else {
            System.out.println("string \"" + args[argNum] + "\" does not match pattern: \"" + patternString + '"');
          }
          
          break;

        case 'F' :
        case 'f' :

          boolean gotOne = false;
          while (m.find()) {
            gotOne = true;
            System.out.println("string \"" + args[argNum] + "\" matches pattern: \"" + patternString + "\":");
            for (int i = 0; i <= m.groupCount(); i++) {
              System.out.println("group[" + i + "]='" + m.group(i) + "'");
            }
          }
          if (!gotOne) {
            System.out.println("string \"" + args[argNum] + "\" not matched by pattern: \"" + patternString + '"');
          }
          
          break;
      }
    }
  }
}
