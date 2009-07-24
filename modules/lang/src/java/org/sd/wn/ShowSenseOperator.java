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


import java.io.PrintStream;

/**
 * A word sense operator for showing related senses.
 * <p>
 * @author Spence Koehler
 */
public class ShowSenseOperator implements WordSenseOperator {
  
  private PrintStream out;

  public ShowSenseOperator() {
    this(System.out);
  }

  public ShowSenseOperator(PrintStream out) {
    this.out = out;
  }

  /**
   * Operate on the given child (expanded) word sense wrapper.
   * <p>
   * The UnwindOperatorException is intended as a mechanism to halt recursive
   * expansion. For example, if the target of a search is found, the exception
   * can be thrown and the handleUnwind method will be called.
   */
  public void operate(WordSenseWrapper childWrapper, WordSenseWrapper parentWrapper) throws UnwindOperatorException {
    final int curDepth = childWrapper.depth();

    for (int i = 0; i <= curDepth; ++i) out.print("\t");
    out.println(childWrapper);
  }

  /**
   * Handle the unwind exception thrown by operate.
   */
  public void handleUnwind(UnwindOperatorException e) {
    //nothing to do.
  }
}
