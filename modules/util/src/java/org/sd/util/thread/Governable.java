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
package org.sd.util.thread;


/**
 * Interface for a governable runnable.
 * <p>
 * The idea here is that if we can divide a long running operation into units,
 * we can instrument our code such that the overall process can be monitored
 * and manipulated between units.
 * <p>
 * Implementations of this interface keep the unit counter up-to-date with
 * progress through the units.
 * <p>
 * Manipulation will typically be performed by passing instances to a
 * GovernableThread to allow that manipulation.
 * 
 * @author Spence Koehler
 */
public interface Governable extends Runnable {
  
  /**
   * Get this instance's UnitCounter for progress and flow control
   * communications.
   *
   * @return a non-null unit counter instance that is updated and monitored
   *         within this class's run method (directly or indirectly).
   */
  public UnitCounter getUnitCounter();

}
