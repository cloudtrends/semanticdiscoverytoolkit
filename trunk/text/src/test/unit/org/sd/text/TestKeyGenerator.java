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


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit Tests for the KeyGenerator class.
 * <p>
 * @author Spence Koehler
 */
public class TestKeyGenerator extends TestCase {

  public TestKeyGenerator(String name) {
    super(name);
  }
  
  public void testKey2Word() {
    final KeyGenerator keyGenerator = new KeyGenerator();

    final StringBuilder builder = new StringBuilder();
    keyGenerator.key2word(32767, 10, builder, '0');
    assertEquals("32767", builder.toString());

    builder.setLength(0);
    keyGenerator.key2word(786, 27, builder, '@');  // 786 = (+ (* 27 27 1) (* 27 2) 3)
    assertEquals("ABC", builder.toString());
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestKeyGenerator.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
