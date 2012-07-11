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
package org.sd.atn;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sd.token.Token;
import org.sd.xml.DomElement;

/**
 * Container for a sequence of RegexData instances.
 * <p>
 * @author Spence Koehler
 */
public class RegexDataContainer {

  private List<RegexData> regexes;
  private boolean hasRequired;

  public RegexDataContainer(DomElement regexesElement) {
    this.regexes = RegexData.load(regexesElement);
    this.hasRequired = false;

    if (regexes != null) {
      for (RegexData regex : regexes) {
        if (regex.isRequired()) {
          hasRequired = true;
          break;
        }
      }
    }
  }

  public int size() {
    return regexes == null ? 0 : regexes.size();
  }

  public List<RegexData> getRegexes() {
    return regexes;
  }

  public boolean hasRequired() {
    return hasRequired;
  }

  public boolean matches(String text, Token token) {
    return matches(text, token, true);
  }

  public boolean matches(String text, Token token, boolean addTokenFeature) {
    boolean result = false;

    if (regexes != null) {
      for (RegexData regex : regexes) {
        final boolean matches = regex.matches(text, token, addTokenFeature);

        if (matches) {
          result = true;
        }

        if (hasRequired) {
          if (!matches && regex.isRequired()) {
            result = false;
            break;
          }
        }
        else if (result) {
          break;
        }
      }
    }
    
    return result;
  }

  /**
   * Determine whether the key matches a regex (including all required).
   *
   * @return null if no match or a non-null (but possibly empty) map if it does match.
   */
  public Map<String, String> matches(String key) {
    boolean matches = false;
    Map<String, String> result = hasRequired ? new HashMap<String, String>() : null;

    if (regexes != null) {
      for (RegexData regex : regexes) {
        final RegexData.MatchResult matchResult = regex.patternMatches(key);

        if (matchResult.matches) {
          matches = true;
          result = matchResult.getAttributes(result);
        }

        if (hasRequired) {
          if (!matchResult.matches && regex.isRequired()) {
            matches = false;
            break;
          }
        }
        else if (matches) {
          break;
        }
      }
    }

    if (!matches) {
      result = null;
    }

    return (result == null && matches) ? RegexData.EMPTY_ATTRIBUTES : result;
  }
}
