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


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Client socket handler.
 * <p>
 * Used by a NodeClient to connect to a socket, send a message and receive a response.
 *
 * @author Spence Koehler
 */
class ClientSocketHandler implements Runnable {
  
  private InetSocketAddress serverAddress;
  private Message message;
  private int checkInterval;
  private LinkedBlockingQueue<Message> responses;
  private int timeLimit;

  private Socket socket;
  private IOException ioe;
  private SocketIO socketIO;

  private final AtomicBoolean isRunning = new AtomicBoolean(true);

  ClientSocketHandler(InetSocketAddress serverAddress, Message message, int checkInterval, LinkedBlockingQueue<Message> responses, int timeLimit) {
    this.serverAddress = serverAddress;
    this.message = message;
    this.checkInterval = checkInterval;
    this.responses = responses;
    this.timeLimit = timeLimit;

    this.socket = null;
    this.ioe = null;
    this.socketIO = null;
  }

  public void run() {
    Socket socket = null;
    SocketIO socketIO = null;

    try {
      socket = connect(checkInterval);

      if (socket != null) {
        // send the message and receive the response
        socketIO = new SocketIO(socket);
        final DataOutputStream dataOut = socketIO.getDataOutput();
        final DataInputStream dataIn = socketIO.getDataInput();

        if (dataOut != null && dataIn != null) {
          final Messenger messenger = new Messenger(dataOut, dataIn);

          // start a thread to kill the socket when time runs out
          final TimerThread timerThread = new TimerThread(socketIO, timeLimit);

          timerThread.start();
          final Message response = messenger.sendMessage(message);
          timerThread.die();

          responses.add(response == null ? new NullMessage() : response);
        }
      }
    }
    catch (IOException ioe) {
      this.ioe = ioe;
    }
    finally {
      try {
        if (socketIO != null) socketIO.close();
        if (socket != null) socket.close();
      }
      catch (IOException e) {
        //todo: determine what to do here... failed closing socketIO
        e.printStackTrace(System.err);
      }
    }

    isRunning.set(false);
  }
  
  public boolean isRunning() {
    return isRunning.get();
  }

  /**
   * Connect a socket to this port address.
   */
  private Socket connect() throws IOException {
/*
    SocketChannel socketChannel = null;

    try {
      socketChannel = SocketChannel.open(serverAddress);
    }
    catch (IllegalArgumentException e) {
      System.err.println(new Date() + ": ClientSocketHandler failed to open '" + serverAddress + "'!");
      throw e;
    }

    return socketChannel.socket();
*/
//    return new Socket(serverAddress.getAddress(), serverAddress.getPort());


    final Socket result = new Socket();
    result.bind(null);
    result.connect(serverAddress, timeLimit);
    return result;
  }

  private Socket connect(int checkInterval) throws IOException {
    Socket result = null;
 
    ConnectException ce = null;
    final long starttime = System.currentTimeMillis();
    final long expiration = starttime + timeLimit;

    while (isRunning.get() && System.currentTimeMillis() < expiration) {
      try {
        result = connect();
        break;
      }
      catch (ConnectException e) {
        ce = e;

        // wait and try again
        synchronized (this) {
          try {
            wait(checkInterval);
          }
          catch (InterruptedException ie) {
            // done waiting and done trying
            break;
          }
        }
      }
    }

    if (result == null) {
      System.err.println(new Date() + ": ClientSocketHandler.connect() Couldn't connect to '" + serverAddress + "'!");
      if (ce != null) {
        ce.printStackTrace(System.err);
      }
    }

    return result;
  }

  /**
   * Shutdown this server.
   */
  public void shutdown() {
    isRunning.set(false);
    try {
      if (socketIO != null) socketIO.close();
      if (socket != null) socket.close();
    }
    catch (IOException ioe) {
      this.ioe = ioe;
    }

    this.socket = null;
    this.socketIO = null;
  }

  /**
   * Get the io exception if it occurred while trying to open the connection.
   *
   * @return the IOException if it occurred or null.
   */
  public IOException getIOException() {
    return ioe;
  }

  private static final class TimerThread extends Thread {
    private final SocketIO socketIO;
    private final int timeLimit;
    private final long starttime;
    private final long expiration;
    private final AtomicBoolean die = new AtomicBoolean(false);

    TimerThread(SocketIO socketIO, int timeLimit) {
      this.socketIO = socketIO;
      this.timeLimit = timeLimit;
      this.starttime = System.currentTimeMillis();
      this.expiration = starttime + timeLimit;
    }

    public void run() {

      while (System.currentTimeMillis() < expiration && !die.get()) {
        //until killed or expired.
//        Thread.yield();
          try {
            Thread.sleep(Math.min(100, timeLimit));
          }
          catch (InterruptedException stop) {break;}
      }

      try {
        if (!die.get()) {
          // ran out of time -- close the socket!
          socketIO.close();
        }
      }
      catch (IOException ignore) {
        // already closed.
      }
    }

    public void die() {
      die.set(true);
    }
  }
}
