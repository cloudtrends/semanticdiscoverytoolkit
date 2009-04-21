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
import org.sd.text.KeyGenerator;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JUnit Tests for the DataPusher class.
 * <p>
 * @author Spence Koehler
 */
public class TestDataPusher extends TestCase {

  private static final String TEST_DATA_PATH = "/datapusher_testdata";
  private static final String POSTFIX_PATH = "test/data/";
  private static final Pattern PARTITION_PATTERN = Pattern.compile("\\w+-(\\d+).txt");
  private static final int PATTERN_GROUP_NUM = 1;

  public TestDataPusher(String name) {
    super(name);
  }
  
  private final String getPath(String root) {
		return "/tmp/TestDataPusher/resources/" + root + "/";
//    return FileUtil.getFilename(this.getClass(), "resources") + root + "/";
  }

  private final List<String> createTestData(String basePath, int numPartitions) throws IOException {
    final List<String> result = new ArrayList<String>();
    final KeyGenerator keyGenerator = new KeyGenerator();

    for (int i = 0; i < numPartitions; ++i) {
      final String filename = basePath + "-" + i + ".txt";
      FileUtil.mkdirs(filename);

      final FileOutputStream out = new FileOutputStream(filename);
      out.close();

      result.add(new File(filename).getName());
    }
    return result;
  }

  public void test1() throws IOException {
    final VirtualCluster cluster = new VirtualCluster("TestDataPusher", "test-5m7n.1-2-4.def", "TestDataPusher");

    final ClusterContext context = cluster.getNode(0);  // any node will do.
    final ClusterDefinition clusterDef = context.getClusterDefinition();
    final String groupName = "group3";
    final int numPartitions = clusterDef.getNumGroupNodes(groupName);

    final String testDataPath = getPath(TEST_DATA_PATH);
    List<String> filenames = createTestData(testDataPath + "test", numPartitions);

    DataPusher.sendDataToNodes(clusterDef, groupName, POSTFIX_PATH, testDataPath, PARTITION_PATTERN, PATTERN_GROUP_NUM, 0, 10);

    // verify the filenames made it to their respective nodes.
    final Collection<ClusterNode> nodes = cluster.getNodes();
    final List<String> groupNodeNames = clusterDef.getGroupNodeNames(groupName, true);

    int nodeNum = 0;
    for (ClusterNode node : nodes) {
      final Config config = node.getConfig();
      final String nodeName = config.getNodeName();
      if (!groupNodeNames.contains(nodeName)) continue;

      final String path = config.getJvmRootDir() + POSTFIX_PATH;
      final File dir = new File(path);
      final File[] files = dir.listFiles(new FileFilter() {
          public boolean accept(File file) {
            final String name = file.getName();
            final Matcher m = PARTITION_PATTERN.matcher(name);
            return m.matches();
          }
        });

//note: this fails when run before ssh identity is established (ssh-add)
//      unless set up w/out a passphrase!
//      ALSO, make sure you can "ssh localhost pwd" without prompts or errors.
      if (files == null || files.length == 0) {
        System.err.println("***NOTE: Failure consistent with environment troubles.");
        System.err.println("         Before debugging this failure, check that you can \"ssh localhost\"");
        System.err.println("         without any prompts or errors. This usually requires 'ssh-add' to");
        System.err.println("         have been invoked.");
      }

      assertTrue("No file found for node=" + nodeNum + "!", files != null && files.length == 1);
      assertTrue(filenames.contains(files[0].getName()));

      ++nodeNum;
    }
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestDataPusher.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
