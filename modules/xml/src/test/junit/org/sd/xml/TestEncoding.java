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
package org.sd.xml;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit Tests for the Encoding class.
 * <p>
 * @author Spence Koehler
 */
public class TestEncoding extends TestCase {

  public TestEncoding(String name) {
    super(name);
  }
  
  public void testHashCode() {
    int id1 = Encoding.ASCII.hashCode();
    int id2 = Encoding.UTF8.hashCode();

    assertFalse(id1 == id2);
  }

  public void testEquals() {
    Encoding type1 = Encoding.ASCII;
    Encoding type2 = Encoding.UTF8;

    assertEquals(type1, type1);
    assertEquals(type2, type2);

    assertFalse(type1.equals(type2));
  }

  public void testToString() {
    String s1 = Encoding.ASCII.toString();
    String s2 = Encoding.UTF8.toString();

    assertFalse(s1.equals(s2));
  }

  public void testGetters1() {
    Encoding[] enums = Encoding.getEncodings();
    for (int i = 0; i < enums.length; ++i) {
      int id = enums[i].getId();
      assertEquals(id, Encoding.getEncoding(id).getId());
    }
  }

  public void testGetters2() {
    Encoding[] enums = Encoding.getEncodings();
    for (int i = 0; i < enums.length; ++i) {
      String label = enums[i].toString();
      assertEquals(label, Encoding.getEncoding(label).toString());
    }
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestEncoding.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
