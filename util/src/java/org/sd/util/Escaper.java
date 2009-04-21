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
package org.sd.util;


/**
 * A utility class to store, apply, and unapply escape mappings to Strings.
 *
 * @author Spence Koehler
 */
public class Escaper {

  private Mapping[] nonOverlapMappings;  // unescaped -> escaped

  private Mapping[] specialBackMappings; // escaped -> unescaped
  private Mapping[] normalBackMappings;  // escaped -> unescaped

  /**
   * Construct with the mappings (in order) to apply.
   * <p>
   * NOTE: The first mappings up to normalSetIndex are mappings form escape
   *       symbols themselves that cannot be applied in an overlapping
   *       fashion. The remainder of the mappings will be applied in an
   *       overlapping way.
   * <p>
   * @param mappings  Strings such that mappings[i][0] is the string whose
   *                  escaped mapping is mappings[i][1].
   * @param normalSetIndex  The index in the mappings at which the normal
   *                        set of overlappable mappings begins (as opposed
   *                        to the escaping of escape symbols that need to
   *                        be applied first without overlap.
   */
  public Escaper(String[][] mappings, int normalSetIndex) {
    this.nonOverlapMappings = new Mapping[mappings.length];
    this.specialBackMappings = new Mapping[normalSetIndex];
    this.normalBackMappings = new Mapping[mappings.length - normalSetIndex];

    for (int i = 0; i < mappings.length; ++i) {
      char[] mappings0 = mappings[i][0].toCharArray();
      char[] mappings1 = mappings[i][1].toCharArray();

      if (i < normalSetIndex) {
        this.specialBackMappings[i] = new Mapping(mappings1, mappings0);
      }
      else {
        this.normalBackMappings[i - normalSetIndex] = new Mapping(mappings1, mappings0);
      }
      this.nonOverlapMappings[i] = new Mapping(mappings0, mappings1);
    }
  }

  /**
   * Apply the escape mappings (forward).
   */
  public String escape(String string) {
    return applyMappings(nonOverlapMappings, string);
  }

  /**
   * Unapply the escape mappings (backward).
   */
  public String unescape(String string) {
    String result = applyMappings(normalBackMappings, string);
    return applyMappings(specialBackMappings, result);
  }

  private String applyMappings(Mapping[] mappings, String string) {
    StringBuffer result = new StringBuffer();

    if (string != null && string.length() > 0) {
      char[] stringChars = string.toCharArray();
      for (int stringIndex = 0; stringIndex < stringChars.length; ++stringIndex) {

        boolean mapped = false;
        for (int mappingIndex = 0; mappingIndex < mappings.length; ++mappingIndex) {
          Mapping mapping = mappings[mappingIndex];

          boolean matches = true;
          for (int sourceIndex = 0;
               sourceIndex < mapping.source.length && stringIndex + sourceIndex < stringChars.length;
               ++sourceIndex) {
            if (stringChars[stringIndex + sourceIndex] != mapping.source[sourceIndex]) {
              matches = false;
              break;
            }
          }

          if (matches) {
            mapped = true;
            result.append(mapping.dest);
            stringIndex += (mapping.source.length - 1);
            break;
          }
        }

        if (!mapped) {
          result.append(stringChars[stringIndex]);
        }
      }
    }

    return result.toString();
  }

  private class Mapping {
    private final char[] source;
    private final char[] dest;

    public Mapping(char[] source, char[] dest) {
      this.source = source;
      this.dest = dest;
    }
  }
}
