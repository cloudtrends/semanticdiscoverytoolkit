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


import org.sd.cluster.config.ClusterException;
import org.sd.cluster.config.Console;
import org.sd.cluster.config.StringResponse;
import org.sd.cluster.io.Response;
import org.sd.io.PublishableString;

import java.io.IOException;
import java.util.Date;

/**
 * A work factory that gets its work from a WorkServer.
 * <p>
 * @author Spence Koehler
 */
public class ClientWorkFactory extends AbstractWorkFactory {
  
  private static final int MAX_CONSECUTIVE_ERRORS = 10;

  private LocalJobId localWorkServerId;
  private String jobId;
  private String myNodeName;
  private Console console;  // for sending/receiving job operate messages

  private boolean complete;
  private UnitOfWork completed;
  private JobCommandMessage workMessage;

  public ClientWorkFactory(LocalJobId localWorkServerId, String jobId, String myNodeName, Console console) {
    this.localWorkServerId = localWorkServerId;
    this.jobId = jobId;
    this.myNodeName = myNodeName;
    this.console = console;
    this.complete = false;
    this.completed = null;

    this.workMessage = new JobCommandMessage(JobCommand.OPERATE, localWorkServerId,
                                             new PublishableString("get|" + jobId + "|" + myNodeName));
  }

  /**
   * Get the next unit of work.
   */
  protected UnitOfWork doGetNext() throws IOException {
    UnitOfWork result = null;

    if (complete) return completed;
    Response response = null;
    int numRemainingErrors = MAX_CONSECUTIVE_ERRORS;

    while (numRemainingErrors > 0) {
      try {
        response = console.sendJobCommandToNode(workMessage, 5000);

//        System.err.println(new Date() + ": " + myNodeName + " : queried for work with message=" + workMessage + " response=" + response);
      }
      catch (ClusterException e) {
        response = null;
        e.printStackTrace(System.err);
        --numRemainingErrors;
      }

      if (response == null) {
        --numRemainingErrors;
        System.err.println(new Date() + ": " + myNodeName + " : No response from server '" +
                           localWorkServerId + "'! " + numRemainingErrors + " consecutive retries remain.");

        if (numRemainingErrors == 0) {
          complete = true;

          System.err.println("Shutting down ClientWorkFactory! server=" + localWorkServerId + " myNodeName=" + myNodeName);
        }
      }
      else {
        numRemainingErrors = 0;  // time to exit loop.

        String responseString = null;

        // get the string out of the response
        if (response instanceof StringResponse) {
          responseString = ((StringResponse)response).getValue();
        }
        else {
          System.err.println(new Date() + " ClientWorkFactory : got unexpected response: '" + response + "' ! Ending.");
        }

        if (responseString == null) {
          // time to quit?
          complete = true;
          result = DONE_UNIT_OF_WORK;
        }
        else if (WorkServer.WORK_IS_DONE_RESPONSE.equals(responseString)) {
          // time to quit.
          complete = true;
          completed = DONE_UNIT_OF_WORK;
          result = DONE_UNIT_OF_WORK;
        }
        else {
          result = new StringUnitOfWork(responseString);
        }
      }
    }

    return result;
  }

  /**
   * Release the unit of work in a thread-safe way.
   * <p>
   * Behavior is undefined if a unit of work is re-released.
   */
  public void release(UnitOfWork unitOfWork) throws IOException {
    // nothing to do.
  }

  /**
   * Explicitly close the resources used by the work factory.
   */
  public void close() throws IOException {
    // nothing to do.
  }

  /**
   * Report whether all units retrieved have been released or
   * accounted for.
   */
  public boolean isComplete() {
    return complete && super.isComplete();
  }

  /**
   * Get an estimate for the remaining work to do.
   */
  public long getRemainingEstimate() {
    long result = super.getRemainingEstimate();

    if (!complete && result == 0) {
      result = -1L;
    }

    return result;
  }
}
