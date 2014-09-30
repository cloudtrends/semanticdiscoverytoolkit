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

import org.sd.io.FileUtil;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * JUnit Tests for the ClusterDefinition class.
 * <p>
 * @author Spence Koehler
 */
public class TestClusterDefinition extends TestCase {

  public TestClusterDefinition(String name) {
    super(name);
  }
  
  private static final Pattern mnlPattern = Pattern.compile("^(\\d+)m(\\d+)n\\.(.*)\\.def$");

  public void testMNLdefs() throws IOException {
    final File clustersResourceDir = FileUtil.getResourceFile(this.getClass(), "resources/clusters/");

    // for each def of form MmNn.i1-i2...-ij.def
    final File[] clusterDefs = clustersResourceDir.listFiles(new FilenameFilter() {
        public boolean accept(File dir, String name) {
          final Matcher m = mnlPattern.matcher(name);
          return m.matches();
        }
      });

    // make sure there are M unique machines, N unique nodes, j levels, k unique nodes per level for each ik
    assertTrue(clusterDefs.length > 0);
    for (File clusterDefFile : clusterDefs) {
      final String name = clusterDefFile.getName();
      final Matcher matcher = mnlPattern.matcher(name);
      final boolean matches = matcher.matches();
      assertTrue(matches);

      final int m = Integer.parseInt(matcher.group(1));
      final int n = Integer.parseInt(matcher.group(2));
      final String levels = matcher.group(3);
      final String[] is = levels.split("-");

      final ClusterDefinition clusterDef = new ClusterDefinition(null, name);
      assertEquals(name, m, clusterDef.getNumMachines());
      assertEquals(name, n, clusterDef.getNumNodes());
      assertEquals(name, is.length, clusterDef.getNumLevels());

      assertEquals(name, n, clusterDef.getNodeNames(0, false).size());
      for (int i = 0; i < clusterDef.getNumLevels(); ++i) {
        final Collection<String> nodesAtLevel = clusterDef.getNodeNames(i + 1, false);
        assertEquals(name, Integer.parseInt(is[i]), nodesAtLevel.size());
      }
    }
  }

  public void testNameSubstitutionConstructor() throws IOException {
    ClusterDefinition clusterDefinition = null;
    clusterDefinition = new ClusterDefinition(null, "3m6n.2-4", "gw", new String[]{"cm1", "cm2", "cm3"});

    String expected = "(gw (cm1-2 cm2-2 cm3-2))";
    String machineTreeString = clusterDefinition.getMachineTree().toString();
    assertEquals("expecting '" + expected + "', got '" + machineTreeString + "'", expected, machineTreeString);

    clusterDefinition = new ClusterDefinition(null, "3m3n.1-2", "gw", new String[]{"cm1", "cm2", "cm3"});
    expected = "(gw (cm1 cm2 cm3))";
    machineTreeString = clusterDefinition.getMachineTree().toString();
    assertEquals("expecting '" + expected + "', got '" + machineTreeString + "'", expected, machineTreeString);
  }

  public void testGetPosition1() throws IOException {
    final ClusterDefinition clusterDefinition = new ClusterDefinition(null, "3m3n.1-2", "gw", new String[]{"cm1", "cm2", "cm3"});

    assertEquals(0, clusterDefinition.getGlobalPosition("Cm1", 0));
    assertEquals(1, clusterDefinition.getGlobalPosition("Cm2", 0));
    assertEquals(2, clusterDefinition.getGlobalPosition("Cm3", 0));

    assertEquals(0, clusterDefinition.getLocalPosition("Cm1", 0));
    assertEquals(0, clusterDefinition.getLocalPosition("Cm2", 0));
    assertEquals(1, clusterDefinition.getLocalPosition("Cm3", 0));
  }

  public void testGetPosition2() throws IOException {
    final ClusterDefinition clusterDefinition = new ClusterDefinition(null, "dev-3a");

    assertEquals(0, clusterDefinition.getGlobalPosition("Suliban", 0));
    assertEquals(1, clusterDefinition.getGlobalPosition("Andorian", 0));
    assertEquals(2, clusterDefinition.getGlobalPosition("Tholian", 0));

    assertEquals(0, clusterDefinition.getLocalPosition("Suliban", 0));
    assertEquals(0, clusterDefinition.getLocalPosition("Andorian", 0));
    assertEquals(1, clusterDefinition.getLocalPosition("Tholian", 0));
  }

  public void testComplexTestClusterDef() throws IOException {
    final ClusterDefinition clusterDef = new ClusterDefinition(null, "complex-test-cluster", "localhost", new String[]{"z", "a", "b", "c", "d", "e"});

    assertTrue(clusterDef.hasGroup("group1"));
    assertTrue(clusterDef.hasGroup("group2"));
    assertTrue(clusterDef.hasGroup("group3"));
    assertTrue(clusterDef.hasGroup("group4"));
    assertTrue(clusterDef.hasGroup("processor"));

    assertFalse(clusterDef.hasGroup("group0"));
    assertFalse(clusterDef.hasGroup("group5"));

    assertEquals(5, clusterDef.getNumGroupNodes("group1"));
    assertEquals(5, clusterDef.getNumGroupNodes("group2"));
    assertEquals(5, clusterDef.getNumGroupNodes("group3"));
    assertEquals(5, clusterDef.getNumGroupNodes("group4"));
    assertEquals(20, clusterDef.getNumGroupNodes("processor"));

    assertEquals(1, clusterDef.getNumGroupNodes("group5"));

    assertEquals(0, clusterDef.getGroupNodePosition("group1", "a", 0));
    assertEquals(1, clusterDef.getGroupNodePosition("group1", "b", 0));
    assertEquals(2, clusterDef.getGroupNodePosition("group1", "c", 0));
    assertEquals(3, clusterDef.getGroupNodePosition("group1", "d", 0));
    assertEquals(4, clusterDef.getGroupNodePosition("group1", "e", 0));

    assertEquals(-1, clusterDef.getGroupNodePosition("group1", "z", 0));  // wrong machineName
    assertEquals(-1, clusterDef.getGroupNodePosition("group1", "e", 1));  // wrong jvmNum
    assertEquals(-1, clusterDef.getGroupNodePosition("group1", "z", 2));  // wrong machineName & jvmNum

    assertEquals(0, clusterDef.getGroupNodePosition("group2", "a", 1));
    assertEquals(1, clusterDef.getGroupNodePosition("group2", "b", 1));
    assertEquals(2, clusterDef.getGroupNodePosition("group2", "c", 1));
    assertEquals(3, clusterDef.getGroupNodePosition("group2", "d", 1));
    assertEquals(4, clusterDef.getGroupNodePosition("group2", "e", 1));

    assertEquals(-1, clusterDef.getGroupNodePosition("group2", "z", 1));  // wrong machineName
    assertEquals(-1, clusterDef.getGroupNodePosition("group2", "e", 0));  // wrong jvmNum
    assertEquals(-1, clusterDef.getGroupNodePosition("group2", "z", 2));  // wrong machineName & jvmNum

    assertEquals(0, clusterDef.getGroupNodePosition("group3", "a", 2));
    assertEquals(1, clusterDef.getGroupNodePosition("group3", "b", 2));
    assertEquals(2, clusterDef.getGroupNodePosition("group3", "c", 2));
    assertEquals(3, clusterDef.getGroupNodePosition("group3", "d", 2));
    assertEquals(4, clusterDef.getGroupNodePosition("group3", "e", 2));

    assertEquals(-1, clusterDef.getGroupNodePosition("group3", "z", 2));  // wrong machineName
    assertEquals(-1, clusterDef.getGroupNodePosition("group3", "e", 1));  // wrong jvmNum
    assertEquals(-1, clusterDef.getGroupNodePosition("group3", "z", 3));  // wrong machineName & jvmNum


    // testing getGroupNodeNames
    for (int i = 1; i <= 4; ++i) {
      // each "groupN" has 5 nodes
      final List<String> groupNodeNames = clusterDef.getGroupNodeNames("group" + i, true);
      assertEquals(5, groupNodeNames.size());

      for (String nodeName : groupNodeNames) {
        // all jvm-0 nodes are in group1, jvm-1 nodes are in group2, etc.
        assertTrue(nodeName.endsWith("-" + (i - 1)));

        // getting the group node names for a single node should return that node
        final List<String> nodeGroup = clusterDef.getGroupNodeNames(nodeName, true);
        assertEquals(1, nodeGroup.size());
        assertEquals(nodeName, nodeGroup.get(0));
      }
    }
  }

  public void testGetAllServerAddresses() throws IOException {
    final ClusterDefinition clusterDef = new ClusterDefinition("spence", "3m3n.1-2", "vorta", new String[]{"suliban", "andorian", "tholian"});
    final InetSocketAddress[] serverAddresses = clusterDef.getServerAddresses(ClusterDefinition.ALL_NODES_GROUP);
    assertNotNull(serverAddresses);
  }

  public void testGetServerAddress() throws IOException {
    final ClusterDefinition clusterDef = new ClusterDefinition("spence", "19m26n.5-7-14", "vorta", new String[]{"founder", "tandaran", "talaxian", "miradorn", "hunter", "suliban", "maquis", "ocampa", "tholian", "bolian", "bajoran", "briori", "shran", "archer", "kirk", "damar", "dukat", "gowron", "laforge"});
    final InetSocketAddress[] serverAddress = clusterDef.getServerAddresses("founder-0");
    assertNotNull(serverAddress);
  }

  public void testVirtualClusterDefinition() throws IOException {
    final VirtualCluster cluster = new VirtualCluster("TestVirtualClusterDefinition", "test-5m7n.1-2-4.def", "virtualClusterDefTest");
    final ClusterDefinition clusterDef = cluster.getClusterDefinition();

    List<String> groupNodes = null;

    groupNodes = clusterDef.getGroupNodeNames("processors", true);
    assertEquals("[localhost-1, localhost-2, localhost-3, localhost-4, localhost-5, localhost-6]", groupNodes.toString());

    groupNodes = clusterDef.getGroupNodeNames("node1-0", true);
    assertEquals("[localhost-0]", groupNodes.toString());

    groupNodes = clusterDef.getGroupNodeNames("node2-0", false);
    assertEquals("[localhost-1]", groupNodes.toString());
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestClusterDefinition.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
