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
package org.sd.match;


/**
 * Bit definition for an osob.
 * <p>
 * @author Spence Koehler
 */
public class OsobBitDefinition extends BitDefinition {
  
  private static final OsobBitDefinition INSTANCE = new OsobBitDefinition();

  public static final OsobBitDefinition getInstance() {
    return INSTANCE;
  }

  // constants for osob bit definition levels.
  public static final String FORM = "FORM";
  public static final String TERM = "TERM";
  public static final String SYNONYM = "SYNONYM";
  public static final String VARIANT = "VARIANT";
  public static final String WORD = "WORD";

  private OsobBitDefinition() {
    super();

    addLevel(FORM, Form.Type._FINAL_.ordinal());
    addLevel(TERM, Decomp.Type._FINAL_.ordinal());
    addLevel(SYNONYM, Synonym.Type._FINAL_.ordinal());
    addLevel(VARIANT, Variant.Type._FINAL_.ordinal());
    addLevel(WORD, Word.Type._FINAL_.ordinal());
  }
}
