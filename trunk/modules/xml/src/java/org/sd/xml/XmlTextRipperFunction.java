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


/**
 * Interface for hooks to be run during xml text ripper iteration.
 * See the MultiFunctionTextRipper class for how this is used.
 * <p>
 * @author Spence Koehler
 */
public interface XmlTextRipperFunction {

  /**
   * Hook to be run after each 'next' result is returned from the ripper.
   *
   * @return true to continue iteration; false to halt iteration.
   */
  public boolean runNextHook(String text, XmlTextRipper xtr);

  /**
   * Hook to be run after iteration through the ripper is complete.
   * <p>
   * Note that this is only called by a MultiFunctionTextRipper when
   * execution continues until the end; that is, when no function returns
   * false during 'execute'.
   */
  public void runDoneHook(XmlTextRipper xtr);
}
