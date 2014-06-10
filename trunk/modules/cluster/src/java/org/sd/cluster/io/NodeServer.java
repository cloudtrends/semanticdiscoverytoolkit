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
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.sd.util.MathUtil;
import org.sd.util.StatsAccumulator;

/**
 * Node server.
 * <p>
 * Multithreaded management of sockets and incoming messages from a NodeClient over a port.
 * <p>
 * Starts the server thread to fill the queue and message handler threads by
 * polling the message queue.
 *
 * @author Spence Koehler
 */
public class NodeServer extends Thread implements NodeServerMXBean {

  /**
   * Identifying prefix string for instances.
   */
  public static final String PREFIX_STRING = "NodeServer";

  /**
   * Maximum shutdown latency, or time between shutting down the server and its thread dying. In millis.
   */
  public static final int SHUTDOWN_LATENCY = 1000;


  private InetSocketAddress mySocketAddress;

  private final Context context;
  private int serverId;
  private String nodeName;
  private LinkedBlockingQueue<MessageBundle> messageQueue;
  private ExecutorService serverThread;         // waits for socket connections
  private ExecutorService socketPool;           // performs socket I/O
  private ExecutorService messageQueueThread;   // polls message queue for messages
  private ExecutorService messageHandlerPool;   // handles messages

  private static final AtomicInteger nextServerId = new AtomicInteger(0);
  private final AtomicBoolean stayAlive = new AtomicBoolean(true);
  private final AtomicBoolean handling = new AtomicBoolean(true);
  private final AtomicBoolean accepting = new AtomicBoolean(true);
  private final AtomicInteger socketThreadId = new AtomicInteger(0);
  private final AtomicInteger messageHandlerThreadId = new AtomicInteger(0);

  private int numSocketThreads;
  private int numMessageHandlerThreads;
  private long starttime;
  private long endtime;

  private Timer severedConnectionsTimer;
  private AtomicBoolean reported = new AtomicBoolean(true);

  private final Object statsMutex = new Object();
  private final Object socketStatsMutex = new Object();
  private StatsAccumulator totalTimeStats;
  private StatsAccumulator socketPreResponseStats;    // time spent waiting to be handled
  private StatsAccumulator socketPostResponseStats;  // time spent closing the socket
  private StatsAccumulator socketOverheadStats;      // total socket lifetime not spent in responding
  private StatsAccumulator messageQueuingStats;      // time spent adding message to queue for handling
  private StatsAccumulator receiveTimeStats;
  private StatsAccumulator responseGenTimeStats;
  private StatsAccumulator sendTimeStats;
  private StatsAccumulator handleTimeStats;
  private long numDroppedConnections;
  private AtomicLong numSeveredConnections = new AtomicLong(0L);
  private AtomicLong numBadMessages = new AtomicLong(0L);
  // private StatsAccumulator numInBytesStats;
  // private StatsAccumulator numOutBytesStats;

  private final Object socketPoolMutex = new Object();
  private final Object messagePoolMutex = new Object();

  /**
   * Construct a node server.
   *
   * @param context                   The context for this server.
   * @param mySocketAddress           The server's socket address for receiving connections.
   * @param numSocketThreads          Number of threads to pool for managing sockets.
   * @param numMessageHandlerThreads  Number of threads to handle messages.
   */
  public NodeServer(Context context, InetSocketAddress mySocketAddress, int numSocketThreads, int numMessageHandlerThreads) {
    super(NodeUtil.buildNodeName(PREFIX_STRING, context.getName(), mySocketAddress.toString(), nextServerId.get()));
    this.context = context;
    this.serverId = nextServerId.getAndIncrement();
    this.nodeName = NodeUtil.buildNodeName(PREFIX_STRING, context.getName(), mySocketAddress.toString(), serverId);
    this.mySocketAddress = mySocketAddress;

    // initialize stats
    this.totalTimeStats = new StatsAccumulator("TotalTime");
    this.socketPreResponseStats = new StatsAccumulator("SocketPrePresponseTime");
    this.socketPostResponseStats = new StatsAccumulator("SocketPostResponseTime");
    this.socketOverheadStats = new StatsAccumulator("SocketOverheadTime");
    this.messageQueuingStats = new StatsAccumulator("MessageQueueingTime");
    this.receiveTimeStats = new StatsAccumulator("ReceiveTime");
    this.responseGenTimeStats = new StatsAccumulator("ResponseGenTime");
    this.sendTimeStats = new StatsAccumulator("SendTimeStats");
    this.handleTimeStats = new StatsAccumulator("HandleTimeStats");
    this.numDroppedConnections = 0L;
    // this.numInBytesStats = new StatsAccumulator("NumInBytesStats");
    // this.numOutBytesStats = new StatsAccumulator("NumOutBytesStats");

    // initialize stable queues
    this.messageQueue = new LinkedBlockingQueue<MessageBundle>();
    this.serverThread
      = Executors.newSingleThreadExecutor(
        new ThreadFactory() {
          public Thread newThread(Runnable r) {
            return new Thread(r, nodeName + "-ServerThread");
          }
        });
    this.messageQueueThread
      = Executors.newSingleThreadExecutor(
        new ThreadFactory() {
          public Thread newThread(Runnable r) {
            return new Thread(r, nodeName + "-MessageQueueThread");
          }
        });

    this.severedConnectionsTimer = new Timer("severed connections timer");
    // schedule the timers to start reporting on the next calendar date at 6am
    Calendar calendar  = Calendar.getInstance();
    calendar.add(Calendar.DATE,1);
    calendar.set(Calendar.HOUR_OF_DAY,6);
    calendar.set(Calendar.MINUTE,00);
    calendar.set(Calendar.SECOND,00);
    severedConnectionsTimer.scheduleAtFixedRate(new SeveredConnectionsTimerTask(),
                                                calendar.getTime(), 86400000 /* every day */);

    // initialize thread pools
    this.starttime = System.currentTimeMillis();
    this.socketPool = null;
    this.messageHandlerPool = null;
    this.numSocketThreads = 0;
    this.numMessageHandlerThreads = 0;
    init(numSocketThreads, numMessageHandlerThreads);
  }

  private final void init(int numSocketThreads, int numMessageHandlerThreads) {
    numSocketThreads = Math.max(numSocketThreads, 1);                  // need at least 1
    numMessageHandlerThreads = Math.max(numMessageHandlerThreads, 1);  // need at least 1

    if (this.socketPool == null || numSocketThreads != this.numSocketThreads) {
      accepting.set(false);
      synchronized (socketPoolMutex) {
        if (this.socketPool != null) {
          System.out.println(new Date() + ": " + nodeName + " shutting down socket pool for reset.");
          this.socketPool.shutdown();  // nicely finish what it's working on. todo: put this in an alternate thread?
          System.out.println(new Date() + ": " + nodeName + " socket pool shutdown for reset complete.");
        }
        this.socketPool
          = Executors.newFixedThreadPool(
            numSocketThreads,
            new ThreadFactory() {
              public Thread newThread(Runnable r) {
                return new Thread(r, nodeName + "-Socket-" + socketThreadId.getAndIncrement());
              }
            });
      }
      accepting.set(true);
    }

    if (this.messageHandlerPool == null || numMessageHandlerThreads != this.numMessageHandlerThreads) {
      handling.set(false);
      synchronized (messagePoolMutex) {
        if (this.messageHandlerPool != null) {
          System.out.println(new Date() + ": " + nodeName + " shutting down message handler pool for reset.");
          this.messageHandlerPool.shutdown();  // nicely finish what it's working on. todo: put this in an alternate thread?
          System.out.println(new Date() + ": " + nodeName + " message handler pool shutdown for reset complete.");
        }
        this.messageHandlerPool
          = Executors.newFixedThreadPool(
            numMessageHandlerThreads,
            new ThreadFactory() {
              public Thread newThread(Runnable r) {
                return new Thread(r, nodeName + "-MessageHandler-" + messageHandlerThreadId.getAndIncrement());
              }
            });
      }
      handling.set(true);
    }

    this.numSocketThreads = numSocketThreads;
    this.numMessageHandlerThreads = numMessageHandlerThreads;
  }

  /**
   * Get the duration of time (milliseconds) that this server has been "up".
   */
  public long getUpTime() {
    final long curtime = endtime > 0 ? endtime : System.currentTimeMillis();
    return curtime - starttime;
  }

  /**
   * Get the duration of time which this server has been "up" as a human readable
   * string.
   */
  public String getUpTimeString() {
    return MathUtil.timeString(getUpTime(), false);
  }

  /**
   * Get the number of socket threads for this server.
   */
  public int getNumSocketThreads() {
    return numSocketThreads;
  }

  /**
   * Get the number of message handler threads for this server.
   */
  public int getNumMessageHandlerThreads() {
    return numMessageHandlerThreads;
  }

  /**
   * Get the current size of the message queue.
   */
  public int getMessageQueueSize() {
    return messageQueue.size();
  }

  /**
   * Get this node server's name.
   */
  public String getServerName() {
    return nodeName;
  }

  /**
   * Determine whether this node is currently "up".
   */
  public boolean isUp() {
    return stayAlive.get();
  }

  /**
   * Reset this server with the given numbers of threads.
   *
   * @param numSocketThreads  The new number of threads for listening on sockets
   *                          (leave unchanged if &lt;= 0).
   * @param numMessageHandlerThreads  The new number of message handler threads
   *                          (leave unchanged if &lt;= 0).
   * @param resetStats  If true, then the stats will all be reset.
   */
  public void reset(int numSocketThreads,  int numMessageHandlerThreads,  boolean resetStats) {

    // reset stats
    if (resetStats) {
      this.totalTimeStats.clear();
      this.socketPreResponseStats.clear();
      this.socketPostResponseStats.clear();
      this.socketOverheadStats.clear();
      this.messageQueuingStats.clear();
      this.receiveTimeStats.clear();
      this.responseGenTimeStats.clear();
      this.sendTimeStats.clear();
      this.handleTimeStats.clear();
      this.numDroppedConnections = 0L;
      this.numSeveredConnections.set(0L);
      this.numBadMessages.set(0L);
      // this.numInBytesStats.clear();
      // this.numOutBytesStats.clear();

      //this.starttime = System.currentTimeMillis();
    }

    // rebuild thread pools
    if (numSocketThreads > 0 || numMessageHandlerThreads > 0) {
      init(numSocketThreads > 0 ? numSocketThreads : this.numSocketThreads,
           numMessageHandlerThreads > 0 ? numMessageHandlerThreads : this.numMessageHandlerThreads);
    }
  }

  /**
   * Pause handling messages, but keep accepting and responding to them.
   * <p>
   * Note that the message queue will fill with uncoming requests while paused.
   */
  public void pauseHandling() {
    handling.compareAndSet(true, false);
  }

  /**
   * Resume handling messages from the queue.
   */
  public void resumeHandling() {
    handling.compareAndSet(false, true);
  }

  /**
   * Determine whether the server is currently handling or will handle messages.
   */
  public boolean isHandling() {
    return handling.get();
  }

  /**
   * Pause accepting messages, but keep handling those from the queue.
   */
  public void pauseAccepting() {
    accepting.compareAndSet(true, false);
  }

  /**
   * Resume accepting messages.
   */
  public void resumeAccepting() {
    accepting.compareAndSet(false, true);
  }

  /**
   * Determine whether the server is currently accepting messages.
   */
  public boolean isAccepting() {
    return accepting.get();
  }

  /**
   * Get the overall stats for the time, in millis, to service requests.
   * <p>
   * This is the sum of receive, responseGen, and send time stats and
   * does <b>not</b> include handle time stats.
   * <p>
   * NOTE: Time is measured from the reception of a request to the sending
   *       of a response.
   */
  public StatsAccumulator getServerTimeStats() {
    return totalTimeStats;
  }

  /**
   * Get the stats for the time, in millis, between accepting a socket and
   * beginning to read from it.
   * <p>
   * Note that this reflects the time spent waiting in the queue for a
   * SocketHandler thread.
   */
  public StatsAccumulator getSocketPreResponseTime() {
    return socketPreResponseStats;
  }

  /**
   * Get the stats for the time, in millis, between writing to a socket and
   * closing it.
   */
  public StatsAccumulator getSocketPostResponseTime() {
    return socketPostResponseStats;
  }

  /**
   * Get the stats for the time, in millis, between accepting a socket and
   * closing it, discounted by the time spent reading, processing, and writing.
   */
  public StatsAccumulator getSocketOverheadTime() {
    return socketOverheadStats;
  }

  /**
   * Get the stats for the time, in millis, between closing a socket and adding
   * the received message to the message queue to be asynchronously handled.
   */
  public StatsAccumulator getMessageQueuingTime() {
    return messageQueuingStats;
  }


  /**
   * Get the stats for the time, in millis, to receive requests.
   */
  public StatsAccumulator getReceiveTimeStats() {
    return receiveTimeStats;
  }

  /**
   * Get the stats for the time, in millis, to generate responses.
   */
  public StatsAccumulator getResponseGenTimeStats() {
    return responseGenTimeStats;
  }

  /**
   * Get the stats for the time, in millis, to send responses.
   */
  public StatsAccumulator getSendTimeStats() {
    return sendTimeStats;
  }

  /**
   * Get the stats for the time, in millis, to handle messages.
   */
  public StatsAccumulator getHandleTimeStats() {
    return handleTimeStats;
  }

  /**
   * Get the number of dropped connections due to saturation.
   */
  public long getNumDroppedConnections() {
    return numDroppedConnections;
  }

  /**
   * Get the number of connections dropped by the client before the server dropped them.
   */
  public long getNumSeveredConnections() {
    return numSeveredConnections.get();
  }

  /**
   * Get the number of bad message attempts made to this server.
   */
  public long getNumBadMessages() {
    return numBadMessages.get();
  }

//we can't really count the bytes at this level w/out too much overhead
  // /**
  //  * Get the stats for the request bytes received.
  //  */
  // public StatsAccumulator getNumInBytesStats() {
  //   return numInBytesStats;
  // }

  // /**
  //  * Get the stats for the response bytes returned.
  //  */
  // public StatsAccumulator getNumOutBytesStats() {
  //   return numOutBytesStats;
  // }


  public void run() {
//    System.out.println("Starting server: " + nodeName);

    ServerSocket serverSocket = null;

    try {
      serverSocket = new ServerSocket(mySocketAddress.getPort());
      serverSocket.setSoTimeout(SHUTDOWN_LATENCY);  // stop waiting to accept periodically
    }
    catch (IOException e) {
      //todo: determine what to do here... can't listen on the port!
      System.err.println("mySocketAddress=" + mySocketAddress);
      e.printStackTrace(System.err);
    }

    // start the socket listener
    if (stayAlive.get()) serverThread.execute(new SocketListener(serverSocket));

    // start the message queue listener
    if (stayAlive.get()) {
      messageQueueThread.execute(new Runnable() {
          public void run() {
            while (stayAlive.get()) {
              if (handling.get()) {
                final MessageBundle messageBundle = getNextMessageBundle(500);
                if (messageBundle != null) handleMessageBundle(messageBundle);
              }
              else {
                try {
                  Thread.sleep(500);
                }
                catch (InterruptedException ignore) {}
              }
            }
          }
        });
    }

    while (stayAlive.get()) {
      synchronized (this) {
        final long starttime = System.currentTimeMillis();

        try {
          wait(SHUTDOWN_LATENCY);
        }
        catch (InterruptedException ie) {
          final long endtime = System.currentTimeMillis();
          System.out.println(new Date(endtime) + ": WARNING: NodeServer.run.wait(" + SHUTDOWN_LATENCY +
                             ") interrupted! (after " + (endtime - starttime) + " millis)");
          ie.printStackTrace(System.out);
//          shutdown(true);
        }
      }
    }
  }

  /**
   * Shutdown this node server.
   * <p>
   * Note that the node server will not be able to come back up after being
   * shutdown.
   *
   * @param now  If true, then performa a more aggressive shutdown, not
   *             necessarily waiting for threads to die nicely.
   */
  public void shutdown(boolean now) {
    if (stayAlive.compareAndSet(true, false)) {
      if (now) {
        serverThread.shutdownNow();
        synchronized (socketPoolMutex) {
          socketPool.shutdownNow();
        }
        messageQueueThread.shutdownNow();
        synchronized (messagePoolMutex) {
          messageHandlerPool.shutdownNow();
        }
      }
      else {
        // shutdown server thread so no new connections are made
        serverThread.shutdown();

        // wait for message

        synchronized (socketPoolMutex) {
          socketPool.shutdown();
        }
        messageQueueThread.shutdown();
        synchronized (messagePoolMutex) {
          messageHandlerPool.shutdown();
        }
      }
    }
    this.endtime = System.currentTimeMillis();
  }

  private MessageBundle getNextMessageBundle(int timeout) {
    MessageBundle result = null;

    long starttime = System.currentTimeMillis();
    int numRetries = 100;

    while (stayAlive.get() && result == null && numRetries > 0) {
      try {
        result = messageQueue.poll(timeout, TimeUnit.MILLISECONDS);
      }
      catch (InterruptedException ie) {
        final long endtime = System.currentTimeMillis();
        System.out.println(new Date(endtime) + ": WARNING: NodeServer.getNextMessage(" + timeout +
                           ") interrupted! (after " + (endtime - starttime) + " millis) [numRetries=" + numRetries + "]");
        ie.printStackTrace(System.out);
        starttime = System.currentTimeMillis();  // reset the clock
        --numRetries;
//        stayAlive.set(false);
      }
    }

    return result;
  }

  private final void addHandledStat(long handledTime) {
    synchronized (handleTimeStats) {
      handleTimeStats.add(handledTime);
    }
  }

  private void handleMessageBundle(final MessageBundle messageBundle) {
    if (stayAlive.get()) {
      boolean handled = false;
      int tryCount = 0;
      while (stayAlive.get() && tryCount < 100) {
        try {
          synchronized (messagePoolMutex) {
            messageHandlerPool.execute(new Runnable() {
                public void run() {
                  final long starttime = System.currentTimeMillis();
                  messageBundle.message.handle(context, messageBundle.connectionContext);
                  addHandledStat(System.currentTimeMillis() - starttime);
                }
              });
          }
          handled = true;
          break;
        }
        catch (RejectedExecutionException e) {
          Thread.yield();
//           try {
//             Thread.sleep(100);
//           }
//           catch (InterruptedException stop) {break;}
          ++tryCount;
        }
      }
      if (!handled) {
        final String msgstr =
          (messageBundle == null || messageBundle.message == null) ? "null" :
          messageBundle.message.toString();

        throw new IllegalStateException("Couldn't handle message '" +
                                        msgstr + "'! " + tryCount + " failures.");
      }
    }
  }

  /**
   * Build an error message for the log for intermittently reported stats.
   * <p>
   * @return null if there is no message.
   */
  private final String buildErrorMessage() {
    if (!reported.get() && (numSeveredConnections.get() > 0L || numBadMessages.get() > 0L)) {
      reported.set(true);
    }
    else {
      return null;
    }

    final StringBuilder result = new StringBuilder();

    result.append(new Date());
    result.append(": WARNING NodeServer had");
    if (numSeveredConnections.get() > 0L) {
      result.append(' ').
        append(numSeveredConnections).
        append(" response connections dropped by clients");
    }
    if (numBadMessages.get() > 0L) {
      result.append(' ').
        append(numBadMessages).
        append(" ill-formed messages ignored");
    }

    return result.toString();
  }

  private final class SocketListener implements Runnable {
    private ServerSocket serverSocket;

    public SocketListener(ServerSocket serverSocket) {
      this.serverSocket = serverSocket;
    }

    public void run() {
      Socket socket = null;

      if (serverSocket == null) {
        shutdown(true);
        return;
      }

      long socketAcceptTime = 0L;
      while (stayAlive.get()) {
//        System.out.println(nodeName + "-SocketListener -- accepting...");
        try {
          if (accepting.get()) {
            socket = serverSocket.accept();
            socketAcceptTime = System.currentTimeMillis();
          }
          else {
            Thread.sleep(500);  // pause while we're not accepting connections
          }
//          System.out.println(nodeName + " ACCEPTED socket!");
        }
        catch (SocketTimeoutException e) {
          if (!stayAlive.get()) {
            shutdown(true);
          }
        }
        catch (IOException e) {
          //todo: determine what to do here...
          e.printStackTrace(System.err);
          shutdown(true);
        }
        catch (InterruptedException ignore) {}

        if (socket != null) {
          final SocketTimingData socketTimingData = new SocketTimingData(socketAcceptTime);

          // start a socket thread
          synchronized (socketPoolMutex) {
            try {
              socketPool.execute(new SocketHandler(socket, socketTimingData));
            }
            catch (RejectedExecutionException e) {
              ++numDroppedConnections;

              // connecting clients that get an empty response should try back later.
              System.err.println(new Date() +
                                 ": WARNING : NodeServer socketPool saturated (" +
                                 numSocketThreads + "). Dropping connection #" +
                                 numDroppedConnections + ".");

              try {
                // be nice and close the socket
                socket.close();
              }
              catch (IOException ioe) {
                //ignore
              }
            }
          }
          socket = null;
        }
        else {
//          System.out.println(nodeName + " NO socket!");
        }
      }
    }
  }
  
  private final void addStats(long receiveTime, long responseGenTime, long sendTime/*, long numInBytes, long numOutBytes*/) {
    final long totalTime = receiveTime + responseGenTime + sendTime;
    synchronized (statsMutex) {
      totalTimeStats.add(totalTime);
      receiveTimeStats.add(receiveTime);
      responseGenTimeStats.add(responseGenTime);
      sendTimeStats.add(sendTime);
      // numInBytesStats.add(numInBytes);
      // numOutBytesStats.add(numOutBytes);
    }
  }

  private final void addStats(SocketTimingData socketTimingData) {
    synchronized (socketStatsMutex) {
      doAddStats(socketPreResponseStats, socketTimingData.getSocketPreResponseTime());
      doAddStats(socketPostResponseStats, socketTimingData.getSocketPostResponseTime());
      doAddStats(socketOverheadStats, socketTimingData.getSocketOverheadTime());
      doAddStats(messageQueuingStats, socketTimingData.getMessageQueuingTime());
    }
  }

  private final void doAddStats(StatsAccumulator stats, long value) {
    if (value > 0) {
      stats.add(value);
    }
  }

  private final class SocketHandler implements Runnable {
    private Socket socket;
    private SocketTimingData socketTimingData;

    public SocketHandler(Socket socket, SocketTimingData socketTimingData) {
      this.socket = socket;
      this.socketTimingData = socketTimingData;
    }

    public void run() {
      socketTimingData.setResponseStartTime(System.currentTimeMillis());

      SocketIO socketIO = null;
      Message message = null;
      ConnectionContext connectionContext = null;

      try {
        // receive message over socket
        socketIO = new SocketIO(socket);
        final DataOutputStream dataOut = socketIO.getDataOutput();
        final DataInputStream dataIn = socketIO.getDataInput();
        if (dataOut != null && dataIn != null) {
          // get message, send response, put message into message queue
          connectionContext = new ConnectionContext(socket);
          final Messenger messenger = new Messenger(dataOut, dataIn);
          message = messenger.receiveMessage(context, connectionContext);  // does both receive and response

          socketTimingData.setResponseEndTime(System.currentTimeMillis());

          if (message == null) {
            // count/log "bad" messages
            numBadMessages.incrementAndGet();
            reported.set(false);
          }
          else {
            // tally stats
            addStats(messenger.getReceiveTime(), messenger.getResponseGenTime(), messenger.getSendTime()
                     /*,messenger.getNumInBytes(), messenger.getNumOutBytes*/);
          }
        }
      }
      catch (ConnectionSeveredException cse) {
        numSeveredConnections.incrementAndGet();
        reported.set(false);
      }
      catch (IOException e) {
        //todo: determine what to do here... failed receiving message/sending response
        e.printStackTrace(System.err);
      }
      finally {
        try {
          if (socketIO != null) socketIO.close();
          socket.close();

          socketIO = null;
          socket = null;
        }
        catch (IOException e) {
          //todo: determine what to do here... failed closing socketIO
          e.printStackTrace(System.err);
        }
      }

      socketTimingData.setSocketClosedTime(System.currentTimeMillis());

      // Add message to queue for handling
      if (message != null && connectionContext != null) {
        //todo: split up receiving message and sending response so that if the message
        //      queue is full we can report that in the response to the client.
        //todo: set an upper-limit queue size and use messageQueue.offer instead of add.
        messageQueue.add(new MessageBundle(message, connectionContext));
      }

      socketTimingData.setMessageQueuedTime(System.currentTimeMillis());

      addStats(socketTimingData);
    }
  }

  private final class MessageBundle {
    public final Message message;
    public final ConnectionContext connectionContext;

    public MessageBundle(Message message, ConnectionContext connectionContext) {
      this.message = message;
      this.connectionContext = connectionContext;
    }
  }

  private final class SeveredConnectionsTimerTask extends TimerTask
  {
    public boolean cancel()
    {
      final String logMessage = buildErrorMessage();

      if (logMessage != null) {
        System.err.println(logMessage);
      }

      return true;
    }

    // run the thread, checking the server for any severed connections 
    // and log a warning if necessary
    public void run()
    {
      final String logMessage = buildErrorMessage();

      if (logMessage != null) {
        System.err.println(logMessage);
      }
    }
  }

  private final class SocketTimingData {
    private long socketAcceptTime;
    private long socketResponseStartTime;
    private long socketResponseEndTime;
    private long socketClosedTime;
    private long messageQueuedTime;

    SocketTimingData(long socketAcceptTime) {
      this.socketAcceptTime = socketAcceptTime;
    }

    final void setResponseStartTime(long socketResponseStartTime) {
      this.socketResponseStartTime = socketResponseStartTime;
    }

    final void setResponseEndTime(long socketResponseEndTime) {
      this.socketResponseEndTime = socketResponseEndTime;
    }

    final void setSocketClosedTime(long socketClosedTime) {
      this.socketClosedTime = socketClosedTime;
    }

    final void setMessageQueuedTime(long messageQueuedTime) {
      this.messageQueuedTime = messageQueuedTime;
    }

    final long getSocketPreResponseTime() {
      long result = -1;
      if (socketResponseStartTime > 0 && socketAcceptTime > 0) {
        result = socketResponseStartTime - socketAcceptTime;
      }
      return result;
    }

    final long getSocketPostResponseTime() {
      long result = -1;
      if (socketClosedTime > 0 && socketResponseEndTime > 0) {
        result = socketClosedTime - socketResponseEndTime;
      }
      return result;
    }

    final long getSocketOverheadTime() {
      long result = -1;
      if (socketClosedTime > 0 && socketAcceptTime > 0 && socketResponseEndTime > 0 && socketResponseStartTime > 0) {
        result = (socketClosedTime - socketAcceptTime) - (socketResponseEndTime - socketResponseStartTime);
      }
      return result;
    }

    final long getMessageQueuingTime() {
      long result = -1;
      if (messageQueuedTime > 0 && socketClosedTime > 0) {
        result = messageQueuedTime - socketClosedTime;
      }
      return result;
    }
  }
}
