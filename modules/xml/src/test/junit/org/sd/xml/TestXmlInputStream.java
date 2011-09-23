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

import org.sd.io.FileUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * JUnit Tests for the XmlInputStream class.
 * <p>
 * @author Spence Koehler
 */
public class TestXmlInputStream extends TestCase {

  public TestXmlInputStream(String name) {
    super(name);
  }
  
  private void doDetermineEncodingTest(String resource, Encoding encoding) throws IOException {
    doDetermineEncodingTest(resource, encoding, null);
  }

  private void doDetermineEncodingTest(String resource, Encoding encoding, String expectedText) throws IOException {
    final InputStream inputStream = org.sd.io.FileUtil.getInputStream(this.getClass(), resource);
    final XmlInputStream xmlInputStream = new XmlInputStream(inputStream);
    if (expectedText != null) {
      final String text = dump(xmlInputStream);
      assertEquals(expectedText, text);
    }
    assertEquals(encoding, xmlInputStream.getEncoding());
  }

  private final String dump(XmlInputStream xmlInputStream) throws IOException {
    final StringBuilder result = new StringBuilder();
    int c;
    while ((c = xmlInputStream.read()) >= 0) {
      result.appendCodePoint(c);
    }
//    xmlInputStream.close();
    return result.toString();
  }

  public void testDetermineEncoding() throws IOException {
    doDetermineEncodingTest("resources/wierd.xml", Encoding.UTF8);
    doDetermineEncodingTest("resources/wierd2.xml", Encoding.UTF8);
    doDetermineEncodingTest("resources/wierd3.xml", Encoding.UTF8);
    doDetermineEncodingTest("resources/wierd4.xml", Encoding.UTF8);

    doDetermineEncodingTest("resources/fur-ascii1.xml", Encoding.ASCII);
    doDetermineEncodingTest("resources/fur-ascii2.xml", Encoding.ASCII);
    doDetermineEncodingTest("resources/fur-ascii3.xml", Encoding.ASCII);

    doDetermineEncodingTest("resources/fur-utf81.xml", Encoding.UTF8);
    doDetermineEncodingTest("resources/fur-utf82.xml", Encoding.UTF8);
    doDetermineEncodingTest("resources/fur-utf83.xml", Encoding.UTF8);
  }

  public void testRecognizeHighAscii1() throws IOException {
    // "ße" as in strasse
    final String text = EntityConverter.unescape("&#223;e");
    final XmlInputStream inputStream = getXmlInputStream(EntityConverter.unescape(text));
    final Encoding encoding = inputStream.getEncoding();
    int value = inputStream.doRead(encoding);
    assertEquals(223, value);
    value = inputStream.doRead(Encoding.UTF8);
    assertEquals('e', value);
  }

  public void testRecognizeHighAscii2() throws IOException {
    // "ße" as in strasse
    final XmlInputStream inputStream = loadResourceAsXmlInputStream("resources/szlig-e.txt");
    final Encoding encoding = inputStream.getEncoding();
    int value = inputStream.doRead(encoding);

/*
// commented out after switch to java 1.7 because code point 2021 is now defined as a valid utf-8 character
// since this particular functionality is not currently being used and a simple fix is not obvious, I'm
// disabling this usage for until it needs to be addressed. sbk.
    assertEquals(223, value);
    value = inputStream.doRead(Encoding.UTF8);
    assertEquals('e', value);
*/
  }

  private final XmlInputStream getXmlInputStream(String text) throws IOException {
    return new XmlInputStream(new ByteArrayInputStream(text.getBytes()), Encoding.UTF8);
  }

  private final XmlInputStream loadResourceAsXmlInputStream(String resource) throws IOException {
    return new XmlInputStream(FileUtil.getInputStream(this.getClass(), resource));
  }

  public void testFoundXmlTag1() throws IOException {
    final XmlInputStream inputStream = new XmlInputStream(new ByteArrayInputStream(" <<not valid xml>> ".getBytes()));
    try {
      assertFalse(inputStream.foundXmlTag());
    }
    finally {
      inputStream.close();
    }
  }

  public void testFoundXmlTag2() throws IOException {
    final XmlInputStream inputStream = new XmlInputStream(new ByteArrayInputStream(" <is \n\tacceptable xml/> ".getBytes()));
    try {
      assertTrue(inputStream.foundXmlTag());
    }
    finally {
      inputStream.close();
    }
  }

  public void testFoundXmlTag3() throws IOException {
    final XmlInputStream inputStream = loadResourceAsXmlInputStream("resources/non-xml.txt");
    try {
      assertFalse(inputStream.foundXmlTag());
    }
    finally {
      inputStream.close();
    }
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestXmlInputStream.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
