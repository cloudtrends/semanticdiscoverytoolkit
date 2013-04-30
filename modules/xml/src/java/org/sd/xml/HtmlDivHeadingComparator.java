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
    GNU Lesser General Public License for more details.\

    You should have received a copy of the GNU Lesser General Public License
    along with The Semantic Discovery Toolkit.  If not, see <http://www.gnu.org/licenses/>.
*/
package org.sd.xml;

import java.util.Comparator;
import org.sd.util.TextHeadingComparator;

/**
 */
public class HtmlDivHeadingComparator 
  implements Comparator<Path> 
{
  private final HtmlHelper htmlHelper;
  private final TextHeadingComparator textComparator;

  public HtmlDivHeadingComparator(boolean useFullHeadings, 
                                  boolean useTextComparator)
  {
    this.htmlHelper = new HtmlHelper(useFullHeadings);
    if(useTextComparator)
      this.textComparator = new TextHeadingComparator();
    else
      this.textComparator = null;
  }

  public int computeHeadingStrength(PathGroup group)
  {
    int result = 0;
    for (Path path : group.getPaths())
    {
      if(PathHelper.isBreak(path) || shouldTerminateGroup(path))
        continue;

      int strength = htmlHelper.computeHeadingStrength(path, true);
      if(strength != HtmlHelper.MAX_STRENGTH && strength > result)
        result = strength;
    }
    
    return result;
  }

  public boolean shouldTerminateGroup(Path path)
  {
    boolean result = false;
    if(PathHelper.isHorizontalRule(path) ||
       htmlHelper.manualHeadingStrength(path.getNode()) > 0)
      result = true;
    return result;
  }

  public int compare(Path path1, Path path2)
  {
    int result = 0;
    if(PathHelper.inlinePaths(path1,path2))
      return result;

    int strength1 = htmlHelper.computeHeadingStrength(path1, true);
    int strength2 = htmlHelper.computeHeadingStrength(path2, true);
    result = Integer.compare(strength1, strength2);

    if(result == 0 && textComparator != null)
    {
      String text1 = path1.getText();
      String text2 = path2.getText();
      result = textComparator.compare(text1, text2);
    }

    return result;
  }
}
