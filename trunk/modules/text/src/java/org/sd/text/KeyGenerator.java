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
package org.sd.text;


import org.sd.util.tree.Tree;
import org.sd.xml.PathHelper;
import org.sd.xml.XmlFactory;
import org.sd.xml.XmlLite;

import java.io.File;
import java.io.IOException;
import java.util.List;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * 
 * <p>
 * @author Spence Koehler
 */
public class KeyGenerator {
  
  private final Map<String, String> string2key;
  private final Map<String, String> key2string;
  private AtomicInteger nextKeyNumber;

  public KeyGenerator() {
    this.string2key = new LinkedHashMap<String, String>();
    this.key2string = new LinkedHashMap<String, String>();
    this.nextKeyNumber = new AtomicInteger(0);
  }

  /**
   * Add the string and generate a key for it.
   * <p>
   * If the string already exists with a key, just return the key.
   */
  public String addString(String string) {
    String key = string2key.get(string);
    if (key == null) {
      key = generateNextKey();
      string2key.put(string, key);
      key2string.put(key, string);
    }
    return key;
  }

  public String generateNextKey() {
    final int keyNumber = nextKeyNumber.getAndIncrement();
    final StringBuilder result = new StringBuilder();
    key2word(keyNumber, 27, result, '@');
    return result.toString();
  }

  protected void key2word(int number, int base, StringBuilder builder, char xbase) {
    int digit = number;
    final int div = (number / base);
    if (div > 0) {
      key2word(div, base, builder, xbase);
      digit = number - base * div;
    }
    builder.append((char)(xbase + digit));
  }


  public static void main(String[] args) throws IOException {
    final Tree<XmlLite.Data> xmlTree = XmlFactory.readXmlTree(new File(args[0]), true, true, false);
    final PathHelper pathHelper = new PathHelper(xmlTree, null);
    final List<Tree<XmlLite.Data>> leaves = pathHelper.getLeaves();

    final int flag = PathHelper.TEXT_DATA; // 0; //NUM_LOCAL_REPEAT_SIBS | NUM_CONSEC_LOCAL_SIBS;

    final KeyGenerator keyGenerator = new KeyGenerator();

    for (Tree<XmlLite.Data> leaf : leaves) {
      final String key = keyGenerator.addString(pathHelper.buildPathKey(leaf, 0, false));
      final String path = pathHelper.buildPathKey(leaf, PathHelper.TEXT_DATA | PathHelper.CONSECUTIVE_LOCAL_REPEAT_INDEX, false);

      System.out.println(key + " : " + path);
    }
  }
}
