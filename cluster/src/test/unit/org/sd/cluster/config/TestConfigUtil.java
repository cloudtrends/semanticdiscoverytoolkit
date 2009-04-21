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
package org.sd.cluster.config;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.File;

import org.sd.io.FileUtil;

/**
 * JUnit Tests for the ConfigUtil class.
 * <p>
 * @author Spence Koehler
 */
public class TestConfigUtil extends TestCase {

  public TestConfigUtil(String name) {
    super(name);
  }
  
  private final void verifyNonEmpty(String string) {
    assertFalse(string == null);
    assertTrue(string.length() > 0);
  }

  public void testGetClusterRootDir() {
    verifyNonEmpty(ConfigUtil.getClusterRootDir());
  }

  public void testGetClusterPath() {
    verifyNonEmpty(ConfigUtil.getClusterPath("data/"));
  }

  public void testGetClusterDevDir() {
    final String devDir = ConfigUtil.getClusterDevDir();
    assertNotNull(devDir);
    assertTrue(devDir, FileUtil.getFile(devDir).exists());
  }

  public void testGetClusterDevConfDir() {
    final String devConfDir = ConfigUtil.getClusterDevConfDir();
    assertNotNull(devConfDir);
    assertTrue(devConfDir, FileUtil.getFile(devConfDir).exists());
  }

  public void testGetClusterDevBinDir() {
    final String devBinDir = ConfigUtil.getClusterDevBinDir();
    assertNotNull(devBinDir);
    assertTrue(devBinDir, FileUtil.getFile(devBinDir).exists());
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestConfigUtil.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
