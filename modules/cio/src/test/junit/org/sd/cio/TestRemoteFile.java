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
package org.sd.cio;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.sd.util.ExecUtil;

import java.io.File;

/**
 * JUnit Tests for the RemoteFile class.
 * <p>
 * @author Spence Koehler
 */
public class TestRemoteFile extends TestCase {

  public TestRemoteFile(String name) {
    super(name);
  }
  
  public void testGetMachine() {
    assertEquals("foo", RemoteFile.getMachine("/mnt/foo/a/b/c"));
    assertEquals("foo", RemoteFile.getMachine("foo:/a/b/c"));
    assertEquals("foo", RemoteFile.getMachine("bar@foo:a/b/c"));

    assertEquals("suliban", RemoteFile.getMachine("/mnt/suliban/data/crawl/global.011007-take2/4e/f1/www.fgic.com"));
  }

  public void testGetPath() {
    assertEquals("/a/b/c", RemoteFile.getPath("/mnt/foo/a/b/c"));
    assertEquals("/a/b/c", RemoteFile.getPath("foo:/a/b/c"));
    assertEquals("a/b/c", RemoteFile.getPath("bar@foo:a/b/c"));

    assertEquals("/data/crawl/global.011007-take2/4e/f1/www.fgic.com", RemoteFile.getPath("/mnt/suliban/data/crawl/global.011007-take2/4e/f1/www.fgic.com"));
  }

  public void testGetMachinePathThroughFile() {
    final File domainDir = new File("/mnt/suliban/data/crawl/global.011007-take2/4e/f1/www.fgic.com");
    final File sitemapFile = new File(domainDir, "0.sitemap.txt.gz");
    final RemoteFile remoteFile = new RemoteFile(sitemapFile, true, false, true);

    final String localFilePath = remoteFile.getLocalHandle().getAbsolutePath();
    assertTrue(localFilePath.endsWith("/cluster/cache/suliban/data/crawl/global.011007-take2/4e/f1/www.fgic.com/0.sitemap.txt.gz"));
  }

  public void testGenerateLocalPath() {
    assertNull(RemoteFile.generateLocalPath(null, null));
    assertNull(RemoteFile.generateLocalPath("foo", null));
    assertNull(RemoteFile.generateLocalPath(null, "foo"));

    String localPath = null;
    localPath = RemoteFile.generateLocalPath("foo", "/a/b/c");
    assertTrue(localPath.endsWith("/cluster/cache/foo/a/b/c"));

    localPath = RemoteFile.generateLocalPath("foo", "a/b/c");
    assertTrue(localPath.endsWith("/cluster/cache/foo/a/b/c"));
  }

  public void testRemoteLocalFilenames() {
    final File remotePath = new File("foo:/tmp/bar.txt");
    final RemoteFile remoteFile = new RemoteFile(remotePath, false, false, true);
    final File localFile = remoteFile.getLocalHandle();

    final String localFilePath = localFile.getAbsolutePath();
    assertTrue(localFilePath.endsWith("/cluster/cache/foo/tmp/bar.txt"));
  }

  public void testParsing() {

    final File file = new File("miradorn:/data/crawl/sd836.row/67/83/aesl.com.pk-Njc0ZDUwMD");

    final String filename = file.toString();
    final String machine = RemoteFile.getMachine(filename);
    final String path = RemoteFile.getPath(filename);
    final String localPath = RemoteFile.generateLocalPath(machine, path);
    final String remoteName = RemoteFile.getRemoteName(file);
    final String localHandle = RemoteFile.getLocalHandle(file).toString();

    assertEquals("miradorn:/data/crawl/sd836.row/67/83/aesl.com.pk-Njc0ZDUwMD", filename);
    assertEquals("miradorn", machine);
    assertEquals("/data/crawl/sd836.row/67/83/aesl.com.pk-Njc0ZDUwMD", path);
    assertEquals("/home/" + ExecUtil.getUser() + "/cluster/cache/miradorn/data/crawl/sd836.row/67/83/aesl.com.pk-Njc0ZDUwMD", localPath);
    assertEquals("miradorn:/data/crawl/sd836.row/67/83/aesl.com.pk-Njc0ZDUwMD", remoteName);
    assertEquals("/home/" + ExecUtil.getUser() + "/cluster/cache/miradorn/data/crawl/sd836.row/67/83/aesl.com.pk-Njc0ZDUwMD", localHandle);
  }

  public void testGetLocalHandleWithAbsolutePath() {
    final File file = new File("hunter^hunter:/data/crawl/ks.recrawl/00/29/advancedrc.com-A2MWUxOTY0");
    final File localHandle = RemoteFile.getLocalHandle(file);
    final String remoteName = RemoteFile.getRemoteName(file);

    assertEquals("/home/" + ExecUtil.getUser() + "/cluster/cache/hunter/data/crawl/ks.recrawl/00/29/advancedrc.com-A2MWUxOTY0", localHandle.getAbsolutePath());
    assertEquals("hunter:/data/crawl/ks.recrawl/00/29/advancedrc.com-A2MWUxOTY0", remoteName);
  }

  public void testGetLocalHandleWithTripleSlashAbsolutePath() {
    final File file = new File("hunter^hunter:///data/crawl/ks.recrawl/00/29/advancedrc.com-A2MWUxOTY0");
    final File localHandle = RemoteFile.getLocalHandle(file);
    final String remoteName = RemoteFile.getRemoteName(file);

    assertEquals("/home/" + ExecUtil.getUser() + "/cluster/cache/hunter/data/crawl/ks.recrawl/00/29/advancedrc.com-A2MWUxOTY0", localHandle.getAbsolutePath());
    assertEquals("hunter:/data/crawl/ks.recrawl/00/29/advancedrc.com-A2MWUxOTY0", remoteName);
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestRemoteFile.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
