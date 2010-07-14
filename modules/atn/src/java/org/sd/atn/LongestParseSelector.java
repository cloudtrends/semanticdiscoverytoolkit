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


import java.util.ArrayList;
import java.util.List;
import org.sd.xml.DomNode;

/**
 * AtnParseSelector that chooses the longest parse.
 * <p>
 * @author Spence Koehler
 */
public class LongestParseSelector implements AtnParseSelector {
  
  public LongestParseSelector(DomNode domNode) {
  }

  public List<AtnParse> selectParses(AtnParseResult parseResult) {
    final List<AtnParse> result = new ArrayList<AtnParse>();

    int maxLen = 0;
    for (int parseIndex = 0; parseIndex < parseResult.getNumParses(); ++parseIndex) {
      final AtnParse parse = parseResult.getParse(parseIndex);
      final int curLen = parse.getEndIndex() - parse.getStartIndex();
      if (curLen > maxLen) maxLen = curLen;
    }

    for (int parseIndex = 0; parseIndex < parseResult.getNumParses(); ++parseIndex) {
      final AtnParse parse = parseResult.getParse(parseIndex);
      int curLen = parse.getEndIndex() - parse.getStartIndex();

      parse.setSelected(curLen == maxLen);

      if (parse.getSelected()) result.add(parse);
    }

    return result;
  }
}
