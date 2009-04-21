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
import org.sd.match.Osob;

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Wrapper around an osob data file.
 * <p>
 * @author Spence Koehler
 */
class OsobData {

  static final String DATA_FILE_NAME = "osobdb.dat";

  private String dbPath;
  private OsobDbParms parms;

  private OsobIndex index;
  private RandomAccessFile raFile;
  private boolean isOpen;

  OsobData(String dbPath, OsobDbParms parms) {
    this.dbPath = dbPath;
    this.parms = parms;
    this.isOpen = false;
  }

  public void open() throws IOException {
    if (!isOpen) {
      this.isOpen = true;

      this.index = new OsobIndex(dbPath, parms);
      index.open();

      final String filename = FileUtil.getFilename(dbPath, DATA_FILE_NAME);
      this.raFile = new RandomAccessFile(filename, "r");
    }
  }

  public void close() throws IOException {
    if (index != null) index.close();
    if (raFile != null) {
      raFile.close();
      raFile = null;
    }
    isOpen = false;
  }
  
  public OsobBlock retrieveBlock(int blockId) throws IOException {
    if (!isOpen) throw new IllegalStateException("can't retrieve block before open!");

    final long pos = index.getDataPosition(blockId);
    final int num = index.getNumOsobs(blockId);
    raFile.seek(pos);

    final OsobBlock result = new OsobBlock(parms.getBlockSize(), parms.getBaseConceptId(blockId));

    for (int i = 0; i < num; ++i) {
      final int numBytes = raFile.readInt();
      final byte[] osobBytes = new byte[numBytes];
      raFile.readFully(osobBytes);
      final Osob osob = new Osob(osobBytes);
      result.addOsob(osob);
    }

    return result;
  }
}
