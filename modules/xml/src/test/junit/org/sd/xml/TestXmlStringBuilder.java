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


import java.io.IOException;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit Tests for the XmlStringBuilder class.
 * <p>
 * @author Spence Koehler
 */
public class TestXmlStringBuilder extends TestCase {

  public TestXmlStringBuilder(String name) {
    super(name);
  }
  

  public void testRootOnly() {
    final XmlStringBuilder xml = new XmlStringBuilder("root");
    final String xmlString = xml.getXmlString();
    assertEquals("<root/>", xmlString);
  }

  public void testRootPlus() {
    final XmlStringBuilder xml = new XmlStringBuilder("root");
    xml.addTagAndText("child id='1'", null);
    final String xmlString = xml.getXmlString();
    assertEquals("<root><child id='1'/></root>", xmlString);
  }

  public void testAddElementAfterEnd() throws IOException {
    final XmlStringBuilder xmlBuilder = new XmlStringBuilder();
    xmlBuilder.setXmlString("<test><a a=\"1\"/></test>");
    xmlBuilder.addElement((DomElement)XmlFactory.buildDomNode("<b b=\"2\"/>", false));
    assertEquals("<test><a a=\"1\"/><b b=\"2\"/>", xmlBuilder.getXmlString());
  }

  public void testAddAttribute() {
    final XmlStringBuilder xmlBuilder = new XmlStringBuilder();
    xmlBuilder.setXmlString("<test><a a=\"1\"/></test>");
    xmlBuilder.getXmlElement().setAttribute("b", "2");

    // test setting a non-existent value
    String got = xmlBuilder.getXmlString();
    assertEquals("<test b=\"2\"><a a=\"1\"/></test>", got);

    // test overwriting an existing value
    xmlBuilder.getXmlElement().setAttribute("b", "it's 2");
    got = xmlBuilder.getXmlString();
    assertEquals("<test b=\"it&apos;s 2\"><a a=\"1\"/></test>", got);
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestXmlStringBuilder.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
