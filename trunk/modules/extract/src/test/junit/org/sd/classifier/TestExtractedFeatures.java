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
import java.net.URISyntaxException;
import java.util.List;

import org.sd.io.FileUtil;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Test class for {@link ExtractedFeatures}.  This really only tests the reading and
 * writing of feature vectors values from/to ARFFs, as all other functionality is
 * tested in the {@link FeatureDictionary} and {@link FeatureVector} classes.
 *  
 * @author Dave Barney
 */
public class TestExtractedFeatures extends TestCase {
  /**
   * Intented to test the reading and writing of arff files. We basically
   * read in an arff (in construction), test the values manually, write out
   * the values to a different arff and comapre the files, which should be
   * identical.
   */
  public void testLoadFeatureVectors() throws IOException, URISyntaxException {
    File arffFile = new File(TestExtractedFeatures.class.getResource("resources/sample_valid.arff").toURI());
    ExtractedFeatures extractedFeatures = new ExtractedFeatures(arffFile, null);
    List<FeatureVector> featureVectors = extractedFeatures.getFeatureVectors();
    
    // test internal contents
    assertTrue(featureVectors.size() == 4);
    assertTrue(featureVectors.get(0).getAttributes().size() == 5);
    assertTrue(featureVectors.get(1).getAttributes().size() == 5);
    assertTrue(featureVectors.get(2).getAttributes().size() == 4);
    assertTrue(featureVectors.get(3).getAttributes().size() == 3);
    
    // Now test writing of file
    File tmpArffFile = File.createTempFile("TestJunit", ".arff");
    tmpArffFile.deleteOnExit();
    
    extractedFeatures.writeToArff(tmpArffFile);
    
    // Files should be identical - test
    String origArffContent = FileUtil.getTextFileAsString(arffFile);
    String newArffContent = FileUtil.getTextFileAsString(tmpArffFile);
    
    assertEquals(origArffContent, newArffContent);
  }
  
  public static Test suite() {
    TestSuite suite = new TestSuite(TestExtractedFeatures.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
