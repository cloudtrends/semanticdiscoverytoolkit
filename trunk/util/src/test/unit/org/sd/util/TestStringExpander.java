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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * JUnit Tests for the StringExpander class.
 * <p>
 * @author Spence Koehler
 */
public class TestStringExpander extends TestCase {

  public TestStringExpander(String name) {
    super(name);
  }
  
  public void testSplitAndExpand() {
    // split on commas, expand by tweaking case
    final StringExpander.RegexSplitExpandFunction commaFunction =
      new StringExpander.RegexSplitExpandFunction(
        new StringExpander.ExpandStrategy() {
          // expand by adding piece, piece upper-cased, and piece lower-cased
          public Collection<String> expand(String piece, StringExpander.Context context) {
            final Set<String> result = new LinkedHashSet<String>();

            result.add(piece);
            result.add(piece.toUpperCase());
            result.add(piece.toLowerCase());

            return result;
          }
        },
        "\\s*\\,\\s*", ", ", false);

    // split on spaces.
    final StringExpander.RegexSplitExpandFunction spaceFunction =
      new StringExpander.RegexSplitExpandFunction("\\s+", " ", false);

    // split on commas, then on spaces
    commaFunction.setNextFunction(spaceFunction);

    final StringExpander stringExpander = new StringExpander(commaFunction);

    final Collection<String> strings = stringExpander.getStrings("Abracadabra, Foo Bar Baz, Now is the Time");

    final String[] expected = new String[] {
      "Abracadabra, Foo Bar Baz, Now is the Time",
      "Abracadabra, Foo Bar Baz, NOW IS THE TIME",
      "Abracadabra, Foo Bar Baz, now is the time",
      "Abracadabra, FOO BAR BAZ, Now is the Time",
      "Abracadabra, FOO BAR BAZ, NOW IS THE TIME",
      "Abracadabra, FOO BAR BAZ, now is the time",
      "Abracadabra, foo bar baz, Now is the Time",
      "Abracadabra, foo bar baz, NOW IS THE TIME",
      "Abracadabra, foo bar baz, now is the time",
      "ABRACADABRA, Foo Bar Baz, Now is the Time",
      "ABRACADABRA, Foo Bar Baz, NOW IS THE TIME",
      "ABRACADABRA, Foo Bar Baz, now is the time",
      "ABRACADABRA, FOO BAR BAZ, Now is the Time",
      "ABRACADABRA, FOO BAR BAZ, NOW IS THE TIME",
      "ABRACADABRA, FOO BAR BAZ, now is the time",
      "ABRACADABRA, foo bar baz, Now is the Time",
      "ABRACADABRA, foo bar baz, NOW IS THE TIME",
      "ABRACADABRA, foo bar baz, now is the time",
      "abracadabra, Foo Bar Baz, Now is the Time",
      "abracadabra, Foo Bar Baz, NOW IS THE TIME",
      "abracadabra, Foo Bar Baz, now is the time",
      "abracadabra, FOO BAR BAZ, Now is the Time",
      "abracadabra, FOO BAR BAZ, NOW IS THE TIME",
      "abracadabra, FOO BAR BAZ, now is the time",
      "abracadabra, foo bar baz, Now is the Time",
      "abracadabra, foo bar baz, NOW IS THE TIME",
      "abracadabra, foo bar baz, now is the time",
    };

    assertEquals(expected.length, strings.size());

    int index = 0;
    for (String string : strings) {
      assertEquals(expected[index++], string);
    }
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestStringExpander.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
