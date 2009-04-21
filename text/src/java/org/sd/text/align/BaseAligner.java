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
package org.sd.text.align;


import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * Base alignment class.
 * <p>
 * Note that alignment is based on the "equals" and "hashCode" methods
 * of T.
 *
 * @author Spence Koehler
 */
public class BaseAligner <T> extends AbstractAligner <T> {

  private Map<T, LinkedList<Integer>> skipped2poss = null;
  private Map<T, LinkedList<Integer>> missed2poss = null;

  public BaseAligner(T[] seq1, T[] seq2) {
    super(seq1, seq2);
  }

  protected void markSkipped(T e2, int pos2) {
    if (skipped2poss == null) skipped2poss = new HashMap<T, LinkedList<Integer>>();
    mark(skipped2poss, e2, pos2);
  }

  protected void markMissed(T e1, int pos1) {
    if (missed2poss == null) missed2poss = new HashMap<T, LinkedList<Integer>>();
    mark(missed2poss, e1, pos1);
  }

  protected int getMissedPenalty(T e1, int pos1) {
    return getPenalty(missed2poss, e1, pos1);
  }

  protected int getSkippedPenalty(T e2, int pos2) {
    return getPenalty(skipped2poss, e2, pos2);
  }

  private final int getPenalty(Map<T, LinkedList<Integer>> elt2poss, T e, int pos) {
    int result = 0;

    if (elt2poss != null) {
      final LinkedList<Integer> poss = elt2poss.get(e);
      if (poss != null && poss.size() > 0) {
        final int pos1 = poss.removeFirst();
        result = pos - pos1;
      }
    }

    return result;
  }
}
