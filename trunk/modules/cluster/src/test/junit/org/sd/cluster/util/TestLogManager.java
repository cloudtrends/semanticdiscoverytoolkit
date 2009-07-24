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
package org.sd.cluster.util;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.File;

/**
 * JUnit Tests for the LogManager class.
 * <p>
 * @author Spence Koehler
 */
public class TestLogManager extends TestCase {

  public TestLogManager(String name) {
    super(name);
  }
  

  public void doGetLatestLogsTest(String[] logNames, String[] expectedLatestNames) {
    final File[] logFiles = new File[logNames.length];
    for (int i = 0; i < logNames.length; ++i) {
      logFiles[i] = new File(logNames[i]);
    }
    final LogManager logManager = new LogManager(logFiles);
    final LogManager.LogInfo[] latestLogs = logManager.getLatestLogs();

    assertEquals(expectedLatestNames.length, latestLogs.length);
    for (int i = 0; i < latestLogs.length; ++i) {
      assertEquals(expectedLatestNames[i], latestLogs[i].logFile.getName());
    }
  }

  public void testGetLatestLogs() {
    // with single node
    doGetLatestLogsTest(new String[] {
        "log-2008-11-05-17:27:35-0.err.gz",
        "log-2008-11-05-17:27:35-0.out.gz",
        "log-2008-11-05-17:37:16-0.err",
        "log-2008-11-05-17:37:16-0.out",
      },
      new String[] {
        "log-2008-11-05-17:37:16-0.out",
        "log-2008-11-05-17:37:16-0.err",
      });

    // with multiple nodes
    doGetLatestLogsTest(new String[] {
        "log-2008-10-31-16:03:04-0.err",
        "log-2008-10-31-16:03:04-0.out",
        "log-2008-10-31-16:03:05-1.err",
        "log-2008-10-31-16:03:05-1.out",
        "log-2008-11-05-17:27:40-0.err",
        "log-2008-11-05-17:27:40-0.out",
        "log-2008-11-05-17:27:40-1.err",
        "log-2008-11-05-17:27:40-1.out",
        "log-2008-11-05-17:37:27-0.err",
        "log-2008-11-05-17:37:27-0.out",
        "log-2008-11-05-17:37:28-1.err",
        "log-2008-11-05-17:37:28-1.out",
      },
      new String[] {
        "log-2008-11-05-17:37:28-1.out",
        "log-2008-11-05-17:37:28-1.err",
        "log-2008-11-05-17:37:27-0.out",
        "log-2008-11-05-17:37:27-0.err",
      });
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestLogManager.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
