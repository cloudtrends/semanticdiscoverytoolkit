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
package org.sd.cluster.config;


/**
 * Interface for clusterable objects.
 * <p>
 * A clusterable object has a cluster context that is set by a NodeServer when
 * it handles a message.
 *
 * @author Spence Koehler
 */
public interface Clusterable {
  
  /**
   * Set this clusterable's clusterContext.
   * <p>
   * Note that this is not set until a cluster node's node server handles
   * a message that uses this clusterable object and the message passes
   * the context along to this clusterable object.
   *
   * @param clusterContext  this object's clusterContext.
   */
  public void setClusterContext(ClusterContext clusterContext);

  /**
   * Get this clusterable's clusterContext.
   *
   * @return this object's clusterContext.
   */
  public ClusterContext getClusterContext();

}
