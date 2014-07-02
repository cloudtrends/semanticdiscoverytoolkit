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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * JUnit Tests for the StatsAccumulator class.
 * <p>
 * @author Spence Koehler
 */
public final class TestStatsAccumulator extends TestCase {

  public TestStatsAccumulator(String name) {
    super(name);
  }
  
  public void testBasics1() {
    StatsAccumulator statsAccumulator = new StatsAccumulator();
    doBasicTest1(statsAccumulator, "");

    statsAccumulator.clear("test");
    doBasicTest1(statsAccumulator, "test");

    statsAccumulator = new StatsAccumulator("test2");
    doBasicTest1(statsAccumulator, "test2");
  }

  private void doBasicTest1(StatsAccumulator statsAccumulator, String expectedLabel) {
    for (int i = 0; i < 5; ++i) {
      statsAccumulator.add(2);
    }

    assertEquals(expectedLabel, statsAccumulator.getLabel());
    assertEquals(5, statsAccumulator.getN());
    assertEquals(2.0, statsAccumulator.getMean(), 0.0005);
    assertEquals(0.0, statsAccumulator.getStandardDeviation(), 0.00005);
    assertEquals(2.0, statsAccumulator.getMin(), 0.0005);
    assertEquals(2.0, statsAccumulator.getMax(), 0.0005);
    assertEquals(10.0, statsAccumulator.getSum(), 0.0005);
    assertEquals(20.0, statsAccumulator.getSumOfSquares(), 0.0005);

    // exercise the path to find i.e. NullPointerExceptions
    assertNotNull(statsAccumulator.toString());
  }

  public void testBasics2() {
    StatsAccumulator statsAccumulator = new StatsAccumulator();
    statsAccumulator.add(10);
    statsAccumulator.add(20);

    assertEquals(2, statsAccumulator.getN());
    assertEquals(10.0, statsAccumulator.getMin(), 0.0005);
    assertEquals(20.0, statsAccumulator.getMax(), 0.0005);
    assertEquals(15.0, statsAccumulator.getMean(), 0.0005);
    assertEquals(30.0, statsAccumulator.getSum(), 0.0005);
  }

  public void testReadWrite() throws IOException {
    StatsAccumulator statsAccumulator = new StatsAccumulator("testio");

    for (int i = 0; i < 100; ++i) {
      statsAccumulator.add(i);
    }

    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    statsAccumulator.write(new DataOutputStream(bos));

    StatsAccumulator other = new StatsAccumulator().readFields(new DataInputStream(new ByteArrayInputStream(bos.toByteArray())));

    assertEquals(statsAccumulator.getLabel(), other.getLabel());
    assertEquals(statsAccumulator.getN(), other.getN());
    assertEquals(statsAccumulator.getMean(), other.getMean(), 0.0005);
    assertEquals(statsAccumulator.getStandardDeviation(), other.getStandardDeviation(), 0.00005);
    assertEquals(statsAccumulator.getMin(), other.getMin(), 0.0005);
    assertEquals(statsAccumulator.getMax(), other.getMax(), 0.0005);
    assertEquals(statsAccumulator.getSum(), other.getSum(), 0.0005);
    assertEquals(statsAccumulator.getSumOfSquares(), other.getSumOfSquares(), 0.0005);
  }

  public void testSummarize() {
    StatsAccumulator c1 = new StatsAccumulator();
    StatsAccumulator c2 = new StatsAccumulator();

    for (int i = 0; i < 10; ++i) {
      c1.add(10);
      c2.add(20);
    }

    StatsAccumulator cs = StatsAccumulator.summarize("xxx", new StatsAccumulator[] {c1, c2});

    assertEquals("xxx", cs.getLabel());
    assertEquals(2, cs.getN());
    assertEquals(15.0, cs.getMean(), 0.0005);
  }

  public void testSummarizeWith() {
    StatsAccumulator c1 = new StatsAccumulator();
    StatsAccumulator c2 = new StatsAccumulator();

    for (int i = 0; i < 10; ++i) {
      c1.add(10);
      c2.add(20);
    }

    StatsAccumulator cs = new StatsAccumulator("xxx");
    cs.summarizeWith(c1);
    cs.summarizeWith(c2);

    assertEquals("xxx", cs.getLabel());
    assertEquals(2, cs.getN());
    assertEquals(15.0, cs.getMean(), 0.0005);
  }

  public void testCombine() {
    StatsAccumulator c1 = new StatsAccumulator();
    StatsAccumulator c2 = new StatsAccumulator();

    for (int i = 0; i < 10; ++i) {
      c1.add(10);
      c2.add(20);
    }

    StatsAccumulator cs = StatsAccumulator.combine("yyy", new StatsAccumulator[] {c1, c2});

    assertEquals("yyy", cs.getLabel());
    assertEquals(20, cs.getN());
    assertEquals(15.0, cs.getMean(), 0.0005);
  }

  public void testCombineWith() {
    StatsAccumulator c1 = new StatsAccumulator();
    StatsAccumulator c2 = new StatsAccumulator();

    for (int i = 0; i < 10; ++i) {
      c1.add(10);
      c2.add(20);
    }

    c1.combineWith(c2);

    assertEquals(20, c1.getN());
    assertEquals(15.0, c1.getMean(), 0.0005);
  }

  public void testReconstruct() {
    final StatsAccumulator statsAccumulator1 = new StatsAccumulator("testing");

    for (int i = 0; i < 111; ++i) {
      statsAccumulator1.add(Math.random() * 1023);
    }

    final String label = statsAccumulator1.getLabel();
    final long n = statsAccumulator1.getN();
    final double min = statsAccumulator1.getMin();
    final double max = statsAccumulator1.getMax();
    final double mean = statsAccumulator1.getMean();
    final double stddev = statsAccumulator1.getStandardDeviation();

    final StatsAccumulator statsAccumulator2
      = new StatsAccumulator(label, n, min, max, mean, stddev);

    assertEquals(label, statsAccumulator2.getLabel());
    assertEquals(n, statsAccumulator2.getN());
    assertEquals(min, statsAccumulator2.getMin(), 0.05);
    assertEquals(max, statsAccumulator2.getMax(), 0.05);
    assertEquals(mean, statsAccumulator2.getMean(), 0.05);
    assertEquals(stddev, statsAccumulator2.getStandardDeviation(), 0.05);
  }

  public void testBuildView() {
    final StatsAccumulator[] stats = new StatsAccumulator[] {
      new StatsAccumulator("stats1", 100, 5.0, 35.0, 7.5, 15.0),
      new StatsAccumulator("stats2", 100, 10.0, 100.0, 75.0, 30.0),
      new StatsAccumulator("stats3", 100, 0.0, 100.0, 50.0, 10.0),
      new StatsAccumulator("stats4", 100, 0.0, 100.0, 50.0, 0.0),
    };


    //minValue=0.0 maxValue=100.0
    final String[] expected1 = new String[] {
      "  5.0   [(  7.5        )      ]                                        35.0",
      " 10.0      [                    (                 75.0             )] 100.0",
      "  0.0 [                      (      50.0    )                       ] 100.0",
      "  0.0 [                           ( 50.0)                           ] 100.0",
    };
    for (int i = 0; i < stats.length; ++i) {
      final StatsAccumulator s = stats[i];
      final String expected = expected1[i];

      final String view = s.buildView(75, 1, 0.0, 100.0);
      assertEquals(expected, view);
    }

    //minValue=min maxValue=max
    final String[] expected2 = new String[] {
      " 5.0 [(    7.5                             )                         ] 35.0",
      " 10.0 [                     (                   75.0               )] 100.0",
      "  0.0 [                      (      50.0    )                       ] 100.0",
      "  0.0 [                           ( 50.0)                           ] 100.0",
    };
    for (int i = 0; i < stats.length; ++i) {
      final StatsAccumulator s = stats[i];
      final String expected = expected2[i];

      final String view = s.buildView(75, 1);
      assertEquals(expected, view);
    }
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestStatsAccumulator.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
