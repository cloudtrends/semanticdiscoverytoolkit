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
import org.sd.match.ConceptModel;
import org.sd.match.Osob;
import org.sd.match.OsobSerializer;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Class to build an osob db.
 * <p>
 * @author Spence Koehler
 */
public class OsobDbBuilder {

  private File dbFile;
  private String dbPath;
  private int blockSize;
  private int cacheSize;

  private Map<Integer, OsobBlock> id2block;  // data collector
  private OsobDbParms parms;
  private int maxBlockId = 0;

  public OsobDbBuilder(String dbPath, int blockSize, int cacheSize) {
    this.dbFile = new File(dbPath);
    if (dbFile.exists()) {
      throw new IllegalStateException("dbPath '" + dbPath + "' must not exist!");
    }

    this.dbPath = dbPath;
    this.blockSize = blockSize;
    this.cacheSize = cacheSize;
  }

  public void open() throws IOException {
    dbFile.mkdirs();
    
    this.parms = new OsobDbParms(dbPath, blockSize, cacheSize);
    this.id2block = new HashMap<Integer, OsobBlock>();
  }

  public void close() throws IOException {
    // create and write index and data
    final DataOutputStream indexOut = new DataOutputStream(new FileOutputStream(FileUtil.getFilename(dbPath, OsobIndex.INDEX_FILE_NAME)));
    final DataOutputStream dataOut = new DataOutputStream(new FileOutputStream(FileUtil.getFilename(dbPath, OsobData.DATA_FILE_NAME)));

    long curDataPos = 0L;

    // write the number of blocks as a header to the index.
    indexOut.writeInt(maxBlockId + 1);

    for (int blockId = 0; blockId <= maxBlockId; ++blockId) {
      indexOut.writeLong(curDataPos);

      final OsobBlock block = id2block.get(blockId);
      if (block == null) {
        indexOut.writeInt(0);
      }
      else {
        indexOut.writeInt(block.getNumOsobs());

        // write the osobs.
        for (Iterator<Osob> iter = block.iterator(); iter.hasNext(); ) {
          final Osob osob = iter.next();
          final byte[] bytes = osob.getBytes();

          dataOut.writeInt(bytes.length);
          curDataPos += 4;

          for (int i = 0; i < bytes.length; ++i) {
            dataOut.write(bytes[i]);
          }
          curDataPos += bytes.length;
        }
      }
    }

    dataOut.close();
    indexOut.close();
  }

  public void add(ConceptModel model) {
    if (!model.hasForms()) return;  // don't add empty forms to the osob db.

    final int conceptId = model.getConceptId();
    final int blockId = parms.getBlockId(conceptId);
    if (blockId > maxBlockId) maxBlockId = blockId;

    OsobBlock block = id2block.get(blockId);
    if (block == null) {
      block = new OsobBlock(parms.getBlockSize(), parms.getBaseConceptId(blockId));
      id2block.put(blockId, block);
    }

    final OsobSerializer serializer = new OsobSerializer(true);
    model.serialize(serializer);
    final Osob osob = serializer.getOsob();
    if (!block.addOsob(osob)) {
      System.err.println("*** WARNING: detected duplicate conceptId '" + osob.getConceptId() + "'! ...overwrote prior.");
    }
  }
}
