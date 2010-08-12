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

/**
 * JUnit Tests for the DetailedUrl class.
 * <p>
 * @author Spence Koehler
 */
public class TestDetailedUrl extends TestCase {

  public TestDetailedUrl(String name) {
    super(name);
  }
  
  public void testCleanupHost() {
    assertEquals("foo.com", DetailedUrl.cleanupHost("foo.com-JUNK"));
    assertEquals("foo-bar.com", DetailedUrl.cleanupHost("foo-bar.com"));
    assertEquals("foo-bar.com", DetailedUrl.cleanupHost("foo-bar.com-"));
    assertEquals("foo-bar.com", DetailedUrl.cleanupHost("foo-bar.com-JUNK"));
  }

  private final void doTest(String input,
                            String expectedNormalized,
                            String expectedProtocol,
                            String expectedHostPrefix,
                            String expectedHost,
                            String expectedPort,
                            String expectedPath,
                            String expectedTarget,
                            String expectedExtension,
                            String expectedQuery,
                            String expectedAnchor) {

    final DetailedUrl dUrl = new DetailedUrl(input);

    assertEquals("bad protocol: " + dUrl.getProtocol(true), expectedProtocol, dUrl.getProtocol(true));
    assertEquals("bad host prefix: " + dUrl.getHostPrefix(true), expectedHostPrefix, dUrl.getHostPrefix(true));
    assertEquals("bad host: " + dUrl.getHost(true, false, false), expectedHost, dUrl.getHost(true, false, false));
    assertEquals("bad port: " + dUrl.getPort(), expectedPort, dUrl.getPort());
    assertEquals("bad path: " + dUrl.getPath(true), expectedPath, dUrl.getPath(true));
    assertEquals("bad target: " + dUrl.getTarget(false), expectedTarget, dUrl.getTarget(false));
    assertEquals("bad extension: " + dUrl.getTargetExtension(true), expectedExtension, dUrl.getTargetExtension(true));
    assertEquals("bad query: " + dUrl.getQuery(), expectedQuery, dUrl.getQuery());
    assertEquals("bad anchor: " + dUrl.getAnchor(), expectedAnchor, dUrl.getAnchor());
    assertEquals("bad normalized url: " + dUrl.getNormalizedUrl(), expectedNormalized, dUrl.getNormalizedUrl());
  }

  public void test1() {
    doTest(
      "HTTPs://Web1.cUSA-HFS.com:8080/hfs/svc/firefighterscu/account/summary.jsp?detect=0&login=true&detectflash=true#anchor1",
      "https://web1.cusa-hfs.com:8080/hfs/svc/firefighterscu/account/summary.jsp?detect=0&login=true&detectflash=true#anchor1",
      "https://",
      "web1.",
      "cusa-hfs.com",
      ":8080",
      "/hfs/svc/firefighterscu/account/",
      "summary",
      ".jsp",
      "?detect=0&login=true&detectflash=true",
      "#anchor1");
  }

  public void test2() {
    doTest(
      "www.google.com",
      "www.google.com",
      "",
      "www.",
      "google.com",
      "",
      "",
      "",
      "",
      "",
      "");
  }

  public void test3() {
    doTest(
      "www.google.com?foo=bar",
      "www.google.com?foo=bar",
      "",
      "www.",
      "google.com",
      "",
      "",
      "",
      "",
      "?foo=bar",
      "");
  }

  public void test4() {
    doTest(
      "FILE:///usr/local/share/data.txt",
      "file:///usr/local/share/data.txt",
      "file://",
      "",
      "",
      "",
      "/usr/local/share/",
      "data",
      ".txt",
      "",
      "");
  }

  public void test5() {
    doTest(
      "semanticdiscovery.com:8081/foo.jsp?bar=baz",
      "semanticdiscovery.com:8081/foo.jsp?bar=baz",
      "",
      "",
      "semanticdiscovery.com",
      ":8081",
      "/",
      "foo",
      ".jsp",
      "?bar=baz",
      "");
  }

  public void test6() {
    doTest(
      "http://www.hammerbill.com/cms/",
      "http://www.hammerbill.com/cms/",
      "http://",
      "www.",
      "hammerbill.com",
      "",
      "/cms/",
      "",
      "",
      "",
      "");
  }

  public void test7() {
    doTest(
      "http://www.rucol.nl?nl/home",
      "http://www.rucol.nl/?nl/home",
      "http://",
      "www.",
      "rucol.nl",
      "",
      "/",
      "",
      "",
      "?nl/home",
      "");
  }

  public void test8() {
    doTest(
      "http://www.wretch.cc/blog/JoanneFan&article_id=2946858#postComments",
      "http://www.wretch.cc/blog/JoanneFan&article_id=2946858#postComments",
      "http://",
      "www.",
      "wretch.cc",
      "",
      "/blog/",
      "JoanneFan",
      "",
      "&article_id=2946858",
      "#postComments");
  }

  public void test9() {
    doTest(
      "http://foo.bar.com/foo-bar",
      "http://foo.bar.com/foo-bar",
      "http://",
      "",
      "foo.bar.com",
      "",
      "/foo-bar",
      "",
      "",
      "",
      "");
  }

  public void test10() {
    doTest(
      "http://web3.foo.com",
      "http://web3.foo.com/",
      "http://",
      "web3.",
      "foo.com",
      "",
      "/",
      "",
      "",
      "",
      "");
      
  }

  public void test11() {
    doTest(
      "../../foo/bar.jpg",
      "../../foo/bar.jpg",
      "",
      "",
      "",
      "",
      "../../foo/",
      "bar",
      ".jpg",
      "",
      "");
      
  }

  public void testFixHref() {
    final DetailedUrl dUrl = new DetailedUrl("http://www.foo.com/bar/baz/index.html");
    final String relativePath = "../foo/zab.jpg";
    assertEquals("http://www.foo.com/bar/foo/zab.jpg", dUrl.fixHref(relativePath));
  }

  public void testFixHrefToRoot() {
    final DetailedUrl dUrl = new DetailedUrl("http://www.foo.com/bar/baz/index.html");
    final String href = "/abc/def/ghi.jpg";
    assertEquals("http://www.foo.com/abc/def/ghi.jpg", dUrl.fixHref(href));
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestDetailedUrl.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
