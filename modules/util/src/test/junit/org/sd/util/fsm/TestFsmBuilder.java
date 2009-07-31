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
package org.sd.util.fsm;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.List;

/**
 * JUnit Tests for the FsmBuilder class.
 * <p>
 * @author Spence Koehler
 */
public class TestFsmBuilder extends TestCase {

  public TestFsmBuilder(String name) {
    super(name);
  }
  

  private final FsmBuilder<String> buildFsmBuilder1(String[] tokens) {
    final FsmBuilder<String> fsmBuilder = new FsmBuilder1<String>();
    for (String token : tokens) {
      fsmBuilder.add(token);
    }
    return fsmBuilder;
  }

  private final FsmBuilder<String> buildFsmBuilder2(String[] tokens) {
    final FsmBuilder<String> fsmBuilder = new FsmBuilder2<String>();
    for (String token : tokens) {
      fsmBuilder.add(token);
    }
    return fsmBuilder;
  }

  private final FsmBuilder<String> doGetSequencesTest(
    boolean keepAll,
    String[] tokens, String[][] expectedSequences,
    int[] expectedMinRepeats, int[] expectedMaxRepeats) {

    final FsmBuilder<String> fsmBuilder = buildFsmBuilder2(tokens);

    final List<FsmSequence<String>> sequences = fsmBuilder.getSequences(keepAll);
    assertEquals(expectedSequences.length, sequences.size());

    for (int i = 0; i < expectedSequences.length; ++i) {
      final String[] expectedSequence = expectedSequences[i];
      final FsmSequence<String> sequence = sequences.get(i);

      assertEquals(expectedSequence.length, sequence.size());
      assertEquals(expectedMinRepeats[i], sequence.getMinRepeat());
      assertEquals(expectedMaxRepeats[i], sequence.getMaxRepeat());

      for (int j = 0; j < expectedSequence.length; ++j) {
        final String expectedToken = expectedSequence[j];
        final String token = sequence.get(j);
        assertEquals("i=" + i + " j=" + j, expectedToken, token);
      }
    }

    return fsmBuilder;
  }

  private final void doTestMatcher(FsmBuilder<String> fsmBuilder, FsmMatcher.Mode mode,
                                   String[][] tokenStrings) {

    final FsmMatcher<String> matcher = fsmBuilder.getMatcher(mode);

    int seqNum = 0;
    for (String[] tokens : tokenStrings) {
      FsmState<String> state = null;
      int tokenNum = 0;
      for (String token : tokens) {
        state = matcher.add(token);
        assertNotNull("seqNum=" + seqNum + ", tokenNum=" + tokenNum + ", token=" + token, state);
        if (tokenNum == 0) {
          assertTrue("seqNum=" + seqNum + ", tokenNum=" + tokenNum + ", token=" + token, state.isAtStart());
        }
        assertEquals("seqNum=" + seqNum + ", tokenNum=" + tokenNum + ", token=" + token, tokenNum, state.getCurPos());
        ++tokenNum;
      }
      assertTrue("seqNum=" + seqNum + ", tokenNum=" + tokenNum, state.isAtAnEnd());
      assertEquals("seqNum=" + seqNum + ", tokenNum=" + tokenNum, tokens.length - 1, state.getCurPos());
      ++seqNum;
    }
  }


  public void test1() {

    doGetSequencesTest(
      true,  // keepAll

      // tokens
      new String[] {
        "a", "b", "c",
        "d", "e", "f",
        "d", "e", "f",
        "d", "e", "f",
        "g", "h", "i",
      },

      // expectedSequences
      new String[][] {
        {"a", "b", "c"},
        {"d", "e", "f"},
        {"g", "h", "i"},
      },

      // expectedMinRepeats
      new int[] {
        1, 3, 1,
      },

      // expectedMaxRepeats
      new int[] {
        1, 3, 1,
      });
  }

  public void test2keepAll() {

    doGetSequencesTest(
      true,  // keepAll

      // tokens
      new String[] {
        "a", "b", "c",
        "d", "e", "f",
        "d", "e", "f",
        "d", "e", "f",
        "g", "h", "i",
        "d", "e", "f",
        "d", "e", "f",
      },

      // expectedSequences
      new String[][] {
        {"a", "b", "c"},
        {"d", "e", "f"},
        {"g", "h", "i"},
        {"d", "e", "f"},
      },

      // expectedMinRepeats
      new int[] {
        1, 2, 1, 2,
      },

      // expectedMaxRepeats
      new int[] {
        1, 3, 1, 3,
      });
  }

  public void test2dontKeepAll() {

    final FsmBuilder<String> fsmBuilder = 
      doGetSequencesTest(
        false,  // keepAll

        // tokens
        new String[] {
          "a", "b", "c",
          "d", "e", "f",
          "d", "e", "f",
          "d", "e", "f",
          "g", "h", "i",
          "d", "e", "f",
          "d", "e", "f",
        },

        // expectedSequences
        new String[][] {
          {"a", "b", "c"},
          {"d", "e", "f"},
          {"g", "h", "i"},
        },

        // expectedMinRepeats
        new int[] {
          1, 2, 1,
        },

        // expectedMaxRepeats
        new int[] {
          1, 3, 1,
        });

    doTestMatcher(
      fsmBuilder, FsmMatcher.Mode.LONGEST,
      new String[][] {
        {"a", "b", "c"},
        {"d", "e", "f"},
        {"d", "e", "f"},
        {"d", "e", "f"},
        {"g", "h", "i"},
        {"d", "e", "f"},
        {"d", "e", "f"},
      });
  }

  public void test2collapseNonRepeating() {

    final FsmBuilder<String> fsmBuilder = 
      doGetSequencesTest(
        false,  // keepAll

        // tokens
        new String[] {
          "a", "b", "c",
          "d", "e", "f",
          "d", "e", "f",
          "d", "e", "f",
          "g", "h", "i",
          "d", "e", "f",
          "d", "e", "f",
          "c", "e", "e", "g", "d", "b",
        },

        // expectedSequences
        new String[][] {
          {"a", "b", "c"},
          {"d", "e", "f"},
          {"g", "h", "i"},
          {"c", "e", "e", "g", "d", "b"},
        },

        // expectedMinRepeats
        new int[] {
          1, 2, 1, 1,
        },

        // expectedMaxRepeats
        new int[] {
          1, 3, 1, 1,
        });
  }


  private final FsmBuilder<String> doCollapseCountTest(String[] tokens, int expectedCollapseCount) {
    final FsmBuilder<String> fsmBuilder = buildFsmBuilder1(tokens);

    final List<FsmSequence<String>> sequences = fsmBuilder.getSequences(false/*don't keepAll*/);
    assertEquals(expectedCollapseCount, sequences.size());

    return fsmBuilder;
  }

  public void testRepeatingSequencesCollapsingToOne() {

    FsmBuilder<String> fsmBuilder = null;

    fsmBuilder=
      doCollapseCountTest(
        // tokens
        new String[] {
          "a",
          "a",
          "a",
          "a",
          "a",
        },

        // expected collapse count
        1);
    
    fsmBuilder=
      doCollapseCountTest(
        // tokens
        new String[] {
          "a", "b",
          "a", "b",
          "a", "b",
          "a", "b",
          "a", "b",
        },

        // expected collapse count
        1);
    
    fsmBuilder=
      doCollapseCountTest(
        // tokens
        new String[] {
          "a", "b", "c",
          "a", "b", "c",
          "a", "b", "c",
          "a", "b", "c",
          "a", "b", "c",
        },

        // expected collapse count
        1);
    
    fsmBuilder=
      doCollapseCountTest(
        // tokens
        new String[] {
          "a",
          "b",
          "b",
        },

        // expected collapse count
        2);

    fsmBuilder=
      doCollapseCountTest(
        // tokens
        new String[] {
          "a",
          "a",
          "b",
        },

        // expected collapse count
        2);
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestFsmBuilder.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
