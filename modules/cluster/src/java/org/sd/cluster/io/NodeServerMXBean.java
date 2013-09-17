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


import org.sd.util.StatsAccumulator;

/**
 * JMX MXBean for the cluster NodeServer.
 * <p>
 * @author Spence Koehler
 */
public interface NodeServerMXBean {

  /**
   * Get the duration of time during which this server has been "up"
   * as a human readable string.
   */
  public String getUpTime();

  /**
   * Get the number of socket threads for this server.
   */
  public int getNumSocketThreads();

  /**
   * Get the number of message handler threads for this server.
   */
  public int getNumMessageHandlerThreads();

  /**
   * Get the current size of the message queue.
   */
  public int getMessageQueueSize();

  /**
   * Get this node server's name.
   */
  public String getServerName();

  /**
   * Determine whether this node server is currently "up".
   */
  public boolean isUp();

  /**
   * Reset this server with the given numbers of threads.
   *
   * @param numSocketThreads  The new number of threads for listening on sockets
   *                          (leave unchanged if &lt;= 0).
   * @param numMessageHandlerThreads  The new number of message handler threads
   *                          (leave unchanged if &lt;= 0).
   * @param resetStats  If true, then the stats will all be reset.
   */
  public void reset(int numSocketThreads,
                    int numMessageHandlerThreads,
                    boolean resetStats);

  /**
   * Pause handling messages, but keep accepting and responding to them.
   * <p>
   * Note that the message queue will fill with uncoming requests while paused.
   */
  public void pauseHandling();

  /**
   * Resume handling messages from the queue.
   */
  public void resumeHandling();

  /**
   * Determine whether the server is currently handling or will handle messages.
   */
  public boolean isHandling();

  /**
   * Pause accepting messages, but keep handling those from the queue.
   */
  public void pauseAccepting();

  /**
   * Resume accepting messages.
   */
  public void resumeAccepting();

  /**
   * Determine whether the server is currently accepting messages.
   */
  public boolean isAccepting();

  /**
   * Get the overall stats for the time, in millis, to service requests.
   * <p>
   * This is the sum of receive, responseGen, and send time stats and
   * does <b>not</b> include handle time stats.
   * <p>
   * NOTE: Time is measured from the reception of a request to the sending
   *       of a response.
   */
  public StatsAccumulator getServerTimeStats();

  /**
   * Get the stats for the time, in millis, to receive requests.
   */
  public StatsAccumulator getReceiveTimeStats();

  /**
   * Get the stats for the time, in millis, to generate responses.
   */
  public StatsAccumulator getResponseGenTimeStats();

  /**
   * Get the stats for the time, in millis, to send responses.
   */
  public StatsAccumulator getSendTimeStats();

  /**
   * Get the stats for the time, in millis, to handle messages.
   */
  public StatsAccumulator getHandleTimeStats();

  /**
   * Get the number of dropped connections due to saturation.
   */
  public long getNumDroppedConnections();

  /**
   * Get the number of connections dropped by the client before the server dropped them.
   */
  public long getNumSeveredConnections();


//we can't really count the bytes at this level w/out too much overhead
  // /**
  //  * Get the stats for the request bytes received.
  //  */
  // public StatsAccumulator getNumInBytesStats();

  // /**
  //  * Get the stats for the response bytes returned.
  //  */
  // public StatsAccumulator getNumOutBytesStats();
}
