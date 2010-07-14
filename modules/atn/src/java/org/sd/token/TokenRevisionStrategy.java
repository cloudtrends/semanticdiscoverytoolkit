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
package org.sd.token;



/**
 * The TokenRevisionStrategy indicates the order for revising tokens.
 * <ul>
 * <li>LSL: Longest-Shortest-Longer: Start with the longest possible token,
 *      revise to the shortest and proceed with the next longer tokens.</li>
 * <li>SO: Shortest-Only: Give the shortest possible token with no revisions.</li>
 * <li>LO: Longest-Only: Give the longest possible token with no revisions.</li>
 * <li>SL: Shortest-to-Longest: Start with the shortest possible token, revise
 *     to the next longer token until reaching the longest.</li>
 * <li>LS: Longest-to-Shortest: Start with the longest possible token, revise
 *     to the next shorter token until reaching the shortest.</li>
 * </ul>
 *
 * @author Spence Koehler
 */
public enum TokenRevisionStrategy {

  LSL, SO, LO, SL, LS  

}
