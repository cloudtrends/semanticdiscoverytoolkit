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
 * JUnit Tests for the Ping class.
 * <p>
 * @author Spence Koehler
 */
public class TestPing extends TestCase {

  public TestPing(String name) {
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

  public void testPing() throws Exception {
    final VirtualCluster cluster = new VirtualCluster("TestPing", "test-5m7n.1-2-4", "TestPing");
    final Ping ping = new Ping();
    try {
      cluster.start();
      final Console console = cluster.getConsole();

      checkResponse(console.sendMessageToNode(ping, "node1", 500));
      checkResponse(console.sendMessageToNode(ping, "node1-0", 500));

      checkResponse(console.sendMessageToNode(ping, "node2-0", 500));
      checkResponse(console.sendMessageToNode(ping, "node4-0", 500));
      checkResponse(console.sendMessageToNode(ping, "node4-1", 500));
      checkResponse(console.sendMessageToNode(ping, "node5-1", 500));

      try {
        console.sendMessageToNode(ping, "node3-3", 500);
        fail("expected an exception when sending to a bad node.");
      }
      catch (ClusterException e) {
        // expected this to happen. success.
      }

      try {
        console.sendMessageToNode(ping, "node300", 500);
        fail("expected an exception when sending to a bad node.");
      }
      catch (ClusterException e) {
        // expected this to happen. success.
      }

// decided not to make this an error. "node4" will always mean "node4-0" as opposed to "node4-1"
//       try {
//         console.sendMessageToNode(ping, "node4", 500);
//         fail("expected an exception when sending to an underspecified node.");
//       }
//       catch (ClusterException e) {
//         // expected this to happen. success.
//       }

      checkResponses(console.sendMessageToNodes(ping, ClusterDefinition.ALL_NODES_GROUP, 500, true), 7);
      checkResponses(console.sendMessageToNodes(ping, "group1", 500, true), 1);
      checkResponses(console.sendMessageToNodes(ping, "group2", 500, true), 2);
      checkResponses(console.sendMessageToNodes(ping, "group3", 500, true), 4);

      try {
        console.sendMessageToNodes(ping, "nonexistent-group", 500, true);
        fail("expected an exception when sending to a bad group.");
      }
      catch (ClusterException e) {
        // expected this to happen. success.
      }

      try {
        console.sendMessageToNodes(ping, (String)null, 500, true);
        fail("expected an exception when sending to self.");
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
    TestSuite suite = new TestSuite(TestPing.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
