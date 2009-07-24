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
package org.sd.text.lucene;


import org.sd.util.MathUtil;

import java.io.File;
import java.io.IOException;
import java.util.Date;

/**
 * Utility class to optimize an index.
 * <p>
 * @author Spence Koehler
 */
public class IndexOptimizer {

  public static final boolean optimize(String dirPath, boolean verbose) throws IOException {
    return optimize(new File(dirPath), verbose);
  }

  public static final boolean optimize(File dirPath, boolean verbose) throws IOException {
    boolean result = false;

    if (dirPath.exists()) {
      final Date startDate = new Date();
      final long startMillis = startDate.getTime();

      System.out.println(startDate + " IndexOptimizer.optimize(" + dirPath + ") starting.");
      try {
        optimize(dirPath);
        result = true;
      }
      catch (Throwable t) {
        System.err.println(new Date() + " caught unknown exception!:");
        t.printStackTrace();
      }

      final Date endDate = new Date();
      final long endMillis = endDate.getTime();

      System.out.println("\tfinished at " + endDate + ". TotalTime=" + MathUtil.timeString(endMillis - startMillis, false));
    }
    else {
      System.out.println("IndexOptimizer ignoring unknown arg '" + dirPath + "'!");
    }

    return result;
  }

  public static final void optimize(String dirPath) throws IOException {
    optimize(new File(dirPath));
  }

  public static final void optimize(File dirPath) throws IOException {
    final LuceneStore luceneStore = new LuceneStore(dirPath);
    luceneStore.open();
    luceneStore.close(true);
  }

  public static final void main(String[] args) throws IOException {

    for (int i = 0; i < args.length; ++i) {
      final String dirPath = args[i];
      optimize(dirPath, true);
    }
  }
}
