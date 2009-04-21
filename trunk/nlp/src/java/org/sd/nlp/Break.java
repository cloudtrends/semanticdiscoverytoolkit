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


/**
 * 
 * <p>
 * @author Spence Koehler
 */
public enum Break {
  
  SOFT_FULL(true, true, true),     // i.e. "-" in "hyphenated-word"  (- is a delim)
  SOFT_SPLIT(true, true, false),   // i.e. "C" and "W" in "camelCaseWord" (C, W are not delims)
  HARD(false, true, true),         // i.e. "!" in "The End!"
  NONE(false, false, false);

  private boolean soft;
  private boolean breaks;
  private boolean skip;

  Break(boolean soft, boolean breaks, boolean skip) {
    this.soft = soft;
    this.breaks = breaks;
    this.skip = skip;
  }

  public boolean isSoft() {
    return soft;
  }

  public boolean breaks() {
    return breaks;
  }

  // when looking for the next start, skip consecutive breaks
  public boolean skip() {
    return skip;
  }
}
