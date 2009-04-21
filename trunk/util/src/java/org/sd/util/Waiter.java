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


import java.util.Date;

/**
 * Utility to wait for an event or time out.
 * <p>
 * @author Spence Koehler
 */
public abstract class Waiter {
	
	public static final boolean waitFor(Waiter waiter, long duration, long checkInterval) {
		boolean result = false;

		final Timer timer = new Timer(duration, new Date());
		while (!timer.reachedTimerMillis()) {
			if (result = waiter.event()) break;

			try {
				Thread.sleep(checkInterval);
			}
			catch (InterruptedException e) {
				break;
			}
		}

		return result;
	}

	/**
	 * Check for the event, returning true if it has occurred; otherwise, false.
	 */
	public abstract boolean event();
}
