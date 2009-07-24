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
package org.sd.util;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit Tests for the PathWrapper class.
 * <p>
 * @author Spence Koehler
 */
public class TestPathWrapper extends TestCase {

  public TestPathWrapper(String name) {
    super(name);
  }
  
  public void testNull() {
    final PathWrapper pathWrapper = new PathWrapper(null);
    assertEquals("", pathWrapper.getPath());
  }

  public void testEmpty() {
    final PathWrapper pathWrapper = new PathWrapper("");
    assertEquals("", pathWrapper.getPath());
  }

  public void testRootOnly() {
    final PathWrapper pathWrapper = new PathWrapper("/");
    assertEquals("", pathWrapper.getPath());
  }

  public void testCombineRedirectingToRoot() {
    final PathWrapper pathWrapper = new PathWrapper("/bar/baz/");
    final String href = "/abc/def";
    assertEquals("/abc/def", pathWrapper.getCombined(href));
  }

  public void testInternalDereferencing() {
    final PathWrapper pathWrapper = new PathWrapper("/foo/./bar/baz/../../abc");
    assertEquals("/foo/abc", pathWrapper.getPath());

    final String href = "../../def/./ghi";
    assertEquals("/def/ghi", pathWrapper.getCombined(href));
  }

  public void testIgnoreInternalRedirectsToRoot() {
    final PathWrapper pathWrapper = new PathWrapper("/foo/./bar//baz/../../abc");
    assertEquals("/foo/abc", pathWrapper.getPath());

    final String href = "../../def//./ghi";
    assertEquals("/def/ghi", pathWrapper.getCombined(href));
  }

  public void testUrls() {
    final PathWrapper pathWrapper = new PathWrapper("http://www.semanticdiscovery.com/semantic-discovery/../blog-monitoring/../seo-tools/../blog-monitoring/../styles_menus/ddsmoothmenu.css");
    assertEquals("http://www.semanticdiscovery.com/styles_menus/ddsmoothmenu.css",
                 pathWrapper.getPath());
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestPathWrapper.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
