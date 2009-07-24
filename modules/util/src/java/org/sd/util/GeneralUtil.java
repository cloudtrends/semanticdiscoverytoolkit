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


import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Collection;
import java.util.Iterator;

/**
 * General static utilities.
 * <p>
 * @author Spence Koehler
 */
public class GeneralUtil {
  
  /** One-argument function for mapcar. */
  public static interface Function<E> {
    public E lambda(E elt);
  }

  /** Two-argument function for mapcar2. */
  public static interface Function2<E> {
    public E lambda(E elt1, E elt2);
  }

  /**
   * Apply the function to each element of the given list.
   */
  public static <E> List<E> mapcar(Function<E> f, List<E> arg) {
    List<E> result = new ArrayList<E>();

    if (arg != null) {
      for (E elt : arg) {
        result.add(f.lambda(elt));
      }
    }

    return result;
  }

  /**
   * Apply the function to the elements within the given lists at corresponding
   * indexes until one of the lists runs out.
   */
  public static <E> List<E> mapcar2(Function2<E> f, List<E> arg1, List<E> arg2) {
    List<E> result = new ArrayList<E>();

    if (arg1 != null && arg2 != null) {
      Iterator<E> it1 = arg1.iterator();
      Iterator<E> it2 = arg2.iterator();
      
      while (it1.hasNext() && it2.hasNext()) {
        E elt1 = it1.next();
        E elt2 = it2.next();
        
        result.add(f.lambda(elt1, elt2));
      }
    }

    return result;
  }

  /**
   * Create a list of all permutations (lists) of the elements within
   * the given list.
   */
  public static <E> List<List<E>> permute(List<E> list1) {
    List<List<E>> result = new ArrayList<List<E>>();

    if (list1 != null) {
      int s1 = list1.size();

      if (s1 > 0) {
        int index = 0;
        for (Iterator<E> it = list1.iterator(); it.hasNext(); ++index) {
          E elt = it.next();
          List<E> list2 = new LinkedList<E>(list1.subList(0, index));
          list2.addAll(list1.subList(index + 1, s1));
          List<List<E>> p2 = permute(list2);

          for (List<E> curList : p2) {
            ((LinkedList<E>)curList).addFirst(elt);
            result.add(curList);
          }
        }
      }
      else {
        result.add(new LinkedList<E>());
      }
    }

    return result;
  }

  /**
   * Group each element from list1 with an element from list2, using every
   * element exactly once. Proper function is guaranteed only when the sizes
   * of both lists are equal.
   * <p>
   * For example (a b) grouped with (1 2) yields:
   * (((a 1) (b 2)) ((a 2) (b 1)))
   * <p>
   * @return a list of groups (lists of lists) or null if the sizes of the
   *         lists are not equal.
   */
  public static <E> List<List<List<E>>> group(List<E> list1, List<E> list2) {
    if (list1 == null || list2 == null && list1.size() == list2.size()) {
      return null;
    }

    List<List<List<E>>> result = new ArrayList<List<List<E>>>();
    List<List<E>> p2 = permute(list2);
    int size = list1.size();
    for (List<E> g2 : p2) {
      List<List<E>> nextList = new ArrayList<List<E>>();
      for (int i = 0; i < size; ++i) {
        E elt1 = list1.get(i);
        E elt2 = g2.get(i);
        List<E> list = new ArrayList<E>();
        list.add(elt1);
        list.add(elt2);
        nextList.add(list);
      }
      result.add(nextList);
    }
    return result;
  }

  /**
   * Add in another list of elements to the given groups.
   * <p>
   * addin(groups(a, b), c) would be the equivalent of groups(a, b, c).
   * <p>
   * This allows permuted groups to be constructed incrementally, which is
   * often necessary to apply incremental filters and keep the overall
   * combinatorics manageable
   * <p>
   * For example for a=(a b), b=(1 2), c=(x y), addin(groups(a, b), c)
   * yields:
   * (((a 1 x) (b 2 y)) ((a 2 x) (b 1 y)) ((a 1 y) (b 2 x)) ((a 2 y) (b 1 x)))
   * <p>
   * The size of list1 must be equal the sizes of the lists used to build
   * the group, or a NullPointerException will be thrown.
   * <p>
   * @return a list of groups (lists of lists).
   */
  public static <E> List<List<List<E>>> addin(List<List<List<E>>> groups, List<E> list1) {
    List<List<List<E>>> result = new ArrayList<List<List<E>>>();

    List<List<E>> list2 = permute(list1);
    for (List<E> p : list2) {
      result.addAll(addinAux(groups, p));
    }

    return result;
  }

  /**
   * Auxiliary to addin, deals with adding one new permutation to the groups.
   */
  private static <E> List<List<List<E>>> addinAux(List<List<List<E>>>groups, List<E> list1) {
    List<List<List<E>>> result = new ArrayList<List<List<E>>>();
    int size = list1.size();

    for (List<List<E>> group: groups) {
      if (group.size() != size) {
        return null;
      }

      List<List<E>> newGroup = new ArrayList<List<E>>();
      for (int i = 0; i < size; ++i) {
        List<E> grouppiece = group.get(i);
        E elt = list1.get(i);

        List<E> newGroupPiece = new ArrayList<E>(grouppiece);
        newGroupPiece.add(elt);
        newGroup.add(newGroupPiece);
      }
      result.add(newGroup);
    }

    return result;
  }

  /**
   * Generate all permuted groups from each list of elements.
   * <p>
   * Beware of the combinatorics here; it may be necessary to use group and
   * addin to incrementally build the groups.
   * <p>
   * For example for a=(a b), b=(1 2), c=(x y), permutedGroups({a, b, c})
   * yields:
   * (((a 1 x) (b 2 y)) ((a 2 x) (b 1 y)) ((a 1 y) (b 2 x)) ((a 2 y) (b 1 x)))
   * <p>
   * The sizes of all element lists must be equal, or a NullPointerException
   * will be thrown.
   * <p>
   * @return a list of groups (lists of lists).
   */
  public static <E> List<List<List<E>>> permutedGroups(List<List<E>>elementLists) {
    List<List<List<E>>> result = new ArrayList<List<List<E>>>();

    if (elementLists == null || elementLists.size() == 0) {
      return result;
    }

    int size = elementLists.size();
    if (size == 1) {
      List<E> elements = elementLists.get(0);
      List<List<E>> groups = new ArrayList<List<E>>();
      for (E elt : elements) {
        List<E> group = new ArrayList<E>();
        group.add(elt);
        groups.add(group);
      }
      result.add(groups);
    }
    else {
      List<E> first = elementLists.get(0);
      List<E> second = elementLists.get(1);
      result = group(first, second);
      
      for (int i = 2; i < size; ++i) {
        List<E> next = elementLists.get(i);
        result = addin(result, next);
      }
    }

    return result;
  }
  
  /**
   * Utility function to combine elements from a list of collections.
   * <p>
   * Given a list of the form (x1 x2 x3 ... xn) where each xi has the form
   * (y1 y2 ... ym), create a list (size m^n) of collections (each size n)
   * that takes one yi from each xi.
   * <p>
   * The resulting list would be of the form (z1 z2 ... zk) where each zi
   * has the form (yj1 yj2 ... yjn) where ji is in the range 1..m.
   * <p>
   * As examples:
   * <p>  ((a b) (c d)) ==&gt; ((a c) (a d) (b c) (b d))
   * <p>  ((a b c) (d e f)) ==&gt; ((a d) (a e) (a f) (b d) (b e) (b f) (c d) (c e) (c f))
   * <p>  ((a b) (c d) (e f)) ==&gt; ((a c e) (a c f) (a d e) (a d f) (b c e) (b c f) (b d e) (b d f))
   * <p>  ((a) (b) (c)) ==&gt; ((a b c))
   * <p>  ((a)) ==&gt; ((a))
   * <p>  ((a b c d)) ==&gt; ((a) (b) (c) (d))
   * <p>
   */
  public static <E> List<Collection<E>> combine(List<Collection<E>> collections) {
    if (collections == null) {
      return null;
    }
    List<Collection<E>> result = new ArrayList<Collection<E>>();
    combineAux(collections, new ArrayList<E>(), result);
    return result;
  }

  /** Recursive auxiliary for combine(collections). */
  private static <E> void combineAux(List<Collection<E>> collections, List<E> objectAccumulator, List<Collection<E>> resultList) {

    int numCollections = collections.size();

    if (numCollections == 0) {
      return;
    }

    Collection<E> firstCollection = collections.iterator().next();
    boolean isEmpty = firstCollection == null || firstCollection.size() == 0;

    if (numCollections == 1) {
      if (!isEmpty) {
        for (Iterator<E> it = firstCollection.iterator(); it.hasNext(); ) {
          E curObject = it.next();
          List<E> nextAccumulator = new ArrayList<E>(objectAccumulator);

          nextAccumulator.add(curObject);
          resultList.add(nextAccumulator);
        }
      }
      else {
        List<E> nextAccumulator = new ArrayList<E>(objectAccumulator);
        nextAccumulator.add(null);
        resultList.add(nextAccumulator);
      }
      return;
    }

    List<Collection<E>> remainder = collections.subList(1, numCollections);

    if (!isEmpty) {
      for (Iterator<E> it = firstCollection.iterator(); it.hasNext(); ) {
        E curObject = it.next();
        List<E> nextAccumulator = new ArrayList<E>(objectAccumulator);
        nextAccumulator.add(curObject);
        combineAux(remainder, nextAccumulator, resultList);
      }
    }
    else {
      List<E> nextAccumulator = new ArrayList<E>(objectAccumulator);
      nextAccumulator.add(null);
      combineAux(remainder, nextAccumulator, resultList);
    }
  }

  /**
   * Get all combinations of the given size of the given elements.
   * <p>
   * Examples:
   * <p>  (a), 1 ==&gt; ((a))
   * <p>  (a b), 1 ==&gt; ((a) (b))
   * <p>  (a b), 2 ==&gt; ((a b))
   * <p>  (a b c), 1 ==&gt; ((a) (b) (c))
   * <p>  (a b c), 2 ==&gt; ((a b) (b c) (a c))
   * <p>
   * @return the combinations or null if the list isn't at least 'size' big.
   */
//   public static <E> List<Collection<E>> getCombinations(List<E> elements, int size) {
//     List<Collection<E>> result = null;

//     if (elements.size() == size) {
//       result = new ArrayList<Collection<E>>();
//       result.add(elements);
//     }
//     else {
//       final int numElts = elements.size();

//       final List<Collection<E>> elementLists = new ArrayList<Collection<E>>();
//       for (int i = 0; i < size; ++i) elementLists.add(elements);
//       result = combine(elementLists);

//       // prune out groups with duplicated elts
//       for (Iterator<Collection<E>> iter = result.iterator(); iter.hasNext(); ) {
//         final Collection<E> combination  = iter.next();
//         if (hasDupe(combination)) iter.remove();
//       }
//     }

//     return result;
//   }
  public static <E> List<LinkedList<E>> getCombinations(List<E> elements, int size) {
    List<LinkedList<E>> result = null;

    final int numElts = elements.size();
    if (size == 0 || size > numElts) return result;

    if (elements.size() == size) {
      result = new ArrayList<LinkedList<E>>();
      result.add(new LinkedList<E>(elements));
    }
    else {
      result = new ArrayList<LinkedList<E>>();

      if (size == 1) {
        for (E elt : elements) {
          final LinkedList<E> eltList = new LinkedList<E>();
          eltList.add(elt);
          result.add(eltList);
        }
      }
      else {
        for (int i = 0; i <= numElts - size; ++i) {
          final E elt = elements.get(i);
          final List<E> subList = elements.subList(i + 1, numElts);
          final List<LinkedList<E>> combos = getCombinations(subList, size - 1);
          for (LinkedList<E> combo : combos) {
            combo.addFirst(elt);
            result.add(combo);
          }
        }
      }
    }

    return result;
  }


//   /**
//    * Determine whether the elements contains *instance* level duplicates.
//    */
//   private static <E> boolean hasDupe(Collection<E> elements) {
//     boolean result = false;

//     final List<E> eltList = new ArrayList<E>(elements);

//     // NOTE: must use "==", not ".equals"
//     final int num = elements.size();
//     for (int i = 0; i < num - 1 && !result; ++i) {
//       final E elt1 = eltList.get(i);
//       for (int j = i + 1; j < num; ++j) {
//         final E elt2 = eltList.get(j);
//         if (elt1 == elt2) {
//           result = true;
//           break;
//         }
//       }
//     }

//     return result;
//   }

  /**
   * Given a list of equal-sized lists, group the ith elements of each
   * inner list with each other.
   * <p>
   * Examples:
   * <p>((a)) generates ((a))
   * <p>((a) (1)) generates ((a 1))
   * <p>((a b c) (1 2 3)) generates ((a 1) (b 2) (c 3))
   * <p>((a b c) (1 2 3) (x y z)) generates ((a 1 x) (b 2 y) (c 3 z))
   */
  public static <E> List<List<E>> zipper(List<List<E>> listOfLists) {
    if (listOfLists == null) {
      return null;
    }

    List<List<E>> result = new ArrayList<List<E>>();
    int innerLength = maxInnerSize(listOfLists);

    for (int i = 0; i < innerLength; ++i) {
      result.add(new ArrayList<E>());
    }

    for (Iterator<List<E>> it1 = listOfLists.iterator(); it1.hasNext(); ) {
      List<E> innerList = it1.next();
      int index = 0;
      for (Iterator<E> it2 = innerList.iterator(); it2.hasNext(); ++index) {
        E element = it2.next();

        List<E> resultList = result.get(index);
        resultList.add(element);
      }

/*
//maybe its better not to pad after all
      // pad missing elements with null when lists aren't same size after all
      for (int i = index; i < innerLength; ++i) {
        List<E> resultList = result.get(i);
        resultList.add(null);
      }
*/
    }

    return result;
  }

  /**
   * Find the maximum size of the inner lists.
   */
  private static <E> int maxInnerSize(List<List<E>> listOfLists) {
    int result = 0;

    for (Iterator<List<E>> it = listOfLists.iterator(); it.hasNext(); ) {
      List<E> list = it.next();
      int size = list.size();
      result = (size > result) ? size : result;
    }

    return result;
  }
                                   
}
