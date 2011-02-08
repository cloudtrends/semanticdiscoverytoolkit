/*
    Copyright 2010 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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
package org.sd.atn;


/**
 * Container for a match result (true or false) that can also optionally
 * specify whether the current token should be incremented (consumed).
 * <p>
 * @author Spence Koehler
 */
public class MatchResult {
  
  public static final MatchResult TRUE = new MatchResult(true, 0);
  public static final MatchResult FALSE = new MatchResult(false, 0);

  /**
   * Get the immutable true or false instance.
   */
  public static final MatchResult getInstance(boolean matched) {
    return matched ? TRUE : FALSE;
  }


  private boolean matched;
  private boolean inc;
  private boolean mutable;

  private MatchResult(boolean matched, int mutable) {
    this(matched);
    this.mutable = false;
  }

  public MatchResult(boolean matched) {
    this(matched, matched);
  }

  public MatchResult(boolean matched, boolean inc) {
    this.matched = matched;
    this.inc = inc;
    this.mutable = true;
  }

  public boolean matched() {
    return matched;
  }

  public void setMatched(boolean matched) {
    if (mutable) this.matched = matched;
    else throw new IllegalStateException("Can't change immutable instance!");
  }

  public boolean inc() {
    return inc;
  }

  public void setInc(boolean inc) {
    if (mutable) this.inc = inc;
    else throw new IllegalStateException("Can't change immutable instance!");
  }
}
