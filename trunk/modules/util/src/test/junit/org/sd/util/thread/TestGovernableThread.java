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


import java.util.concurrent.atomic.AtomicBoolean;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit Tests for the GovernableThread class.
 * <p>
 * @author Spence Koehler
 */
public class TestGovernableThread extends TestCase {

  public TestGovernableThread(String name) {
    super(name);
  }
  

  private final void runThrough(boolean verbose, long toBeDone, long toSleep, long checkInterval) {
    final Sleeper sleeper = new Sleeper(toBeDone, toSleep);  // sleep 'toBeDone' times for 'toSleep' millis each

    final GovernableThread thread = new GovernableThread(sleeper);
    final UnitCounter uc = sleeper.getUnitCounter();  // grab a handle to the counter

    if (verbose) System.out.println(uc);
    thread.start();  // start the sleeper
    if (verbose) System.out.println(uc);

    if (checkInterval > 0) {
      while (!uc.hasEnded()) {
        try {
          Thread.sleep(checkInterval);
          if (verbose) System.out.println(uc);
        }
        catch (InterruptedException ignore){}
      }
    }

    if (verbose) System.out.println(uc);
  }

  public void testCompletes() {
    runThrough(false, 20, 5, 0);
  }

  public void testPauseFor() {
    final Sleeper sleeper = new Sleeper(2000, 5);  // sleep 200 times for 5 millis each

    final GovernableThread thread = new GovernableThread(sleeper);
    final UnitCounter uc = sleeper.getUnitCounter();  // grab a handle to the counter

    thread.start();  // start the sleeper
    thread.pauseFor(50, 5);  // pause for 50 millis, checking each 5 millis
    dowait(5);  // wait long enough for sleeper to catch the pause
    final long countDone = uc.doneSoFar();  // sleeper should be paused now
    dowait(10); // sleep long enough for sleeper to proceed if it weren't paused
    assertEquals(countDone, uc.doneSoFar());  // shouldn't have incremented while paused
    dowait(100);  // sleep beyond the pause time so sleeper will have proceeded
    assertTrue(countDone < uc.doneSoFar());  // should have incremented now
    
    thread.kill(true, 0, false);  // no need to finish sleeping now.
  }

  private final void dowait(long millis) {
    try {
      Thread.sleep(millis);
    }
    catch (InterruptedException ignore) {}
  }

  private final GovernableThread doKillTest(String id, boolean nice, long waitMillis, boolean interrupt) {
    final Sleeper sleeper = new Sleeper(200, 5);  // sleep 200 times for 5 millis each

    final GovernableThread thread = new GovernableThread(sleeper);
    final UnitCounter uc = sleeper.getUnitCounter();  // grab a handle to the counter

    final long starttime = System.currentTimeMillis();
    thread.start();  // start the sleeper
    if (!thread.kill(nice, waitMillis, interrupt)) {  // interrupt
      try {
        thread.join();
      }
      catch (InterruptedException ignore) {System.err.println(id + " interrupted!");}
    }
    else {
//      System.err.println(id + " killed!");
    }
    final long endtime = System.currentTimeMillis();
    
    final long countDone = uc.doneSoFar();
    dowait(10); // sleep long enough for sleeper to proceed if it is still alive

    assertTrue(uc.hasEnded());                // counter should be marked as ended
    assertFalse(thread.isAlive());            // thread should be dead
    assertEquals(countDone, uc.doneSoFar());  // shouldn't have incremented
    assertTrue(endtime - starttime < 1000);   // prove that thread didn't end naturally

    return thread;
  }

  public void testInterruptNoWait() {
    doKillTest("testInterruptNoWait", false, 0, true);
  }

  public void testInterruptWithWait() {
    doKillTest("testInterruptWithWait", false, 5, true);
  }

  public void testNiceKillNoWait() {
    doKillTest("testNiceKillNoWait", true, 0, false);
  }

  public void testNiceKillWithWait() {
    doKillTest("testNiceKillWithWait", true, 5, false);
  }

  public void testNiceKillWithInterruptNoWait() {
    doKillTest("testNiceKillWithInterruptNoWait", true, 0, true);
  }

  public void testNiceKillWithInterruptAndWait() {
    doKillTest("testNiceKillWithInterruptAndWait", true, 5, true);
  }


  private final GovernableThread doRunTest(String id, long duration, long waitMillis, boolean interrupt,
                                           GovernableThread.RunResult expectedRunResult) {
    final GovernableThread thread = GovernableThread.newGovernableThread(new Sleeper(20, 5), true);  // 100 millis

    final long starttime = System.currentTimeMillis();
    final GovernableThread.RunResult runResult = thread.runFor(duration, waitMillis, interrupt);
    final long endtime = System.currentTimeMillis();

    assertEquals(id, expectedRunResult, runResult);
    assertTrue((endtime - starttime) <= (duration + waitMillis + 2/*fluff*/));

    return thread;
  }

  public void testRunToCompletion() {
    // 100 millis should complete 
    doRunTest("testRunToCompletion", 200, 5, false,
              GovernableThread.RunResult.COMPLETED);  // without interrupt
    doRunTest("testRunToCompletion", 200, 5, true,
              GovernableThread.RunResult.COMPLETED);  // with interrupt
  }

  public void testRunPartial() {
    // 100 millis should not complete 
    doRunTest("testRunToCompletion", 10, 10, false,
              GovernableThread.RunResult.PARTIAL);  // without interrupt
    doRunTest("testRunToCompletion", 10, 10, true,
              GovernableThread.RunResult.PARTIAL);  // with interrupt
  }


  public void testUnknownLimit1() {
    final Sleeper sleeper = new Sleeper(-1, 5);  // sleep 'toBeDone' times for 'toSleep' millis each

    final GovernableThread thread = new GovernableThread(sleeper);
    final UnitCounter uc = sleeper.getUnitCounter();  // grab a handle to the counter
    final boolean verbose = false;

    if (verbose) System.out.println(uc);
    thread.start();  // start the sleeper
    if (verbose) System.out.println(uc);

    final long startTime = System.currentTimeMillis();
    while (!uc.hasEnded()) {
      try {
        Thread.sleep(10);
        if (verbose) System.out.println(uc);
        if ((System.currentTimeMillis() - startTime) > 50) {
          uc.die().set(true);
        }
      }
      catch (InterruptedException ignore){}
    }

    if (verbose) System.out.println(uc);
  }

  private final GovernableThread doRunCountOrTimeLimitTest(long numTimes, final long toSleep, long duration,
                                                           long waitMillis, boolean interrupt) {
    final GovernableThread thread = 
      GovernableThread.newGovernableThread(new BaseGovernable(numTimes) {
          protected boolean doOperation(long workUnit, AtomicBoolean die) {
            dowait(toSleep);
            return true;
          }
        }, true);
    thread.runFor(duration, waitMillis, interrupt);
    return thread;
  }

  public void testRunCountOrTimeLimitHittingCount() {
    // go 10 times, sleeping for 1 ms, killing if longer than 100ms checked every 5ms
    final GovernableThread thread = doRunCountOrTimeLimitTest(10, 1, 100, 5, false);
    final UnitCounter uc = thread.getUnitCounter();
    assertTrue(uc.hasEnded());
    assertEquals(10, uc.doneSoFar());
  }

  public void testRunCountOrTimeLimitHittingTime() {
    // go 10 times, sleeping for 1 ms, killing if longer than 5ms checked every 1ms
    final GovernableThread thread = doRunCountOrTimeLimitTest(10, 1, 5, 1, false);
    final UnitCounter uc = thread.getUnitCounter();
    assertTrue(uc.hasEnded());
    assertTrue(10 > uc.doneSoFar());
  }


  private static final class Sleeper extends BaseGovernable {
    private long toSleep;
    private boolean wasInterrupted;
    private long opCount;
    private boolean wasQuit;

    public Sleeper(long toBeDone, long toSleep) {
      super(toBeDone);
      this.toSleep = toSleep;
      this.wasInterrupted = false;
      this.opCount = 0L;
      this.wasQuit = false;
    }

    protected boolean doOperation(long workUnit, AtomicBoolean die) {
      try {
        Thread.sleep(toSleep);
        ++opCount;
      }
      catch (InterruptedException e) {
        wasInterrupted = true;
      }
      return true;
    }

    protected void quit(long workUnit, boolean operated) {
      this.wasQuit = true;
    }

    public boolean wasQuit() {
      return wasQuit;
    }

    public boolean wasInterrupted() {
      return wasInterrupted;
    }

    public long getOpCount() {
      return opCount;
    }
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestGovernableThread.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
