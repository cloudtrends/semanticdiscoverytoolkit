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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * JUnit Tests for the GeneralUtil class.
 * <p>
 * @author Spence Koehler
 */
public class TestGeneralUtil extends TestCase {

  public TestGeneralUtil(String name) {
    super(name);
  }
  
  public void testPermute() {
    String[] elements = {"a", "b", "c", "d"};
    String[][] permutes =
      {{"a", "b", "c", "d",}, {"a", "b", "d", "c",}, {"a", "c", "b", "d",}, {"a", "c", "d", "b",},
       {"a", "d", "b", "c",}, {"a", "d", "c", "b",}, {"b", "a", "c", "d",}, {"b", "a", "d", "c",},
       {"b", "c", "a", "d",}, {"b", "c", "d", "a",}, {"b", "d", "a", "c",}, {"b", "d", "c", "a",},
       {"c", "a", "b", "d",}, {"c", "a", "d", "b",}, {"c", "b", "a", "d",}, {"c", "b", "d", "a",},
       {"c", "d", "a", "b",}, {"c", "d", "b", "a",}, {"d", "a", "b", "c",}, {"d", "a", "c", "b",},
       {"d", "b", "a", "c",}, {"d", "b", "c", "a",}, {"d", "c", "a", "b",}, {"d", "c", "b", "a",},};
    List<String> elist = Arrays.asList(elements);

    List<List<String>> list2 = GeneralUtil.permute(elist);

    assertEquals(permutes.length, list2.size());
    int i = 0;
    for (Iterator<? extends List<String>> it = list2.iterator(); it.hasNext(); ++i) {
      List<String> outer = it.next();
      int j = 0;
      for (Iterator<String> it2 = outer.iterator(); it2.hasNext() ; ++j) {
        assertEquals(permutes[i][j], it2.next());
      }
    }
  }

  public void testGroup() {
    String[] e1 = {"a", "b", "c"};
    String[] e2 = {"1", "2", "3"};
    String[][][] groups =
      {{{"a", "1"}, {"b", "2"}, {"c", "3"}},
       {{"a", "1"}, {"b", "3"}, {"c", "2"}},
       {{"a", "2"}, {"b", "1"}, {"c", "3"}},
       {{"a", "2"}, {"b", "3"}, {"c", "1"}},
       {{"a", "3"}, {"b", "1"}, {"c", "2"}},
       {{"a", "3"}, {"b", "2"}, {"c", "1"}}};

    List<List<List<String>>> result = GeneralUtil.group(Arrays.asList(e1), Arrays.asList(e2));

    assertEquals(groups.length, result.size());
    int i = 0;
    for (Iterator<List<List<String>>> it = result.iterator(); it.hasNext(); ++i) {
      List<List<String>> outer = it.next();
      int j = 0;
      for (Iterator<List<String>> it2 = outer.iterator(); it2.hasNext(); ++j) {
        List<String> nextInner = it2.next();
        int k = 0;
        for (Iterator<String> it3 = nextInner.iterator(); it3.hasNext(); ++k) {
          assertEquals(groups[i][j][k], it3.next());
        }
      }
    }
  }

  public void testAddin() {
    String[] e1 = {"a", "b"};
    String[] e2 = {"1", "2"};
    String[] e3 = {"x", "y"};
    String[][][] groups =
      {{{"a", "1", "x"}, {"b", "2", "y"}},
       {{"a", "2", "x"}, {"b", "1", "y"}},
       {{"a", "1", "y"}, {"b", "2", "x"}},
       {{"a", "2", "y"}, {"b", "1", "x"}}};

    List<List<List<String>>> gs = GeneralUtil.group(Arrays.asList(e1), Arrays.asList(e2));
    List<List<List<String>>> result = GeneralUtil.addin(gs, Arrays.asList(e3));

    assertEquals(groups.length, result.size());
    int i = 0;
    for (Iterator<List<List<String>>> it = result.iterator(); it.hasNext(); ++i) {
      List<List<String>> outer = it.next();
      int j = 0;
      for (Iterator<List<String>> it2 = outer.iterator(); it2.hasNext(); ++j) {
        List<String> nextInner = it2.next();
        int k = 0;
        for (Iterator<String> it3 = nextInner.iterator(); it3.hasNext(); ++k) {
          assertEquals(groups[i][j][k], it3.next());
        }
      }
    }
  }

  public void testPermutedGroups() {
    String[] e1 = {"a", "b"};
    String[] e2 = {"1", "2"};
    String[] e3 = {"x", "y"};
    String[][][] groups =
      {{{"a", "1", "x"}, {"b", "2", "y"}},
       {{"a", "2", "x"}, {"b", "1", "y"}},
       {{"a", "1", "y"}, {"b", "2", "x"}},
       {{"a", "2", "y"}, {"b", "1", "x"}}};

    List<List<String>> arg = new ArrayList<List<String>>();
    arg.add(Arrays.asList(e1));
    arg.add(Arrays.asList(e2));
    arg.add(Arrays.asList(e3));
    List<List<List<String>>> result = GeneralUtil.permutedGroups(arg);

    assertEquals(groups.length, result.size());
    int i = 0;
    for (Iterator<List<List<String>>> it = result.iterator(); it.hasNext(); ++i) {
      List<List<String>> outer = it.next();
      int j = 0;
      for (Iterator<List<String>> it2 = outer.iterator(); it2.hasNext(); ++j) {
        List<String> nextInner = it2.next();
        int k = 0;
        for (Iterator<String> it3 = nextInner.iterator(); it3.hasNext(); ++k) {
          assertEquals(groups[i][j][k], it3.next());
        }
      }
    }
  }

  public void testZipper1() {
    String[][] input = new String[][] {
      {"a"},
    };

    doZipperTest(input, input);
  }

  public void testZipper2() {
    String[][] input = new String[][] {
      {"a"},
      {"1"},
    };

    String[][] output = new String[][] {
      {"a", "1"},
    };

    doZipperTest(input, output);
  }

  public void testZipper3() {
    String[][] input = new String[][] {
      {"a", "b", "c"},
      {"1", "2", "3"},
    };

    String[][] output = new String[][] {
      {"a", "1"},
      {"b", "2"},
      {"c", "3"},
    };

    doZipperTest(input, output);
  }

  public void testZipper4() {
    String[][] input = new String[][] {
      {"a", "b", "c"},
      {"1", "2", "3"},
      {"x", "y", "z"},
    };

    String[][] output = new String[][] {
      {"a", "1", "x"},
      {"b", "2", "y"},
      {"c", "3", "z"},
    };

    doZipperTest(input, output);
  }

  public void testZipper5() {
    String[][] input = new String[][] {
      {"a", "b", "c"},
      {"1", "2",},
      {"x", "y", "z"},
    };

    String[][] output = new String[][] {
      {"a", "1", "x"},
      {"b", "2", "y"},
      {"c", "z"},
    };

    doZipperTest(input, output);
  }

  private void doZipperTest(String[][] input, String[][] output) {
    List<List<String>> arg = toDoubleList(input);
    List<List<String>> expected = toDoubleList(output);

    List<List<String>> result = GeneralUtil.zipper(arg);

    assertEquals(expected.toString(), result.toString());
  }

  private List<List<String>> toDoubleList(String[][] data) {
    List<List<String>> result = new ArrayList<List<String>>();
    for (int i = 0; i < data.length; ++i) {
      result.add(Arrays.asList(data[i]));
    }
    return result;
  }

  public void testCombine() {

    String[][][] combineData // combineInput, expectedOutput

      // ((a b) (c d)) ==> ((a c) (a d) (b c) (b d))
      = {{{"a,b", "c,d"}, {"a,c a,d b,c b,d"}},

         // ((a b c) (d e f)) ==> ((a d) (a e) (a f) (b d) (b e) (b f) (c d) (c e) (c f))
         {{"a,b,c", "d,e,f"}, {"a,d a,e a,f b,d b,e b,f c,d c,e c,f"}},

         // ((a b) (c d) (e f)) ==> ((a c e) (a c f) (a d e) (a d f) (b c e) (b c f) (b d e) (b d f))
         {{"a,b", "c,d", "e,f"}, {"a,c,e a,c,f a,d,e a,d,f b,c,e b,c,f b,d,e b,d,f"}},

         // ((a) (b) (c)) ==> ((a b c))
         {{"a", "b", "c"}, {"a,b,c"}},

         // ((a)) ==> ((a))
         {{"a"}, {"a"}},

         // ((a b c d)) ==> ((a) (b) (c) (d))
         {{"a,b,c,d"}, {"a b c d"}}};

    for (int i = 0; i < combineData.length; ++i) {
      List<Collection<String>> input = strings2List(combineData[i][0]);
      String expected = combineData[i][1][0];

      List<Collection<String>> combined = GeneralUtil.combine(input);
      String output = asString(combined);

      assertEquals(expected, output);
    }
  }

  /** Auxiliary for testCombine. */
  private List<Collection<String>> strings2List(String[] strings) {
    List<Collection<String>> result = new ArrayList<Collection<String>>();
    for (int i = 0; i < strings.length; ++i) {
      String[] subList = strings[i].split("[\\,]");
      result.add(Arrays.asList(subList));
    }
    return result;
  }

  /** Auxiliary for testCombine. */
  private String asString(List<Collection<String>> collections) {
    StringBuffer result = new StringBuffer();
    for (Iterator<Collection<String>> it = collections.iterator(); it.hasNext();) {
      Collection<String> curList = it.next();
      for (Iterator<String> it2 = curList.iterator(); it2.hasNext();) {
        String curObject = it2.next();
        result.append(curObject.toString());
        if (it2.hasNext()) {
          result.append(',');
        }
      }
      if (it.hasNext()) {
        result.append(' ');
      }
    }
    return result.toString();
  }

  private final void doGetCombinationsTest(String[] elts, int size, String[][] expected) {
    final List<String> elements = Arrays.asList(elts);
    final List<LinkedList<String>> combos = GeneralUtil.getCombinations(elements, size);

    if (combos == null) {
      assertNull(expected);
    }
    else {
      if (expected == null) {
        System.out.println("combos: " + combos);
      }
      else {
        assertEquals(combos.toString(), expected.length, combos.size());
        int i = 0;
        for (Iterator<LinkedList<String>> iteri = combos.iterator(); iteri.hasNext(); ) {
          final LinkedList<String> collection = iteri.next();
          int j = 0;
          for (Iterator<String> iterj = collection.iterator(); iterj.hasNext(); ) {
            final String elt = iterj.next();
            assertEquals("(" + i + "," + j + ")", expected[i][j], elt);
            ++j;
          }
          ++i;
        }
      }
    }
  }

  public void testGetCombinations() {
    doGetCombinationsTest(new String[]{"a"}, 1, new String[][]{{"a"}});
    doGetCombinationsTest(new String[]{"a"}, 2, null);

    doGetCombinationsTest(new String[]{"a", "b"}, 1, new String[][]{{"a"}, {"b"}});
    doGetCombinationsTest(new String[]{"a", "b"}, 2, new String[][]{{"a", "b"}});
    doGetCombinationsTest(new String[]{"a", "b"}, 3, null);

    doGetCombinationsTest(new String[]{"a", "b", "c"}, 1, new String[][]{{"a"}, {"b"}, {"c"}});
    doGetCombinationsTest(new String[]{"a", "b", "c"}, 2, new String[][]{{"a", "b"}, {"a", "c"}, {"b", "c"}});
    doGetCombinationsTest(new String[]{"a", "b", "c"}, 3, new String[][]{{"a", "b", "c"}});
    doGetCombinationsTest(new String[]{"a", "b", "c"}, 4, null);

    doGetCombinationsTest(new String[]{"a", "b", "c", "d"}, 1, new String[][]{{"a"}, {"b"}, {"c"}, {"d"}});
    doGetCombinationsTest(new String[]{"a", "b", "c", "d"}, 2, new String[][]{{"a", "b"}, {"a", "c"}, {"a", "d"}, {"b", "c"}, {"b", "d"}, {"c", "d"}});
    doGetCombinationsTest(new String[]{"a", "b", "c", "d"}, 3, new String[][]{{"a", "b", "c"}, {"a", "b", "d"}, {"a", "c", "d"}, {"b", "c", "d"}});
    doGetCombinationsTest(new String[]{"a", "b", "c", "d"}, 4, new String[][]{{"a", "b", "c", "d"}});
    doGetCombinationsTest(new String[]{"a", "b", "c", "d"}, 5, null);
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestGeneralUtil.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
