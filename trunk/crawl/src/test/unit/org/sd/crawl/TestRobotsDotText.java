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
package org.sd.crawl;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * JUnit Tests for the RobotsDotText class.
 * <p>
 * @author Spence Koehler
 */
public class TestRobotsDotText extends TestCase {

  public TestRobotsDotText(String name) {
    super(name);
  }
  

  public void testGetRobotsUrl() {
    assertEquals("http://www.foo.com/robots.txt",
                 RobotsDotText.getRobotsUrl("http://www.foo.com/bar/baz.html").getCleanString());
  }

  public void doDisallowTest(InputStream robotsTextStream, String[] allowUrls, String[] disallowUrls, Long crawlDelay) throws IOException {

    final RobotsDotText robots = new RobotsDotText(robotsTextStream);

    if (allowUrls != null) {
      for (String allowUrl : allowUrls) {
        final UrlData urlData = new UrlData(allowUrl);
        assertNull("should allow: " + allowUrl, robots.allows(urlData));
        if (crawlDelay != null) {
          assertEquals(crawlDelay, urlData.getOverrideCrawlDelay());
        }
      }
    }

    if (disallowUrls != null) {
      for (String disallowUrl : disallowUrls) {
        assertNotNull("shouldn't allow: " + disallowUrl, robots.allows(new UrlData(disallowUrl)));
      }
    }

    robotsTextStream.close();
  }

  private final void doDisallowTest(String string, String[] allowUrls, String[] disallowUrls) throws IOException {
    doDisallowTest(string, allowUrls, disallowUrls, null);
  }

  private final void doDisallowTest(String string, String[] allowUrls, String[] disallowUrls, Long crawlDelay) throws IOException {
    final InputStream robotsTextStream = new ByteArrayInputStream(string.getBytes());
    doDisallowTest(robotsTextStream, allowUrls, disallowUrls, crawlDelay);
    robotsTextStream.close();
  }

  public void testEmpty() throws IOException {
    doDisallowTest("",
                   new String[] {
                     "http://www.foo.com/foo.html",
                     "http://www.foo.com/bar.html",
                     "http://www.foo.com/baz.html",
                   },
                   null);
  }

  public void testAllowAllGeneral() throws IOException {
    doDisallowTest("User-agent: *\nDisallow:\n",
                   new String[] {
                     "http://www.foo.com/foo.html",
                     "http://www.foo.com/bar.html",
                     "http://www.foo.com/baz.html",
                   },
                   null);
  }

  public void testAllowAllOnlyForSD() throws IOException {
    doDisallowTest("User-agent: semanticdiscovery\nDisallow:\n\nUser-agent: *\nDisallow: /\n",
                   new String[] {
                     "http://www.foo.com/foo.html",
                     "http://www.foo.com/bar.html",
                     "http://www.foo.com/baz.html",
                   },
                   null);
  }

  public void testDisallowAllGeneral1() throws IOException {
    doDisallowTest("User-agent: *\nDisallow: /\n",
                   null,
                   new String[] {
                     "http://www.foo.com/foo.html",
                     "http://www.foo.com/bar.html",
                     "http://www.foo.com/baz.html",
                   });
  }

  public void testDisallowAllForSD() throws IOException {
    doDisallowTest("User-agent: Google\nDisallow:\n\nUser-agent: semanticdiscovery\nDisallow: /\n",
                   null,
                   new String[] {
                     "http://www.foo.com/foo.html",
                     "http://www.foo.com/bar.html",
                     "http://www.foo.com/baz.html",
                   });
  }

  public void testDisallowAllForEveryone() throws IOException {
    doDisallowTest("User-agent: *\nDisallow: /\n",
                   null,
                   new String[] {
                     "http://www.foo.com/foo.html",
                     "http://www.foo.com/bar.html",
                     "http://www.foo.com/baz.html",
                   });
  }

  public void testAllowSomeForEveryone() throws IOException {
    doDisallowTest("User-agent: *\nDisallow: /foo.html\nDisallow: /bar.html",
                   new String[] {
                     "http://www.foo.com/baz.html",
                   },
                   new String[] {
                     "http://www.foo.com/foo.html",
                     "http://www.foo.com/bar.html",
                   });
  }

  public void testAllowSomeForSD() throws IOException {
    doDisallowTest("User-agent: *\nDisallow: /foo.html\n\nUser-agent: semanticdiscovery\nDisallow: /bar.html",
                   new String[] {
                     "http://www.foo.com/baz.html",
                   },
                   new String[] {
                     "http://www.foo.com/foo.html",
                     "http://www.foo.com/bar.html",
                   });
  }

  public void testCrawlDelayForSD() throws IOException {
    doDisallowTest("User-agent: semanticdiscovery\nCrawl-delay: 2500",
                   new String[] {
                     "http://www.foo.com/foo.html",
                     "http://www.foo.com/bar.html",
                     "http://www.foo.com/baz.html",
                   },
                   null,
                   2500L);
  }

  public void testCrawlDelayForAll() throws IOException {
    doDisallowTest("User-agent: *\nDisallow:\nCrawl-delay: 2500",
                   new String[] {
                     "http://www.foo.com/foo.html",
                     "http://www.foo.com/bar.html",
                     "http://www.foo.com/baz.html",
                   },
                   null,
                   2500L);
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestRobotsDotText.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
