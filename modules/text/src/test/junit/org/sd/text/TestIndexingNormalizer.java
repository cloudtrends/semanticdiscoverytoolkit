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
package org.sd.text;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.sd.nlp.NormalizedString;

/**
 * JUnit Tests for the IndexingNormalizer class.
 * <p>
 * @author Spence Koehler
 */
public class TestIndexingNormalizer extends TestCase {

  public TestIndexingNormalizer(String name) {
    super(name);
  }
  
  public void doTest(Integer options, String input, String expectedNormalized, String[] expectedTokens) {
    final NormalizedString nstring = IndexingNormalizer.getInstance(options).normalize(input);
    final String[] pieces = nstring.split();

    assertEquals(expectedNormalized, nstring.getNormalized());
    assertEquals(expectedTokens.length, pieces.length);
    for (int i = 0; i < pieces.length; ++i) {
      assertEquals(expectedTokens[i], pieces[i]);
    }
  }

  public void testMixed() {
    doTest(IndexingNormalizer.DEFAULT_NORMALIZATION_OPTIONS,
           "foo-bar baz.com foo@bar.co.uk barBaz -.14159 http://www.foo.com",
           "foo bar baz.com foo@bar.co.uk barbaz -.14159 foo.com/",
           new String[] {
             "foo",
             "bar",
             "baz",
             "com",
             "foo@bar.co.uk",
             "bar",
             "barbaz",
             "baz",
             "14159",
             "foo.com/",
           });
  }

  public void testNumbers() {
    doTest(IndexingNormalizer.DEFAULT_NORMALIZATION_OPTIONS, "-3.14159", "-3.14159", new String[]{"3.14159"});
    doTest(IndexingNormalizer.DEFAULT_NORMALIZATION_OPTIONS, "2.99792458E8", "2.99792458e8", new String[]{"2.99792458e8"});
    doTest(IndexingNormalizer.DEFAULT_NORMALIZATION_OPTIONS, "P747-3", "p747-3", new String[]{"p747-3"});
    doTest(IndexingNormalizer.DEFAULT_NORMALIZATION_OPTIONS, "H2O", "h2o", new String[]{"h", "2", "o"});
  }

  public void testEmails() {
    doTest(IndexingNormalizer.DEFAULT_NORMALIZATION_OPTIONS, "Spence@SemanticDiscovery.com", "spence@semanticdiscovery.com", new String[]{"spence@semanticdiscovery.com"});
  }

  public void testUrls() {
    doTest(IndexingNormalizer.DEFAULT_NORMALIZATION_OPTIONS,
           "http://www.SemanticDiscovery.com/blogs/MyBlogFeed.xml",
           "semanticdiscovery.com/blogs/MyBlogFeed.xml",
           new String[]{"semanticdiscovery.com/blogs/MyBlogFeed.xml"});
  }

  public void testCamelCasing() {
    doTest(IndexingNormalizer.DEFAULT_NORMALIZATION_OPTIONS, "myBlogFeed 31st", "myblogfeed 31st", new String[]{"my", "myblogfeed", "blog", "feed", "31", "st"});
  }

  public void testDots() {
    doTest(IndexingNormalizer.DEFAULT_NORMALIZATION_OPTIONS, "Ph.D., M.D. a..B B.Y.U. U.S.A", "phd md a .b byu usa", new String[]{"phd", "md", "a", "b", "byu", "usa"});
  }

  public void testProblemCase1() {
    doTest(IndexingNormalizer.DEFAULT_INDEXING_OPTIONS, "URL: http://www.", "url /", new String[]{"url"});
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestIndexingNormalizer.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
