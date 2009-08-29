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
import java.util.Comparator;
import java.util.List;

import org.sd.io.FileUtil;

/**
 * Utility to iterate over multiple sorted file lines simultaneously, getting
 * all equal lines according to the lineComparer with each 'next'.
 * <p>
 * @author Spence Koehler
 */
public class MultiFileLineIterator extends MultiFileIterator<String> {

  /**
   * Iterate over the given file lines simultaneously.
   */
  public MultiFileLineIterator(File[] files, Comparator<String> lineComparer) {
    super(files, lineComparer);
  }

  /**
   * Build a RecordIterator instance for the given file.
   */
  protected FileRecordIterator<String> buildRecordIterator(File file) throws IOException {
    return new FileLineIterator(file);
  }


  public static void main(String[] args) throws IOException {
    // args: files

    final File[] files = new File[args.length];
    for (int i = 0; i < args.length; ++i) files[i] = new File(args[i]);

    final MultiFileLineIterator iter = new MultiFileLineIterator(files, new Comparator<String>() {
        public int compare(String s1, String s2) {
          return s1.compareTo(s2);
        }
      });

    while (iter.hasNext()) {
      final List<String> equallines = iter.next();
      System.out.println(equallines.size() + ":" + equallines.get(0));
    }

    iter.close();
  }
}
