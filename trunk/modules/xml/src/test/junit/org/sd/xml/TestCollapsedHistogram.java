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
package org.sd.xml;


import java.util.HashSet;
import java.util.Set;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit Tests for the CollapsedHistogram class.
 * <p>
 * @author Spence Koehler
 */
public class TestCollapsedHistogram extends TestCase {

  public TestCollapsedHistogram(String name) {
    super(name);
  }
  

  public void testCollectSamples() {
    final CollapsedHistogram<Integer> chisto = new CollapsedHistogram<Integer>(5);

    chisto.add(1, 1000, 1000);  // 1 bucket with 1000 instances of key=1000
    chisto.add(1, 100, 100);    // 1 bucket 100 instances of key=100

    // collapsed element of 10 buckets with 1 instance each of keys 0 through 9
    for (int i = 0; i < 10; ++i) {
      chisto.add(10, i, 1);
    }

    // make sure we collected 5 samples
    assertEquals(5, chisto.getRankFrequency(2).getElement().getSampleCollector().getNumSamples());

    // make sure the samples are all different
    final Set<Integer> sampleValues = new HashSet<Integer>();
    for (Integer sampleValue : chisto.getRankFrequency(2).getElement().getSampleCollector().getSamples()) {
      sampleValues.add(sampleValue);
    }
    assertEquals(5, sampleValues.size());
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestCollapsedHistogram.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
