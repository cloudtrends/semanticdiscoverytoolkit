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
package org.sd.xml;


import java.util.ArrayList;
import java.util.List;

/**
 * A wrapper around an XmlTextRipper that allows multiple functions to
 * be called during iteration.
 * <p>
 * @author Spence Koehler
 */
public class MultiFunctionTextRipper {
  
  private List<XmlTextRipperFunction> xtrFunctions;

  /**
   * Default constructor.
   * <p>
   * Add functions to be called during execute.
   */
  public MultiFunctionTextRipper() {
    this.xtrFunctions = new ArrayList<XmlTextRipperFunction>();
  }

  /**
   * Construct with the given functions.
   * <p>
   * More functions can be added through addFunction before a call to execute.
   */
  public MultiFunctionTextRipper(XmlTextRipperFunction[] xtrFunctions) {
    this();

    for (XmlTextRipperFunction xtrFunction : xtrFunctions) {
      addFunction(xtrFunction);
    }
  }

  /**
   * Add a function to be run during execution.
   */
  public final MultiFunctionTextRipper addFunction(XmlTextRipperFunction xtrFunction) {
    xtrFunctions.add(xtrFunction);
    return this;
  }

  /**
   * Iterate (or continue iterating) through the ripper's text, calling
   * function hooks until a function causes a halt or ripper iteration
   * terminates.
   * <p>
   * NOTE: it is the responsibility of the caller to close the XmlTextRipper.
   *
   * @return an xtrFunction whose hook halted iteration or null if ripper
   *         iteration reached the end or if there are no xtrFunctions.
   */
  public XmlTextRipperFunction execute(XmlTextRipper xtr) {

    if (xtrFunctions.size() == 0) return null;

    while (xtr.hasNext()) {
      final String text = xtr.next();

      for (XmlTextRipperFunction xtrFunction : xtrFunctions) {
        if (!xtrFunction.runNextHook(text, xtr)) {
          return xtrFunction;
        }
      }
    }

    for (XmlTextRipperFunction xtrFunction : xtrFunctions) {
      xtrFunction.runDoneHook(xtr);
    }

    return null;
  }
}
