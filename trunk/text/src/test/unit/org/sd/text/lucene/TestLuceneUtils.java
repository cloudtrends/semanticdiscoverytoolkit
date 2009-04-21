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
package org.sd.text.lucene;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.Query;

/**
 * JUnit Tests for the LuceneUtils class.
 * <p>
 * @author Spence Koehler
 */
public class TestLuceneUtils extends TestCase {

  public TestLuceneUtils(String name) {
    super(name);
  }
  
  public void testBuildQuery1() {
    final String queryString = "+contentHash:19FA61D75522A4669B44E39C1D2E1726C530232130D407F89AFEE0964997F7A73E83BE698B288FEBCF88E3E03C4F0757EA8964E59B63D93708B138CC42A66EB3 +publishDay:20071130";
    final Query query = LuceneUtils.buildQuery(queryString, null, null);
    query.toString(); // throws NullPointerException when poorly formatted.
  }

  public void testSplitSimpleQueries() {
    final String[] simpleQueries = LuceneUtils.splitSimpleQueries("+(+foo:bar +foo:baz) ^(bar:foo) (baz=foo ^baz=bar)");
    assertEquals(3, simpleQueries.length);

    assertEquals("+(+foo:bar +foo:baz)", simpleQueries[0]);
    assertEquals("^(bar:foo)", simpleQueries[1]);
    assertEquals("(baz=foo ^baz=bar)", simpleQueries[2]);
  }

  private final void doFieldIdSplitTest(String queryString, String expectedFieldIdName, Class expectedFieldIdClass, String expectedRemainder) {
    final LuceneUtils.FieldIdSplit fieldIdSplit = new LuceneUtils.FieldIdSplit(queryString);

    assertEquals(expectedFieldIdName, fieldIdSplit.getFieldIdName());
    assertEquals(expectedFieldIdClass, fieldIdSplit.getFieldIdClass());
    assertEquals(expectedRemainder, fieldIdSplit.getRemainder());
  }

  public void testFieldIdSplit() {
    doFieldIdSplitTest("field:value", null, null, "field:value");
    doFieldIdSplitTest("org.sd.text.lucene.LuceneFieldId/field:value", "org.sd.text.lucene.LuceneFieldId", LuceneFieldId.class, "field:value");

    doFieldIdSplitTest("field1:value1 ^field2:value2", null, null, "field1:value1 ^field2:value2");
    doFieldIdSplitTest("org.sd.text.lucene.LuceneFieldId/field1:value1 ^field2:value2", "org.sd.text.lucene.LuceneFieldId", LuceneFieldId.class, "field1:value1 ^field2:value2");
  }

  private final void doOccurSplitTest(String queryString, BooleanClause.Occur expectedOccur, String expectedRemainder) {
    final LuceneUtils.OccurSplit occurSplit = new LuceneUtils.OccurSplit(queryString);

    assertEquals(expectedOccur, occurSplit.getOccur());
    assertEquals(expectedRemainder, occurSplit.getRemainder());
  }

  public void testOccurSplit() {
    doOccurSplitTest("field:value", BooleanClause.Occur.SHOULD, "field:value");
    doOccurSplitTest("+field:value", BooleanClause.Occur.MUST, "field:value");
    doOccurSplitTest("^field:value", BooleanClause.Occur.MUST_NOT, "field:value");

    doOccurSplitTest("(field:value)", BooleanClause.Occur.SHOULD, "field:value");
    doOccurSplitTest("+(field:value)", BooleanClause.Occur.MUST, "field:value");
    doOccurSplitTest("^(field:value)", BooleanClause.Occur.MUST_NOT, "field:value");
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestLuceneUtils.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
