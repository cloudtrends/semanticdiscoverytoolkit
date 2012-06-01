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
package org.sd.atn.interp;


import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sd.atn.Parse;
import org.sd.xml.DomElement;

/**
 * NodeMatcher for RuleId's.
 * <p>
 * @author Spence Koehler
 */
public class RuleIdMatcher implements NodeMatcher {
  
  private Pattern pattern;

  RuleIdMatcher(DomElement matchElement, InnerResources resources) {
    //todo: decode attributes, for now, only doing regex 'matches'
    this.pattern = Pattern.compile(matchElement.getTextContent());
  }

  public boolean matches(Parse parse) {
    boolean result = false;
    final String ruleId = parse.getRuleId();
    if (ruleId != null && !"".equals(ruleId)) {
      final Matcher m = pattern.matcher(ruleId);
      result = m.matches();
    }
    return result;
  }
}
