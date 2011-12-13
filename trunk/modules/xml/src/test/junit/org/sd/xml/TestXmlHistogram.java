/*
    Copyright 2011 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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
 * JUnit Tests for the XmlHistogram class.
 * <p>
 * @author Spence Koehler
 */
public class TestXmlHistogram extends TestCase {

  public TestXmlHistogram(String name) {
    super(name);
  }
  

  public void testRoundTrip() {
    // construct a histogram
    final XmlHistogram xh = new XmlHistogram();
    xh.add("test", 3);
    xh.add("it", 5);

    // convert to xml
    final XmlStringBuilder xml = xh.asXml("h");

    // reconstitute from xml
    final XmlHistogram xh2 = new XmlHistogram(xml);

    // verify content
    assertEquals(2, xh2.getNumRanks());
    assertEquals("it", xh2.getElement(0));
    assertEquals(5, xh2.getFrequencyCount(0));
    assertEquals("test", xh2.getElement(1));
    assertEquals(3, xh2.getFrequencyCount(1));
  }

  public void testKeySort() {
    // construct a histogram
    final XmlHistogram xh = new XmlHistogram();
    xh.add("foo", 10);
    xh.add("bar", 5);
    xh.add("bash", 3);

    // convert to xml
    final XmlStringBuilder xml = xh.asXml("h", true);

    StringBuilder builder = new StringBuilder();
    xml.getXmlElement().asFlatString(builder);

    StringBuilder expected = new StringBuilder();
    expected.append("<h total=\"18\" bins=\"3\">");
    expected.append("<key count=\"5\">bar</key>");
    expected.append("<key count=\"3\">bash</key>");
    expected.append("<key count=\"10\">foo</key>");
    expected.append("</h>");

    assertEquals(expected.toString(), builder.toString());
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestXmlHistogram.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
