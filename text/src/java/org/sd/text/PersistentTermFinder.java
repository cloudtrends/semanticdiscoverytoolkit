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


import org.sd.nlp.Normalizer;

import java.io.File;
import java.io.IOException;
import java.util.Date;

/**
 * Wrapper around a term finder to persist and re-use a persisted version
 * build from words in a file.
 * <p>
 * The persisted form will be named <file>.ptf for "persisted term finder."
 *
 * @author Spence Koehler
 */
public class PersistentTermFinder {

  public static final TermFinder getTermFinder(String label, File file, Normalizer normalizer) throws IOException {
    TermFinder result = null;

    final String name = file.getName();
    final File dir = file.getParentFile();
    final File persisted = new File(dir, name + ".ptf");

    if (persisted.exists()) {
      System.out.println(new Date() + ": Loading persisted term finder '" + persisted + "'...");
      result = loadPersistedTermFinder(persisted, label, file, normalizer);
      System.out.println(new Date() + ":  done loading persisted term finder '" + persisted + ".");
    }
    else {
      System.out.println(new Date() + ": Loading term finder '" + file + "'...");
      result = new TermFinder(label, normalizer);
      result.loadTerms(file);
      System.out.println(new Date() + ": Persisting term finder to '" + persisted + "'...");
      persistTermFinder(result, persisted);
      System.out.println(new Date() + ": done loading/persisting term finder '" + persisted + ".");
    }

    return result;
  }

  public static final TermFinder loadPersistedTermFinder(File persisted, String label, File terms, Normalizer normalizer) throws IOException {
    final TermFinder result = new TermFinder(label, normalizer);
    result.loadTerms(terms, persisted);
    return result;
  }

  public static final void persistTermFinder(TermFinder termFinder, File persisted) throws IOException {
    termFinder.persistTo(persisted);
  }
}
