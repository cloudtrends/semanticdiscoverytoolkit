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

import org.sd.io.FileUtil;
import org.sd.util.LineBuilder;
import org.sd.util.MathUtil;
import org.sd.util.StringUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;

/**
 * JUnit Tests for the LuceneFieldId class.
 * <p>
 * @author Spence Koehler
 */
public class TestLuceneFieldId extends TestCase {

  public TestLuceneFieldId(String name) {
    super(name);
  }
  

  public void testIndexingAndRetrieval() throws IOException {
    // initialize and open index
    final File luceneDir = new File("/tmp/TestLuceneFieldId/index");
    if (luceneDir.exists()) FileUtil.deleteDir(luceneDir);  // start from scratch
    final LuceneStore luceneStore = new LuceneStore(luceneDir);
    luceneStore.open();

    // make and index documents
    File recordsFile = FileUtil.getFile(this.getClass(), "resources/testLuceneFieldId-records-1.txt");
    loadAndIndexRecords(recordsFile, luceneStore);

    // close index and open searcher
    luceneStore.close(false);

    // open searcher
    final LuceneSearcher searcher = new LuceneSearcher(luceneDir);
    searcher.open();

    boolean verbose = false;

    //
    // retrieve and verify documents
    //

    // test acronyms
    doSearchTest(XFieldId.TEXT, searcher, "b.y.u", new int[]{0, 1}, verbose);
    doSearchTest(XFieldId.TEXT, searcher, "phd", new int[]{0, 1}, verbose);

    // test camel-casing
    doSearchTest(XFieldId.DOMAIN, searcher, "bar", new int[]{1}, verbose);
    doSearchTest(XFieldId.DOMAIN, searcher, "foobar", new int[]{1}, verbose);
    doSearchTest(XFieldId.DOMAIN, searcher, "FOOBAR", new int[]{1}, verbose);

    // test url as single token
    doSearchTest(XFieldId.TEXT, searcher, "foobar", null, verbose);  // or at least not in {3} if it appears in other texts later
    doSearchTest(XFieldId.TEXT, searcher, "http://www.foobar.com/baz.html", new int[]{3}, true);

    // test url expectations
    doSearchTest(XFieldId.DOMAIN, searcher, "com", null, verbose);
    doSearchTest(XFieldId.URL, searcher, "com", new int[]{0, 1}, verbose);
    doSearchTest(XFieldId.EMAIL, searcher, "foo", new int[]{0, 1}, verbose);
    doSearchTest(XFieldId.EMAIL, searcher, "bar", new int[]{1}, verbose);
    doSearchTest(XFieldId.EMAIL, searcher, "baz", new int[]{0, 1}, verbose);

    // test mixed english and chinese
    doSearchTest(XFieldId.TEXT, searcher, "chinese", new int[]{2}, verbose);
    doSearchTest(XFieldId.TEXT, searcher, "english", new int[]{2}, verbose);
    doSearchTest(XFieldId.TEXT, searcher, StringUtil.buildString(new int[]{0x7368}), new int[]{2}, verbose);
    doSearchTest(XFieldId.TEXT, searcher, StringUtil.buildString(new int[]{0x68EE, 0x6797}), new int[]{2}, verbose);

    // test date searches
    doSearchTest(XFieldId.MONTH_NUMBER, searcher, "2", new int[]{2}, true);

    // close searcher and clean-up
    searcher.close();
    if (luceneDir.exists()) FileUtil.deleteDir(luceneDir);  // delete
  }

  /**
   * NOTE: expectedIds is a subset of hits that must be present, not an exhaustive list!
   */
  private final void doSearchTest(LuceneFieldId fieldId, LuceneSearcher searcher, String text, int[] expectedIds, boolean verbose) throws IOException {
    final List<Record> hits = search(fieldId, searcher, text);
    final Map<Integer, Record> id2rec = new HashMap<Integer, Record>();
    if (hits != null) {
      for (Record hit : hits) {
        id2rec.put(hit.id, hit);
      }
    }

    if (verbose) System.out.println("\n" + fieldId.getLabel() + ".search(" + text + ")[" + hits.size() + " hits]=");

    int index = 0;

    if (hits == null) {
      assertNull(expectedIds);
      if (verbose) System.out.println("No hits.");
    }
    else if (expectedIds != null) {
      for (int expectedId : expectedIds) {
        final Record hit = id2rec.get(expectedId);

        assertNotNull("expected id " + expectedId, hit);

        if (verbose) System.out.println(hit);
      }
    }
  }

  private static final List<Record> search(LuceneFieldId fieldId, LuceneSearcher searcher, String text) throws IOException {
    List<Record> result = null;

    final Query query = fieldId.getQuery(text, null);
    System.out.println("query=" + query.toString());
    final TopDocs topDocs = searcher.search(query);
    if (topDocs != null && topDocs.totalHits > 0) {
      result = new ArrayList<Record>();
      final int numHits = topDocs.totalHits;
      for (int i = 0; i < numHits; ++i) {
        final Document doc = searcher.getDocument(topDocs.scoreDocs[i].doc);
        final float score = topDocs.scoreDocs[i].score;
        result.add(new Record(doc, score));
      }
    }

    return result;
  }

  private static final void loadAndIndexRecords(File recordsFile, LuceneStore luceneStore) throws IOException {
    final BufferedReader reader = FileUtil.getReader(recordsFile);
    String line = null;
    while ((line = reader.readLine()) != null) {
      if ("".equals(line) || line.charAt(0) == '#') continue;

      final Record record = new Record(line);
      final Document doc = record.asLuceneDocument();
      if (doc != null) {
        luceneStore.addDocument(doc);
      }
    }
    reader.close();
  }

  private static final class Record {
    int id;
    String key;
    String text;
    String title;
    long date;       // in millis
    String url;
    String email;

    float score;
    String dateString;

    Record(String line) {
      final String[] pieces = StringUtil.splitFields(line, 7);

      this.id = Integer.parseInt(pieces[0]);
      this.key = pieces[1];
      this.text = pieces[2];
      this.title = pieces[3];
      this.date = Long.parseLong(pieces[4]);
      this.url = pieces[5];
      this.email = pieces[6];
    }

    Record(Document doc, float score) throws IOException {
      this.score = score;

      this.id = Integer.parseInt(doc.get(XFieldId.ID.getLabel()));
      this.key = doc.get(XFieldId.KEY.getLabel());
      this.text = doc.get(XFieldId.TEXT.getLabel());
      this.title = doc.get(XFieldId.TITLE.getLabel());
      this.dateString = doc.get(XFieldId.DATE.getLabel());
      this.url = doc.get(XFieldId.URL.getLabel());
      this.email = doc.get(XFieldId.EMAIL.getLabel());
    }

    public Document asLuceneDocument() {
      boolean result = false;
      final Document document = new Document();

      Date recordDate = new Date(date);
      Calendar calendar = new GregorianCalendar();
      calendar.setTime(recordDate);
      int month = calendar.get(Calendar.MONTH);

      result |= XFieldId.ID.addField(document, id);
      result |= XFieldId.KEY.addField(document, key);
      result |= XFieldId.TEXT.addField(document, text);
      result |= XFieldId.TITLE.addField(document, title);
      result |= XFieldId.DATE.addDayResolutionField(document, recordDate);
      result |= XFieldId.DOMAIN.addDomainField(document, url);
      result |= XFieldId.URL.addUrlField(document, url, false);
      result |= XFieldId.EMAIL.addField(document, email);

      result |= XFieldId.MONTH_NUMBER.addField(document, month);

      return result ? document : null;
    }

    public String toString() {
      final LineBuilder result = new LineBuilder();

      result.
        append(id).
        append(key).
        append(text).
        append(title).
        append(dateString).
        append(url).
        append(email).
        append(MathUtil.doubleString(score, 3));

      return result.toString();
    }
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestLuceneFieldId.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
