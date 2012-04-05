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
import org.sd.util.tree.Tree;

import java.io.IOException;

/**
 * JUnit Tests for the XmlTagRipper class.
 * <p>
 * @author Spence Koehler
 */
public class TestXmlTagRipper extends TestCase {

  public TestXmlTagRipper(String name) {
    super(name);
  }
  
  private static final String TEST1_XML = "resources/xml-text-ripper-test-data-1.xml";
  private static final String TEST2_XML = "resources/xml-tag-ripper-test-data-1.xml";
  private static final String TEST3_XML = "resources/xml-tag-ripper-test-data-2.xml";
  private static final String TEST4_XML = "resources/xml-tag-ripper-test-data-3.xml";
  private static final String TEST5_XML = "resources/xml-tag-ripper-test-data-4.xml";

  public void testNormalIteration() throws IOException {
    final String filename = FileUtil.getFilename(this.getClass(), TEST1_XML);
    final XmlTagRipper ripper = new XmlTagRipper(filename);

    final String[] expected = new String[] {
      "root", "a", "b", "c", "d", "e",
    };

    int index = 0;
    while (ripper.hasNext()) {
      final XmlLite.Tag tag = ripper.next();
      assertEquals(expected[index++], tag.name);
    }
    assertEquals(expected.length, index);
  }

  public void testNodeRipping() throws IOException {
    final String filename = FileUtil.getFilename(this.getClass(), TEST2_XML);
    final XmlTagRipper ripper = new XmlTagRipper(filename);

    final String expectedC = "<c><d><e>4</e></d></c>";

    // iterate up to the "C" node.
    while (ripper.hasNext()) {
      final XmlLite.Tag tag = ripper.next();
      if ("c".equals(tag.name)) break;
    }

    final Tree<XmlLite.Data> cNode = ripper.ripNode(XmlFactory.XML_LITE_IGNORE_COMMENTS);
    final String cString = XmlLite.asXml(cNode, false).replaceAll("\\s+", "");

    assertEquals(expectedC, cString);
    assertTrue(ripper.hasNext());

    final XmlLite.Tag fTag = ripper.next();
    assertEquals("f", fTag.name);
    assertFalse(ripper.hasNext());
  }

  public void testScriptIsTag() throws IOException {
    final String filename = FileUtil.getFilename(this.getClass(), TEST3_XML);
    final XmlTagRipper ripper = new XmlTagRipper(filename, true, null);

    boolean foundScriptAsTag = false;

    // iterate up to the "script" node.
    while (ripper.hasNext()) {
      final XmlLite.Tag tag = ripper.next();
      if ("script".equals(tag.name)) {
        foundScriptAsTag = true;
        break;
      }
    }

    assertTrue(foundScriptAsTag);
  }

  public void testRipMultipleChildren() throws IOException {
    final String filename = FileUtil.getFilename(this.getClass(), TEST3_XML);
    final XmlTagRipper ripper = new XmlTagRipper(filename, true, null);

    // iterate up to the "ul" node.
    while (ripper.hasNext()) {
      final XmlLite.Tag tag = ripper.next();
      if ("ul".equals(tag.name)) {
        final Tree<XmlLite.Data> ulNode = ripper.ripNode(XmlFactory.XML_LITE_IGNORE_COMMENTS);
        assertEquals(3, ulNode.numChildren());
        break;
      }
    }
  }

  public void testRipNoChildren() throws IOException {
    final String filename = FileUtil.getFilename(this.getClass(), TEST3_XML);
    final XmlTagRipper ripper = new XmlTagRipper(filename, true, null);

    // iterate up to the "head" node.
    while (ripper.hasNext()) {
      final XmlLite.Tag tag = ripper.next();
      if ("head".equals(tag.name)) {
        final Tree<XmlLite.Data> ulNode = ripper.ripNode(XmlFactory.XML_LITE_IGNORE_COMMENTS);
        assertEquals(0, ulNode.numChildren());
        break;
      }
    }
  }

  public void testRipEmbeddedXmlInHtmlAttribute() throws IOException {
    final String filename = FileUtil.getFilename(this.getClass(), TEST4_XML);
    final XmlTagRipper ripper = new XmlTagRipper(filename, true, null);

    final String[] onmouseovers = new String[] {
      // first 'a' tag's onmouseover attribute
      "addInfo(event, '<u>(Hannah) Channa Breslavsky</u><br>Original member of the Schloime Family Society<br><b>Residence on 1/10/1920:</b> Ohio with Rifka and Joseph Dolinsky<br><b>Residence on 1/30/1920:</b> Eldridge St NY, NY with Esther and Louis Gratz<br><b>Residence in 1930:</b> Bay 32nd St Brooklyn, NY with grandchildren')",

      // second 'a' tag's onmouseover attribute
      "addInfo(event, '<u>(Hannah) Channa Breslavsky</u><br><b>\\\"Burial\\\":</b> Mount Lebanon Cemetery Glendale, Queens, NY')",
    };

    // iterate up to the "a" nodes.
    int aNum = 0;
    while (ripper.hasNext()) {
      final XmlLite.Tag tag = ripper.next();
      if ("a".equals(tag.name)) {
        final Tree<XmlLite.Data> aNode = ripper.ripNode(XmlFactory.XML_LITE_IGNORE_COMMENTS);

        assertEquals("#", tag.getAttribute("href"));
        assertEquals(onmouseovers[aNum], tag.getAttribute("onmouseover"));

        ++aNum;
      }
    }
  }

  public void testIgnoreEmbeddedBackslashInXml() throws IOException {
    final String filename = FileUtil.getFilename(this.getClass(), TEST5_XML);
    final XmlTagRipper ripper = new XmlTagRipper(filename, false, null);

    final String[][][] expected = new String[][][] {
      {
        {"fingerprint", "42148:2583|1892|1093|353|547|1043"},
        {"imageid", "a353:2583"},
        {"highlightrectangles", "Tennant~547,1043,649,1069|H.~661,1044,690,1065|G\\,~700,1044,735,1071|Bell~753,1043,800,1065|and~807,1043,851,1064|Bear~859,1044,915,1067|Imi,~923,1046,970,1072|tiigh~985,1043,1041,1073|street~574,1072,638,1091|\\~1036,1070,1047,1089"},
        {"dbid", "2583"},
        {"freetext", "Tennant H. G\\, Bell and Bear Imi, tiigh street \\"},
        {null, "Tennant H. G\\, Bell and Bear Imi, tiigh street \\"},
      },
      {
        {"fingerprint", "42148:2583|1930|1149|149|61|293"},
        {"imageid", "a149:2583"},
        {"highlightrectangles", "Morton~61,295,152,321|Thomas,~166,295,267,322|beer~279,295,327,316|retailer,~341,294,430,321|St.~444,294,475,316|James'~486,293,566,315|street~91,324,159,343"},
        {"dbid", "2583"},
        {"freetext", "Morton Thomas, beer retailer, St. James' street"},
        {null, "Morton Thomas, beer retailer, St. James' street"},
      },
    };

    // iterate up to the "in" nodes. check attributes and text.
    int inNum = 0;
    while (ripper.hasNext()) {
      final XmlLite.Tag tag = ripper.next();
      if ("in".equals(tag.name)) {
        final Tree<XmlLite.Data> inNode = ripper.ripNode(XmlFactory.XML_LITE_IGNORE_COMMENTS);

        final String[][] expectedAttributes = expected[inNum++];
        for (String[] expectedAttribute : expectedAttributes) {
          final String key = expectedAttribute[0];
          final String value = expectedAttribute[1];
          if (key != null) {
            // check attribute
            assertEquals(value, tag.getAttribute(key));
          }
          else {
            // check text
            final String text = inNode.getChildren().get(0).getData().asText().text;
            assertEquals(value, text);
          }
        }
      }
    }
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestXmlTagRipper.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
