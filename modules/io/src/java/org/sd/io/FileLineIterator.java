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


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;

/**
 * A FileRecordIterator over each line of a text file.
 * <p>
 * @author Spence Koehler
 */
public class FileLineIterator extends FileRecordIterator<String> {
  
  private BufferedReader reader;

  /**
   * Construct to iterate over the lines of the given file.
   */
  public FileLineIterator(File file) throws IOException {
    super(file);
  }

  /**
   * Initialize for reading the file.
   */
  protected void init(File file) throws IOException {
    this.reader = FileUtil.getReader(file);
  }

  /**
   * Read the next line.
   */
  protected String readNextRecord() throws IOException {
    return reader.readLine();
  }

  /**
   * Close the reader.
   */
  public void close() throws IOException {
    reader.close();
  }
}
