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
package org.sd.io;


import java.io.File;

/**
 * Operator to apply to a file with records of type T.
 * <p>
 * @author Spence Koehler
 */
public interface FileOperator {

  /**
   * Initialization hook called before operating on any files.
   *
   * @return false to curtail any operations; true to proceed.
   */
  public boolean initializeHook();

  /**
   * Operate on the given file.
   *
   * @return false if operation indicates further processing of files should
   *         halt; otherwise, true.
   */
  public boolean operate(File file);

  /**
   * Finalize hook called after operating on all files.
   */
  public void finalizeHook();
}
