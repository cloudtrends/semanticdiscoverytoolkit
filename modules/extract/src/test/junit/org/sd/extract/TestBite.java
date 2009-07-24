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
package org.sd.extract;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.sd.io.FileUtil;
import org.sd.util.tree.Tree;
import org.sd.xml.XmlFactory;
import org.sd.xml.XmlLite;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * JUnit Tests for the Bite class.
 * <p>
 * @author Spence Koehler
 */
public class TestBite extends TestCase {

  public TestBite(String name) {
    super(name);
  }
  
  public void testIsNumberText() {
    assertTrue("1 comment", Bite.isNumberText("1 comment"));
    assertFalse("1. comment", Bite.isNumberText("1. comment"));
  }

  public void testIsTextNumber() {
    assertTrue("comments(0)", Bite.isTextNumber("comments(0)"));
    assertFalse("comments(0).", Bite.isTextNumber("comments(0)."));
  }

  public void testBuildingBitesFromDeserializedTree() throws IOException {
    final BlogSnippetExtractor snippetizer = new BlogSnippetExtractor(2000);

    // the purpose of this test is to make sure that when we serialize the xml
    // for transfer and deserialize it somewhere else, we haven't lost the
    // information for computing the bites. The information would be lost in
    // cases where we can't store the extraction attribute in the xml for
    // some reason. The only reason I can think of for this right now would
    // be if we processed a plain text file (i.e. with no html formatting).

    for (int i = 4; i <= 18; i += 14) {
      final String numberString = (i < 10 ? "0" : "") + i;
      doBuildingBitesFromDeserializedTreeTest(snippetizer, FileUtil.getFile(this.getClass(), "resources/snippet-sample" + numberString + ".html.gz"));
    }
  }

  private final void doBuildingBitesFromDeserializedTreeTest(BlogSnippetExtractor snippetizer, File blogFile) throws IOException {
    // run blogSnippetExtractor over a blog file
    final List<ExtractionSnippet> snippets = snippetizer.getSnippets(blogFile);

    assertNotNull(snippets);

    for (ExtractionSnippet snippet : snippets) {
      for (Post post : snippet.getPosts()) {
        // getIndexableContent from posts
        final String originalIndexableContent = post.getIndexableContent().replaceAll("\\s+", " ");
        // compare to Bite.getIndexableContent( Bite.buildBites(getContentTree serialized, deserialized) )
        final String reserializedIndexableContent = getReserializedIndexableContent(post);

        assertEquals(blogFile.toString(), originalIndexableContent, reserializedIndexableContent);
      }
    }
  }
  
  private final String getReserializedIndexableContent(Post post) throws IOException {
    final Tree<XmlLite.Data> xmlTree = XmlFactory.buildXmlTree(XmlLite.asXml(post.getContentTree(), false), true, true);
    return Bite.getIndexableContent(Bite.buildBites(xmlTree)).replaceAll("\\s+", " ");
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestBite.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
