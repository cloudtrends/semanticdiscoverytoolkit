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
package org.sd.cluster.job;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.sd.cluster.config.ConfigUtil;
import org.sd.io.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

/**
 * JUnit Tests for the JobOutputIterator class.
 * <p>
 * @author Spence Koehler
 */
public class TestAllNodesJobOutputIterator extends TestCase {

  public TestAllNodesJobOutputIterator(String name) {
    super(name);
  }
  

  private final String buildPath(String clusterDir, int jvmNum, String jobId, String dataDirAndFile) {
    final StringBuilder result = new StringBuilder();

    result.append(clusterDir).append("jvm-").append(jvmNum).append("/data/output/").
      append(jobId).append('/').append(dataDirAndFile);

    return result.toString();
  }

  public void testIteratingAndDeleting() throws IOException {
    // create
    //
    //  /home/<USER>/cluster/jvm-0/data/output/AllNodesJobOutputIteratorTest/
    //  |-- datadir_01
    //  |   `-- foo-1.txt
    //  /home/<USER>/cluster/jvm-1/data/output/AllNodesJobOutputIteratorTest/
    //  |-- datadir_02
    //  |   |-- bar-1.txt
    //  |   |-- node1-0.out
    //  |   |-- node1-1.out
    //  |   |-- node2-0.out
    //  |   `-- node3-0.out
    //  /home/<USER>/cluster/jvm-2/data/output/AllNodesJobOutputIteratorTest/
    //  |-- datadir_03
    //  |   `-- baz-1.txt
    //  /home/<USER>/cluster/jvm-3/data/output/AllNodesJobOutputIteratorTest/
    //  |-- datadir_04
    //  |   `-- foo-2.txt
    //  /home/<USER>/cluster/jvm-4/data/output/AllNodesJobOutputIteratorTest/
    //  |-- datadir_05
    //  |   |-- bar-2.txt
    //  |   |-- node2-1.out
    //  |   `-- node3-1.out
    //  /home/<USER>/cluster/jvm-5/data/output/AllNodesJobOutputIteratorTest/
    //  `-- datadir_06
    //      `-- baz-2.txt
    //
    final String jobId = "AllNodesJobOutputIteratorTest";
    final String dataDirPattern = "^datadir_\\d+$";
		final String testDir = "/tmp/testAllNodesJobOutputIterator/";
    final String clusterDir = testDir + "cluster/";
		ConfigUtil.setClusterRootDir(clusterDir);

    final String[] paths = new String[] {
      buildPath(clusterDir, 0, jobId, "datadir_01/foo-1.txt"),
      buildPath(clusterDir, 1, jobId, "datadir_02/bar-1.txt"),
      buildPath(clusterDir, 1, jobId, "datadir_02/node1-0.out"),
      buildPath(clusterDir, 1, jobId, "datadir_02/node1-1.out"),
      buildPath(clusterDir, 1, jobId, "datadir_02/node2-0.out"),
      buildPath(clusterDir, 1, jobId, "datadir_02/node3-0.out"),
      buildPath(clusterDir, 2, jobId, "datadir_03/baz-1.txt"),
      buildPath(clusterDir, 3, jobId, "datadir_04/foo-2.txt"),
      buildPath(clusterDir, 4, jobId, "datadir_05/bar-2.txt"),
      buildPath(clusterDir, 4, jobId, "datadir_05/node2-1.out"),
      buildPath(clusterDir, 4, jobId, "datadir_05/node3-1.out"),
      buildPath(clusterDir, 5, jobId, "datadir_06/baz-2.txt"),
    };
    for (String path : paths) FileUtil.touch(path);


    // expect
    //   node1-0.out, node1-1.out, node2-0.out, node3-0.out, node2-1.out, node3-1.out
    final String[] expected = new String[] {
      buildPath(clusterDir, 1, jobId, "datadir_02/node1-0.out"),
      buildPath(clusterDir, 1, jobId, "datadir_02/node1-1.out"),
      buildPath(clusterDir, 1, jobId, "datadir_02/node2-0.out"),
      buildPath(clusterDir, 1, jobId, "datadir_02/node3-0.out"),
      buildPath(clusterDir, 4, jobId, "datadir_05/node2-1.out"),
      buildPath(clusterDir, 4, jobId, "datadir_05/node3-1.out"),
    };

    int eInd = 0;

    // iterate
    eInd = 0;
    for (Iterator<File> iter = new AllNodesJobOutputIterator(jobId, dataDirPattern, true); iter.hasNext(); ++eInd) {
      final File outfile = iter.next();
      final String expect = expected[eInd];
      assertEquals(eInd + ":" + outfile, expect, outfile.getAbsolutePath());
    }
    assertEquals(expected.length, eInd);  // make sure we got all of 'em

    // skip forward
    final AllNodesJobOutputIterator skipIter = new AllNodesJobOutputIterator(jobId, dataDirPattern, true);
    for (int i = expected.length - 1; i >= 0; --i) {
      final File skipToFile = new File(expected[i]);
      assertTrue(skipToFile.toString(), skipIter.setToOutputFile(skipToFile));
      assertEquals(skipToFile, skipIter.next());
    }
    assertFalse(skipIter.setToOutputFile(new File("/tmp/foo.txt")));

    // delete
    eInd = 0;
    for (Iterator<File> iter = new AllNodesJobOutputIterator(jobId, dataDirPattern, true); iter.hasNext(); ++eInd) {
      final File outfile = iter.next();
      final String expect = expected[eInd];
      assertEquals(eInd + ":" + outfile, expect, outfile.getAbsolutePath());

      iter.remove();
    }
    assertEquals(expected.length, eInd);  // make sure we got all of 'em

    // verify deleted
    eInd = 0;
    for (Iterator<File> iter = new AllNodesJobOutputIterator(jobId, dataDirPattern, true); iter.hasNext(); ++eInd) {
      final File outfile = iter.next();
    }
    assertEquals(0, eInd);  // make sure we got all of 'em


    // clean-up
		FileUtil.deleteDir(new File(testDir));
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestAllNodesJobOutputIterator.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
