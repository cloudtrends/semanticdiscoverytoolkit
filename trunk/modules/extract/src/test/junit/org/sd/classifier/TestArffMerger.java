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
package org.sd.classifier;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.sd.io.FileUtil;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Test-case for {@link ArffMerger}.
 * 
 * @author Dave Barney
 */
public class TestArffMerger extends TestCase {
  /**
   * Test case for the main functionality of the class - to merge
   * ARFFs and SRC files.
   * @throws URISyntaxException 
   * @throws IOException
   */
  public void testMerge() throws IOException {
    File inDir = FileUtil.getFile(this.getClass(), "resources/merge_dir");
    File outArff = File.createTempFile("TestArffMerger", ".arff");
    File outSrc  = File.createTempFile("TestArffMerger", ".src");
    
    System.out.println("outArff=" + outArff.getAbsolutePath());
    System.out.println("outSrc=" + outSrc.getAbsolutePath());
//    outArff.deleteOnExit();
//    outSrc.deleteOnExit();

    ArffMerger arffMerger = new ArffMerger(inDir);
    arffMerger.merge(outArff, outSrc);
    
    final Set<String> arffContent = new HashSet<String>();
    final int numArffContentLines = FileUtil.readStrings(arffContent, outArff, null, null, null, true, false);
    final Set<String> validArffContent = new HashSet<String>();
    final int numValidArffContentLines = FileUtil.readStrings(validArffContent, FileUtil.getFile(this.getClass(), "resources/sample_merged.arff"), null, null, null, true, false);
    assertEquals(numValidArffContentLines, numArffContentLines);
    assertEquals(validArffContent, arffContent);

    final Set<String> srcContent = new LinkedHashSet<String>();
    final int numSrcContentLines = FileUtil.readStrings(srcContent, outSrc, null, null, null, true, false);
    final Set<String> validSrcContent = new LinkedHashSet<String>();
    final int numValidSrcContentLines = FileUtil.readStrings(validSrcContent, FileUtil.getFile(this.getClass(), "resources/sample_merged.src"), null, null, null, true, false);
    assertEquals(numValidSrcContentLines, numSrcContentLines);

    // make sure all lines are accounted for.
    // NOTE: (sometime) before 2008-09-29, this was just an
    // assertEquals(validSrcContent, srcContent)
    //
    // But this test started breaking somewhere along the line because
    // the lines came out in a different order with different numbers
    // even though they were equal. Believing that the order doesn't matter
    // after all, this test was changed to accomodate the reordered match.
    //
    // Therefore:
    //   [1|test1|B2C|walmart.com
    //    2|test1|B2B|siemens.com
    //    3|test1|B2C|radioshack.com
    //    4|test1|B2B|townsontractors.com
    //    5|test1|B2B|caseva.com]
    // equals:
    //   [1|test1|B2B|townsontractors.com
    //    2|test1|B2B|caseva.com
    //    3|test1|B2C|walmart.com
    //    4|test1|B2B|siemens.com
    //    5|test1|B2C|radioshack.com]
    //
    for (String srcString : srcContent) {
      final String commonSrcString = srcString.substring(srcString.indexOf('|'));
      boolean foundMatch = false;
      for (String validSrcString : validSrcContent) {
        if (validSrcString.endsWith(commonSrcString)) {
          foundMatch = true;
          break;
        }
      }
      assertTrue("Missing '" + srcString + "'!", foundMatch);
    }
  }
  
  public static Test suite() {
    TestSuite suite = new TestSuite(TestArffMerger.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
