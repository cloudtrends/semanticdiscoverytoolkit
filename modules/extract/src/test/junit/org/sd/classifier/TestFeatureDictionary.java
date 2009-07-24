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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Text cases for {@link FeatureDictionary}.
 * 
 * @author Dave Barney
 */
public class TestFeatureDictionary extends TestCase {
  
  public void testInitArff() throws IOException, URISyntaxException {
    // Test valid ARFF
    File arffFile = new File(TestFeatureDictionary.class.getResource("resources/sample_valid.arff").toURI());
    FeatureDictionary featureDictionary = new FeatureDictionary(arffFile);
    
    assertEquals(featureDictionary.getName(), "SAMPLE_TEST");
    assertEquals(featureDictionary.getClassificationAttributeName(), "CLASS");
    
    String[] classificationLabels = new String[] {"Class1", "Class2"};
    for (String label : classificationLabels) {
      assertNotNull(featureDictionary.getClassificationAttribute(label));
    }
    
    String[] values = new String[] {"value1", "value2"};
    String[] loadedValues = featureDictionary.getNominalFeatureValues(featureDictionary.getNominalFeatureAttribute("NominalAttribute", "value1"));
    assertTrue(values.length == loadedValues.length);
    for (int i=0; i < values.length; i++) {
      assertEquals(values[i], loadedValues[i]);
    }
    

    
    // Test invalid ARFF 1
    arffFile = new File(TestFeatureDictionary.class.getResource("resources/sample_invalid1.arff").toURI());
    boolean exceptionThrown = false;
    try {
      featureDictionary = new FeatureDictionary(arffFile);
    } catch (IllegalArgumentException iae) {
      exceptionThrown = true;
    }
    assertTrue(exceptionThrown);

    
    // Test invalid ARFF 2
    arffFile = new File(TestFeatureDictionary.class.getResource("resources/sample_invalid2.arff").toURI());
    exceptionThrown = false;
    try {
      featureDictionary = new FeatureDictionary(arffFile);
    } catch (IllegalArgumentException iae) {
      exceptionThrown = true;
    }
    assertTrue(exceptionThrown);

    
    // Test invalid ARFF 3
    arffFile = new File(TestFeatureDictionary.class.getResource("resources/sample_invalid3.arff").toURI());
    exceptionThrown = false;
    try {
      featureDictionary = new FeatureDictionary(arffFile);
    } catch (IllegalArgumentException iae) {
      exceptionThrown = true;
    }
    assertTrue(exceptionThrown);
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestFeatureDictionary.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
