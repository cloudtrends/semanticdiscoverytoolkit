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

import java.util.*;
import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * JUnit tests for the FeatureVector class
 * <p>
 * @author Ryan McGuire, Spence Koehler
 */
public class TestFeatureVector extends TestCase{

  public TestFeatureVector(String name) {
    super(name);
  }

  /**
   * Sparse FeatureAttribute implementation for testing purposes only
   */
  private class FeatureAttributeTest implements FeatureAttribute{
    public FeatureDictionary getFeatureDictionary(){ return null; }
    public String getName(){ return null; }
    public String[] getBagOfWords() { return null; }
    public Double toDouble(double value){  return null; }
    public Double toDouble(int value){ return null;  }
    public Double toDouble(String value){ return null;  }
    public boolean isNumeric(){ return true;  }
    public boolean isNominal(){ return true; }
    public boolean isBagOfWords(){ return true;  }
    public NumericFeatureAttribute asNumeric(){ return null; }
    public NominalFeatureAttribute asNominal(){ return null; }
    public Double getDefaultValue() { return null; }
  }
  
  /**
   * Test setting and retrieving a Double value
   */
  public void testDoubleSet(){
    FeatureVector vector = new FeatureVector();
    FeatureAttribute attribute = new FeatureAttributeTest();
    vector.setValue(attribute,3.0);
    Double value = vector.getValue(attribute);
    assertEquals(3.0,value);
  }

  /**
   * Test retrieving the set of all attributes added to a FeatureVector
   */
  public void testGetAttributes(){
    FeatureVector vector = new FeatureVector();
    FeatureAttribute attr1 = new FeatureAttributeTest();
    FeatureAttribute attr2 = new FeatureAttributeTest();
    FeatureAttribute attr3 = new FeatureAttributeTest();
    FeatureAttribute attr4 = new FeatureAttributeTest();
    vector.setValue(attr1,1.0);
    vector.setValue(attr2,2.0);
    vector.setValue(attr3,3.0);
    vector.setValue(attr4,4.0);
    //Now see if we get the same set of attrs back.
    Set<FeatureAttribute> attrSet = vector.getAttributes();
    Set<FeatureAttribute>expectedSet = new HashSet<FeatureAttribute>(Arrays.asList(attr1,attr2,attr3,attr4));
    System.out.println(expectedSet);
    System.out.println(attrSet);
    assertTrue(attrSet.equals(expectedSet));
  }
  
  public static Test suite() {
    TestSuite suite = new TestSuite(TestFeatureVector.class);
    return suite;
  }
  
  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }
}
