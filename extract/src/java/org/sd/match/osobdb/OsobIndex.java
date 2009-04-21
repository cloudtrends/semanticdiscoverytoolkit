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

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Wrapper around an osob index file.
 * <p>
 * @author Spence Koehler
 */
public class OsobIndex {

  static final String INDEX_FILE_NAME = "osobdb.idx";

  private String dbPath;
  private OsobDbParms parms;

  private long[] dataPos;  // data seek pos for each block
  private int[] numOsobs;  // num actual osobs in each block

  OsobIndex(String dpPath, OsobDbParms parms) {
    this.dbPath = dpPath;
    this.parms = parms;
  }

  public void open() throws IOException {
    final String filename = FileUtil.getFilename(dbPath, INDEX_FILE_NAME);
    final DataInputStream dataInput = new DataInputStream(new FileInputStream(filename));
    final int numBlocks = dataInput.readInt();

    this.dataPos = new long[numBlocks];
    this.numOsobs = new int[numBlocks];

    for (int i = 0; i < numBlocks; ++i) {
      dataPos[i] = dataInput.readLong();
      numOsobs[i] = dataInput.readInt();
    }

    dataInput.close();
  }

  public void close() throws IOException {
    // release memory
    dataPos = null;
    numOsobs = null;
  }

  public long getDataPosition(int blockId) {
    return dataPos[blockId];
  }

  public int getNumOsobs(int blockId) {
    return numOsobs[blockId];
  }
}
