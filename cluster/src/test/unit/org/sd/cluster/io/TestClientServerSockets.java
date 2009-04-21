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


import junit.framework.Test;
import junit.framework.TestSuite;
import org.sd.testtools.BaseTestCase;
import org.sd.testtools.PortServer;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import java.io.IOException;

/**
 * JUnit Tests for the SocketServer class.
 * <p>
 * @author Spence Koehler
 */
public class TestClientServerSockets extends BaseTestCase {

  public TestClientServerSockets(String name) {
    super(name);
  }
  
  /**
   * Start up the server, then connect with a client.
   */
  public void testRoundTripWhileServerWaits() throws UnknownHostException, IOException, InterruptedException {
    NodeServer server = null;
    NodeClient client = null;

    try {
      final int port = PortServer.getInstance().getNextTestPort();
      final InetSocketAddress serverAddress = new InetSocketAddress(InetAddress.getLocalHost(), port);

      // start listening on server
      server = new NodeServer(new SimpleContext("testRoundTripWhileServerWaits"), serverAddress, 1, 1);
      server.start();
    
      // start a client
      client = new NodeClient("testRoundTripWhileServerWaits", serverAddress.getAddress(), 1);
      client.start();

      // validate "up" server state
      assertTrue(server.isUp());
      assertTrue(client.isUp());

      // do message round trip
      final Message message = new TestMessenger.SimpleMessage((byte)123, true, 456, "789", 10.1112, "zzz");
      final Message response = client.sendMessage(serverAddress, message, 10, 1000, 1000);

      assertEquals(message, response);

      // shutdown the server
      server.shutdown(false);
      assertFalse(server.isUp());

      // shutdown the client
      client.shutdown(false);
      assertFalse(client.isUp());
    }
    finally {
      // make sure both server and client are shutdown
      if (server != null) server.shutdown(true);
      if (client != null) client.shutdown(true);
    }
  }

  /**
   * Start attempting to connect with a client, then start up the server.
   */
  public void testRoundTripWhileClientWaits() throws UnknownHostException, IOException {
    NodeServer server = null;
    NodeClient client = null;

    try {
      final int port = PortServer.getInstance().getNextTestPort();
      final InetSocketAddress serverAddress = new InetSocketAddress(InetAddress.getByName("localhost"), port);

      // start a client
      client = new NodeClient("testRoundTripWhileClientWaits", serverAddress.getAddress(), 1);
      client.start();

      final Message message = new TestMessenger.SimpleMessage((byte)32, true, 2006, "testing123", 3.1415926535, "zzz");
      client.sendMessageAsync(serverAddress, message, 10);

      // start listening on server
      server = new NodeServer(new SimpleContext("testRoundTripWhileClientWaits"), serverAddress, 1, 1);
      server.start();

      // get response from server
      final Message response = client.getResponseAsync(1000, 1000);
      assertEquals(message, response);

      // shutdown the server
      server.shutdown(false);
      assertFalse(server.isUp());

      // shutdown the client
      client.shutdown(false);
      assertFalse(client.isUp());
    }
    finally {
      // make sure both server and client are shutdown
      if (server != null) server.shutdown(true);
      if (client != null) client.shutdown(true);      
    }
  }

  public void testMultipleServersSamePort() throws UnknownHostException, IOException {
    NodeServer server1 = null;
    NodeServer server2 = null;
    NodeClient client = null;

    try {
      final int port = PortServer.getInstance().getNextTestPort();
      final InetSocketAddress serverAddress = new InetSocketAddress(InetAddress.getLocalHost(), port);

      // start listening on server(s)
      server1 = new NodeServer(new SimpleContext("testMultipleServersSamePort-1"), serverAddress, 1, 1);
      server1.start();

      server2 = new NodeServer(new SimpleContext("testMultipleServersSamePort-2"), serverAddress, 1, 1);
      server2.start();

      // start a client
      client = new NodeClient("testMultipleServersSamePort", serverAddress.getAddress(), 1);
      client.start();

      // do message round trip
      final Message message = new TestMessenger.SimpleMessage((byte)123, true, 456, "789", 10.1112, "zzz");
      final Message response = client.sendMessage(serverAddress, message, 10, 1000, 1000);

      assertEquals(message, response);

      server1.shutdown(false);
      server2.shutdown(false);
      client.shutdown(false);

      assertFalse(server1.isUp());
      assertFalse(server2.isUp());
    }
    finally {
      // make sure both server and client are shutdown
      if (server1 != null) server1.shutdown(true);
      if (server2 != null) server2.shutdown(true);
      if (client != null) client.shutdown(true);
    }
  }

  public void testMultipleClientsSameServer() throws UnknownHostException, IOException {
    NodeServer server = null;
    NodeClient client1 = null;
    NodeClient client2 = null;

    try {
      final int port = PortServer.getInstance().getNextTestPort();
      final InetSocketAddress serverAddress = new InetSocketAddress(InetAddress.getLocalHost(), port);

      // start up a server
      server = new NodeServer(new SimpleContext("testMultipleClientsSameServer"), serverAddress, 2, 2);
      server.start();

      // start up 2 clients
      client1 = new NodeClient("testMultipleClientsSameServer-1", serverAddress.getAddress(), 1);
      client1.start();
      client2 = new NodeClient("testMultipleClientsSameServer-2", serverAddress.getAddress(), 1);
      client2.start();

      // send messages from clients
      final Message message1 = new TestMessenger.SimpleMessage((byte)123, true, 456, "789", 10.1112, "zzz");
      client1.sendMessageAsync(serverAddress, message1, 10);
      final Message message2 = new TestMessenger.SimpleMessage((byte)131, true, 415, "1617", 18.1920, "zzz");
      client2.sendMessageAsync(serverAddress, message2, 10);

      // get responses from server
      final Message response2 = client2.getResponseAsync(1000, 1000);
      assertEquals(message2, response2);
      final Message response1 = client1.getResponseAsync(1000, 1000);
      assertEquals(message1, response1);

      // shutdown the server
      server.shutdown(false);
      assertFalse(server.isUp());

      // shutdown the client
      client1.shutdown(false);
      assertFalse(client1.isUp());
      client2.shutdown(false);
      assertFalse(client2.isUp());
    }
    finally {
      // make sure both server and client are shutdown
      if (server != null) server.shutdown(true);      
      if (client1 != null) client1.shutdown(true);
      if (client2 != null) client2.shutdown(true);
    }
  }

  public void testSingleClientMultipleMessagesToServer() throws UnknownHostException, IOException {
    NodeServer server = null;
    NodeClient client = null;

    try {
      final int port = PortServer.getInstance().getNextTestPort();
      final InetSocketAddress serverAddress = new InetSocketAddress(InetAddress.getLocalHost(), port);

      // start up a server
      server = new NodeServer(new SimpleContext("testSingleClientMultipleMessagesToServer"), serverAddress, 2, 2);
      server.start();

      // start up a client
      client = new NodeClient("testSingleClientMultipleMessagesToServer", serverAddress.getAddress(), 2);
      client.start();

      // send messages from client
      final Message message1 = new TestMessenger.SimpleMessage((byte)123, true, 456, "789", 10.1112, "zzz");
      final Message response1 = client.sendMessage(serverAddress, message1, 10, 1000, 1000);
      final Message message2 = new TestMessenger.SimpleMessage((byte)131, true, 415, "1617", 18.1920, "zzz");
      final Message response2 = client.sendMessage(serverAddress, message2, 10, 1000, 1000);

      // check the responses
      assertEquals(message1, response1);
      assertEquals(message2, response2);

      server.shutdown(false);
      client.shutdown(false);
    }
    finally {
      if (server != null) server.shutdown(true);
      if (client != null) client.shutdown(true);
    }
  }

  // one client to many servers
  public void testMulticastFromSingleClient() throws UnknownHostException, IOException {
    NodeServer server1 = null;
    NodeServer server2 = null;
    NodeClient client = null;

    try {
      final int port1 = PortServer.getInstance().getNextTestPort();
      final int port2 = PortServer.getInstance().getNextTestPort();
      final InetSocketAddress serverAddress1 = new InetSocketAddress(InetAddress.getLocalHost(), port1);
      final InetSocketAddress serverAddress2 = new InetSocketAddress(InetAddress.getLocalHost(), port2);

      // start listening on server(s)
      server1 = new NodeServer(new SimpleContext("testMulticastFromSingleClient-1"), serverAddress1, 2, 2);
      server1.start();

      server2 = new NodeServer(new SimpleContext("testMulticastFromSingleClient-2"), serverAddress2, 2, 2);
      server2.start();

      // start a client
      client = new NodeClient("testMulticastFromSingleClient", serverAddress1.getAddress(), 1);
      client.start();

      // do message round trip
      final Message message1 = new TestMessenger.SimpleMessage((byte)123, true, 456, "789", 10.1112, "zzz");
      final Message message2 = new TestMessenger.SimpleMessage((byte)131, true, 415, "1617", 18.1920, "zzz");

      final Message[] responses1 = client.sendMessage(new InetSocketAddress[]{serverAddress1, serverAddress2}, message1, 10, 1000, 1000);
      final Message[] responses2 = client.sendMessage(new InetSocketAddress[]{serverAddress1, serverAddress2}, message2, 10, 1000, 1000);

      assertEquals(message1, responses1[0]);
      assertEquals(message1, responses1[1]);

      assertEquals(message2, responses2[0]);
      assertEquals(message2, responses2[1]);

      server1.shutdown(false);
      server2.shutdown(false);
      client.shutdown(false);

      assertFalse(server1.isUp());
      assertFalse(server2.isUp());
    }
    finally {
      // make sure both server and client are shutdown
      if (server1 != null) server1.shutdown(true);
      if (server2 != null) server2.shutdown(true);
      if (client != null) client.shutdown(true);
    }
  }

  // two clients to many servers
  public void testMulticastFromMultiClients() throws UnknownHostException, IOException {
    NodeServer server1 = null;
    NodeServer server2 = null;
    NodeClient client1 = null;
    NodeClient client2 = null;

    try {
      final int port1 = PortServer.getInstance().getNextTestPort();
      final int port2 = PortServer.getInstance().getNextTestPort();
      final InetSocketAddress serverAddress1 = new InetSocketAddress(InetAddress.getLocalHost(), port1);
      final InetSocketAddress serverAddress2 = new InetSocketAddress(InetAddress.getLocalHost(), port2);

      // start listening on server(s)
      server1 = new NodeServer(new SimpleContext("testMulticastFromMultiClients-1"), serverAddress1, 2, 2);
      server1.start();

      server2 = new NodeServer(new SimpleContext("testMulticastFromMultiClients-2"), serverAddress2, 2, 2);
      server2.start();

      // start two clients
      client1 = new NodeClient("testMulticastFromMultiClients-1", serverAddress1.getAddress(), 1);
      client1.start();
      client2 = new NodeClient("testMulticastFromMultiClients-2", serverAddress2.getAddress(), 1);
      client2.start();

      // do message round trip
      final Message message1 = new TestMessenger.SimpleMessage((byte)123, true, 456, "789", 10.1112, "zzz");
      final Message message2 = new TestMessenger.SimpleMessage((byte)131, true, 415, "1617", 18.1920, "zzz");

      client1.sendMessagesAsync(new InetSocketAddress[]{serverAddress1, serverAddress2}, message1, 10);
      client2.sendMessagesAsync(new InetSocketAddress[]{serverAddress1, serverAddress2}, message2, 10);

      final Message[] responses2 = client2.getResponsesAsync(2, 1000, 1000);
      final Message[] responses1 = client1.getResponsesAsync(2, 1000, 1000);

      assertEquals(message1, responses1[0]);
      assertEquals(message1, responses1[1]);

      assertEquals(message2, responses2[0]);
      assertEquals(message2, responses2[1]);

      server1.shutdown(false);
      server2.shutdown(false);
      client1.shutdown(false);
      client2.shutdown(false);
    }
    finally {
      // make sure both server and client are shutdown
      if (server1 != null) server1.shutdown(true);
      if (server2 != null) server2.shutdown(true);
      if (client1 != null) client1.shutdown(true);
      if (client2 != null) client2.shutdown(true);
    }
  }

  private final class SimpleContext implements Context {
    private String name;

    public SimpleContext(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestClientServerSockets.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
