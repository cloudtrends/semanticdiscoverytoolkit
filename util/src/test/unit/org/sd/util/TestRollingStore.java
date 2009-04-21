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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * JUnit Tests for the RollingStore class.
 * <p>
 * @author Spence Koehler
 */
public class TestRollingStore extends TestCase {

  public TestRollingStore(String name) {
    super(name);
  }
  

  public void testRolling() throws IOException {
    final MyRollingStore rollingStore = new MyRollingStore(10);

    int numRollovers = 0;
    for (int index = 0; index < 1000; ++index) {
      final RollingStore.Store beforeStore = rollingStore.getStore();
      final boolean rollover = rollingStore.addElement(index);
      final RollingStore.Store afterStore = rollingStore.getStore();
      if (rollover) {
        ++numRollovers;

        assertFalse(beforeStore == afterStore);
      }
      else {
        assertEquals(beforeStore, afterStore);
      }
    }

    assertEquals(1000 / 10, numRollovers);
  }


  private static final class MyRollingStore extends RollingStore <Integer> {
    private int docLimit;
    private int num;

    MyRollingStore(int docLimit) {
      super(null, false);
      this.docLimit = docLimit;
      this.num = 0;
      super.initialize(null);
    }
    
    protected List<File> nextAvailableFile() {
      final List<File> result = new ArrayList<File>();
      final File nextFile = new File("foo-" + (num++) + ".txt");
      result.add(nextFile);
      return result;
    }

    public File getStoreRoot(){
      return null;
    }

    protected Store<Integer> buildStore(File storeFile) {
      return new MyStore(storeFile, docLimit);
    }

    protected void afterOpenHook(boolean firstTime, List<File> nextAvailable, File justOpenedStore) {
      //nothing to do.
    }
  }

  private static final class MyStore extends RollingStore.CountingStore<Integer> {

    MyStore(File storeDir, int docLimit) {
      super(storeDir, docLimit, 0L);
    }

    public void open() throws IOException {
      //nothing to do.
    }

    protected boolean doAddElement(Integer value) throws IOException {
      return false;
    }

    public void close(boolean closingInThread) throws IOException {
      //nothing to do.
    }
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestRollingStore.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
