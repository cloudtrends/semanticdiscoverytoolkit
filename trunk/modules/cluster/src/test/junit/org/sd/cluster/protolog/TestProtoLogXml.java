/*
    Copyright 2011 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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
package org.sd.cluster.protolog;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.sd.cluster.protolog.codegen.ProtoLogProtos.*;
import org.sd.xml.XmlStringBuilder;

/**
 * JUnit Tests for the ProtoLogXml class.
 * <p>
 * @author Spence Koehler
 */
public class TestProtoLogXml extends TestCase {

  public TestProtoLogXml(String name) {
    super(name);
  }
  

  public void testXmlRoundTrip() {
    final String xml = "<event _id=\"456\" _time=\"1307738691178\" _type=\"START\" _who=\"me\" _what=\"test\" att=\"s_testing1|i_123\" _A_=\"s_testing2|l_987\"></event>";

    final ProtoLogXml protoLogXml = new ProtoLogXml();

    final EventEntry eventEntry = protoLogXml.fromXml(xml);
    final XmlStringBuilder genxml = protoLogXml.asXml(eventEntry);

    assertEquals(xml, genxml.getXmlString());
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestProtoLogXml.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
