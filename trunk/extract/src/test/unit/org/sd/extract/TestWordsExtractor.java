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

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * JUnit Tests for the WordsExtractor class.
 * <p>
 * @author Spence Koehler
 */
public class TestWordsExtractor extends TestCase {

  public TestWordsExtractor(String name) {
    super(name);
  }
  
  private final void verify(ExtractionHarness harness, File file, String[] expectedWords) throws IOException {
    final ExtractionResults results = harness.run(file, null, false);
    final List<Extraction> extractions = results.getExtractions();

    if (expectedWords == null) {
      System.out.println(file + " (" + extractions.size() + " extractions)");
      for (Extraction extraction : extractions) {
        System.out.println("\t" + extraction);
      }
    }
    else {
      assertEquals(expectedWords.length, extractions.size());
      final Iterator<Extraction> extractionIter = extractions.iterator();
      for (String expectedWord : expectedWords) {
        final Extraction extraction = extractionIter.next();
        final String extractionString = extraction.asString();
        assertEquals(expectedWord, expectedWord, extractionString);
      }
    }
  }

  public void test1() throws IOException {
    final WordsExtractor extractor = new WordsExtractor("foo");
    final ExtractionHarness harness = new ExtractionHarness();
    harness.addExtractor(extractor);

    verify(harness,
           FileUtil.getFile(this.getClass(), "resources/testWordsExtractor-test1.html"),
           new String[] {
             "foo",
             "bar",
             "now",
             "is",
             "the",
             "time",
             "and",
             "camelcase",
             "camel",
             "case",
             "too",
             "how",
             "about",
             "wwwbazcom",
             "last",
             "modified",
             "mon",
             "sep",
             "24",
             "15",
             "39",
             "30",
             "mdt",
             "2007",
           });
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestWordsExtractor.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
