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


/**
 * Common feature constants.
 * <p>
 * @author Spence Koehler
 */
public class CommonFeatures {
  
  /**
   * Feature type for the xml node containing the token's input text.
   */
  public static final String INPUT_XML_NODE = "inputXmlNode";

  /**
   * Feature type for a Normalized version of a token. The value would typically be the normalized form.
   */
  public static final String NORMALIZED = "normalized";

  /**
   * Feature type for a syntactic Category of a token. The value would be up to the categorizer.
   */
  public static final String CATEGORY = "category";

  /**
   * Feature type for Punctuation. The value would be up to the categorizer.
   */
  public static final String PUNCTUATION = "punctuation";

  /**
   * Feature type for Abbreviation. The value would typically be the full form of the abbreviated word.
   */
  public static final String ABBREVIATION = "abbreviation";

}
