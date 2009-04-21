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


import org.sd.cluster.config.ClusterDefinition;
import org.sd.cluster.config.ClusterException;
import org.sd.cluster.config.Console;
import org.sd.cluster.io.Response;
import org.sd.cluster.job.JobCommandMessage;
import org.sd.cluster.job.LocalJobId;

import java.util.LinkedList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Helper utility to send/receive messages from jobs.
 * <p>
 * @author Spence Koehler
 */
public class JobCommunicator {
  
  private Console console;
  private String description;
  private String myNodeId;
  private LocalJobId destId;
  private String myGroupName;
  private int numRetries;
  private int timeoutMillis;

  private int myGroupSize;
  private boolean alwaysRotate;
  private LinkedList<String> serverNodeNames;
  private String currentServer;

  private final Object ROLL_MUTEX = new Object();

  public JobCommunicator(Console console, String description, String myNodeId, LocalJobId destId, String myGroupName, int numRetries, int timeoutMillis) {
    this.console = console;
    this.description = description;
    this.myNodeId = myNodeId;
    this.destId = destId;
    this.myGroupName = myGroupName;
    this.numRetries = numRetries;
    this.timeoutMillis = timeoutMillis;
    this.alwaysRotate = false;

    init();
  }

  /**
   * Initialize for round-robin server pinging.
   */
  private final void init() {

    // NOTE: only need to compute something if there is more than one destination (server) for this group of clients.
    this.serverNodeNames = null;

    if (destId.getJobDescription() != null) {
      final ClusterDefinition clusterDef = console.getClusterDefinition();
      final List<String> allServerNodeNames = clusterDef.getGroupNodeNames(destId.getNodeName(), true);

      final int numServerNodes = (allServerNodeNames == null) ? 0 : allServerNodeNames.size();
      if (numServerNodes == 1) {
        this.serverNodeNames = new LinkedList<String>(allServerNodeNames);
      }
      else if (numServerNodes > 1) {
        // there is more than one destination
        this.serverNodeNames = new LinkedList<String>();

        final List<String> clientNodeNames = clusterDef.getGroupNodeNames(myGroupName, true);
        this.myGroupSize = clientNodeNames.size();

        // order servers based on myNodeName's position in clientNodeNames
        final int startServerNodeIndex = clientNodeNames.indexOf(myNodeId) % numServerNodes;
        for (int count = 0; count < numServerNodes; ++count) {
          this.serverNodeNames.add(allServerNodeNames.get((count + startServerNodeIndex) % numServerNodes));
        }
      }
    }

    if (serverNodeNames != null && serverNodeNames.size() > 0) {
      currentServer = serverNodeNames.get(0);

      if (myGroupSize < serverNodeNames.size()) {
        // fewer clients than servers, so clients will always rotate through servers
        alwaysRotate = true;
        System.out.println(new Date() + ": NOTE: '" + description + "' initial server to contact is '" + currentServer + "'.");
      }
      else {
        // fewer or same servers than clients so each client can focus on a server
        System.out.println(new Date() + ": NOTE: '" + description + "' default server to contact is '" + currentServer + "'.");
      }
    }
    else {
      currentServer = destId.getNodeName();
      System.out.println(new Date() + ": NOTE: '" + description + "' only server to contact is '" + destId.getNodeName() + "'.");
    }
  }

  /**
   * Get the server currently being used (i.e. that from which the last
   * response was received).
   */
  public String getCurrentServer() {
    return currentServer;
  }

  /**
   * Send the given job command to only one node.
   * <p>
   * If the contained localJobId identifies multiple target nodes, then choose
   * one deterministically and for even distribution. On failure to contact a
   * node, roll over to an alternate.
   */
  public WorkResponse getResponse(JobCommandMessage workMessage, AtomicBoolean pause) {
    WorkResponse result = null;
    WorkResponse waitingResponse = null;
    WorkResponse doneResponse = null;

    if (serverNodeNames == null) {
      result = doGetResponse(workMessage, pause);
    }
    else {
      synchronized (ROLL_MUTEX) {
        int num = serverNodeNames.size();
        for (int i = 0; i < num; ++i) {
          final String serverNodeName = serverNodeNames.getFirst();

          final JobCommandMessage theWorkMessage =
            new JobCommandMessage(workMessage.getJobCommand(),
                                  new LocalJobId(destId.getJobDescription(), serverNodeName),
                                  workMessage.getPayload());

          if (i > 0) {
            System.out.println(new Date() + ": NOTE: '" + description + "' rolling over to next server '" + serverNodeName + "'!");

            // NOTE: we end up in this spot after a server has timed out for 10 retries of 60 seconds each.
            //       When a server dies, the logs show us doing this once every 10 minutes after having cycled
            //       through all other servers. Instead of wasting 10 minutes every second or so checking if
            //       the dead server has come alive, we will disable it by removing it from consideration as an
            //       active server.
            // Disable "last" serverNodeName. todo: automatically re-enable it after some time (i.e. 1 day?) or when given a signal it has been fixed.
            --num;
            final String disabledServer = serverNodeNames.removeLast();
            System.err.println(new Date() + ": WARNING: Disabling non-responsive server node '" +
                               disabledServer + "'! (" + num + " servers remain active: " +
                               serverNodeNames + ")");
          }

          result = doGetResponse(theWorkMessage, pause);

//        final boolean isDoneResponse = result != null && result.getStatus() == WorkResponseStatus.DONE;
          boolean isDoneResponse = false;
          boolean isWaitResponse = false;
          if (result != null) {
            if (result.getStatus() == WorkResponseStatus.DONE) {
              isDoneResponse = true;
            }
            else if (result.getStatus() == WorkResponseStatus.WAITING) {
              isWaitResponse = true;
            }
          }

          if (!isDoneResponse && !isWaitResponse && result != null) {
            currentServer = serverNodeName;

            if (alwaysRotate) {
              // set it up so that next time we start with the next server
              // this is here so we will rotate through the servers and give
              // each a chance to offload work to fewer clients than servers.
              serverNodeNames.addLast(serverNodeNames.removeFirst());
            }
            break;
          }
          else {
            // roll
            serverNodeNames.addLast(serverNodeNames.removeFirst());

            // update waiting/done
            if (isWaitResponse) {
              waitingResponse = result;

              // a server shouldn't go from done to waiting, but just in case:
              if (doneResponse != null &&
                  waitingResponse.getNodeName().equals(doneResponse.getNodeName())) {
                // unset done response if same server has now given the waiting signal
                doneResponse = null;
              }
            }
            else if (isDoneResponse) {
              doneResponse = result;

              // if the same server replies done who was waiting:
              if (waitingResponse != null &&
                  doneResponse.getNodeName().equals(waitingResponse.getNodeName())) {
                // unset waiting response if same server has now given the done signal
                waitingResponse = null;
              }
            }
          }

          if (pause != null && pause.get()) break;
        }
      }
    }

    return result != null ? result : waitingResponse != null ? waitingResponse : doneResponse;
  }

  private final WorkResponse doGetResponse(JobCommandMessage workMessage, AtomicBoolean pause) {
    WorkResponse workResponse = null;
    Response response = null;

    for (int retry = 0; retry < numRetries; ++retry) {
      if (pause != null && pause.get()) break;

      boolean error = false;
      try {
        response = console.sendJobCommandToNode(workMessage, timeoutMillis);
      }
      catch (ClusterException e) {
        response = null;
        System.err.println(new Date() + ": " + description + ": exception while requesting work from " +
                           workMessage.getLocalJobId() + " (try #" + retry + "/" + numRetries + ", timeout=" + timeoutMillis + ")");
        e.printStackTrace(System.err);
        error = true;
      }

      if (response == null) {
        if (!error) {
          System.err.println(new Date() + ": " + description + ": " + myNodeId + ": No response from server '" +
                             workMessage.getLocalJobId() + "' (try #" + retry + "/" + numRetries + ", timeout=" + timeoutMillis + ")");
        }
      }
      else {
        break;
      }
    }

    if (response != null) {
      if (response instanceof WorkResponse) {
        workResponse = (WorkResponse)response;
      }
      else {
        System.err.println(new Date() + ": " + description + ": got unexpected response: '" + response + "' from '" +
                           destId + "' !");
      }
    }

    return workResponse;
  }
}
