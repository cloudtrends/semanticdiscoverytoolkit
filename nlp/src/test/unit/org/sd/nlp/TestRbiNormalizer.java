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


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.sd.nlp.StringWrapper;

/**
 * JUnit Tests for the RbiNormalizer class.
 * <p>
 * @author Spence Koehler
 */
public class TestRbiNormalizer extends TestCase {

  public TestRbiNormalizer(String name) {
    super(name);
  }
  
  private final void verify(String input, String expected) {
    final RbiNormalizer normalizer = RbiNormalizer.getInstance();
    final String normalized = normalizer.normalize(new StringWrapper(input).getSubString(0)).getNormalized();
    assertEquals("got=" + normalized + " expected=" + expected, expected, normalized);
  }

  public void testGeneralNormalization() {
    verify("AbraCadaBra", "abracadabra");
    verify("  Abra  Cada    Bra ", "abra cada bra");
    verify("Abra.CadaBra", "abra cadabra");
    verify("Ph.D.", "ph d");
    verify("a- bcd-ef - g-", "a-bcd-ef g");
    verify("f'rump  'frump'", "frump frump");
    verify("a/b cd/ /ef gh/ij", "a/b cd ef gh ij");
    verify("r2/d2", "r2/d2");
    verify("b2/ ab", "b2 ab");
    verify("/ab", "ab");
    verify("!@#$", "");
    verify("!@#$foo$#@!", "foo");
    verify(" !  @ # $  foo $  # @  !  -  ", "foo");
    verify("abc (ABC)", "abc ABC");
    verify("abc () ABC", "abc ABC");
    verify("{abc}-(ABC)", "abc-abc");
  }

  public void testDutchNormalization() {
    verify("Aan- en Afwezigheidsborden", "aan-en afwezigheidsborden");
    verify("Aan-/afwezigborden", "aan-afwezigborden");
    verify("Camera's", "cameras");
  }

  public void testRemoveSpaceHyphenSpace() {
    verify("testing - this", "testing this");
    verify("testing-this", "testing-this");
    verify("testing -this", "testing-this");
    verify("testing- this", "testing-this");
  }

  public void testAllCapsRules() {
    verify("I/O Jacks", "I/O jacks");
    verify("A Frames", "A frames");
    verify("A-Frames", "a-frames");
    verify("FOO.COM", "foo com");
    verify("WWW.FOO.COM", "www foo com");
    verify("at WWW.FOO.COM", "at WWW foo com");
    verify("Division of Child and Family Services (DCFS)", "division of child and family services DCFS");
    verify("COM", "COM");
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestRbiNormalizer.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
