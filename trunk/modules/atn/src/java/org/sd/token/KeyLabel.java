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
package org.sd.token;


/**
 * Enumeration of labels intended for use as keys to identify word types
 * based on word characteristics.
 * <p>
 * @author Spence Koehler
 */
public enum KeyLabel {

  AllLower('l'),
  Capitalized('c'), 
  AllCaps('C'), 
  SingleLower('i'), 
  SingleUpper('I'), 
  LowerMixed('m'), 
  UpperMixed('M'), 
  Number('n'), 
  MixedNumber('N'), 
  Special('s');

  private char defaultChar;

  KeyLabel(char c) {
    this.defaultChar = c;
  }

  public char getDefaultChar() {
    return defaultChar;
  }

  public static final KeyLabel getInstance(char c) {
    KeyLabel result = KeyLabel.Special;

    switch (c) {
      case 'l' : result = KeyLabel.AllLower; break;
      case 'c' : result = KeyLabel.Capitalized; break;
      case 'C' : result = KeyLabel.AllCaps; break;
      case 'i' : result = KeyLabel.SingleLower; break;
      case 'I' : result = KeyLabel.SingleUpper; break;
      case 'm' : result = KeyLabel.LowerMixed; break;
      case 'M' : result = KeyLabel.UpperMixed; break;
      case 'n' : result = KeyLabel.Number; break;
      case 'N' : result = KeyLabel.MixedNumber; break;
    }

    return result;
  }

  public static final String asString(KeyLabel[] keyLabels) {
    final StringBuilder result = new StringBuilder();

    if (keyLabels != null) {
      for (KeyLabel keyLabel : keyLabels) {
        result.append(keyLabel.getDefaultChar());
      }
    }

    return result.toString();
  }
}
