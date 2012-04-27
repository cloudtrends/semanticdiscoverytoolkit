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
package org.sd.atn;


import java.util.List;
import org.sd.util.Usage;
import org.sd.xml.DataProperties;
import org.sd.xml.DomElement;

/**
 * Interface for getting an interpretatin for a parse if available.
 * <p>
 * NOTE: Implementations of this must be thread-safe!
 *
 * @author Spence Koehler
 */
@Usage(notes = "Interface for getting an interpretation for a parse if available.")
public interface ParseInterpreter {
  
  /**
   * Get classifications offered by this interpreter.
   * 
   * Note that classifications are applied to parse-based tokens. Access
   * to the potential classifications is intended to help with monitoring,
   * introspection, and other high-level tools for building grammars and
   * parsers.
   */
  public String[] getClassifications();

  /**
   * Get the interpretations for the parse or null.
   */
  public List<ParseInterpretation> getInterpretations(Parse parse, DataProperties overrides);

  /**
   * Supplement this interpreter according to the given domElement.
   */
  public void supplement(DomElement domElement);
}
