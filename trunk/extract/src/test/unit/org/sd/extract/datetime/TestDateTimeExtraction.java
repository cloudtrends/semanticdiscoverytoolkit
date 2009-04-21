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
package org.sd.extract.datetime;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.sd.extract.Extraction;
import org.sd.extract.Extractor;
import org.sd.extract.ExtractionGroup;
import org.sd.extract.ExtractionPipeline;
import org.sd.extract.ExtractionResults;
import org.sd.extract.ExtractionRunner;
import org.sd.io.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 * JUnit Tests for the DateTimeExtraction class.
 * <p>
 * @author Spence Koehler
 */
public class TestDateTimeExtraction extends TestCase {

  public TestDateTimeExtraction(String name) {
    super(name);
  }
  
  private static final String TEST1_RESOURCE = "resources/test1.html";
  private static final String TEST2_RESOURCE = "resources/test2.html";

  private final void verify(String resource, String[] expectedPathKeys, String[][] expectedFields) throws IOException {
    final ExtractionRunner runner = ExtractionRunner.buildExtractionRunner(false, false, false, new Extractor[] {
        new DateTimeExtractor(2000, false, 3)});

    final File file = FileUtil.getFile(this.getClass(), TEST1_RESOURCE);

    final ExtractionResults results = runner.run(file, null, true);
    final Collection<ExtractionGroup> groups = results.getExtractionGroups(DateTimeExtractor.EXTRACTION_TYPE);
    
    if (expectedPathKeys == null && expectedFields == null) {
      System.out.println("expectedPathKeys=new String[]{");
      for (ExtractionGroup group : groups) {
        System.out.println('"' + group.getPathKey() + "\",");
      }
      System.out.println("};\n");

      System.out.println("expectedFields=new String[][]{");
      for (ExtractionGroup group : groups) {
        final List<Extraction> extractions = group.getExtractions();
        System.out.print("{");
        for (Extraction extraction : extractions) {
          System.out.print('"' + extraction.getInterpretation().getFieldsString() + "\",");
        }
        System.out.println("},");
      }
      System.out.println("};\n");
    }
    else {
      assertEquals(expectedPathKeys.length, groups.size());

      int index = 0;
      for (ExtractionGroup group : groups) {
        assertEquals(expectedPathKeys[index], group.getPathKey());

        final List<Extraction> extractions = group.getExtractions();
        assertEquals(expectedFields[index].length, extractions.size());

        int index2 = 0;
        for (Extraction extraction : extractions) {
          final String fieldsString = extraction.getInterpretation().getFieldsString();
          assertEquals("(" + index + ", " + index2 + ") " + fieldsString,
                       expectedFields[index][index2], fieldsString);
          ++index2;
        }
        ++index;
      }
    }
  }

  public void test1() throws IOException {
    verify(TEST1_RESOURCE,
           new String[]{"html.body.h1", "html.body.ul.li", "html.body"},
           new String[][] {
             {"2007|5|28|-1|-1|-1|0|0"},
             {"2001|0|10|-1|-1|-1|0|0",
              "2002|1|11|-1|-1|-1|0|0",
              "2003|2|12|-1|-1|-1|0|0",
              "2004|3|13|-1|-1|-1|0|0",
              "2005|4|14|-1|-1|-1|0|0"},
//with timezone enabled, output would be:
//             {"2007|5|28|1|27|44|1|28800000",
//              "2007|5|28|1|46|16|1|28800000",},
             {"2007|5|28|1|27|44|1|0",
              "2007|5|28|1|46|16|1|0",},
           });
  }
  
  public void test2() throws IOException {
    verify(TEST2_RESOURCE,
           new String[]{
             "html.body.h1",
             "html.body.ul.li",
             "html.body",
           },
           new String[][]{
             {"2007|5|28|-1|-1|-1|0|0",},
             {"2001|0|10|-1|-1|-1|0|0","2002|1|11|-1|-1|-1|0|0","2003|2|12|-1|-1|-1|0|0","2004|3|13|-1|-1|-1|0|0","2005|4|14|-1|-1|-1|0|0",},
//with timezone enabled, output would be:
//             {"2007|5|28|1|27|44|1|28800000","2007|5|28|1|46|16|1|28800000",},
             {"2007|5|28|1|27|44|1|0","2007|5|28|1|46|16|1|0",},
           });
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestDateTimeExtraction.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
