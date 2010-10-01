/*
    Copyright 2010 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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


import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import org.sd.cluster.io.Message;
import org.sd.cluster.io.NodeClient;
import org.sd.util.ExecUtil;

/**
 * A simple client that works independently of any cluster.
 * <p>
 * @author Spence Koehler
 */
public class SimpleClient {
  
  private NodeClient nodeClient;

  private final Object SEND_MESSAGE_MUTEX = new Object();

  public SimpleClient(String name, int numSocketThreads) {
    try {
      this.nodeClient =
        new NodeClient(
          "simple-client[" + name + "](" + ExecUtil.getUser() + "@" + ExecUtil.getMachineName(),
          InetAddress.getLocalHost(),
          numSocketThreads);
    }
    catch (UnknownHostException e) {
      throw new IllegalStateException(e);
    }
  }

  public void close()  {
    nodeClient.shutdown(false);
  }

  public final Message sendMessage(InetSocketAddress serverAddress, Message message, int timeout) {
    Message response = null;

    synchronized (SEND_MESSAGE_MUTEX) {
      response = nodeClient.sendMessage(serverAddress, message, 10, 100, timeout);
    }

    return response;
  }

  public final Message[] sendMessage(InetSocketAddress[] serverAddresses, Message message, int timeout) {
    Message[] responses = null;

    synchronized (SEND_MESSAGE_MUTEX) {
      responses = nodeClient.sendMessage(serverAddresses, message, 10, 100, timeout);
    }

    return responses;
  }
}
