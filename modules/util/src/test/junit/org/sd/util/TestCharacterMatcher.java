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


import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit Tests for the CharacterMatcher class.
 * <p>
 * @author Spence Koehler
 */
public class TestCharacterMatcher extends TestCase {

  public TestCharacterMatcher(String name) {
    super(name);
  }
  

  private final void verify(String source, String target, String[] splitStrings, boolean[] splitMatches) {
    final CharacterMatcher matcher = new CharacterMatcher(source, target);
    final List<CharacterMatcher.Substring> split = matcher.split();

    if (splitStrings != null) {
      assertEquals(splitStrings.length, split.size());
      int index = 0;
      for (String splitString : splitStrings) {
        final CharacterMatcher.Substring substring = split.get(index);
        assertEquals(splitStrings[index], substring.toString());
        assertEquals(splitMatches[index], substring.isMatch());
        ++index;
      }
    }
    else {
      System.out.println("\nsplit(" + source + ", " + target + ")=");
      for (CharacterMatcher.Substring substring : split) {
        System.out.println("\"" + substring.toString() + "\",");
      }
      for (CharacterMatcher.Substring substring : split) {
        System.out.println(substring.isMatch() + ",");
      }
    }
  }

  public void testSimpleBeginMatch() {
    final String[] splitStrings = new String[]{"foo", "testing"};
    final boolean[] splitMatches = new boolean[]{true, false};
    verify("foo", "footesting", splitStrings, splitMatches);
  }

  public void testSimpleEndMatch() {
    final String[] splitStrings = new String[]{"testing", "foo"};
    final boolean[] splitMatches = new boolean[]{false, true};
    verify("foo", "testingfoo", splitStrings, splitMatches);
  }

  public void testSimpleMidMatch() {
    final String[] splitStrings = new String[]{"testing", "foo", "this"};
    final boolean[] splitMatches = new boolean[]{false, true, false};
    verify("foo", "testingfoothis", splitStrings, splitMatches);
  }

  public void testComplex1() {
    final String[] splitStrings = new String[] {
      "t", "e", "st", "abcdef", "o", "fghi", "su", "b", "str", "i", "n",
      "g", "jklmn", "m", "a", "t", "c", "h", "e", "s", "op",
    };
    final boolean[] splitMatches = new boolean[] {
      false, true, false, true, true, true, false, true, false, true, true,
      true, true, true, true, false, true, true, true, false, true,
    };
    verify("abcdefghijklmnop", "testabcdefofghisubstringjklmnmatchesop", splitStrings, splitMatches);
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestCharacterMatcher.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
