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

  public void testAddChildToTerminal() throws IOException {
    final XmlStringBuilder xmlBuilder = new XmlStringBuilder();
    xmlBuilder.setXmlString("<test a=\"1\"/>");
    xmlBuilder.addElement((DomElement)XmlFactory.buildDomNode("<b b=\"2\"/>", false));
    assertEquals("<test a=\"1\"><b b=\"2\"/></test>", xmlBuilder.getXmlString());
  }

  public void testAddChildToTerminalElement() throws IOException {
    //NOTE: this is really testing DomNode.addChild, not XmlStringBuilder!

    final XmlStringBuilder xmlBuilder = new XmlStringBuilder();
    xmlBuilder.setXmlString("<test a=\"1\"/>");
    final DomElement domElement = xmlBuilder.getXmlElement();

    final DomElement childElement = (DomElement)XmlFactory.buildDomNode("<b b=\"2\"/>", false);
    domElement.addChild(childElement);

    final StringBuilder xmlString = new StringBuilder();
    domElement.asFlatString(xmlString);
    assertEquals("<test a=\"1\"><b b=\"2\"/></test>", xmlString.toString());
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

  public void testEscape() 
  {
    String[] inputs = new String[] {
      "She was born Sept. 30, 1915, at Winfield, the daughter of Earle and Edith Youle. She was the oldest of four children, including a brother and two sisters. She attended Winfield public schools and graduated from Winfield High School in 1932. She received her bachelor's degree in public school music with\n"+
      "                 minors in English and art, from Southwestern College in 1937. A\n"+
      "                 resident of Wichita since 1967, she taught elementary music and\n"+
      "                 art in Woodward, Okla., Oxford, Garden City and Pratt.",
    };
    String[] expected = new String[] {
      "<test>She was born Sept. 30, 1915, at Winfield, the daughter of Earle and Edith Youle. She was the oldest of four children, including a brother and two sisters. She attended Winfield public schools and graduated from Winfield High School in 1932. She received her bachelor&apos;s degree in public school music with&#x0A;                 minors in English and art, from Southwestern College in 1937. A&#x0A;                 resident of Wichita since 1967, she taught elementary music and&#x0A;                 art in Woodward, Okla., Oxford, Garden City and Pratt.</test>",
    };
    
    for(int i = 0; i < inputs.length; i++)
    {
      String in = inputs[i];
      String ex = expected[i];

      XmlStringBuilder builder = new XmlStringBuilder();
      builder.addTagAndText("test", in);
      assertEquals(ex, builder.getXmlElement().toString().trim());
    }
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestXmlStringBuilder.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
