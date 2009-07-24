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
import org.sd.text.KeyGenerator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * JUnit Tests for the WorkBatch class.
 * <p>
 * @author Spence Koehler
 */
public class TestWorkBatch extends TestCase {

  private static final String TEST_DATA_PATH ="workbatch_testdata";

  public TestWorkBatch(String name) {
    super(name);
  }
  
  private final String getPath(String root) {
    return "/tmp/TestWorkBatch/resources/" + root + "/";
//    return FileUtil.getFilename(this.getClass(), "resources") + "/" + root + "/";
  }

  private final List<String> createTestData(String basePath, int level1size, int level2size) throws IOException {
    final List<String> result = new ArrayList<String>();
    final KeyGenerator keyGenerator = new KeyGenerator();
    
    for (int i = 0; i < level1size; ++i) {
      final String level1dir = basePath + keyGenerator.generateNextKey() + "/";
      for (int j = 0; j < level2size; ++j) {
        final String level2dir = level1dir + keyGenerator.generateNextKey() + "/";
        final String filename = level2dir + keyGenerator.generateNextKey() + ".txt";
        FileUtil.mkdirs(filename);

        final FileOutputStream out = new FileOutputStream(filename);
        out.close();

        result.add(filename);
      }
    }
    return result;
  }

  private boolean unitsHas(Collection<UnitOfWork> units, String string) {
    for (UnitOfWork unit : units) {
      if (string.equals(((StringUnitOfWork)unit).getString())) return true;
    }
    return false;
  }

  private final void verifyBatches(List<String> expected,
                                   Collection<UnitOfWork> units1,
                                   Collection<UnitOfWork> units2,
                                   Collection<UnitOfWork> units3) {
    int index = 0;
    for (String string : expected) {
      final int partition = index % 3;
      final Collection<UnitOfWork> curUnits = partition == 0 ? units1 : partition == 1 ? units2 : units3;
      String unitString = FileUtil.getBasePath(string); // just get the directory
      unitString = unitString.substring(0, unitString.length() - 1);  // remove trailing slash
      final boolean hasUnits = unitsHas(curUnits, unitString);  // find the directory in the work units
      assertTrue("string=" + string + " index=" + index + " partition=" + partition, hasUnits);
      ++index;
    }

    assertEquals(expected.size(), units1.size() + units2.size() + units3.size());
  }

  public void testWorkBatches() throws IOException {
    final String path = getPath(TEST_DATA_PATH);
    final List<String> expected = createTestData(path, 5, 4);

    Collections.sort(expected);

    final DirectoryBatchMaker batchMaker1 = new DirectoryBatchMaker(path, 0, 3);
    final DirectoryBatchMaker batchMaker2 = new DirectoryBatchMaker(path, 1, 3);
    final DirectoryBatchMaker batchMaker3 = new DirectoryBatchMaker(path, 2, 3);

    Collection<UnitOfWork> units1 = batchMaker1.createWorkUnits();
    Collection<UnitOfWork> units2 = batchMaker2.createWorkUnits();
    Collection<UnitOfWork> units3 = batchMaker3.createWorkUnits();

    verifyBatches(expected, units1, units2, units3);

    final String batch1path = path + "batch1.dat";
    final String batch2path = path + "batch2.dat";
    final String batch3path = path + "batch3.dat";

    // make sure batch files don't exist
    new File(batch1path).delete();
    new File(batch2path).delete();
    new File(batch3path).delete();

    WorkBatch workBatch1 = new WorkBatch(batch1path, batchMaker1);
    WorkBatch workBatch2 = new WorkBatch(batch2path, batchMaker2);
    WorkBatch workBatch3 = new WorkBatch(batch3path, batchMaker3);

    units1 = workBatch1.getWorkUnits();
    units2 = workBatch2.getWorkUnits();
    units3 = workBatch3.getWorkUnits();

    verifyBatches(expected, units1, units2, units3);

    // make sure batch files do exist
    workBatch1.save();
    workBatch2.save();
    workBatch3.save();

    workBatch1 = new WorkBatch(batch1path, batchMaker1);
    workBatch2 = new WorkBatch(batch2path, batchMaker2);
    workBatch3 = new WorkBatch(batch3path, batchMaker3);

    units1 = workBatch1.getWorkUnits();
    units2 = workBatch2.getWorkUnits();
    units3 = workBatch3.getWorkUnits();

    verifyBatches(expected, units1, units2, units3);

//    FileUtil.deleteDir(new File(path), true);
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestWorkBatch.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
