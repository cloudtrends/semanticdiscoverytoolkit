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


import org.sd.xml.XmlTextRipper;
import org.sd.xml.XmlTextRipperFunction;
import org.sd.xml.XtrFunctionCallback;

/**
 * An XmlTextRipperFunction to apply a patternFinder.
 * <p>
 * @author Spence Koehler
 */
public class PatternFinderFunction implements XmlTextRipperFunction {

  private PatternFinder patternFinder;
  private XtrFunctionCallback callback;
  private int acceptPartial;

  public PatternFinderFunction(PatternFinder patternFinder, XtrFunctionCallback callback, int acceptPartial) {
    this.patternFinder = patternFinder;
    this.callback = callback;
    this.acceptPartial = acceptPartial;
  }

  /**
   * Hook to be run after each 'next' result is returned from the ripper.
   *
   * @return true to continue iteration; false to halt iteration.
   */
  public boolean runNextHook(String text, XmlTextRipper xtr) {
    boolean result = true;

    final boolean hasPattern = patternFinder.hasPattern(text, acceptPartial);
    if (hasPattern) {
      result = callback.analyze(text);
    }

    return result;
  }

  /**
   * Hook to be run after iteration through the ripper is complete.
   * <p>
   * Note that this is only called by a MultiFunctionTextRipper when
   * execution continues until the end; that is, when no function returns
   * false during 'execute'.
   */
  public void runDoneHook(XmlTextRipper xtr) {
  }
}
