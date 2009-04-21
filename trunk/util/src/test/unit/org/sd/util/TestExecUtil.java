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

import java.io.File;

/**
 * JUnit Tests for the ExecUtil class.
 * <p>
 * @author Spence Koehler
 */
public class TestExecUtil extends TestCase {

  public TestExecUtil(String name) {
    super(name);
  }
  
  private final void verifyNonEmpty(String string) {
    assertFalse(string == null);
    assertTrue(string.length() > 0);
  }

  public void testGetUser() {
    verifyNonEmpty(ExecUtil.getUser());
  }

  public void testGetUserHome() {
    final String userHome = ExecUtil.getUserHome();
    verifyNonEmpty(userHome);
    assertTrue(new File(userHome).exists());
  }

  public void testGetMachineName() {
    verifyNonEmpty(ExecUtil.getMachineName());
  }

  public void testGetProcessId() {
    assertFalse(0 == ExecUtil.getProcessId());
  }

  public void testIsUp() {
    assertTrue(ExecUtil.isUp(ExecUtil.getProcessId()));
    assertFalse(ExecUtil.isUp(-1000));
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestExecUtil.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
