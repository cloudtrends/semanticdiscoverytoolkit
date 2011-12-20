/*
    Copyright 2011 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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


import java.util.List;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit Tests for the SampleCollector class.
 * <p>
 * @author Spence Koehler
 */
public class TestSampleCollector extends TestCase {

  public TestSampleCollector(String name) {
    super(name);
  }
  

  public void testConsiderMoreThanMax() {
    final SampleCollector<Integer> collector = new SampleCollector<Integer>(5);
    for (int i = 0; i < 100; ++i) {
      collector.consider(i);
    }
    assertEquals(5, collector.getNumSamples());
    assertEquals(100, collector.getTotalCount());

    final List<Integer> samples = collector.getSamples();
    assertEquals(5, samples.size());
    for (Integer sample : samples) {
      assertTrue(sample < 100 && sample >= 0);
    }
  }

  public void testConsiderLessThanMax() {
    final SampleCollector<Integer> collector = new SampleCollector<Integer>(5);
    for (int i = 0; i < 4; ++i) {
      collector.consider(i);
    }
    assertEquals(4, collector.getNumSamples());
    assertEquals(4, collector.getTotalCount());

    final List<Integer> samples = collector.getSamples();
    assertEquals(4, samples.size());
    for (int i = 0; i < 4; ++i) {
      assertEquals(i, (int)samples.get(i));
    }
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestSampleCollector.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
