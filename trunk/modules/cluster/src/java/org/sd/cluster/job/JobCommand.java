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
package org.sd.cluster.job;


/**
 * Enumeration of job commands to be handled through the JobManager.
 * <p>
 * @author Spence Koehler
 */
public enum JobCommand {
  
  OPERATE,    // operate on the accompanying work request string

  PAUSE,      // pause the job
  RESUME,     // resume the job
  FLUSH,      // flush the job

  BOUNCE,     // interrupt, re-initialize, and restart the job
  INTERRUPT,  // interrupt the job

  PERSIST,    // pause and persist the job
  RESTORE,    // restore the persisted job (fail if running?)

  STATUS,     // get JobStatus for the job
  PROBE,      // probe for detailed status
  DETAIL,     // get details from job
  PURGE,      // kill and clean-up


  SPLIT;      // split work with another job
}
