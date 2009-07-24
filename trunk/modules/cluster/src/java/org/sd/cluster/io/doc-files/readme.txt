
Client/Server low-level communications.

General concepts/notes:

- We expect to have a node client and a node server on every machine (real or virtual).
- In general processes invoked on a server are asynchronous with results pushed forward to other servers.
- Messages from a client to a server do include a synchronous send/response, but this should only be used for acknowledgement and status, not for performing the work.
- Servers wait for client connections.
- A client will wait for a connection to a server; a message can be successfully sent when the server happens to be started after the client sends it.

NodeClient

- A node client is a thread.
- Each node client has an id composed of a user-supplied name, the client's ip address, and its JVM instance count.
- A client has a pool of threads for connecting to node server sockets.
  - in general, the size of this pool should be based on the number of servers a client will send a message to up to some limit.
- A client has a single response queue thread for waiting for responses.
- A client sends a message instance to one or more servers.
  - each server is identified by an IP address and a port contained in an InetSocketAddress.
  - for each message sent, a response queue is set up for receiving responses.
    - responses are polled for at a "timeout" rate.
    - one response is collected per server or until a timeLimit is reached.
      - if timed out, the last response or responses in the result will be null.
    - responses are not necessarily received in the order that the servers are defined.
  - a socket connection is made with each server using a thread from the pool.
    - I/O over a connected socket is managed by a ClientSocketHandler, which waits for the server's response, placing it in the response queue.
  - the response queue thread pools the response queue, waiting until all responses to the message are received from the servers.
  - the responses are returned to the caller.

NodeServer

- A node server is a thread.
- Each node server has an id composed of a user-supplied name, the server's ip address, and its JVM instance count.
- A server has a socket listener thread that waits for client socket connections.
  - it listens in a loop, attempting to connect for a period of time (shutdown_latency) before looping.
  - when a connection is accepted, the thread runs a socket handler on a thread from the socket handler thread pool.
- A server has a socket handler pool of threads for managing client socket connections.
- A server socket handler runnable/thread uses a SocketIO instance to open and close socket streams around receiving a message and sending a response through a messenger.
- A server socket handler runnable/thread uses a Messenger to perform the I/O over the socket.
- A server socket handler thread places received messages in a message queue.
- A server has a message queue thread that polls the message queue for incoming messages.
  - When a message is received, the message is handled on a thread from the message handler thread pool.
- A server handles a message by invoking the message's handle method.

ClientSocketHandler

- A client socket handler connects to a server socket, delivers a message, and collects the response, placing them in a queue.
- A client socket handler is a runnnable, executed by a client on a thread.
- A client socket handler polls for a socket connection with a server at a checkInterval for the connection.
  - This enables the client to wait for a server to start or restart.
- A client socket handler uses a SocketIO instance to open and close socket streams around sending/receiving a message through a messenger.
- A client socket handler uses a Messenger to perform the I/O over the socket.

Messenger

- A messenger sends and receives messages over the input/output streams of a socket.

SocketIO

- A socket i/o wraps socket streams as data input/output streams.
- A socket i/o handles shutting down sockets and closing streams.
