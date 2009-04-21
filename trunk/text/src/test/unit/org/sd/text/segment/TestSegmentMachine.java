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
package org.sd.text.segment;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit Tests for the SegmentMachine class.
 * <p>
 * @author Spence Koehler
 */
public class TestSegmentMachine extends TestCase {

  public TestSegmentMachine(String name) {
    super(name);
  }
  
  public void testBasics() {
    final SegmentSequence sequence = SegmentFactory.splitMultiAndSingle(null, "a b c, -- foo, bar, baz, -- x, y, z");
    final SegmentMachine machine = new SegmentMachine(sequence);

    machine.forward(1, false);
    assertEquals("a b c", machine.getCurText(true));
    machine.forwardUntil(CommonFeatures.DELIM, false);
    assertEquals(", -- ", machine.getCurText(true));
    machine.forwardThrough(CommonFeatures.MULTI_DELIM, true);
    assertEquals("foo, bar, baz, -- ", machine.getCurText(true));

    machine.forwardThrough(CommonFeatures.MULTI_DELIM, true);  // aren't any more. shouldn't go anywhere
    assertEquals("", machine.getCurText(true));
    machine.forwardToEnd();  //should hit end.
    assertEquals("x, y, z", machine.getCurText(true));

    machine.backward(2, false);
    assertEquals(", z", machine.getCurText(false));
    machine.backwardUntil(CommonFeatures.DELIM, false);  // shouldn't go anywhere, we're already there
    assertEquals(", z", machine.getCurText(false));
    machine.backwardUntil(CommonFeatures.MULTI_DELIM, true);
    assertEquals("x, y, z", machine.getCurText(true));
    machine.backwardThrough(CommonFeatures.MULTI_DELIM, true);
    assertEquals(", -- ", machine.getCurText(false));
    machine.backwardThrough("bogusFeature", true);   // shouldn't go anywhere, 'cause feature won't be found.
    assertEquals(", -- ", machine.getCurText(true));
    machine.backwardToBeginning();
    assertEquals("a b c, -- foo, bar, baz", machine.getCurText(true));
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestSegmentMachine.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
