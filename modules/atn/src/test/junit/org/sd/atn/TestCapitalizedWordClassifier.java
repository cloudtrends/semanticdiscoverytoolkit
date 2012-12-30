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
package org.sd.atn;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.sd.token.Break;
import org.sd.token.StandardTokenizerFactory;
import org.sd.token.StandardTokenizerOptions;
import org.sd.token.TokenRevisionStrategy;
import org.sd.xml.DomElement;
import org.sd.xml.XmlStringBuilder;

/**
 * JUnit Tests for the CapitalizedWordClassifier class.
 * <p>
 * @author Spence Koehler
 */
public class TestCapitalizedWordClassifier extends TestCase {

  public TestCapitalizedWordClassifier(String name) {
    super(name);
  }
  

  public void testExcludeAllCapsIncludeSingleLetter() {
    final ResourceManager resourceManager = new ResourceManager();
    final DomElement xml = new XmlStringBuilder().setXmlString(
      "<word excludeAllCaps='true'>\n" +
      "  <jclass>org.sd.atn.CapitalizedWordClassifier</jclass>\n" +
      "</word>"
      ).getXmlElement();

    final CapitalizedWordClassifier classifier = new CapitalizedWordClassifier(xml, resourceManager, null);

    for (char c = 'A' ; c <= 'Z'; ++c) {
      assertTrue(classifier.doClassify(StandardTokenizerFactory.getFirstToken(c + ""), null));
    }

    for (char c = 'A' ; c <= 'Z'; ++c) {
      assertFalse(classifier.doClassify(StandardTokenizerFactory.getFirstToken("A" + c), null));
    }

    classifier.setAllCapsMinLen(3);

    for (char c = 'A' ; c <= 'Z'; ++c) {
      assertTrue(classifier.doClassify(StandardTokenizerFactory.getFirstToken("A" + c), null));
    }

    for (char c = 'A' ; c <= 'Z'; ++c) {
      assertFalse(classifier.doClassify(StandardTokenizerFactory.getFirstToken("AB" + c), null));
    }
  }

  public void testLowerCaseInitial() {
    final ResourceManager resourceManager = new ResourceManager();
    final DomElement xml = new XmlStringBuilder().setXmlString(
      "<word lowerCaseInitial='true'>\n" +
      "  <jclass>org.sd.atn.CapitalizedWordClassifier</jclass>\n" +
      "</word>"
      ).getXmlElement();

    final CapitalizedWordClassifier classifier = new CapitalizedWordClassifier(xml, resourceManager, null);

    // accept lower-case letter followed by '.'
    for (char c = 'a' ; c <= 'z'; ++c) {
      assertTrue(classifier.doClassify(StandardTokenizerFactory.getFirstToken(c + "."), null));
    }

    // accept case letter not followed by '.'
    for (char c = 'A' ; c <= 'Z'; ++c) {
      assertTrue(classifier.doClassify(StandardTokenizerFactory.getFirstToken(c + ""), null));
    }
  }

  public void testNoSingleLetterButYesLowerCaseInitial() {
    final ResourceManager resourceManager = new ResourceManager();
    final DomElement xml = new XmlStringBuilder().setXmlString(
      "<word singleLetter='false' lowerCaseInitial='true'>\n" +
      "  <jclass>org.sd.atn.CapitalizedWordClassifier</jclass>\n" +
      "</word>"
      ).getXmlElement();

    final CapitalizedWordClassifier classifier = new CapitalizedWordClassifier(xml, resourceManager, null);

    // don't accept single letter not followed by '.'
    for (char c = 'A' ; c <= 'Z'; ++c) {
      assertFalse(classifier.doClassify(StandardTokenizerFactory.getFirstToken(c + ""), null));
    }

    // accept lower-case letter followed by '.'
    for (char c = 'a' ; c <= 'z'; ++c) {
      assertTrue(classifier.doClassify(StandardTokenizerFactory.getFirstToken(c + "."), null));
    }

    // accept upper-case letter followed by '.'
    for (char c = 'A' ; c <= 'Z'; ++c) {
      assertTrue(classifier.doClassify(StandardTokenizerFactory.getFirstToken(c + "."), null));
    }
  }

  public void testDoAcceptDash() {
    final ResourceManager resourceManager = new ResourceManager();
    final DomElement xml = new XmlStringBuilder().setXmlString(
      "<word acceptDash='true'>\n" +
      "  <jclass>org.sd.atn.CapitalizedWordClassifier</jclass>\n" +
      "</word>"
      ).getXmlElement();

    final CapitalizedWordClassifier classifier = new CapitalizedWordClassifier(xml, resourceManager, null);

    final StandardTokenizerOptions tOpts = new StandardTokenizerOptions();
    tOpts.setRevisionStrategy(TokenRevisionStrategy.SO);
    tOpts.setEmbeddedDashBreak(Break.NO_BREAK);

    // do accept a dash
    assertTrue(classifier.doClassify(StandardTokenizerFactory.getFirstToken("Ramirez", tOpts), null));
    assertTrue(classifier.doClassify(StandardTokenizerFactory.getFirstToken("Martinez", tOpts), null));
    assertTrue(classifier.doClassify(StandardTokenizerFactory.getFirstToken("Ramirez-Martinez", tOpts), null));
    assertTrue(classifier.doClassify(StandardTokenizerFactory.getFirstToken("Ramirez-martinez", tOpts), null));
  }

  public void testDontAcceptDash() {
    final ResourceManager resourceManager = new ResourceManager();
    final DomElement xml = new XmlStringBuilder().setXmlString(
      "<word acceptDash='false'>\n" +
      "  <jclass>org.sd.atn.CapitalizedWordClassifier</jclass>\n" +
      "</word>"
      ).getXmlElement();

    final CapitalizedWordClassifier classifier = new CapitalizedWordClassifier(xml, resourceManager, null);

    final StandardTokenizerOptions tOpts = new StandardTokenizerOptions();
    tOpts.setRevisionStrategy(TokenRevisionStrategy.SO);
    tOpts.setEmbeddedDashBreak(Break.NO_BREAK);

    // don't accept a dash
    assertTrue(classifier.doClassify(StandardTokenizerFactory.getFirstToken("Ramirez", tOpts), null));
    assertTrue(classifier.doClassify(StandardTokenizerFactory.getFirstToken("Martinez", tOpts), null));
    assertFalse(classifier.doClassify(StandardTokenizerFactory.getFirstToken("Ramirez-Martinez", tOpts), null));
    assertFalse(classifier.doClassify(StandardTokenizerFactory.getFirstToken("Ramirez-martinez", tOpts), null));
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestCapitalizedWordClassifier.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
