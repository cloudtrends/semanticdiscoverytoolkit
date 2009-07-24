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
package org.sd.cio;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.sd.io.FileUtil;
import org.sd.util.NameGenerator;

import java.io.File;
import java.io.IOException;

/**
 * JUnit Tests for the FilenameGenerator class.
 * <p>
 * @author Spence Koehler
 */
public class TestFilenameGenerator extends TestCase {

  private static final boolean VERBOSE = false;


  public TestFilenameGenerator(String name) {
    super(name);
  }
  

  public void testIncrementing() throws IOException {
    final File baseDir = new File("/tmp/TestFilenameGenerator");

    // clean up (in case of prior failure)
    if (baseDir.exists()) FileUtil.deleteDirContents(baseDir);
    else baseDir.mkdirs();

    final NameGenerator dirNameGen = new NumericFilenameGenerator("", 1, 5);
    final NameGenerator fileNameGen = new NumericFilenameGenerator(".tmp", 1, 7);

    final FilenameGenerator dirGen = new FilenameGenerator(baseDir, dirNameGen, 5);
    String subdir = dirGen.getNextLocalName(FilenameGenerator.CreateFlag.DIR);

    int dircount = 0;

    while (subdir != null) {
      if (dircount >= 5) fail("dirGen didn't stop at limit!");

      final File curdir = new File(baseDir, subdir);
      if (VERBOSE) {
        System.out.println("curdir=" + curdir + " dircount=" + dircount);
      }

      ++dircount;

      int filecount = 0;
      FilenameGenerator fileGen = new FilenameGenerator(curdir, fileNameGen, 5);
      String filename = fileGen.getNextLocalName(FilenameGenerator.CreateFlag.FILE);

      while (filename != null) {
        if (filecount >= 5) fail("fileGen didn't stop at limit!");

        final File curfile = new File(curdir, filename);
        if (VERBOSE) {
          System.out.println("\tcurfile=" + curfile + " filecount=" + filecount);
        }

        ++filecount;
        filename = fileGen.getNextLocalName(FilenameGenerator.CreateFlag.FILE);
      }

      subdir = dirGen.getNextLocalName(FilenameGenerator.CreateFlag.DIR);
    }

    // clean up
    FileUtil.deleteDir(baseDir);
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestFilenameGenerator.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
