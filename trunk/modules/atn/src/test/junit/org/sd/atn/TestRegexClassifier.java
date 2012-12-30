/*
    Copyright 2010 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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
package org.sd.atn;


import java.io.IOException;
import org.sd.token.StandardTokenizerFactory;
import org.sd.token.Token;
import org.sd.xml.DomElement;
import org.sd.xml.XmlFactory;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit Tests for the RegexClassifier class.
 * <p>
 * @author Spence Koehler
 */
public class TestRegexClassifier extends TestCase {

  public TestRegexClassifier(String name) {
    super(name);
  }
  

  public void testBasics() throws IOException {
    final String eltString = "<twoDigits><jclass>org.sd.token.plugin.RegexClassifier</jclass><regexes><regex type='matches' group1='num'>^([0-9][0-9])$</regex></regexes></twoDigits>";

    final DomElement element = (DomElement)(XmlFactory.loadDocument(eltString, false).getDocumentElement());
    final RegexClassifier regexClassifier = new RegexClassifier(element, null, null);

    final Token token = StandardTokenizerFactory.getFirstToken("84");
    assertTrue(regexClassifier.doClassify(token, null));

    final String value = (String)token.getFeatureValue("num", null);
    assertEquals("84", value);
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestRegexClassifier.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
