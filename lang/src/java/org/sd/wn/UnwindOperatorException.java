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


/**
 * An exception to use as a mechanism to halt recursive word sense expansion.
 * <p>
 * See the WordSenseOperator interface with its operate and handleUnwind
 * methods.
 *
 * @author Spence Koehler
 */
public class UnwindOperatorException extends Exception {

  private static final long serialVersionUID = 42L;

  private WordSenseWrapper childWrapper;
  private WordSenseWrapper parentWrapper;

  public UnwindOperatorException(WordSenseWrapper childWrapper, WordSenseWrapper parentWrapper) {
    super();

    this.childWrapper = childWrapper;
  }

  public WordSenseWrapper getChildWrapper() {
    return childWrapper;
  }

  public WordSenseWrapper getParentWrapper() {
    return parentWrapper;
  }
}
