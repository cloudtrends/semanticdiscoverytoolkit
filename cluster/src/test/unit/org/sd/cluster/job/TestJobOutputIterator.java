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

import org.sd.io.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * JUnit Tests for the JobOutputIterator class.
 * <p>
 * @author Spence Koehler
 */
public class TestJobOutputIterator extends TestCase {

  public TestJobOutputIterator(String name) {
    super(name);
  }
  

  public void testIteratingAndDeleting() throws IOException {
    // create
    //
    //  /tmp/TestJobOutputIterator
    //  |-- datadir_01
    //  |   `-- foo-1.txt
    //  |-- datadir_02
    //  |   |-- bar-1.txt
    //  |   |-- node1-0.out
    //  |   |-- node1-1.out
    //  |   |-- node2-0.out
    //  |   `-- node3-0.out
    //  |-- datadir_03
    //  |   `-- baz-1.txt
    //  |-- datadir_04
    //  |   `-- foo-2.txt
    //  |-- datadir_05
    //  |   |-- bar-2.txt
    //  |   |-- node2-1.out
    //  |   `-- node3-1.out
    //  `-- datadir_06
    //      `-- baz-2.txt
    //
    final File jobIdPath = new File("/tmp/TestJobOutputIterator");

    // clean-up
    if (jobIdPath.exists()) {
      FileUtil.deleteDir(jobIdPath);
    }

    final String[] paths = new String[] {
      "/tmp/TestJobOutputIterator/datadir_01/foo-1.txt",
      "/tmp/TestJobOutputIterator/datadir_02/bar-1.txt",
      "/tmp/TestJobOutputIterator/datadir_02/node1-0.out",
      "/tmp/TestJobOutputIterator/datadir_02/node1-1.out",
      "/tmp/TestJobOutputIterator/datadir_02/node2-0.out",
      "/tmp/TestJobOutputIterator/datadir_02/node3-0.out",
      "/tmp/TestJobOutputIterator/datadir_03/baz-1.txt",
      "/tmp/TestJobOutputIterator/datadir_04/foo-2.txt",
      "/tmp/TestJobOutputIterator/datadir_05/bar-2.txt",
      "/tmp/TestJobOutputIterator/datadir_05/node2-1.out",
      "/tmp/TestJobOutputIterator/datadir_05/node3-1.out",
      "/tmp/TestJobOutputIterator/datadir_06/baz-2.txt",
    };
    for (String path : paths) FileUtil.touch(path);


    // expect
    //   node1-0.out, node1-1.out, node2-0.out, node3-0.out, node2-1.out, node3-1.out
    final String[] expected = new String[] {
      "/tmp/TestJobOutputIterator/datadir_02/node1-0.out",
      "/tmp/TestJobOutputIterator/datadir_02/node1-1.out",
      "/tmp/TestJobOutputIterator/datadir_02/node2-0.out",
      "/tmp/TestJobOutputIterator/datadir_02/node3-0.out",
      "/tmp/TestJobOutputIterator/datadir_05/node2-1.out",
      "/tmp/TestJobOutputIterator/datadir_05/node3-1.out",
    };

    int eInd = 0;

    // iterate
    eInd = 0;
    for (Iterator<File> iter = new JobOutputIterator(jobIdPath, "^datadir_\\d+$", true); iter.hasNext(); ++eInd) {
      final File outfile = iter.next();
      final String expect = expected[eInd];
      assertEquals(eInd + ":" + outfile, expect, outfile.getAbsolutePath());
    }
    assertEquals(expected.length, eInd);  // make sure we got all of 'em

    // skip forward
    final JobOutputIterator skipIter = new JobOutputIterator(jobIdPath, "^datadir_\\d+$", true);
    for (int i = expected.length - 1; i >= 0; --i) {
      final File skipToFile = new File(expected[i]);
      assertTrue(skipToFile.toString(), skipIter.setToOutputFile(skipToFile));
      assertEquals(skipToFile, skipIter.next());
    }
    assertFalse(skipIter.setToOutputFile(new File("/tmp/foo.txt")));

    // delete
    eInd = 0;
    for (Iterator<File> iter = new JobOutputIterator(jobIdPath, "^datadir_\\d+$", true); iter.hasNext(); ++eInd) {
      final File outfile = iter.next();
      final String expect = expected[eInd];
      assertEquals(eInd + ":" + outfile, expect, outfile.getAbsolutePath());

      iter.remove();
    }
    assertEquals(expected.length, eInd);  // make sure we got all of 'em

    // verify deleted
    eInd = 0;
    for (Iterator<File> iter = new JobOutputIterator(jobIdPath, "^datadir_\\d+$", true); iter.hasNext(); ++eInd) {
      final File outfile = iter.next();
    }
    assertEquals(0, eInd);  // make sure we got all of 'em


    // clean-up
    FileUtil.deleteDir(jobIdPath);
  }

  public void testMarker() throws IOException{
    // create
    //
    //  /tmp/TestJobOutputIterator
    //  |-- datadir_01
    //  |-- datadir_02
    //  |   |-- node1-0.out
    //  |   |-- node1-1.out
    //  |   |-- node2-0.out
    //  |   `-- node3-0.out
    //  |-- datadir_03
    //  |-- datadir_04
    //  |-- datadir_05
    //  |   |-- node2-1.out
    //  |   `-- node3-1.out
    //  `-- datadir_06
    //
    final File jobIdPath = new File("/tmp/TestJobOutputIterator");

    final String[] paths = new String[] {
      "/tmp/TestJobOutputIterator/datadir_01/foo.txt",
      "/tmp/TestJobOutputIterator/datadir_02/foo.txt",
      "/tmp/TestJobOutputIterator/datadir_02/node1-0.out/foo.txt",
      "/tmp/TestJobOutputIterator/datadir_02/node1-1.out/foo.txt",
      "/tmp/TestJobOutputIterator/datadir_02/node2-0.out/foo.txt",
      "/tmp/TestJobOutputIterator/datadir_02/node3-0.out/foo.txt",
      "/tmp/TestJobOutputIterator/datadir_03/foo.txt",
      "/tmp/TestJobOutputIterator/datadir_04/foo.txt",
      "/tmp/TestJobOutputIterator/datadir_05/foo.txt",
      "/tmp/TestJobOutputIterator/datadir_05/node2-1.out/foo.txt",
      "/tmp/TestJobOutputIterator/datadir_05/node3-1.out/foo.txt",
      "/tmp/TestJobOutputIterator/datadir_06/foo.txt",
      "/tmp/TestJobOutputIterator/datadir_06/node1-0.out/foo.txt",
      "/tmp/TestJobOutputIterator/datadir_06/node1-1.out/foo.txt",
      "/tmp/TestJobOutputIterator/datadir_06/node2-0.out/foo.txt",
      "/tmp/TestJobOutputIterator/datadir_06/node3-0.out/foo.txt",
      "/tmp/TestJobOutputIterator/datadir_06/node4-0.out/foo.txt",
      "/tmp/TestJobOutputIterator/datadir_06/node5-0.out/foo.txt",
      "/tmp/TestJobOutputIterator/datadir_06/node6-0.out/foo.txt",
      "/tmp/TestJobOutputIterator/datadir_06/node7-0.out/foo.txt",
    };
    for (String path : paths) FileUtil.touch(path);

    final String[] expected = new String[] {
      "/tmp/TestJobOutputIterator/datadir_02/node1-0.out",
      "/tmp/TestJobOutputIterator/datadir_02/node1-1.out",
      "/tmp/TestJobOutputIterator/datadir_02/node2-0.out",
      "/tmp/TestJobOutputIterator/datadir_02/node3-0.out",
      "/tmp/TestJobOutputIterator/datadir_05/node2-1.out",
      "/tmp/TestJobOutputIterator/datadir_05/node3-1.out",
      "/tmp/TestJobOutputIterator/datadir_06/node1-0.out",
      "/tmp/TestJobOutputIterator/datadir_06/node1-1.out",
      "/tmp/TestJobOutputIterator/datadir_06/node2-0.out",
      "/tmp/TestJobOutputIterator/datadir_06/node3-0.out",
      "/tmp/TestJobOutputIterator/datadir_06/node4-0.out",
      "/tmp/TestJobOutputIterator/datadir_06/node5-0.out",
      "/tmp/TestJobOutputIterator/datadir_06/node6-0.out",
      "/tmp/TestJobOutputIterator/datadir_06/node7-0.out",
    };

    // iterate
    int dirCount = 0;
    for (Iterator<File> iter = new JobOutputIterator(jobIdPath, "^datadir_\\d+$", null, true, "TestMarker"); iter.hasNext() && dirCount < 3; ++dirCount) {
      iter.next();
    }

    assertEquals(FileUtil.countLines("/tmp/TestJobOutputIterator/TestMarker.marker"),3);

    // test restart and roll forward
    for (Iterator<File> iter = new JobOutputIterator(jobIdPath, "^datadir_\\d+$", null, true, "TestMarker"); iter.hasNext(); ) {
      iter.next();
    }

    // check for backup file and bufferSize
    assertTrue(new File("/tmp/TestJobOutputIterator/TestMarker.marker").exists());
    assertTrue(new File("/tmp/TestJobOutputIterator/TestMarker.marker.bup").exists());
    assertEquals(FileUtil.countLines("/tmp/TestJobOutputIterator/TestMarker.marker"),4);
    assertEquals(FileUtil.countLines("/tmp/TestJobOutputIterator/TestMarker.marker.bup"),10);

    // verify contents
    ArrayList<String> markerLines = FileUtil.readLines("/tmp/TestJobOutputIterator/TestMarker.marker");
    for(int i = 10; i < markerLines.size(); i++){
      String line = markerLines.get(i);
      assertEquals(i + ":" + line, expected[i], line);
    } 

    // test restart with done marker
    Iterator<File> iterDone = new JobOutputIterator(jobIdPath, "^datadir_\\d+$", null, true, "TestMarker");
    assertFalse(iterDone.hasNext());

    // iterate with two seperate markers
    int dirCount1 = 0;
    for (Iterator<File> iter = new JobOutputIterator(jobIdPath, "^datadir_\\d+$", null, true, "TestMarkerJob1"); iter.hasNext() && dirCount1 < 6; ++dirCount1) {
      iter.next();
    }
    int dirCount2 = 0;
    for (Iterator<File> iter = new JobOutputIterator(jobIdPath, "^datadir_\\d+$", null, true, "TestMarkerJob2"); iter.hasNext(); ++dirCount2) {
      iter.next();
    }

    // check for marker file and buffer size
    assertTrue(new File("/tmp/TestJobOutputIterator/TestMarkerJob1.marker").exists());
    assertFalse(new File("/tmp/TestJobOutputIterator/TestMarkerJob1.marker.bup").exists());
    assertEquals(FileUtil.countLines("/tmp/TestJobOutputIterator/TestMarkerJob1.marker"),6);

    assertTrue(new File("/tmp/TestJobOutputIterator/TestMarkerJob2.marker").exists());
    assertTrue(new File("/tmp/TestJobOutputIterator/TestMarkerJob2.marker.bup").exists());
    assertEquals(FileUtil.countLines("/tmp/TestJobOutputIterator/TestMarkerJob2.marker.bup"),10);
    assertEquals(FileUtil.countLines("/tmp/TestJobOutputIterator/TestMarkerJob2.marker"),4);

    // verify contents
    ArrayList<String> markerLinesJob1 = FileUtil.readLines("/tmp/TestJobOutputIterator/TestMarkerJob1.marker");
    for(int i = 0; i < markerLinesJob1.size(); i++){
      String line = markerLinesJob1.get(i);
      assertEquals(i + ":" + line, expected[i], line);
    } 

    ArrayList<String> markerLinesJob2 = FileUtil.readLines("/tmp/TestJobOutputIterator/TestMarkerJob2.marker");
    for(int i = 10; i < markerLinesJob2.size(); i++){
      String line = markerLinesJob2.get(i);
      assertEquals(i + ":" + line, expected[i], line);
    } 

    // clean-up
    FileUtil.deleteDir(jobIdPath);
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestJobOutputIterator.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
