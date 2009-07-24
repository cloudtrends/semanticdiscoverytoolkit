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
package org.sd.cluster.job.work;


import org.sd.cluster.config.ClusterContext;

import java.util.Properties;

/**
 * Queue work server that loads in file paths.
 * <p>
 * @author Spence Koehler
 */
public class FileQueueWorkServer extends QueueWorkServer {
  
  public FileQueueWorkServer(Properties properties) {
    super(properties);

    //todo: parse out properties
  }

  /**
   * Initialize this work server.
   */
  protected boolean doInitialize(ClusterContext clusterContext, String jobIdString, String dataDirName) {
    boolean result = super.doInitialize(clusterContext, jobIdString, dataDirName);

    if (result) {
      //todo: walk startDir; load queue with matching paths.
      //  addWork(new WorkRequest(key, new PublishableString(path)));
    }

    return result;
  }
}
