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


import org.sd.io.FileUtil;
import org.sd.util.ExecUtil;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Wrapper around a file that may exist remotely.
 * <p>
 * This places files into the current user's machine-level cluster cache.
 * <ul>
 * <li>A file that is local to the machine will be symbolically linked to the cache area.</li>
 * <li>A file that is accessible over a mount will be copied to the cache area.</li>
 * <li>A file that is otherwise remote will be rsynced to the cache area.</li>
 * </ul>
 * The file in the cache will be used as the local handle.
 * <p>
 * When a RemoteFile is closed, if keepLocal is false, then the locally cached link or copy
 * will be deleted.
 * <p>
 * If the file already exists in the local cache area, no transfer or update will be done
 * unless the forceCopy flag is true.
 *
 * @author Spence Koehler
 */
public class RemoteFile {
  
  private File file;
  private boolean keepLocal;
  private boolean forceCopy;
  private boolean dontCopy;

  private String filename;
  private String machine;
  private String path;
  private File localFile;

  /**
   * Construct a handle to a remote file by placing it in the local
   * cache area. If !keepLocal, then delete the copied file on close.
   * <p>
   * Don't force a copy if the file already exists and do allow the
   * copy.
   */
  public RemoteFile(File file, boolean keepLocal) {
    this(file, keepLocal, false, false);
  }

  /**
   * Construct a handle to a remote file by placing it in the local
   * cache area.
   */
  public RemoteFile(File file, boolean keepLocal, boolean forceCopy, boolean dontCopy) {
    this.file = file;
    this.keepLocal = keepLocal;
    this.forceCopy = forceCopy;
    this.dontCopy = dontCopy;

    init();
  }

  /**
   * Get the local file handle to the file.
   */
  public File getLocalHandle() {
    return (localFile != null) ? localFile : file;
  }

  /**
   * Get this instance's keepLocal flag.
   */
  public boolean keepLocal() {
    return keepLocal;
  }

  /**
   * Set this instance's keepLocal flag. Note that its effect is applied
   * when this instance is closed.
   */
  public void setKeepLocal(boolean keepLocal) {
    this.keepLocal = keepLocal;
  }

  /**
   * Close this remote file handle, deleting the local file from the cache
   * if this instance is not to keep local copies.
   */
  public void close() {
    if (!keepLocal && localFile != null && localFile.exists()) {
      localFile.delete();
    }
  }

  public String toString() {
    final StringBuilder result = new StringBuilder();

    result.append(filename);
    if (localFile != null) {
      result.append(" (local=").
        append(localFile.getAbsolutePath()).append(')');
    }

    return result.toString();
  }

  private final void init() {
//    this.filename = file.getAbsolutePath();
    this.filename = file.toString();
    
    this.machine = getMachine(filename);
    this.path = getPath(filename);
    final String localPath = generateLocalPath(machine, path);
    this.localFile = (localPath != null) ? new File(localPath) : null;
    
    if (localFile != null && !localFile.exists()) {
      copyRemoteToLocal();
    }
  }

  public static final File getLocalHandle(File file) {
    final String filename = file.toString();
    final String machine = getMachine(filename);
    final String path = getPath(filename);
    final String localPath = generateLocalPath(machine, path);

    return (localPath != null) ? new File(localPath) : file;
  }

  private static final Pattern REMOTE_MOUNT_PATTERN = Pattern.compile("^/mnt/([^/]+)(/.*)$");
  private static final Pattern REMOTE_SSH_PATTERN = Pattern.compile("^([^:]+):(.*)$");

  protected static final String getMachine(String filename) {
    String machine = null;

    Matcher m = REMOTE_MOUNT_PATTERN.matcher(filename);

    if (!m.matches()) {
      m = REMOTE_SSH_PATTERN.matcher(filename);
    }

    if (m.matches()) {
      machine = m.group(1).toLowerCase();

      final int atPos = machine.indexOf('@');
      if (atPos >= 0) {
        machine = machine.substring(atPos + 1);
      }
    }

    // if machine is of the form a^b, then return b (a=newMachine, b=origMachine)
    if (machine != null) {
      final int cPos = machine.indexOf('^');
      if (cPos >= 0) {
        machine = machine.substring(cPos + 1);
      }
    }

    return machine;
  }

  protected static final String getPath(String filename) {
    String path = null;

    Matcher m = REMOTE_MOUNT_PATTERN.matcher(filename);

    if (!m.matches()) {
      m = REMOTE_SSH_PATTERN.matcher(filename);
    }

    if (m.matches()) {
      path = m.group(2);
//      path = m.group(2).toLowerCase();
    }

    return path;
  }

  /**
   * Given a remote file path in the form of &lt;machine&gt;:&lt;path&gt; or
   * /mnt/&lt;machine&gt;/&lt;path&gt;, generate a local path for the file
   * under the current user's cluster/cache.
   *
   * @return the localPath or null if unable to parse the remote file.
   */
  public static final String generateLocalPath(String remoteFile) {
    final String machine = getMachine(remoteFile);
    if (machine == null) return null;

    final String path = getPath(remoteFile);
    if (path == null) return null;

    return generateLocalPath(machine, path);
  }

  protected static final String generateLocalPath(String machine, String path) {
    if (machine == null || path == null || path.length() == 0) return null;

    final StringBuilder result = new StringBuilder();

    if (path.indexOf("/cluster/cache") < 0) {
      result.
        append(ExecUtil.getUserHome()).
        append("/cluster/cache/").
        append(machine);

      if (path.charAt(0) != '/') {
        result.append('/');
      }
    }
    // else assume path already points to a cluster cache on this machine.

    result.append(path);

    return result.toString();
  }

  /**
   * Copy the source file to the cache area if it is locally accessible.
   * <p>
   * If it is locally accessible over a mount point, then copy the file;
   * otherwise, symbolically link the file.
   *
   * @return a handle on the cached file if successful or null.
   */
  public static final File copyToCache(File source) {
    final File result = getLocalHandle(source);

    if (result != null && !result.exists() && source.exists()) {
      // make parent directories if needed.
      if (!FileUtil.makeParentDirectories(result)) {
        System.err.println("***WARNING: mkdirs '" + result.getParentFile().getAbsolutePath() + "' failed!");
      }
      else {
        String command = null;
        final String filename = source.getAbsolutePath();
        final String local = result.getAbsolutePath();
        final String machine = getMachine(local);

        if (machine == null || machine.equals(ExecUtil.getMachineName().toLowerCase())) {
          // file exists locally. can just do an "ln -s"
          command = "ln -s -f " + filename + " " + local;
        }
        else {
          // file exists over a mount point. copy the file over.
          command = "cp -f " + filename + " " + local;
        }

        ExecUtil.executeProcess(command);
      }
    }

    return (result != null && result.exists()) ? result : null;
  }

  private final void copyRemoteToLocal() {
    if (dontCopy || localFile == null || (localFile.exists() && !forceCopy)) return;

    // link or rsync the remote file
    final File localCopy = copyToCache(file);

    if (localCopy == null) {
      // couldn't copy so try to rsync.
      final String remote = getRemoteName();
      new DataBroker().pullWithRsync(remote, localFile);
    }
  }

  public final String getRemoteName() {
    return getRemoteName(filename, machine, path);
  }

  public static final String getRemoteName(File file) {
    final String filename = file.toString();
    return getRemoteName(filename);
  }

  public static final String getRemoteName(String filename) {
    final String machine = getMachine(filename);
    final String path = getPath(filename);

    return getRemoteName(filename, machine, path);
  }

  public static final String getRemoteName(String filename, String machine, String path) {
    String result = filename;

    if (filename.indexOf(':') < 0) {
      // need to convert "/mnt/machine/path" to "machine:path"
      result = machine + ":" + path;
    }

    int caretPos = result.indexOf('^');
    if (caretPos > 0) {
      // need to rebuild "a^b:path" as "b:path"
      result = result.substring(caretPos + 1);
    }

    return result;
  }
}
