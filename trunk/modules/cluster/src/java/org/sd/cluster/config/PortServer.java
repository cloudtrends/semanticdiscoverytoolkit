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


import org.sd.io.FileLock;
import org.sd.io.FileUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;

/**
 * Class to serve out unique ports for tests.
 * <p>
 * @author Spence Koehler
 */
public final class PortServer {

  private static final int LOCK_WAIT_TIMEOUT = 1000;
  private static final int AGE_LIMIT = (1000 * 60 * 5);  // start over after 5 minutes

  private static final PortServer INSTANCE = new PortServer();

  public static final PortServer getInstance() {
    return INSTANCE;
  }

  private FileLock<Integer> fileLock;
  private FileLock.LockOperation<Integer> lockOperation;

  private PortServer() {
//new RuntimeException("Created PortServer Instance!!!").printStackTrace(System.err);

    final String path = "/tmp/" + System.getProperty("user.name") + "/port.txt";
    this.fileLock = new FileLock<Integer>(path, LOCK_WAIT_TIMEOUT);
    this.lockOperation = new FileLock.LockOperation<Integer>() {
        public Integer operate(String filename) throws IOException {
          int result = 11001;

          final File file = new File(path);
          if (file.exists() && FileUtil.getAge(file) < AGE_LIMIT) {
            // start over if too old; otherwise, read and increment.
            final String contents = FileUtil.readAsString(path);
            result = Integer.parseInt(contents) + 1;
          }

          // write out the port used for the next go around
          final BufferedWriter writer = FileUtil.getWriter(path);
          writer.write(Integer.toString(result));
          writer.flush();
          writer.close();

          return result;
        }
      };
  }

  public final int getNextTestPort() {
    final int result = fileLock.operateWhileLocked(lockOperation);
System.err.println("PORT=" + result);
    return result;
  }
}
