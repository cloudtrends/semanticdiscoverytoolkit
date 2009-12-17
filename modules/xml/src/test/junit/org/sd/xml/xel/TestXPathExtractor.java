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
package org.sd.xml.xel;


import java.io.IOException;
import java.util.List;
import java.util.Map;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.sd.util.StringUtil;
import org.sd.util.tree.Tree;
import org.sd.xml.XmlFactory;
import org.sd.xml.XmlLite;

/**
 * JUnit Tests for the XPathExtractor class.
 * <p>
 * @author Spence Koehler
 */
public class TestXPathExtractor extends TestCase {

  public TestXPathExtractor(String name) {
    super(name);
  }
  

  public void testRecordExtraction() throws IOException {
    final String xmlString =
      "<root>\n" +
         "<record>\n" +
           "<field1>content1.1</field1>\n" +
           "<field2><foo>FOO1</foo><ignore>IGNORE1</ignore><bar>BAR1</bar></field2>\n" +
           "<field3 attr=\"value1\">content1.3</field3>\n" +
         "</record>\n" +
         "<record>\n" +
           "<field1>content2.1</field1>\n" +
           "<field2><foo>FOO2</foo><ignore>IGNORE2</ignore><bar>BAR2</bar></field2>\n" +
           "<field3 attr=\"value2\">content2.3</field3>\n" +
         "</record>\n" +
         "<record>\n" +
           "<field1>content3.1</field1>\n" +
           "<field2><foo>FOO3</foo><ignore>IGNORE3</ignore><bar>BAR3</bar></field2>\n" +
           "<field3 attr=\"value3\">content3.3</field3>\n" +
         "</record>\n" +
       "</root>";

    final Tree<XmlLite.Data> xmlTree = XmlFactory.buildXmlTree(xmlString, true, false);
    final XPathExtractor xpathExtractor = new XPathExtractor(xmlTree);
    xpathExtractor.setExcludes("**.ignore", false);
    final XelExtraction recordExtractions = xpathExtractor.extract("root.record");
    final List<XelExtractor> recordExtractors = recordExtractions.asExtractors();
    final Map<String, String> label2xpath =
      StringUtil.toMap(new String[][] {
          {"f1", "record.field1"},
          {"f2", "record.field2"},
          {"foo", "record.field2.foo"},
          {"bar", "record.field2.bar"},
          {"f3", "record.field3"},
          {"f3attr", "record.field3@attr"},
        });
    final String[][] expectedRecords = new String[][] {
      {"f1", "content1.1", "f2", "FOO1 BAR1", "foo", "FOO1", "bar", "BAR1", "f3", "content1.3", "f3attr", "value1"},
      {"f1", "content2.1", "f2", "FOO2 BAR2", "foo", "FOO2", "bar", "BAR2", "f3", "content2.3", "f3attr", "value2"},
      {"f1", "content3.1", "f2", "FOO3 BAR3", "foo", "FOO3", "bar", "BAR3", "f3", "content3.3", "f3attr", "value3"},
    };

    int recNum = 0;
    for (XelExtractor recordExtractor : recordExtractors) {
      final Map<String, List<String>> records = recordExtractor.getText(label2xpath);
      verifyMappings(expectedRecords[recNum], records);
      ++recNum;
    }
  }

  /**
   * Verify that the map contains all expected pairs of the form
   * {key1, value1, key2, value2, ...}.
   */
  private final void verifyMappings(String[] expectedPairs, Map<String, List<String>> map) {
    for (int i = 0; i < expectedPairs.length; i += 2) {
      final List<String> values = map.get(expectedPairs[i]);
      assertTrue("missing '" + expectedPairs[i] + "' -> '" + expectedPairs[i + 1] + "'!",
                 values.contains(expectedPairs[i + 1]));
    }
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestXPathExtractor.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
