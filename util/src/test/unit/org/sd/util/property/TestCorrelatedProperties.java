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
package org.sd.util.property;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.sd.io.FileUtil;
import org.sd.util.PropertiesParser;

import java.io.IOException;
import java.util.Properties;

/**
 * JUnit Tests for the CorrelatedProperties class.
 * <p>
 * @author Spence Koehler
 */
public class TestCorrelatedProperties extends TestCase {

  public TestCorrelatedProperties(String name) {
    super(name);
  }
  
  public void testExtractProperties1() throws IOException {
    final PropertySchema propertySchema = new PropertySchema();
    propertySchema.load(FileUtil.getFile(this.getClass(), "resources/test-extractProperties-1-schema.txt"));
    final PropertiesParser propertiesParser = new PropertiesParser(new String[] {
      FileUtil.getFilename(this.getClass(), "resources/test-extractProperties-1.properties"),
    });
    final CorrelatedProperties correlatedProperties = new CorrelatedProperties(propertySchema, propertiesParser);
    final Properties properties = new Properties();

    assertTrue(correlatedProperties.extractProperties(properties, "ClassifierRunner"));
    assertEquals("testDict", properties.getProperty("FeatureDictionary"));
    assertEquals("testFeatureExtractor", properties.getProperty("FeatureExtractor"));

    assertTrue(correlatedProperties.extractProperties(properties, "testDict"));
    assertEquals("/tmp/testArff.txt", properties.getProperty("inputArff"));
    
    assertTrue(correlatedProperties.extractProperties(properties, "testFeatureExtractor"));
    assertEquals("testFEP", properties.getProperty("FeatureExtractionPipeline"));

    assertTrue(correlatedProperties.extractProperties(properties, "testFEP"));
    assertEquals("emailExtractor,primaryBagOfWordsExtractor", properties.getProperty("FeatureExtractor"));

    assertTrue(correlatedProperties.extractProperties(properties, "emailRegexExtractor"));
    assertEquals("email", properties.getProperty("extractionType"));
    assertEquals(".*\\b([a-zA-Z0-9\\_\\-\\.]+@[a-zA-Z0-9\\_\\-\\.]+\\.[a-zA-Z][a-zA-Z]+)\\b.*", properties.getProperty("pattern"));
    
    assertTrue(correlatedProperties.extractProperties(properties, "bagOfWordsExtractor"));
    assertEquals("bagOfWords", properties.getProperty("extractionType"));
    assertEquals("testBag", properties.getProperty("bagName"));
    assertEquals("1000", properties.getProperty("maxWords"));
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestCorrelatedProperties.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
