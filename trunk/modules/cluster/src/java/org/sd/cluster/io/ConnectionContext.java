/*
    Copyright 2011 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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


import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Context for a message's (socket) connection.
 * <p>
 * @author Spence Koehler
 */
public class ConnectionContext {
  
  private Socket socket;
  private final AtomicInteger monitorID = new AtomicInteger(0);

  public ConnectionContext(Socket socket) {
    this.socket = socket;
  }

  public Socket getSocket() {
    return socket;
  }

  public boolean isConnected() {
    return socket.isConnected() && !socket.isClosed();
  }

  public SocketAddress getRemoteAddress() {
    //NOTE: SocketAddress.toString is of form "0:0:0:0:0:0:0:1:44945" (includes connecting port)
    return socket.getRemoteSocketAddress();
  }

  public InetAddress getInetAddress() {
    //NOTE: InetAddress.toString is of form "0:0:0:0:0:0:0:1" (doesn't include connecting port)
    return socket.getInetAddress();
  }

  /**
   * Add a monitor with the given boolean on this instance's socket.
   * <p>
   * The monitor will check the socket connection, setting "die" to true
   * when the connection closes. The monitor will end when "die" is true
   * regardless of whether the connection closed. The monitor does *not*
   * affect the state of the connection.
   *
   * @param die  Monitor variable that is set to true when the connection
   *             is closed or that aborts the monitor when set to true.
   * @param checkInterval  The number of millis to sleep between checking
   *                       the socket connection.
   */
  public void addConnectionMonitor(AtomicBoolean die, long checkInterval) {
    new ConnectionMonitor(die, checkInterval).start();
  }


  private final class ConnectionMonitor extends Thread {

    private AtomicBoolean die;
    private long checkInterval;
    
    public ConnectionMonitor(AtomicBoolean die, long checkInterval) {
      super("Monitor-" + monitorID.getAndIncrement() + "-" + getRemoteAddress().toString());
      this.die = die;
    }

    public void run() {
      while (!die.get()) {
        if (!isConnected()) {
          die.set(true);
          break;
        }
        try {
          sleep(checkInterval);
        }
        catch (InterruptedException e) {
          // stop monitoring
          break;
        }
      }
    }
  }
}
