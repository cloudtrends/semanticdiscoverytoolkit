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
package org.sd.match.osobdb;


import org.sd.io.FileUtil;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Encapsulation of osob db parameters.
 * <p>
 * @author Spence Koehler
 */
class OsobDbParms {

  static final String PARMS_FILE_NAME = "osobdb.parms";

  private String filename;
  private int blockSize;
  private int cacheSize;

  /**
   * Construct a (reader) parms instance.
   */
  OsobDbParms(String dbPath) throws IOException {
    this.filename = FileUtil.getFilename(dbPath, PARMS_FILE_NAME);
    load();
  }
  
  /**
   * Construct a (writer) parms instance.
   */
  OsobDbParms(String dbPath, int blockSize, int cacheSize)  throws IOException {
    this.filename = FileUtil.getFilename(dbPath, PARMS_FILE_NAME);
    this.blockSize = blockSize;
    this.cacheSize = cacheSize;

    dump();
  }

  public int getBlockSize() {
    return blockSize;
  }

  public int getCacheSize() {
    return cacheSize;
  }

  public int getBlockId(int conceptId) {
    return (conceptId / blockSize);
  }

  public int getBaseConceptId(int blockId) {
    return blockId * blockSize;
  }

  private final void load() throws IOException {
    final DataInputStream dataIn = new DataInputStream(new FileInputStream(filename));

    this.blockSize = dataIn.readInt();
    this.cacheSize = dataIn.readInt();

    dataIn.close();
  }

  private final void dump() throws IOException {
    final DataOutputStream dataOut = new DataOutputStream(new FileOutputStream(filename));

    dataOut.writeInt(blockSize);
    dataOut.writeInt(cacheSize);

    dataOut.close();

    final BufferedWriter writer = FileUtil.getWriter(filename + ".txt");
    writer.write("blockSize=" + blockSize);
    writer.newLine();
    writer.write("cacheSize=" + cacheSize);
    writer.newLine();
    writer.close();
  }
}
