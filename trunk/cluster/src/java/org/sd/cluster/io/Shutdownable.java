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


/**
 * Interface for objects that are to be shutdown with the cluster.
 * <p>
 * Implementing classes should register themselves with the cluster
 * context's job manager, from which the shutdown method will be called
 * when the cluster is shutdown.
 *
 * @author Spence Koehler
 */
public interface Shutdownable {

  /**
   * Shutdown the instance immediately (if now) or gracefully.
   */
  public void shutdown(boolean now);
}
