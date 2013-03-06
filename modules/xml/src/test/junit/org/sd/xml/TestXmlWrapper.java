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
package org.sd.xml;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * 
 * <p>
 * @author asanderson
 */
public class TestXmlWrapper extends TestCase
{
  public TestXmlWrapper(String name) {
    super(name);
  }

  public void testWrapper()
  {
    final XmlWrapper wrapper = new XmlWrapper("event");
    wrapper.addAttribute("d", "SUIR and MARGARET O' NEILL of 22 YORK ST on 16 June 1869");
    System.out.println(wrapper.getXmlStringBuilder().getXmlElement().toString());
  }

  public void testWrapper2()
  {
    XmlStringBuilder builder = new XmlStringBuilder();
    builder.addTag("event d=\"SUIR and MARGARET O' NEILL of 22 YORK ST on 16 June 186\"");
    final XmlWrapper wrapper = new XmlWrapper(builder.getXmlElement());
    System.out.println(wrapper.getXmlStringBuilder().getXmlElement().toString());
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestXmlWrapper.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }

}
