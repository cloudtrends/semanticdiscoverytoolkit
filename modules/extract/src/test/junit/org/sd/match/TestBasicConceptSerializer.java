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
package org.sd.match;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit Tests for the BasicConceptSerializer class.
 * <p>
 * @author Spence Koehler
 */
public class TestBasicConceptSerializer extends TestCase {

  public TestBasicConceptSerializer(String name) {
    super(name);
  }
  
  public void test1() {
    final BasicConceptSerializer serializer = new BasicConceptSerializer();

    final ConceptModel model1 = TestConceptModel.buildTestModel();
    model1.serialize(serializer);

    final String basicConcept = serializer.asString();
    assertEquals("123 F FULL_UK_DESC D MISCELLANEOUS S PRIMARY V NORMAL W NORMAL valves D MISCELLANEOUS S PRIMARY V NORMAL W NORMAL butterfly D MISCELLANEOUS S PRIMARY V NORMAL W NORMAL ethylene W NORMAL propylene W NORMAL diene W NORMAL monomer W NORMAL seat S ACRONYM V NORMAL W ACRONYM epdm W NORMAL seat D MISCELLANEOUS S PRIMARY V NORMAL W NORMAL lugged W FUNCTIONAL and W NORMAL tapped S CONJ V NORMAL W NORMAL lugged S CONJ V NORMAL W NORMAL tapped", basicConcept);

    final ConceptModel model2 = new BasicConceptDeserializer().buildConceptModel(basicConcept);

    final String model1tree = model1.getTreeString();
    final String model2tree = model2.getTreeString();

    assertEquals(model1tree, model2tree);
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestBasicConceptSerializer.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
