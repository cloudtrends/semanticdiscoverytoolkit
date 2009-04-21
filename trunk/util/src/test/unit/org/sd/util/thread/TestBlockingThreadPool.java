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
import java.util.concurrent.TimeUnit;
import java.util.Date;

/**
 * JUnit Tests for the BlockingThreadPool class.
 * <p>
 * @author Spence Koehler
 */
public class TestBlockingThreadPool extends TestCase {

  public TestBlockingThreadPool(String name) {
    super(name);
  }

  public void testTransfer(){
    BlockingThreadPool pool = new BlockingThreadPool("TestPool", 1, 10, 1);

    pool.add(new TimeHook("test"), 10, TimeUnit.SECONDS);
    pool.waitForQueueToEmpty(1000, 100);
    
    assertEquals(1, pool.getNumRun());

    pool.stopTransferingWork();
    assertEquals(false, pool.isTransferingWork());
    for(int i = 0; i < 10; i++){
      pool.add(new TimeHook(String.valueOf(i)), 10, TimeUnit.SECONDS);
    }

    try {
      Thread.sleep(100);
    }
    catch(InterruptedException inte){
    }

    assertEquals(1, pool.getNumRun());
    assertEquals(10, pool.getNumQueued());

    assertEquals(false, pool.add(new TimeHook("extra"), 100, TimeUnit.MILLISECONDS));

    pool.resumeTransferingWork();
    assertEquals(true, pool.isTransferingWork());

    pool.waitForQueueToEmpty(1000, 100);

    assertEquals(11, pool.getNumRun());
    assertEquals(0, pool.getNumQueued());
    

    pool.stopTransferingWork();
    assertEquals(false, pool.isTransferingWork());
    for(int i = 0; i < 10; i++){
      pool.add(new TimeHook(String.valueOf(i)), 10, TimeUnit.SECONDS);
    }

    PoolAddThread t = new PoolAddThread(new TimeHook("extra-threaded"), pool, 1000);
    t.start();

    pool.resumeTransferingWork();
    assertEquals(true, pool.isTransferingWork());

    pool.waitForQueueToEmpty(3000, 100);

    assertEquals(true, t.added);
    assertEquals(22, pool.getNumRun());
    assertEquals(0, pool.getNumQueued());

    pool.shutdown(true);
  }

  private static final class PoolAddThread extends Thread {
    TimeHook hook;
    BlockingThreadPool pool;

    long timeout = 1000;
    boolean added = false;

    public PoolAddThread(TimeHook hook, BlockingThreadPool pool, long timeout){
      this.hook = hook;
      this.pool = pool;
    }

    public void run(){
      this.added = pool.add(hook, timeout, TimeUnit.MILLISECONDS);
    }
  }

  private static final class TimeHook implements HookedRunnable {
    private String id;
    private final AtomicBoolean die = new AtomicBoolean(false);

    public TimeHook(String id) {
      this.id = id;
    }

    public void preRunHook(){
      
    }

    public void run(){
      System.err.println("TimeHook-" + id + " current time: " + System.currentTimeMillis());
    }

    public void postRunHook(){

    }

    public void exceptionHook(Throwable t){
      System.err.println(new Date() + ": TimeHook-" + id + " failed unexpectedly!");
      t.printStackTrace(System.err);
    }

    public void die() {
      die.set(true);
    }
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestBlockingThreadPool.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
