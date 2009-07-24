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
package org.sd.text;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.sd.util.tree.Tree;
import java.util.List;

/**
 * JUnit Tests for the MungedWordFiner class.
 * <p>
 * @author Spence Koehler
 */
public class TestMungedWordFinder extends TestCase {

  public TestMungedWordFinder(String name) {
    super(name);
  }
  
  public void test1() {
    final MungedWordFinder finder = new MungedWordFinder();
    finder.addWordSet("fbb", new String[]{"foo", "bar", "baz"});

    List<MungedWordFinder.WordSequence> splits = null;
    String splitsString = null;

    splits = finder.getBestSplits("foo");
    splitsString = finder.getSplitsAsString(splits);
    assertEquals("fbb=foo", splitsString);

    splits = finder.getBestSplits("foobar");
    splitsString = finder.getSplitsAsString(splits);
    assertEquals("fbb=foo,fbb=bar", splitsString);

    splits = finder.getBestSplits("f");
    splitsString = finder.getSplitsAsString(splits);
    assertEquals("U=f", splitsString);

    splits = finder.getBestSplits("foozbar");
    splitsString = finder.getSplitsAsString(splits);
    assertEquals("fbb=foo,U=z,fbb=bar", splitsString);

    splits = finder.getBestSplits("afoobbarcbazd");
    splitsString = finder.getSplitsAsString(splits);
    assertEquals("U=a,fbb=foo,U=b,fbb=bar,U=c,fbb=baz,U=d", splitsString);

    splits = finder.getBestSplits("10foo20");
    splitsString = finder.getSplitsAsString(splits);
    assertEquals("N=10,fbb=foo,N=20", splitsString);

    splits = finder.getBestSplits("1foobar");
    splitsString = finder.getSplitsAsString(splits);
    assertEquals("N=1,fbb=foo,fbb=bar", splitsString);
  }

  public void test2() {
    final MungedWordFinder finder = new MungedWordFinder();
    finder.addWordSet("fbb", new String[]{"choice", "mover", "move", "s", "sw", "world", "or", "def", "wi", "ld", "de", "id"});

    final List<MungedWordFinder.WordSequence> splits = finder.getBestSplits("1stchoicemoversworldwide");
    final String splitsString = finder.getSplitsAsString(splits);
    assertEquals("N=1st,fbb=choice,fbb=mover,fbb=s,fbb=world,fbb=wi,fbb=de",
                 splitsString);
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestMungedWordFinder.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
