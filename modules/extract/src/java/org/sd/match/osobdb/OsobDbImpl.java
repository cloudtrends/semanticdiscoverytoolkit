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


import org.sd.match.Osob;
import org.sd.match.OsobDB;

import java.io.IOException;

/**
 * File-based db reader implementation of the osobdb interface.
 * <p>
 * @author Spence Koehler
 */
class OsobDbImpl implements OsobDB {
  
  private String dbPath;
  private OsobDbParms parms;
  private OsobBlockCache blockCache;
  private OsobData osobData;

  /**
   * Construct a new osob db (reader) instance pointing to the given path.
   *
   * @param dbPath  path to osobdb directory.
   */
  OsobDbImpl(String dbPath) throws IOException {
    this.dbPath = dbPath;
    init();
  }

  private final void init() throws IOException {
    this.parms = new OsobDbParms(dbPath);
    this.blockCache = new OsobBlockCache(parms.getCacheSize());
    this.osobData = new OsobData(dbPath, parms);
  }

  public void open() throws IOException {
    osobData.open();
  }

  public void close() throws IOException {
    osobData.close();
  }

  /**
   * Get the given osob from the database.
   */
  public Osob getOsob(int conceptId) throws IOException {
    // translate conceptId to blockId
    final int blockId = parms.getBlockId(conceptId);

    OsobBlock block = null;

    // get block id from block cache
    synchronized (blockCache) {
      block = blockCache.getBlock(blockId);
      if (block == null) {
        // if block isn't in cache, retrieve block by id from osob data
        block = osobData.retrieveBlock(blockId);
        blockCache.putBlock(blockId, block);
      }
    }

    // get osob from block
    return block.getOsob(conceptId);
  }

  public static void main(String[] args) throws IOException {
    //arg0: osob db path
    //args1+: concept ids

    final OsobDB osobDB = new OsobDbImpl(args[0]);
    osobDB.open();

    for (int i = 1; i < args.length; ++i) {
      final Osob osob = osobDB.getOsob(Integer.parseInt(args[i]));
      if (osob != null) {
        System.out.println(osob.getTreeString());
      }
      else {
        System.out.println("No osob found for " + args[i]);
      }
    }
  }
}
