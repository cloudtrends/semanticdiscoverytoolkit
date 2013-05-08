/*
    Copyright 2013 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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
package org.sd.io;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;
import org.sd.io.OperatingSystemUtil.OperatingSystem;
import static org.sd.io.OperatingSystemUtil.OperatingSystemFamily.*;

/**
 * A wrapper class to represent a directory mounted with NFS
 * <p>
 * @author Abe Sanderson
 */
public class NFSMount
{
  // todo: read the /etc/fstab file to build with mount/remote directores
  private static final String FILE_SEPARATOR = System.getProperty("file.separator");
  private final String remoteDir;
  private final File mountDir; 
  private final OperatingSystem remoteOS; 

  public NFSMount(String remoteDir, OperatingSystem remoteOS, 
                  File mountDir)
  {
    this.remoteDir = remoteDir;
    this.remoteOS = remoteOS;
    this.mountDir = mountDir;
  }

  public File getLocalFile(String path)
    throws IOException
  {
    File result = mountDir;

    if(path.startsWith(remoteDir))
      path = path.substring(remoteDir.length());

    // split path into folder parts
    String splitChar = (remoteOS.getOSFamily() == WINDOWS ? "\\" : "/");
    String[]  parts = path.split(Pattern.quote(splitChar));
    for(String p : parts)
      result = new File(result, p);

    return result;
  }

  public String buildRemotePath(File file)
    throws IOException
  {
    StringBuilder result = new StringBuilder(remoteDir);
    
    // strip mountdir path
    String path = file.getAbsolutePath();
    String mountPath = mountDir.getAbsolutePath();
    if(path.startsWith(mountPath))
      path = path.substring(mountPath.length());
    else
      throw new IOException("file is not a subfile of this nfs mount");

    // split path into folder parts
    String pathChar = (remoteOS.getOSFamily() == WINDOWS ? "\\" : "/");
    String[] parts = path.split(FILE_SEPARATOR);
    for(String p : parts)
    {
      if(p.length() > 0)
        result.append(pathChar).append(p);
    }

    return result.toString();
  }
}
