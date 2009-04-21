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
package org.sd.io;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * JUnit Tests for the NumberedFile class.
 * <p>
 * @author Spence Koehler
 */
public class TestNumberedFile extends TestCase {

  public TestNumberedFile(String name) {
    super(name);
  }
  
  public void testBasics() throws IOException {
    final File testDir = new File("/tmp/TestNumberedFile");

    final NumberedFile nFile = new NumberedFile(testDir, "foo-", ".txt");
    
    FileUtil.deleteDir(testDir);  // make sure cleaned up.
    assertNull(nFile.getExistingAndNext());

    assertEquals(-1, nFile.getNumber(new File(testDir, "bar-1.txt")));  // doesn't match pattern.
    assertEquals(0, nFile.getNumber(new File(testDir, "foo-0.txt")));
    assertEquals(123, nFile.getNumber(new File(testDir, "foo-123.txt")));

    final File[] paths = new File[] {
      new File(testDir, "foo-2.txt"),
      new File(testDir, "foo-4.txt"),
    };

    testDir.mkdirs();
    List<File> testFiles = nFile.getExistingAndNext();

    assertEquals(1, testFiles.size());
    assertEquals(0, nFile.getNumber(testFiles.get(0)));

    // create the paths.
    for (File path : paths) FileUtil.touch(path);

    testFiles = nFile.getExistingAndNext();
    assertEquals(3, testFiles.size());
    assertEquals(5, nFile.getNumber(testFiles.get(testFiles.size() - 1)));

    FileUtil.deleteDir(testDir);  // clean up
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestNumberedFile.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
