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
package org.sd.token;


/**
 * Interface for identifying the segment pointer strategy.
 * <p>
 * Note that this is essentially a factory for a SegmentPointerFinder
 * to encapsulate this process in a thread-safe way.
 *
 * @author Spence Koehler
 */
public interface SegmentPointerStrategy {
  
  /**
   * Get the thread-safe finder for the given input.
   */
  public SegmentPointerFinder getFinder(String input);

}
