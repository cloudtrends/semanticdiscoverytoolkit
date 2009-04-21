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


import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Utility class to build an FSM grammar from input.
 * <p>
 * @author Spence Koehler
 */
public interface FsmBuilder <T> {

  public void add(T token);

  /**
   * Get a matcher for this builder's (current) sequences.
   * <p>
   * NOTE: A new instance of a matcher will be built on each invocation and
   * the returned matcher will always be based on the current state of this
   * builder.
   */
  public FsmMatcher<T> getMatcher(FsmMatcher.Mode mode);

  /**
   * Get the (collapsed) sequences.
   */
  public List<FsmSequence<T>> getSequences(boolean keepAll);

}
