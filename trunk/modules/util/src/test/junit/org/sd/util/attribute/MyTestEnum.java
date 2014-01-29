/*
    Copyright 2013 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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
package org.sd.util.attribute;


/**
 * Simple enumeration for package testing -- attributes describing a car.
 * <p>
 * @author Spence Koehler
 */
public enum MyTestEnum implements Canonical {

  MAKE(true), MODEL(true), STYLE(true), YEAR(true), OTHER(false);

  private boolean canonical;

  MyTestEnum(boolean canonical) {
    this.canonical = canonical;
  }

  public boolean isCanonical() {
    return canonical;
  }
}
