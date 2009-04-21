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
package org.sd.cluster.config;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.sd.cluster.io.Response;

/**
 * JUnit Tests for the Shutdown class.
 * <p>
 * @author Spence Koehler
 */
public class TestShutdown extends TestCase {

  public TestShutdown(String name) {
    super(name);
  }
  
  private final void checkResponse(Response response) {
    assertNotNull(response);
  }

  private final void checkResponses(Response[] responses, int expectedCount) {
    assertNotNull(responses);
    assertEquals(expectedCount, responses.length);

    for (Response response : responses) {
      assertNotNull(response);
    }
  }

  public void testShutdown() throws Exception {
    final VirtualCluster cluster = new VirtualCluster("TestShutdown", "test-5m7n.1-2-4", "TestShutdown");
    final ShutdownMessage shutdown = new ShutdownMessage(0);
    final Ping ping = new Ping();

    try {
      cluster.start();
      final Console console = cluster.getConsole();

      checkResponses(console.sendMessageToNodes(ping, ClusterDefinition.ALL_NODES_GROUP, 500, true), 7);
      checkResponses(console.sendMessageToNodes(shutdown, ClusterDefinition.ALL_NODES_GROUP, 500, true), 7);

      // wait for nodes to shutdown.
      try {
        Thread.sleep(shutdown.getCountdown() + 1000);
      }
      catch (InterruptedException e) {
      }

      try {
        final Response[] responses = console.sendMessageToNodes(ping, ClusterDefinition.ALL_NODES_GROUP, 500, true);
        fail("expected an exception when sending to downed nodes.");
      }
      catch (ClusterException e) {
        // expected this to happen. success.
      }

      try {
        console.sendMessageToNodes(shutdown, ClusterDefinition.ALL_NODES_GROUP, 500, true);
        fail("expected an exception when sending to downed nodes.");
      }
      catch (ClusterException e) {
        // expected this to happen. success.
      }
    }
    finally {
      cluster.shutdown();
    }
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestShutdown.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
