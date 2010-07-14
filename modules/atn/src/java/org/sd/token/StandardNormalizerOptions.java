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
package org.sd.token;


import org.sd.xml.DataProperties;
import org.sd.xml.DomElement;

/**
 * Options for StandardNormalizer instances.
 * <p>
 * @author Spence Koehler
 */
public class StandardNormalizerOptions {
  
  /**
   * Option for common (lowercasing) input.
   * 
   * Default is true.
   */
  private boolean commonCase;
  public boolean getCommonCase() {
    return commonCase;
  }
  public void setCommonCase(boolean commonCase) {
    this.commonCase = commonCase;
  }

  /**
   * Option to replace non-letters-or-digits with whitespace.
   * 
   * Examples: "Ph.D." becomes "Ph D", "M. D." becomes "M  D",
   *           "Humpty-Dumpty" becomes "Humpty Dumpty",
   *           "$9.99" becomes "9 99"
   * 
   * Note that this takes effect before CompactWhite (when both are set).
   * 
   * Default is true.
   */
  private boolean replaceSymbolsWithWhite;
  public boolean getReplaceSymbolsWithWhite() {
    return replaceSymbolsWithWhite;
  }
  public void setReplaceSymbolsWithWhite(boolean replaceSymbolsWithWhite) {
    this.replaceSymbolsWithWhite = replaceSymbolsWithWhite;
  }

  /**
   * Option for compacting consecutive whitespace to a single space char.
   * 
   * Default is true.
   */
  private boolean compactWhite;
  public boolean getCompactWhite() {
    return compactWhite;
  }
  public void setCompactWhite(boolean compactWhite) {
    this.compactWhite = compactWhite;
  }


  /**
   * Construct with default options.
   */
  public StandardNormalizerOptions() {
    this.commonCase = true;
    this.replaceSymbolsWithWhite = true;
    this.compactWhite = true;
  }

  /**
   * Construct with the options in the element.
   */
  public StandardNormalizerOptions(DomElement optionsElement) {
    DataProperties options = new DataProperties(optionsElement);
    init(options);
  }

  /**
   * Construct with the given options.
   */
  public StandardNormalizerOptions(DataProperties options) {
    init(options);
  }

  private void init(DataProperties options) {
    this.commonCase = options.getBoolean("commonCase", true);
    this.replaceSymbolsWithWhite = options.getBoolean("replaceSymbolsWithWhite", true);
    this.compactWhite = options.getBoolean("compactWhite", true);
  }
}
