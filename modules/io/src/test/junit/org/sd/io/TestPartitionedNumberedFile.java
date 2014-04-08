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
 * JUnit Tests for the PartitionedNumberedFile class.
 * <p>
 * @author Spence Koehler
 */
public class TestPartitionedNumberedFile extends TestCase {

  public TestPartitionedNumberedFile(String name) {
    super(name);
  }

  public void testPartitioning() throws IOException {
    final File testDir = new File("/tmp/TestPartitionedNumberedFile");

    final PartitionedNumberedFile nFile = 
      new PartitionedNumberedFile(testDir, "foo-", ".txt");
    
    FileUtil.deleteDir(testDir);  // make sure cleaned up.
    assertNull(nFile.getExistingAndNext());

    assertEquals(-1, nFile.getNumber(new File(testDir, "bar-1.txt")));  // doesn't match pattern.
    assertEquals(0, nFile.getNumber(new File(testDir, "foo-0.txt")));
    assertEquals(565732468, nFile.getNumber(new File(testDir, "foo-565732468.txt")));


    final File partDir1 = new File(testDir, "0");
    final File partDir2 = new File(partDir1, "0");
    final File[] paths = new File[] {
      new File(partDir2, "foo-1023.txt"),
      new File(partDir2, "foo-1024.txt"),
    };

    partDir2.mkdirs();
    List<File> testFiles = nFile.getExistingAndNext();

    assertEquals(1, testFiles.size());
    assertEquals(0, nFile.getNumber(testFiles.get(0)));

    // create the paths.
    for (File path : paths) FileUtil.touch(path);

    testFiles = nFile.getExistingAndNext();
    assertEquals(3, testFiles.size());
    assertEquals("/tmp/TestPartitionedNumberedFile/0/1024/foo-1025.txt", 
                 (testFiles.get(testFiles.size() - 1)).getAbsolutePath());

    FileUtil.deleteDir(testDir);  // clean up
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestPartitionedNumberedFile.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
