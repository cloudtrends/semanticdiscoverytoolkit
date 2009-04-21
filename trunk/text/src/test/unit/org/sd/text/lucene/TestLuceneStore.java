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

import java.io.IOException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;

/**
 * JUnit Tests for the LuceneUtils class.
 * <p>
 * @author Spence Koehler
 */
public class TestLuceneStore extends TestCase {

  public TestLuceneStore(String name) {
    super(name);
  }
  
  public void testDedupe() throws IOException{
    LuceneStore store = new LuceneStore("/tmp/testDedupe", "key");
    store.open();
    Document doc1 = new Document();
    doc1.add(new Field("key", "1234", Field.Store.YES, Field.Index.NOT_ANALYZED));
    doc1.add(new Field("contents", "foo", Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
    Document doc2 = new Document();
    doc2.add(new Field("key", "1235", Field.Store.YES, Field.Index.NOT_ANALYZED));
    doc2.add(new Field("contents", "bar", Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
    Document doc3 = new Document();
    doc3.add(new Field("key", "1236", Field.Store.YES, Field.Index.NOT_ANALYZED));
    doc3.add(new Field("contents", "baz", Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
    store.addDocument(doc1);
    store.addDocument(doc2);
    store.addDocument(doc3);
    store.close();

    LuceneSearcher searcher = new LuceneSearcher("/tmp/testDedupe");
    searcher.open();
    TopDocs topDocs = searcher.search(new TermQuery(new Term("key", "1234")));
    assertEquals(topDocs.totalHits, 1);
    assertEquals(searcher.getDocument(topDocs.scoreDocs[0].doc).get("contents"), "foo");
    searcher.close();

    store.open();
    Document doc4 = new Document();
    doc4.add(new Field("key", "1234", Field.Store.YES, Field.Index.NOT_ANALYZED));
    doc4.add(new Field("contents", "bash", Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
    store.addDocument(doc4);
    store.close();
    
    searcher.open();
    topDocs = searcher.search(new TermQuery(new Term("key", "1234")));
    assertEquals(topDocs.totalHits, 1);
    assertEquals(searcher.getDocument(topDocs.scoreDocs[0].doc).get("contents"), "bash");
    searcher.close();
  }

  public void testRAMBufferSize() throws IOException{
    LuceneStore store = new LuceneStore("/tmp/testRAMBufferSize/");

    // should return the value from the LuceneStore, not the IndexWriter 
    store.setBufferSize(268435456);
    assertEquals(store.getBufferSize(), 268435456);
    store.close();

    LuceneStore store2 = new LuceneStore("/tmp/testRAMBufferSize/");

    store2.open();
    assertEquals(store2.getBufferSize(), 16777216);
    store2.close();

    store2.setBufferSize(104857600);
    store2.open();
    assertEquals(store2.getBufferSize(), 104857600);
    store2.close();
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestLuceneStore.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
