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

import java.util.List;
import java.util.Map;

/**
 * JUnit Tests for the FrequencyMap class.
 * <p>
 * @author Spence Koehler
 */
public class TestFrequencyMap extends TestCase {

  public TestFrequencyMap(String name) {
    super(name);
  }
  

  public void testSimple() {
    final FrequencyMap<String, String> fmap = new FrequencyMap<String, String>();

    final String[] keys = new String[] {"foo", "bar", "baz"};

    // put 1 key-a, 2 key-b, 3 key-c values for each key
    for (String key : keys) {
      for (int i = 0; i < 3; ++i) {
        for (int j = 0; j < i + 1; ++j) {
          final StringBuilder builder = new StringBuilder();
          builder.append(key).append('-').append((char)('a' + (2 - j)));
          fmap.put(key, builder.toString());
        }
      }
    }

    for (String key : keys) {
      assertEquals(key + "-c", fmap.getMostFrequentValue(key));

      final List<FrequencyMap.FValue<String>> fvals = fmap.getValues(key);

      assertEquals(3, fvals.size());

      assertEquals(3, fvals.get(0).getFreq());
      assertEquals(key + "-c", fvals.get(0).getValue());

      assertEquals(2, fvals.get(1).getFreq());
      assertEquals(key + "-b", fvals.get(1).getValue());

      assertEquals(1, fvals.get(2).getFreq());
      assertEquals(key + "-a", fvals.get(2).getValue());
    }

    final Map<String, String> trimmed = fmap.getMostFrequentMappings();
    assertEquals(3, trimmed.size());

    for (String key : keys) {
      assertEquals(key + "-c", trimmed.get(key));
    }
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestFrequencyMap.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
