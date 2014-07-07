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
package org.sd.token;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.sd.xml.DataProperties;

/**
 * JUnit Tests for the StandardTokenizer class.
 * <p>
 * @author Spence Koehler
 */
public class TestSegmentTokenizer extends TestCase {

  public TestSegmentTokenizer(String name) {
    super(name);
  }
  

  protected Tokenizer buildTokenizer(String text, SegmentPointerFinderFactory ptrFinderFactory, StandardTokenizerOptions tokenizerOptions, DataProperties dataProperties) {
    final SegmentPointerFinder ptrFinder = ptrFinderFactory.getSegmentPointerFinder(text);
    return new SegmentTokenizer(ptrFinder, tokenizerOptions, dataProperties);
  }

  public void testNamedEntities1() {
    final DataProperties dataProperties = new DataProperties();
    dataProperties.set("segmentHardBoundaries", NamedEntitySegmentFinder.ENTITY_LABEL);
    dataProperties.set("segmentUnbreakables", NamedEntitySegmentFinder.ENTITY_LABEL);

    final SegmentTokenizer tokenizer = 
      NamedEntitySegmentFinder.getSegmentTokenizer("This is a John Smith 3-5280", 
                                                   false, dataProperties);

    final TokenizeTest tokenizeTest =
      new TokenizeTest("NamedEntities1", tokenizer,
                       new String[] {
                         "This",
                         "is",
                         "a",
                         "John Smith",
                         "3",
                         "5280",
                       },
                       null,
                       new String[]{
                         null,
                         null,
                         null,
                         NamedEntitySegmentFinder.ENTITY_LABEL,
                         null,
                         null,
                       });

    assertTrue(tokenizeTest.runTest());
  }

  public void testNamedEntities2a() {
    final DataProperties dataProperties = new DataProperties();
    dataProperties.set("segmentHardBoundaries", NamedEntitySegmentFinder.ENTITY_LABEL);
    dataProperties.set("segmentUnbreakables", NamedEntitySegmentFinder.ENTITY_LABEL);

    final SegmentTokenizer tokenizer = 
      NamedEntitySegmentFinder.getSegmentTokenizer("JOHNSON, Mary D.", 
                                                   false, dataProperties);

    final TokenizeTest tokenizeTest =
      new TokenizeTest("NamedEntities1", tokenizer,
                       new String[] {
                         "JOHNSON", 
                         "Mary D",
                       },
                       null,
                       new String[]{
                         NamedEntitySegmentFinder.ENTITY_LABEL,
                         null,
                         //NamedEntitySegmentFinder.ENTITY_LABEL,
                       });

    assertTrue(tokenizeTest.runTest());
  }

  public void testNamedEntities2b() {
    final DataProperties dataProperties = new DataProperties();
    dataProperties.set("segmentHardBoundaries", NamedEntitySegmentFinder.ENTITY_LABEL);
    dataProperties.set("segmentUnbreakables", NamedEntitySegmentFinder.ENTITY_LABEL);

    final SegmentTokenizer tokenizer = 
      NamedEntitySegmentFinder.getSegmentTokenizer("JOHNSON, Mary D.", 
                                                   true, dataProperties);

    final TokenizeTest tokenizeTest =
      new TokenizeTest("NamedEntities1", tokenizer,
                       new String[] {
                         "JOHNSON, Mary D", 
                       },
                       null,
                       new String[]{
                         NamedEntitySegmentFinder.ENTITY_LABEL,
                       });

    assertTrue(tokenizeTest.runTest());
  }

  public void testNamedEntities2c() {
    final DataProperties dataProperties = new DataProperties();
    dataProperties.set("segmentHardBoundaries", NamedEntitySegmentFinder.ENTITY_LABEL);
    dataProperties.set("segmentUnbreakables", NamedEntitySegmentFinder.ENTITY_LABEL);

    final SegmentTokenizer tokenizer = 
      NamedEntitySegmentFinder.getSegmentTokenizer("BASS, BUFORD H. Asst. Prof, of Health and Physical Education. B.S. in Ed., M.S. in Ed., Illinois State Normal University. BAUCOM, ADRIAN.", 
                                                   true, dataProperties);

    final TokenizeTest tokenizeTest =
      new TokenizeTest("NamedEntities1", tokenizer,
                       new String[] {
                         "BASS, BUFORD H. Asst. Prof",
                         "of", 
                         "Health", 
                         "and", 
                         "Physical Education. B.S",
                         "in", 
                         "Ed., M.S", 
                         "in", 
                         "Ed., Illinois State Normal University. BAUCOM, ADRIAN", 
                       },
                       null,
                       // todo: any of the inner segments besides the first dont carry the entity flag in this test path
                       new String[]{
                         NamedEntitySegmentFinder.ENTITY_LABEL,
                         null,
                         NamedEntitySegmentFinder.ENTITY_LABEL,
                         null,
                         NamedEntitySegmentFinder.ENTITY_LABEL,
                         null,
                         NamedEntitySegmentFinder.ENTITY_LABEL,
                         null,
                         NamedEntitySegmentFinder.ENTITY_LABEL,
                       });

    assertTrue(tokenizeTest.runTest());
  }



  public static Test suite() {
    TestSuite suite = new TestSuite(TestSegmentTokenizer.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
