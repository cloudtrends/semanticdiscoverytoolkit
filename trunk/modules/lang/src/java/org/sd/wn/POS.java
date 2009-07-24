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


import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Enumeration of part of speech types.
 * <p>
 * @author Spence Koehler
 */
public enum POS {
  
  NOUN("noun"),
  VERB("verb"),
  ADJ("adj"),
  ADV("adv"),
  ADJ_SATELLITE("adj"),
  PREP("prep"),
  CONJ("conj");


  public static final Map<String, POS> NAME_TO_POS = new HashMap<String, POS>();
  public static final Map<String, POS> ABBREV_TO_POS = new HashMap<String, POS>();
  static {
    ABBREV_TO_POS.put("n", NOUN);
    ABBREV_TO_POS.put("v", VERB);
    ABBREV_TO_POS.put("a", ADJ);
    ABBREV_TO_POS.put("r", ADV);
    ABBREV_TO_POS.put("s", ADJ_SATELLITE);

    ABBREV_TO_POS.put("1", NOUN);
    ABBREV_TO_POS.put("2", VERB);
    ABBREV_TO_POS.put("3", ADJ);
    ABBREV_TO_POS.put("4", ADV);
    ABBREV_TO_POS.put("5", ADJ_SATELLITE);

    NAME_TO_POS.put(NOUN.name, NOUN);
    NAME_TO_POS.put(VERB.name, VERB);
    NAME_TO_POS.put(ADJ.name, ADJ);
    NAME_TO_POS.put(ADV.name, ADV);
    NAME_TO_POS.put(PREP.name, PREP);
    NAME_TO_POS.put(CONJ.name, CONJ);
  }

  public final String name;

  POS(String name) {
    this.name = name;
  }

  /**
   * Get the part of speech by name.
   */
  public static final POS getPOS(String name) {
    return NAME_TO_POS.get(name);
  }

  /**
   * Get the index file for this part of speech.
   */
  public File getIndexFile(File dictDir) {
    return new File(dictDir, "index." + name);
  }

  /**
   * Get the data file for this part of speech.
   */
  public File getDataFile(File dictDir) {
    return new File(dictDir, "data." + name);
  }

  /**
   * Get the exception file for this part of speech.
   */
  public File getExceptionFile(File dictDir) {
    return new File(dictDir, name + ".exc");
  }
}
