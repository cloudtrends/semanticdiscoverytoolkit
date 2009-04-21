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
package org.sd.wn;


import java.util.HashMap;
import java.util.Map;

/**
 * A pointer filter that only expands along a single relation.
 * <p>
 * @author Spence Koehler
 */
public class SingleRelationFilter extends ConsistentPointerFilter {
  
  private static final Map<PointerSymbol, SingleRelationFilter> INSTANCES = new HashMap<PointerSymbol, SingleRelationFilter>();

  public static final SingleRelationFilter getInstance(PointerSymbol relation) {
    SingleRelationFilter result = INSTANCES.get(relation);
    if (result == null) {
      result = new SingleRelationFilter(relation);
      INSTANCES.put(relation, result);
    }
    return result;
  }

  private SingleRelationFilter(PointerSymbol relation) {
    super();
    addAcceptPointer(relation);
  }

  //java -Xmx640m org.sd.wn.SingleRelationFilter /usr/local/share/download/wordnet/WordNet-3.0/dict hypernym "pump"
  public static void main(String[] args) throws java.io.IOException {
    //arg0: dictFile
    //arg1: pointerSymbolName
    //args2+: words to lookup

    try {
      final java.io.File dictDir = args[0].length() > 1 ? new java.io.File(args[0]) : null;
      final  PointerSymbol pointerSymbol = PointerSymbol.valueOf(PointerSymbol.class, args[1].toUpperCase());
      final int wordsIndex = 2;

      final SingleRelationFilter pointerFilter = new SingleRelationFilter(pointerSymbol);

      WordSenseIterator.showAll(System.out, dictDir, args, wordsIndex, pointerFilter);
    }
    finally {
      WordNetUtils.closeAll();
    }
  }
}
