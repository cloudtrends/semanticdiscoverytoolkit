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


import org.sd.io.Publishable;
import org.sd.cluster.io.Response;

/**
 * 
 * <p>
 * @author Spence Koehler
 */
public class EmptyJob extends AbstractJob {
  
  public String getDescription() {
    return null;
  }

  public Response operate(Publishable request) {
    return null;
  }

  public void start() {  // start processing job's work.
  }

  public void stop() {  // end processing job in this jvm; could still suspend? can't resume.
  }

  public void pause() {  // pause a job in this jvm to be resumed.
  }

  public void resume() {  // resume paused job.
  }

  public void suspend() {  // suspend to disk for restoration through JobManager in another jvm
  }

  public void shutdown(boolean now) {  // called when cluster is being shutdown
  }
}
