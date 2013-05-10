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

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.util.List;

/**
 * JUnit Tests for the XmlTextRipper class.
 * <p>
 * @author Spence Koehler
 */
public class TestXmlTextRipper extends TestCase {

  public TestXmlTextRipper(String name) {
    super(name);
  }
  
  private static final String TEST1_XML = "resources/xml-text-ripper-test-data-1.xml";
  private static final String TEST2_XML = "resources/xml-text-ripper-test-data-2.html";
  private static final String TEST2B_XML = "resources/xml-text-ripper-test-data-2b.html";
  private static final String TEST3_XML = "resources/xml-text-ripper-test-data-3.html";

  private final void doRipperTest(XmlTextRipper ripper, String[] expectedText, String[][] expectedTags, String[] expectedSavedTags) {
    int index = 0;
    while (ripper.hasNext()) {
      final String text = ripper.next();
      assertEquals(expectedText[index], text);

      final List<XmlLite.Tag> tags = ripper.getTags();
      if (expectedTags != null) {
        final String[] eTags = expectedTags[index];
        assertEquals("index=" + index + " tags=" + tags, eTags.length, tags.size());
        for (int i = 0; i < eTags.length; ++i) {
          assertEquals(eTags[i], tags.get(i).name);
        }
      }
      else {
        assertNull(tags);
      }

      ++index;
    }
    assertEquals(expectedText.length, index);

    final List<XmlLite.Tag> savedTags = ripper.getSavedTags();

    if (expectedSavedTags != null) {
      index = 0;
      for (XmlLite.Tag savedTag : savedTags) {
        assertEquals(expectedSavedTags[index++], savedTag.name);
      }
      assertEquals(expectedSavedTags.length, index);
    }
    else {
      assertNull(savedTags);
    }
  }

  public void test1() throws IOException {
    final String filename = FileUtil.getFilename(this.getClass(), TEST1_XML);
    final XmlTextRipper ripper = new XmlTextRipper(filename, true);

    final String[] expected = new String[] {
      "1", "2", "3", "4", "5",
    };

    doRipperTest(ripper, expected, null, null);
    ripper.close();
  }

  public void test2NoEmpties() throws IOException {
    final InputStream inputStream = FileUtil.getInputStream(this.getClass(), TEST1_XML);
    final XmlTextRipper ripper = new XmlTextRipper(inputStream, true, new XmlTagStack(), new XmlTagParser(false, true), null, null, false);

    final String[] expectedText = new String[] {
      "1", "2", "3", "4", "5",
    };
    final String[][] expectedTags = new String[][] {
      {}, {"root"}, {"root", "b"}, {"root", "c", "d", "e"}, {},
    };

    doRipperTest(ripper, expectedText, expectedTags, null);
    ripper.close();
  }

  public void test2WithEmpties() throws IOException {
    final InputStream inputStream = FileUtil.getInputStream(this.getClass(), TEST1_XML);
    final XmlTextRipper ripper = new XmlTextRipper(inputStream, true, new XmlTagStack(), new XmlTagParser(false, true), null, null, true);

    final String[] expectedText = new String[] {
      "1", "2", "", "3", "4", "5",
    };
    final String[][] expectedTags = new String[][] {
      {}, {"root"}, {"root", "a"}, {"root", "b"}, {"root", "c", "d", "e"}, {},
    };

    doRipperTest(ripper, expectedText, expectedTags, null);
    ripper.close();
  }

  public void testHtmlRippingNoEmpties() throws IOException {
    final File file = FileUtil.getFile(this.getClass(), TEST2_XML);
    final XmlTextRipper ripper = XmlTextRipper.buildHtmlRipper(file);

    final String[] expectedText = new String[] {
      "testing xml text ripper", "heading 1", "testing 1, 2, 3...",
    };
    final String[][] expectedTags = new String[][] {
      {"html", "head", "title"},
      {"html", "body", "h1"},
      {"html", "body"},
    };
    final String[] expectedSavedTags = new String[] {
      "meta", "meta", "meta",
    };

    doRipperTest(ripper, expectedText, expectedTags, expectedSavedTags);
    ripper.close();
  }

  public void testKeepEmptyHtml1() throws IOException {
    final File file = FileUtil.getFile(this.getClass(), "resources/xml-text-ripper-test-data-0.xml");
    final XmlTextRipper ripper = XmlTextRipper.buildHtmlRipper(file, true);

    final String[] expectedText = new String[] {
      "This is some text", "", "and some more text",
    };
    final String[][] expectedTags = new String[][] {
      {},
      {"br"},
      {},
    };
    final String[] expectedSavedTags = null;

    doRipperTest(ripper, expectedText, expectedTags, expectedSavedTags);
    ripper.close();
  }

  public void testHtmlRippingWithEmpties() throws IOException {
    final File file = FileUtil.getFile(this.getClass(), TEST2_XML);
    final XmlTextRipper ripper = XmlTextRipper.buildHtmlRipper(file, true);

    final String[] expectedText = new String[] {
      "", "testing xml text ripper", "", "", "heading 1", "testing 1, 2, 3...", "", "",
    };
    final String[][] expectedTags = new String[][] {
      {"html", "head", "meta"}, // from unterminated meta
      {"html", "head", "title"},
      {"html", "head", "meta"},  // from unterminated meta
      {"html", "head", "meta"},  // from unterminated meta
      {"html", "body", "h1"},
      {"html", "body"},
      {"html", "body", "hr"},
      {"html", "body", "address"},
    };
    final String[] expectedSavedTags = new String[] {
      "meta", "meta", "meta",
    };

    doRipperTest(ripper, expectedText, expectedTags, expectedSavedTags);
    ripper.close();
  }

  public void testHtmlRippingWithEmpties2() throws IOException {
    final File file = FileUtil.getFile(this.getClass(), TEST2B_XML);
    final XmlTextRipper ripper = XmlTextRipper.buildXmlRipper(file, true);

    final String[] expectedText = new String[] {
      "", "testing xml text ripper", "", "", "heading 1", "testing 1, 2, 3...", "", "",
    };
    final String[][] expectedTags = new String[][] {
      {"html", "head", "meta"},
      {"html", "head", "title"},
      {"html", "head", "META"},
      {"html", "head", "Meta"},
      {"html", "body", "h1"},
      {"html", "body"},
      {"html", "body", "hr"},
      {"html", "body", "address"},
    };
    final String[] expectedSavedTags = null;

    doRipperTest(ripper, expectedText, expectedTags, expectedSavedTags);
    ripper.close();
  }

  public void testHtmlRippingWithEmpties3() throws IOException {
    final File file = FileUtil.getFile(this.getClass(), TEST3_XML);
    final XmlTextRipper ripper = 
      new XmlTextRipper(FileUtil.getInputStream(file), true,
                        new ValidatingHtmlTagStack(), 
                        XmlFactory.HTML_TAG_PARSER_IGNORE_COMMENTS, 
                        null, null, true);
    
    final String[] expectedText = new String[] {
      "", "Osprey Obituaries", 
      "", "AYERS, Barbara", "", 
      "Wednesday, March 07, 2007", 
      "Obituary", ": AYERS, Barbara - At Marshall Gowland Manor, Sarnia, on Monday, March 5, 2007. BARBARA AYERS (nee Robbins), 67 years, of Sarnia and formerly of ... [", "View", "]"
    };
    final String[][] expectedTags = new String[][] {
      {"html", "head", "meta"},
      {"html", "head", "title"},
      {"html", "body", "table", "tr", "td", "b", "a"},
      {"html", "body", "table", "tr", "td", "b"},
      {"html", "body", "table", "tr", "td", "br"},
      {"html", "body", "table", "tr", "td"},
      {"html", "body", "table", "tr", "td", "b"},
      {"html", "body", "table", "tr", "td"},
      {"html", "body", "table", "tr", "td", "a"},
      {"html", "body", "table", "tr", "td"},
    };
    final String[] expectedSavedTags = null;

    doRipperTest(ripper, expectedText, expectedTags, expectedSavedTags);
    ripper.close();
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestXmlTextRipper.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
