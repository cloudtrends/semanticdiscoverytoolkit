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

/**
 * JUnit Tests for the FileUtil class.
 * <p>
 * @author Spence Koehler
 */
public class TestFileUtil extends TestCase {

  public TestFileUtil(String name) {
    super(name);
  }
  
  private final void doLastLineTest(String resource, String expectedLastLine) {
    final String filename = FileUtil.getFilename(this.getClass(), "resources/" + resource);
    final String lastLine = FileUtil.getLastLine(filename);

    assertEquals(expectedLastLine, lastLine);
  }

  public void testGetLastLine() {
    doLastLineTest("last-line-test1.txt", "this is the last line");
    doLastLineTest("last-line-test2.txt", "this is the last line");
    doLastLineTest("last-line-test3.txt", "this is the last line");
  }

  public void testGetLastLine2() {
    final String filename = FileUtil.getFilename(this.getClass(), "resources/last-line-test4.txt");
    final String lastLine = FileUtil.getLastLine(filename);

    assertTrue(lastLine.startsWith("FileResponse"));
  }

  public void testCopyTextFile() throws IOException {
    //copy a text file
    final String src = FileUtil.getFilename(this.getClass(), "resources/copy_file_src.txt");
    final String preFileData = FileUtil.readAsString(src);
    final File destFile = File.createTempFile("testCopyTextFile", ".copy.txt");
    final boolean copied = FileUtil.copyFile(FileUtil.getFile(src), destFile);

    assertTrue(copied);

    final String postFileData = FileUtil.readAsString(destFile);
    assertEquals(preFileData, postFileData);

    destFile.delete();
  }

  public void testCopyBinaryFile() throws IOException {
    //copy a binary (gz) file
    final String src = FileUtil.getFilename(this.getClass(), "resources/copy_file_src.txt.gz");
    final String preFileData = FileUtil.readAsString(src);
    final File destFile = File.createTempFile("testCopyBinaryFile", ".copy.txt.gz");
    final boolean copied = FileUtil.copyFile(FileUtil.getFile(src), destFile);

    assertTrue(copied);

    final String postFileData = FileUtil.readAsString(destFile);
    assertEquals(preFileData, postFileData);

    destFile.delete();
  }

  public void testCopyGzippedFileSquashCR() throws IOException {
    //copy a binary (gz) file
    final String src = FileUtil.getFilename(this.getClass(), "resources/copy_cr_file_src.txt.gz");
    final String expectedDest = FileUtil.getFilename(this.getClass(), "resources/copy_cr_file_dest.txt.gz");
    final String preFileData = FileUtil.readAsString(expectedDest);
    final File destFile = File.createTempFile("testCopyGzippedFileSquashCR", ".copy.txt.gz");
    final boolean copied = FileUtil.copyFileNoCR(FileUtil.getFile(src), destFile);

    assertTrue(copied);

    final String postFileData = FileUtil.readAsString(destFile);
    assertEquals(preFileData, postFileData);

    destFile.delete();
  }

  public void testGetFreeDiskSpace() {
    final File userHome = new File(System.getProperty("user.home"));
    final long freeDiskSpace = FileUtil.getFreeDiskSpace(userHome);
    System.out.println("freeDiskSpace(" + userHome + ")=" + freeDiskSpace);
    assertTrue(freeDiskSpace >= 0L);
  }

  public void testGetBaseName() {
    final String filename = "foo.txt";

    assertEquals("foo", FileUtil.getBaseName(filename, null));
    assertEquals("foo", FileUtil.getBaseName(filename, ".txt"));
    assertEquals("foo.txt", FileUtil.getBaseName(filename, ".csv"));
    assertEquals("foo", FileUtil.getBaseName(filename, ".txt"));
    assertEquals("foo", FileUtil.getBaseName(filename, null));
  }

  public void testGetBasePath() {
    assertEquals("/foo/bar/bash/", 
                 FileUtil.getBasePath("/foo/bar/bash/"));
    assertEquals("/foo/bar/", 
                 FileUtil.getBasePath("/foo/bar/bash"));
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestFileUtil.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
