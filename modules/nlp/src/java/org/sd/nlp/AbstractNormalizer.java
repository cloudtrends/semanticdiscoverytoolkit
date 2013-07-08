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
package org.sd.nlp;


import org.sd.io.FileUtil;
import org.sd.util.LRU;
import org.sd.util.LineBuilder;

import java.util.List;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;

/**
 * Abstract normalizer that implements the "utility" normalize method.
 * <p>
 * @author Spence Koehler
 */
public abstract class AbstractNormalizer implements Normalizer {
  
  /**
   * Normalize the substring's original text.
   */
  public abstract NormalizedString normalize(StringWrapper.SubString subString);

  //NOTE: We used to have an LRU cache here but it was found to be the source
  //      of a memory leak. If the cache is needed in the future, provisions
  //      must be added to properly clear out the entries to prevent a future
  //      leak. In the meantime, the cache has been removed. (sbk 2008-05-23.)

  protected AbstractNormalizer() {
  }

  /**
   * Normalize the string.
   */
  public NormalizedString normalize(String string) {
    NormalizedString result = null;

    if (string == null || string.length() == 0) return GeneralNormalizedString.EMPTY;

    final StringWrapper.SubString subString = new StringWrapper(string).getSubString(0);
    if (subString != null) {
      result = normalize(subString);
    }

    return result;
  }


  /**
   * Utility method to exercize a normalizer from its main.
   */
  protected static final void doNormalize(Normalizer normalizer, String[] args) throws IOException {
    for (int i = 0; i < args.length; ++i) {
      final String arg = args[i];
      final File file = new File(arg);

      if (file.exists()) {
        // this is a file to normalize
        final BufferedReader reader = FileUtil.getReader(file);
        String line = null;
        while ((line = reader.readLine()) != null) {
          doNormalize(normalizer, line);
        }
        reader.close();
      }
      else {
        // this is a string to normalize
        doNormalize(normalizer, arg);
      }
    }
  }

  protected static final void doNormalize(Normalizer normalizer, String string) {
    final NormalizedString nstring = normalizer.normalize(string);
    final String[] pieces = nstring.split();
    final LineBuilder builder = new LineBuilder();
    builder.append(string).append(nstring.getNormalized()).append(pieces);

    System.out.println(builder.toString());
  }
}
