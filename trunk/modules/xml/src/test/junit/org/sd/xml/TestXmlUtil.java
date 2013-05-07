/*
    Copyright 2013 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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

import org.sd.util.tree.Tree;

import java.io.IOException;
import java.util.List;

/**
 * JUnit Tests for the XmlUtil class.
 * <p>
 * @author Abe Sanderson
 */
public class TestXmlUtil extends TestCase {

  public TestXmlUtil(String name) {
    super(name);
  }

  public void testEscape() 
  {
    String[] inputs = new String[] {
      "She was born Sept. 30, 1915, at Winfield, the daughter of Earle and Edith Youle. She was the oldest of four children, including a brother and two sisters. She attended Winfield public schools and graduated from Winfield High School in 1932. She received her bachelor's degree in public school music with\n"+
      "                 minors in English and art, from Southwestern College in 1937. A\n"+
      "                 resident of Wichita since 1967, she taught elementary music and\n"+
      "                 art in Woodward, Okla., Oxford, Garden City and Pratt.",
    };
    String[] expected = new String[] {
      "She was born Sept. 30, 1915, at Winfield, the daughter of Earle and Edith Youle. She was the oldest of four children, including a brother and two sisters. She attended Winfield public schools and graduated from Winfield High School in 1932. She received her bachelor&apos;s degree in public school music with&#x0A;                 minors in English and art, from Southwestern College in 1937. A&#x0A;                 resident of Wichita since 1967, she taught elementary music and&#x0A;                 art in Woodward, Okla., Oxford, Garden City and Pratt.",
    };
    
    for(int i = 0; i < inputs.length; i++)
    {
      String in = inputs[i];
      String ex = expected[i];
      assertEquals(ex, XmlUtil.escape(in));
    }
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestXmlUtil.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
