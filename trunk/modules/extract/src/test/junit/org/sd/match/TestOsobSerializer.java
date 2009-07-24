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
 * JUnit Tests for the OsobSerializer class.
 * <p>
 * @author Spence Koehler
 */
public class TestOsobSerializer extends TestCase {

  public TestOsobSerializer(String name) {
    super(name);
  }
  
  public void testUnsorted() {
    final OsobSerializer serializer = new OsobSerializer(false);

    final ConceptModel model1 = TestConceptModel.buildTestModel();
    model1.serialize(serializer);

    final String encodedOsob = serializer.asString();
    assertEquals("AAAAewEBEQEABnZhbHZlcwABEQEACWJ1dHRlcmZseQABEQEACGV0aHlsZW5lAAAAAQEJcHJvcHlsZW5lAAAAAQIFZGllbmUAAAABAwdtb25vbWVyAAAAAQQEc2VhdAAAFQMABGVwZG0AAAABAQRzZWF0AAERAQAGbHVnZ2VkAAAABQEDYW5kAAAAAQIGdGFwcGVkAAAXAQAGbHVnZ2VkAAAXAQAGdGFwcGVk", encodedOsob);

    final ConceptModel model2 = new OsobDeserializer().buildConceptModel(encodedOsob);

    final String model1tree = model1.getTreeString();
    final String model2tree = model2.getTreeString();

    assertEquals(model1tree, model2tree);
  }

  public void testReconstruction() {
    final String treeString = "(C148187 (F2 (T1 (S0 (V0 W0|fireplaces))) (T2 (S0 (V0 W0|english)))) (F2 (T1 (S0 (V0 W0|fire W0|places))) (T2 (S0 (V0 W0|english)))) (F4 (T0 (S0 (V0 W0|haarden))) (T0 (S0 (V0 W0|engels)))))";
    final ConceptModel model1 = new ConceptModel();
    model1.loadWithTreeString(treeString);

    final OsobSerializer serializer = new OsobSerializer(false);
    model1.serialize(serializer);
    final String encodedOsob = serializer.asString();

    final ConceptModel model2 = new OsobDeserializer().buildConceptModel(encodedOsob);
    final String curTreeString = model2.getTreeString();

    assertEquals(treeString, curTreeString);
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestOsobSerializer.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
