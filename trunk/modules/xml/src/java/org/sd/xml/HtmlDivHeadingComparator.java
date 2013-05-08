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
  private final boolean ignoreBreaks;
  private final HtmlHelper htmlHelper;
  private final TextHeadingComparator textComparator;

  public HtmlDivHeadingComparator() {
    this(true, true, true);
  }
  public HtmlDivHeadingComparator(boolean useFullHeadings, 
                                  boolean useTextComparator) {
    this(true, useFullHeadings, useTextComparator);
  }
  public HtmlDivHeadingComparator(boolean ignoreBreaks,
                                  boolean useFullHeadings, 
                                  boolean useTextComparator)
  {
    this.ignoreBreaks = ignoreBreaks;
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
      if(strength > result) result = strength;
    }
    
    return result;
  }
  public Path getLastPath(PathGroup group)
  {
    Path lastPath = group.getLastPath(!ignoreBreaks);
    return lastPath;
  }

  public boolean shouldTerminateGroup(Path path)
  {
    boolean result = false;
    if(PathHelper.isHorizontalRule(path) ||
       htmlHelper.manualHeadingStrength(path.getNode()) > 0)
      result = true;
    return result;
  }

  public int compare(PathGroup group, Path path)
  {
    // if ignoreBreaks is turned on, get the last non break path
    Path last1 = group.getLastPath();
    Path last2 = group.getLastPath(!ignoreBreaks);

    boolean inlinePaths = false;
    if(last1 != null)
      inlinePaths = last1.equals(last2);
    return compare(last2, path, inlinePaths);
  }

  public int compare(Path path1, Path path2) {
    return compare(path1, path2, true);
  }
  public int compare(Path path1, Path path2, boolean possibleInlinePaths)
  {
    int result = 0;
    if(path1 == null)
      return result;

    // ignore inline paths
    if(possibleInlinePaths)
    {
      if(PathHelper.inlinePaths(path1,path2))
        return 0;
    }

    // if ignore breaks is on
    // break is always equivalent to the other path compared
    if(ignoreBreaks)
    {
      if(PathHelper.isBreak(path1) || 
         PathHelper.isBreak(path2))
        return 0;
    }

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
