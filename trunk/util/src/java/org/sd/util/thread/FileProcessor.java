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
package org.sd.util.thread;


import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Interface for processing a file.
 * <p>
 * @author Spence Koehler
 */
public interface FileProcessor {

  /**
   * Process the input file, optionally using the output file for output.
   * 
   * @param input   A file handle to the input to be processed.
   * @param output  A file handle to the output to be collected (possibly null).
   * @param die     A value that may turn true during processing, indicating
   *                that the processing should be aborted.
   */
  public void processFile(File input, File output, AtomicBoolean die);
  
}
