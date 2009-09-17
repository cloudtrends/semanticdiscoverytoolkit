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
package org.sd.cio.mapreduce;


import java.io.File;

import org.sd.util.NameGenerator;

/**
 * A common base for a FlushActionFactory that generates FlushAction instances
 * for Mappers or Reducers.
 * <p>
 * @author Spence Koehler
 */
public abstract class CommonFlushActionFactory<K, V, A> extends AbstractFlushActionFactory<K, V, A> {
  
  protected abstract String getOutputFilePrefix(A actionKey);
  protected abstract SimpleTextRecordFileStrategy<K, V> buildFlushFileStrategy();
  protected abstract FlushAction<K, V, A> buildFlushAction(A actionKey, File flushDir, NameGenerator nameGenerator, int maxPairs, SimpleTextRecordFileStrategy<K, V> flushFileStrategy);

  private File outDir;
  private final int maxPairs;
  private final Integer chainNum;
  private final int numDigits;

  protected CommonFlushActionFactory(File outDir, int maxPairs, Integer chainNum, int numDigits) {
    super();
    this.outDir = outDir;
    this.maxPairs = maxPairs;
    this.chainNum = chainNum;
    this.numDigits = numDigits;
  }

  protected final FlushAction<K, V, A> buildFlushAction(MapperPair<K, V, A> mapperPair) {
    final A actionKey = mapperPair.getActionKey();
    final String prefix = getOutputFilePrefix(actionKey);
    final File flushDir = new File(outDir, prefix);

    return buildFlushAction(actionKey,
                            flushDir,
                            ResourceHelper.buildNameGenerator(prefix + "-", ".gz", numDigits, chainNum),
                            maxPairs,
                            buildFlushFileStrategy());
  }
}
