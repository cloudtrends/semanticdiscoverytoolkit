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

/**
 * Utilities to help handle operating system operations and identification
 * <p>
 * @author Abe Sanderson
 */
public class OperatingSystemUtil
{
  public enum OperatingSystemFamily {
    LINUX,
      UNIX,
      MAC,
      WINDOWS;
  }

  public enum OperatingSystem 
  {
    LINUX("Linux", OperatingSystemFamily.LINUX),
      MAC_OS("Mac OS", OperatingSystemFamily.MAC),
      MAC_OSX("Mac OS X", OperatingSystemFamily.MAC),
      WIN_2000("Windows 2000", OperatingSystemFamily.WINDOWS),
      WIN_95("Windows 95", OperatingSystemFamily.WINDOWS),
      WIN_98("Windows 98", OperatingSystemFamily.WINDOWS),
      WIN_NT("Windows NT", OperatingSystemFamily.WINDOWS),
      WIN_ME("Windows Me", OperatingSystemFamily.WINDOWS),
      WIN_XP("Windows XP", OperatingSystemFamily.WINDOWS),
      WIN_VISTA("Windows Vista", OperatingSystemFamily.WINDOWS),
      WIN_7("Windows 7", OperatingSystemFamily.WINDOWS),
      WIN_8("Windows 8", OperatingSystemFamily.WINDOWS);

    
    private String osName;
    private OperatingSystemFamily osFamily;

    OperatingSystem(String osName, 
                    OperatingSystemFamily osFamily)
    {
      this.osName = osName;
      this.osFamily = osFamily;
    }

    public String getOSName() { return this.osName; }
    public OperatingSystemFamily getOSFamily() { return this.osFamily; }
  }
}
