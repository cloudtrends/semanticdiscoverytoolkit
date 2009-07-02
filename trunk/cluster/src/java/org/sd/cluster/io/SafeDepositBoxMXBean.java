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
package org.sd.cluster.io;


import java.util.List;

/**
 * JMX MXBean for the cluster SafeDepositBox.
 * <p>
 * @author Spence Koehler
 */
public interface SafeDepositBoxMXBean {

	/**
	 * Get the total number of drawers (active and incinerated).
	 */
	public long getTotalDrawerCount();

	/**
	 * Get the number of incinerated drawers.
	 */
	public long getNumIncineratedDrawers();

	/**
	 * Get the number of active drawers (filled or filling).
	 */
	public long getNumActiveDrawers();

	/**
	 * Get the number of active drawers that are filled.
	 */
	public long getNumFilledDrawers();

	/**
	 * Get the number of active drawers that are yet to be filled.
	 */
	public long getNumFillingDrawers();

	/**
	 * Get the keys for the filled drawers.
	 */
	public List<String> getFilledKeys();

	/**
	 * Get the contents.toString from the drawer with the given key.
	 */
	public String getContentsString(String key);

	/**
	 * Incinerate the drawer with the given key.
	 */
	public void incinerate(String key);

	/**
	 * Incinerate all drawers that are older than the given age (in millis).
	 *
	 * @return the number of drawers incinerated.
	 */
	public long incinerateOlder(long age);

}
