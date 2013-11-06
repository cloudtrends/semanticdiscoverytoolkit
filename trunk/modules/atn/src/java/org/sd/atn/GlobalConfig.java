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
package org.sd.atn;


/**
 * Global configuration for atn package.
 * <p>
 * @author Spence Koehler
 */
public class GlobalConfig {

  public static final String DISABLE_LOAD_VERBOSITY_ENV = "DISABLE_ATN_LOAD_VERBOSITY";


  private static boolean verboseLoad = false;
  static {
    reset();
  }

  public static final void reset() {
    verboseLoad = !"true".equals(System.getenv(DISABLE_LOAD_VERBOSITY_ENV));
  }

  public static final boolean verboseLoad() {
    return verboseLoad;
  }

  public static final void setVerboseLoad(boolean val) {
    verboseLoad = val;
  }
}
