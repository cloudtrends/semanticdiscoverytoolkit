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
package org.sd.xml;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.sd.io.FileUtil;
import org.sd.util.tree.Tree;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * JUnit Tests for the XmlLite class.
 * <p>
 * @author Spence Koehler
 */
public class TestXmlLite extends TestCase {

  public TestXmlLite(String name) {
    super(name);
  }
  
  private final String getTreeString(Tree<XmlLite.Data> xmlTree) throws IOException {
    return XmlLite.asXml(xmlTree, false).replaceAll("\n", "");
  }

  private final void validate(XmlLite xmlLite, String input, String output) throws IOException {
    final Tree<XmlLite.Data> xmlTree = xmlLite.parse(input);
    final String xmlString = getTreeString(xmlTree);
    assertEquals(xmlString + " tree=" + xmlTree, output, xmlString);
  }

  public void testBasicReversibilityIgnoreComments() throws IOException {
    final XmlLite xmlLite = XmlFactory.HTML_LITE_IGNORE_COMMENTS;

    validate(xmlLite, "<foo/>", "<foo/>");
    validate(xmlLite, "", "");
    validate(xmlLite, "<foo bar=baz/>", "<foo bar=\"baz\"/>");
    validate(xmlLite, "<foo bar=baz>foobarbaz</foo>", "<foo bar=\"baz\">foobarbaz</foo>");
    validate(xmlLite, "testing123", "<root bogus=\"true\">testing123</root>");
    validate(xmlLite, "<foo><bar>baz</bar></baz>", "<foo><bar>baz</bar><baz/></foo>");
    validate(xmlLite, "<foo><!-- junk! even has < and > in it! and an extra -- to boot! --></foo>", "<foo/>");
    validate(xmlLite, "<foo><!-- junk! even has < and > in it! and an extra -- to boot! --><bar>baz</bar></foo>", "<foo><bar>baz</bar></foo>");

    validate(xmlLite, "<html><body>testing<p>this<br>stuff</body></html>", "<html><body>testing<p>this<br/>stuff</p></body></html>");
    validate(xmlLite, "<html><body>testing<p/>this<br/>stuff</body></html>", "<html><body>testing<p/>this<br/>stuff</body></html>");
    validate(xmlLite, "<html><body>testing<p>this</p><br>stuff</br></body></html>", "<html><body>testing<p>this</p><br/>stuff<br/></body></html>");

    validate(xmlLite,
             "<foo att1=\"value1a value1b value1c\" att2=\"value2a value2b\"/>",
             "<foo att1=\"value1a value1b value1c\" att2=\"value2a value2b\"/>");

    validate(xmlLite,
             "<foo att1='value1a value1b value1c' att2='value2a value2b'/>",
             "<foo att1=\"value1a value1b value1c\" att2=\"value2a value2b\"/>");

    validate(xmlLite, "<foo att1=\"\" att2=\"bar\" att3=\"\"/>", "<foo att1=\"\" att2=\"bar\" att3=\"\"/>");
    validate(xmlLite, "<foo att1= att2=\"bar\" att3=/>", "<foo att1=\"\" att2=\"bar\" att3=\"\"/>");
  }

  public void testBasicReversibilityKeepComments() throws IOException {
    final XmlLite xmlLite = XmlFactory.XML_LITE_KEEP_COMMENTS;

    validate(xmlLite, "<foo><!-- junk! even has < and > in it! and an extra -- to boot! --></foo>", "<foo><!-- junk! even has < and > in it! and an extra -- to boot! --></foo>");
    validate(xmlLite, "<foo><!-- junk! even has < and > in it! and an extra -- to boot! --><bar>baz</bar></foo>", "<foo><!-- junk! even has < and > in it! and an extra -- to boot! --><bar>baz</bar></foo>");
  }

  public void testAddBogusRoot() throws IOException {
    final XmlLite xmlLite = XmlFactory.XML_LITE_KEEP_COMMENTS;

    validate(xmlLite, "<foo></foo><bar></bar>", "<root bogus=\"true\"><foo/><bar/></root>");
    validate(xmlLite, "<foo/><bar/>", "<root bogus=\"true\"><foo/><bar/></root>");
    validate(xmlLite, "<foo/><bar></bar><baz/>", "<root bogus=\"true\"><foo/><bar/><baz/></root>");
  }

  private final void doFullReversibilityTest(boolean ignoreComments, String resource, String explicitCharset, Encoding encoding, boolean includeXmlHeader) throws IOException {
    final Encoding[] encodingUsed = new Encoding[1];
    
    final InputStream inputStream = FileUtil.getInputStream(this.getClass(), resource);
    final XmlInputStream xmlInputStream = new XmlInputStream(inputStream);

    // this file says its charset is utf-8 but it is really ascii.
    assertEquals(explicitCharset, xmlInputStream.getExplicitCharset());

    final Tree<XmlLite.Data> xmlTree = XmlFactory.readXmlTree(FileUtil.getFile(this.getClass(), resource), ignoreComments, true, encodingUsed, null, true);
    assertNotNull(xmlTree);
    assertEquals(encoding, encodingUsed[0]);
    final String xmlString = XmlLite.asXml(xmlTree, includeXmlHeader);
    assertTrue(xmlString + " tree=" + xmlTree, xmlString.length() > 100);

    final Tree<XmlLite.Data> reTree = XmlFactory.buildXmlTree(xmlString, ignoreComments, true);
    final String reString = XmlLite.asXml(reTree, includeXmlHeader);

    assertEquals("\nxmlString=\n" + xmlString + "\nreString=\n" + reString + "\n", xmlString, reString);
  }

  public void testFullReversibility() throws IOException {
    doFullReversibilityTest(true, "resources/cobra.1144202523.1.1144292807.0.html.gz", "utf-8", Encoding.ASCII, true);
    doFullReversibilityTest(false, "resources/cobra.1144202523.1.1144292807.0.html.gz", "utf-8", Encoding.ASCII, true);
    doFullReversibilityTest(true, "resources/cobra.1144202523.1.1144292807.0.html.gz", "utf-8", Encoding.ASCII, false);
    doFullReversibilityTest(false, "resources/cobra.1144202523.1.1144292807.0.html.gz", "utf-8", Encoding.ASCII, false);

    doFullReversibilityTest(true, "resources/michelob.1144120638.4.1144211026.0.html.gz", null, Encoding.ASCII, true);
    doFullReversibilityTest(false, "resources/michelob.1144120638.4.1144211026.0.html.gz", null, Encoding.ASCII, true);
    doFullReversibilityTest(true, "resources/michelob.1144120638.4.1144211026.0.html.gz", null, Encoding.ASCII, false);
    doFullReversibilityTest(false, "resources/michelob.1144120638.4.1144211026.0.html.gz", null, Encoding.ASCII, false);

    doFullReversibilityTest(true, "resources/miller.1144205896.11.1144296432.0.html.gz", "deutsch", Encoding.ASCII, true);
    doFullReversibilityTest(false, "resources/miller.1144205896.11.1144296432.0.html.gz", "deutsch", Encoding.ASCII, true);
    doFullReversibilityTest(true, "resources/miller.1144205896.11.1144296432.0.html.gz", "deutsch", Encoding.ASCII, false);
    doFullReversibilityTest(false, "resources/miller.1144205896.11.1144296432.0.html.gz", "deutsch", Encoding.ASCII, false);
  }

//todo: add tests for proper normalization/standardization

  public void testBadAttribute() throws IOException {
    final XmlLite xmlLite = XmlFactory.HTML_LITE_KEEP_COMMENTS;

    validate(xmlLite, "<a href=\" />", "<a href=\" /&gt;\"></a>");  // expected was "<a href=\"\"/>"
    validate(xmlLite, "<a href=\" >", "<a href=\" &gt;\"></a>");  // expected was "<a href=\"\"></a>"
  }

  private final void doReadToEndOfScriptTest(String script, String expected) throws IOException {
    final XmlInputStream inputStream = getXmlInputStream(script);
    final StringBuilder builder = new StringBuilder();
    final int pos = XmlTagParser.readToEndOfScript(inputStream, builder, true);
    assertEquals(expected, builder.toString());
  }

  public void testReadToEndOfScript() throws IOException {
    doReadToEndOfScriptTest("</script>", "");
    doReadToEndOfScriptTest("</SCRIPT>", "");
    doReadToEndOfScriptTest("</Script>", "");
    doReadToEndOfScriptTest("<!-- <!-- junk //--></script>", "<!-- <!-- junk //-->");
    doReadToEndOfScriptTest("<!-- <!-- junk <script type=\"embedded\"/>//--></script>", "<!-- <!-- junk <script type=\"embedded\"/>//-->");

//disabled behavior of working over nested script tags. process was confused by embedded script code that generated script tags:
//document.write("<script type=\"text/javascript\" src=\"http://fondsmedia.com/inc/chCounter/additional.php?res_width=" + screen.width + "&res_height=" + screen.height + "&js=true\"></" + "script>");
//couldn't find end.
/*
    doReadToEndOfScriptTest("junk <script type=\"JavaScript\"> more junk </script></script>", "junk <script type=\"JavaScript\"> more junk </script>");
    doReadToEndOfScriptTest("junk <SCRIPT type=\"JavaScript\"> more junk </script></script>", "junk <SCRIPT type=\"JavaScript\"> more junk </script>");
    doReadToEndOfScriptTest("junk <SCRIPT type=\"JavaScript\"> more junk </SCRIPT></script>", "junk <SCRIPT type=\"JavaScript\"> more junk </SCRIPT>");
*/

//todo: we want the following behavior, but haven't implemented it yet.
//    doReadToEndOfScriptTest("junk <SCRIPT type=\"JavaScript\"> more junk </script><body>not junk</body>", "junk <SCRIPT type=\"JavaScript\"> more junk ");
  }

  private final void doIncrementalTest(String xml, String expectedTopTreeString, String[] expectedChildTreeStrings) throws IOException {
    final XmlInputStream inputStream = getXmlInputStream(xml);
    final XmlLite xmlLite = XmlFactory.XML_LITE_IGNORE_COMMENTS;

    final Tree<XmlLite.Data> top = xmlLite.getTop(inputStream);
    assertEquals(expectedTopTreeString, getTreeString(top));

    for (int i = 0; i < expectedChildTreeStrings.length; ++i) {
      final Tree<XmlLite.Data> child = xmlLite.getNextChild(inputStream);
      assertEquals(expectedChildTreeStrings[i], getTreeString(child));
    }
  }

  public void testIncremental1() throws IOException {
    doIncrementalTest("<?xml version=\"1.0\"?><top><alpha><a>a</a><b>b</b><c>c</c></alpha><beta><d>d</d><e>e</e><f>f</f></beta></top>",
                      "<top></top>", new String[]{"<alpha><a>a</a><b>b</b><c>c</c></alpha>",
                                                  "<beta><d>d</d><e>e</e><f>f</f></beta>",
                                                  "<top/>", ""});
  }

  private final XmlInputStream getXmlInputStream(String text) throws IOException {
    return new XmlInputStream(new ByteArrayInputStream(text.getBytes()), Encoding.UTF8);
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestXmlLite.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
