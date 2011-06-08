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


import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Node client.
 * <p>
 * Multithreaded management of client messages sent to a NodeServer.
 *
 * @author Spence Koehler
 */
public class NodeClient extends Thread {
  
  /**
   * Identifying prefix string for instances.
   */
  public static final String PREFIX_STRING = "NodeClient";

  /**
   * Maximum shutdown latency, or time between shutting down the server and its thread dying. In millis.
   */
  public static final int SHUTDOWN_LATENCY = 1000;


  private int clientId;
  private String nodeName;
  private ExecutorService socketPool;            // makes socket connections
  private ExecutorService responseQueueThread;   // polls response queue for responses
  private LinkedBlockingQueue<Message> asyncResponses;  // async responses for JUnit testing
  private LinkedBlockingQueue<ClientSocketHandler> clientSocketHandlers;

  private final AtomicBoolean stayAlive = new AtomicBoolean(true);
  private static final AtomicInteger nextClientId = new AtomicInteger(0);
  private final AtomicInteger socketThreadId = new AtomicInteger(0);

  /**
   * Construct a node client.
   *
   * @param name              A string to distinguish one client in the same jvm from another. ok if null.
   * @param inetAddress       The inet address of the client for identification.
   * @param numSocketThreads  Number of threads to pool for managing sockets.
   */
  public NodeClient(String name, InetAddress inetAddress, int numSocketThreads) {
    super(NodeUtil.buildNodeName(PREFIX_STRING, name, inetAddress.toString(), nextClientId.get()));
    this.clientId = nextClientId.getAndIncrement();
    this.nodeName = NodeUtil.buildNodeName(PREFIX_STRING, name, inetAddress.toString(), clientId);
    this.clientSocketHandlers = new LinkedBlockingQueue<ClientSocketHandler>();

    this.socketPool
      = Executors.newFixedThreadPool(
        numSocketThreads,
        new ThreadFactory() {
          public Thread newThread(Runnable r) {
            return new Thread(r, nodeName + "-Socket-" + socketThreadId.getAndIncrement());
          }
        });
    this.responseQueueThread
      = Executors.newSingleThreadExecutor(
        new ThreadFactory() {
          public Thread newThread(Runnable r) {
            return new Thread(r, nodeName + "-ResponseQueueThread");
          }
        });
  }

  public boolean isUp() {
    return stayAlive.get();
  }

  public void run() {
    while (stayAlive.get()) {
      synchronized (this) {
        try {
          wait(SHUTDOWN_LATENCY);
        }
        catch (InterruptedException ie) {
          shutdown(true);
        }
      }
    }
  }

  public void shutdown(boolean now) {
    stayAlive.set(false);

//    System.out.println("shutting down NodeClient (now=" + now + ") " + nodeName);

    for (Iterator<ClientSocketHandler> it = clientSocketHandlers.iterator(); it.hasNext(); ) {
      final ClientSocketHandler csh = it.next();
      csh.shutdown();
    }

    if (now) {
      socketPool.shutdownNow();
      responseQueueThread.shutdownNow();
    }
    else {
      socketPool.shutdown();
      responseQueueThread.shutdown();
    }
  }

  /**
   * Send the message to the server and get its response.
   * <p>
   * NOTE: it is up to the consumer of this method to do handle the response.
   */
  public Message sendMessage(InetSocketAddress serverAddress, Message message, int checkInterval, int timeout, int timeLimit) {
    final LinkedBlockingQueue<Message> responses = new LinkedBlockingQueue<Message>();
    doSendMessage(serverAddress, message, checkInterval, responses, timeLimit);
    final Message[] result = waitForResponses(responses, 1, timeout, timeLimit);
    return result == null ? null : result[0];
  }

  // for JUnit testing...
  // NOTE: this will currenlty only work for one async call at a time!!!
  void sendMessageAsync(InetSocketAddress serverAddress, Message message, int checkInterval) {
    asyncResponses = new LinkedBlockingQueue<Message>();
    doSendMessage(serverAddress, message, checkInterval, asyncResponses, 1000);
  }

  // for JUnit testing
  // NOTE: this will currenlty only work for one async call at a time!!!
  Message getResponseAsync(int timeout, int timeLimit) {
    final Message[] result = waitForResponses(asyncResponses, 1, timeout, timeLimit);
    return result == null ? null : result[0];    
  }

  /**
   * Multicast the message to the servers.
   * <p>
   * NOTE: it is up to the consumer of this method to do handle the responses, which are not
   *       guaranteed to be in any particular order. That is, response[i] does not necessarily
   *       correspond to serverAddresses[i].
   */
  public Message[] sendMessage(InetSocketAddress[] serverAddresses, Message message, int checkInterval, int timeout, int timeLimit) {
    final LinkedBlockingQueue<Message> responses = new LinkedBlockingQueue<Message>();
    for (int i = 0; i < serverAddresses.length; ++i) {
      doSendMessage(serverAddresses[i], message, checkInterval, responses, timeLimit);
    }
    final Message[] result = waitForResponses(responses, serverAddresses.length, timeout, timeLimit);
    return result;
  }

  /**
   * Multicast the messages to the servers, sending message[i] to server[i].
   * <p>
   * NOTE: it is up to the consumer of this method to do handle the responses, which are not
   *       guaranteed to be in any particular order. That is, response[i] does not necessarily
   *       correspond to serverAddresses[i].
   */
  public Message[] sendMessages(InetSocketAddress[] serverAddresses, Message[] messages, int checkInterval, int timeout, int timeLimit) {
    final LinkedBlockingQueue<Message> responses = new LinkedBlockingQueue<Message>();
    for (int i = 0; i < serverAddresses.length; ++i) {
      doSendMessage(serverAddresses[i], messages[i], checkInterval, responses, timeLimit);
    }
    final Message[] result = waitForResponses(responses, serverAddresses.length, timeout, timeLimit);
    return result;
  }

  // for JUnit testing...
  // NOTE: this will currenlty only work for one async call at a time!!!
  void sendMessagesAsync(InetSocketAddress[] serverAddresses, Message message, int checkInterval) {
    asyncResponses = new LinkedBlockingQueue<Message>();
    for (int i = 0; i < serverAddresses.length; ++i) {
      doSendMessage(serverAddresses[i], message, checkInterval, asyncResponses, 1000);
    }
  }

  // for JUnit testing
  // NOTE: this will currenlty only work for one async call at a time!!!
  Message[] getResponsesAsync(int numWaitingFor, int timeout, int timeLimit) {
    return waitForResponses(asyncResponses, numWaitingFor, timeout, timeLimit);
  }

  private void doSendMessage(InetSocketAddress serverAddress, Message message, int checkInterval, LinkedBlockingQueue<Message> responses, int timeLimit) {
    final ClientSocketHandler csh = new ClientSocketHandler(serverAddress, message, checkInterval, responses, timeLimit);
    clientSocketHandlers.add(csh);
    socketPool.execute(csh);
  }

//todo: determine how to link responses with the server that sent them; need to add server id + message id in server to link response to its server index?
  /**
   * Wait until we have received all responses or until we hit the timeLimit.
   * <p>
   * Note that responses will come in a random order.
   *
   * @param responseQueue  queue to poll for responses
   * @param numWaitingFor  the number of responses we're waiting for
   * @param timeout        time to wait while polling the queue
   * @param timeLimit      maximum number of millis to wait for all responses.
   *
   * @return the responses
   */
  private Message[] waitForResponses(final LinkedBlockingQueue<Message> responseQueue, final int numWaitingFor, final int timeout, final int timeLimit) {
    Message[] result = null;

//    final AtomicInteger nextResultIndex = new AtomicInteger(0);

    // start the response queue listener
    try {
      final Future<Message[]> futureResult
        = responseQueueThread.submit(new Callable<Message[]>() {
          public Message[] call() {
            final Message[] responses = new Message[numWaitingFor];
            int nextResultIndex = 0;
            final long startTime = System.currentTimeMillis();
            long curTime = 0L;
            while (nextResultIndex < numWaitingFor) {
              final Message response = getNextResponse(responseQueue, timeout);
              if (response != null) {
                responses[nextResultIndex++] = response;
              }
              curTime = System.currentTimeMillis();
              if ((curTime - startTime) >= timeLimit) {
                System.out.println("NodeClient ran out of time! (start=" + startTime + ", cur=" + curTime + ", delta=" + (curTime - startTime) + ", timeLimit=" + timeLimit + ", timeout=" + timeout + ", numWaitingFor=" + numWaitingFor + ", nextResultIndex=" + nextResultIndex);
                break;
              }
            }
            return responses;
          }
        });

      result = futureResult.get();
    }
    catch (InterruptedException ie) {
      //todo: ...
    }
    catch (ExecutionException ee) {
      //todo: ...
      ee.printStackTrace(System.err);
    }

    for (Iterator<ClientSocketHandler> it = clientSocketHandlers.iterator(); it.hasNext(); ) {
      final ClientSocketHandler csh = it.next();
      if (csh == null || !csh.isRunning()) it.remove();
    }

    return result;
  }

  private Message getNextResponse(LinkedBlockingQueue<Message> responseQueue, int timeout) {
    Message result = null;
    try {
      result = responseQueue.poll(timeout, TimeUnit.MILLISECONDS);
    }
    catch (InterruptedException ie) {
      stayAlive.set(false);
    }

    return result;
  }
}
