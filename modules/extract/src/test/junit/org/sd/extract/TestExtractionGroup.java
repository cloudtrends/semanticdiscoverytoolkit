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
package org.sd.extract;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.sd.io.FileUtil;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 * JUnit Tests for the ExtractionGroup class.
 * <p>
 * @author Spence Koehler
 */
public class TestExtractionGroup extends TestCase {

  public TestExtractionGroup(String name) {
    super(name);
  }
  
  private static final String TEST_EXTRAPOLATION_FILE1 = "resources/test-extrapolation-1.html";
  private static final String TEST_EXTRAPOLATION_FILE2 = "resources/test-extrapolation-2.html";

  public void testExtrapolation_SingleIndexGroup() throws IOException {
    final ExtractionPipeline megaExtractor = ExtractionPipeline.buildDefaultHtmlPipeline(true, true, false, new Extractor[]{new RegexExtractor("foo", "^Foo$")});

    final TextContainer textContainer = megaExtractor.runExtractors(FileUtil.getFile(this.getClass(), TEST_EXTRAPOLATION_FILE1), null, false);
    final ExtractionResults extractionResults = textContainer.getExtractionResults(false);

    final List<Extraction> headings = extractionResults.getExtractions(HeadingExtractor.EXTRACTION_TYPE);
    final HeadingOrganizer headingOrganizer = new HeadingOrganizer(headings);
    final Collection<ExtractionGroup> fooGroups = extractionResults.getExtractionGroups("foo");

    final String[][] expected = new String[][]{{"Foo", "true"}, {"Foo-1", null}, {"Foo-2", null}, {"Foo-3", null}};

//     final String[][] expected = new String[][]{{"Foo", "true"}, {"Foo-1", null}, {"Foo-2", null}, {"Foo-3", null},
//                                                {"Bar", null}, {"Bar-1", null}, {"Bar-2", null}, {"Bar-3", null},
//                                                {"Baz", null}, {"Baz-1", null}, {"Baz-2", null}, {"Baz-3", null}};

    assertEquals(1, fooGroups.size());

    for (ExtractionGroup fooGroup : fooGroups) {
      final ExtractionGroup extractionGroup = fooGroup.buildExtrapolatedGroup(headingOrganizer);
      final List<Extraction> extractions = extractionGroup.getExtractions();
      
      int index = 0;
      for (Extraction extraction : extractions) {
        assertEquals(expected[index][0], extraction.getText());
        if ("true".equals(expected[index][1])) {
          assertFalse(extraction.isExtrapolated());
        }
        else {
          assertTrue(extraction.isExtrapolated());
        }
        ++index;
      }
    }
  }

  public void testExtrapolation_MultiIndexGroup() throws IOException {
    final ExtractionPipeline megaExtractor = ExtractionPipeline.buildDefaultHtmlPipeline(true, true, false, new Extractor[]{new RegexExtractor("foo", "^[FB][oa][orz]$")});

    final TextContainer textContainer = megaExtractor.runExtractors(FileUtil.getFile(this.getClass(), TEST_EXTRAPOLATION_FILE1), null, false);
    final ExtractionResults extractionResults = textContainer.getExtractionResults(false);

    final List<Extraction> headings = extractionResults.getExtractions(HeadingExtractor.EXTRACTION_TYPE);
    final HeadingOrganizer headingOrganizer = new HeadingOrganizer(headings);
    final Collection<ExtractionGroup> fooGroups = extractionResults.getExtractionGroups("foo");

    final String[][] expected = new String[][]{{"Foo", "true"}, {"Foo-1", null}, {"Foo-2", null}, {"Foo-3", null},
                                               {"Bar", "true"}, {"Bar-1", null}, {"Bar-2", null}, {"Bar-3", null},
                                               {"Baz", "true"}, {"Baz-1", null}, {"Baz-2", null}, {"Baz-3", null}};

    assertEquals(1, fooGroups.size());

    for (ExtractionGroup fooGroup : fooGroups) {
      final ExtractionGroup extrapolatedGroup = fooGroup.buildExtrapolatedGroup(headingOrganizer);
      final List<Extraction> extractions = extrapolatedGroup.getExtractions();
      
      int index = 0;
      for (Extraction extraction : extractions) {
        assertEquals(expected[index][0], extraction.getText());
        if ("true".equals(expected[index][1])) {
          assertFalse(extraction.isExtrapolated());
        }
        else {
          assertTrue(extraction.isExtrapolated());
        }
        ++index;
      }
    }
  }

  public void testExtrapolation_KeyGroup_Column_SingleTable() throws IOException {
    final ExtractionPipeline megaExtractor =
      ExtractionPipeline.buildDefaultHtmlPipeline(true, true, false, new Extractor[] {
        new RegexExtractor("foo", "^Foo-\\d$"),
        new RegexExtractor("bar", "^Bar-\\d$"),
        new RegexExtractor("baz", "^Baz-\\d$"),
      });

    final TextContainer textContainer = megaExtractor.runExtractors(FileUtil.getFile(this.getClass(), TEST_EXTRAPOLATION_FILE2), null, false);
    final ExtractionResults extractionResults = textContainer.getExtractionResults(false);

    final List<Extraction> headings = extractionResults.getExtractions(HeadingExtractor.EXTRACTION_TYPE);
    final HeadingOrganizer headingOrganizer = new HeadingOrganizer(headings);
    final Collection<ExtractionGroup> fooGroups = extractionResults.getExtractionGroups("foo");

    final String[][] expected = new String[][]{{"Foo-1", "true"}, {"F-2", null}, {"F-3", null}, {"F-4", null},
    };
//                                               {"Bar", null}, {"Bar-1", "true"}, {"Bar-2", "true"}, {"Bar-3", "true"},
//                                               {"Baz", null}, {"Baz-1", "true"}, {"Baz-2", "true"}, {"Baz-3", "true"}};

    assertEquals(1, fooGroups.size());

    for (ExtractionGroup fooGroup : fooGroups) {
      final ExtractionGroup extrapolatedGroup = fooGroup.buildExtrapolatedGroup(headingOrganizer);
      final List<Extraction> extractions = extrapolatedGroup.getExtractions();
      
      int index = 0;
      for (Extraction extraction : extractions) {
        assertEquals(expected[index][0], extraction.getText());
        if ("true".equals(expected[index][1])) {
          assertFalse(extraction.isExtrapolated());
        }
        else {
          assertTrue(extraction.isExtrapolated());
        }
        ++index;
      }
    }
  }

  public void testExtrapolation_KeyGroup_Column_MultiTable() throws IOException {
//I'm here...
  }

  public void testExtrapolation_KeyGroup_Row() throws IOException {
//I'm here...
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestExtractionGroup.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
