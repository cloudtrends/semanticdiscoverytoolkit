/*
    Copyright 2011 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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
 * Enumeration of types for Pass/Fail results.
 * <p>
 * @author Spence Koehler
 */
public enum PassFail {
  
  PASS(true, true), FAIL(false, false), NOT_APPLICABLE(true, false);

  private boolean accept;
  private boolean conditionalAccept;

  PassFail(boolean accept, boolean conditionalAccept) {
    this.accept = accept;
    this.conditionalAccept = conditionalAccept;
  }

  public boolean accept() {
    return accept;
  }

  public boolean conditionalAccept() {
    return conditionalAccept;
  }

  public PassFail reverse() {
    PassFail result = this;

    if (result != PassFail.NOT_APPLICABLE) {
      result = getInstance(!accept);
    }

    return result;
  }

  public static PassFail getInstance(boolean accept) {
    return accept ? PassFail.PASS : PassFail.FAIL;
  }

  public static PassFail getInstance(boolean accept, boolean notApplicable) {
    PassFail result = null;

    if (accept) {
      result = notApplicable ? PassFail.NOT_APPLICABLE : PassFail.PASS;
    }
    else {
      result = PassFail.FAIL;
    }

    return result;
  }
}
