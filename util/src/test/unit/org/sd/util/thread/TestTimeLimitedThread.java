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
package org.sd.util.thread;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * JUnit Tests for the TimeLimitedThread class.
 * <p>
 * @author Spence Koehler
 */
public class TestTimeLimitedThread extends TestCase {

  public TestTimeLimitedThread(String name) {
    super(name);
  }
  
  public void testFoo() {
  }

  public void testExceedingTimeLimit() {
    final EternalCounter counter = new EternalCounter();
    final TimeLimitedThread runner = new TimeLimitedThread(counter);
    runner.run(250);  // run for 250 milliseconds

    final int curCount = counter.getCurCount();
    assertTrue(curCount > 0);
    try {
      Thread.sleep(100);   // give counter some time to continue if it is still running
    }
    catch (InterruptedException e) {
    }

    assertEquals(curCount, counter.getCurCount());
  }

  private static final class EternalCounter implements Killable {
    private int curCount;
    private AtomicBoolean die = new AtomicBoolean(false);

    public EternalCounter() {
      this.curCount = 0;
    }

    public void die() {
      die.set(true);
    }

    public void run() {
      while (!die.get()) {
        ++curCount;

        if (Thread.currentThread().isInterrupted()) break;
      }
    }

    public int getCurCount() {
      return curCount;
    }
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestTimeLimitedThread.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
