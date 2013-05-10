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
import org.sd.util.WordIterator;

/**
 */
public class HtmlDivHeadingComparator 
  implements Comparator<Path> 
{
  private final boolean ignoreBreaks;
  private final boolean useCumulativeTags;
  private final HtmlHelper htmlHelper;
  private final TextHeadingComparator textComparator;

  public HtmlDivHeadingComparator() {
    this(true, true, true, true);
  }
  public HtmlDivHeadingComparator(boolean useFullHeadings, 
                                  boolean useTextComparator) {
    this(true, true, useFullHeadings, useTextComparator);
  }
  public HtmlDivHeadingComparator(boolean ignoreBreaks,
                                  boolean useCumulativeTags,
                                  boolean useFullHeadings, 
                                  boolean useTextComparator)
  {
    this.ignoreBreaks = ignoreBreaks;
    this.useCumulativeTags = useCumulativeTags;
    this.htmlHelper = new HtmlHelper(useFullHeadings);
    if(useTextComparator)
      this.textComparator = new TextHeadingComparator();
    else
      this.textComparator = null;
  }

  public double computeHeadingStrength(PathGroup group) {
    return computeHeadingStrength(group, true);
  }
  public double computeHeadingStrength(Path path) {
    return computeHeadingStrength(path, true);
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


  private double computeHeadingStrength(PathGroup group, boolean useText)
  {
    double total = 0.0;
    
    int count = 0;
    String gtext = group.getText();
    for(WordIterator it = new WordIterator(gtext); it.hasNext(); it.next())
      count++;

    for (Path path : group.getPaths())
    {
      if(PathHelper.isBreak(path) || shouldTerminateGroup(path))
        continue;

      int count2 = 0;
      String ptext = path.getText();
      for(WordIterator it = new WordIterator(ptext); it.hasNext(); it.next())
        count2++;

      double weight = (count != 0 ? (double)count2 / (double)count : 0.0);
      double strength = computeHeadingStrength(path, useText);
      total += strength * weight;
    }
    return total;
  }

  private double computeHeadingStrength(Path path, boolean useText) 
  {
    double result = 0.0;
    result = htmlHelper.computeHeadingStrength(path, useCumulativeTags);
    if(useText && result == 0.0 && textComparator != null)
    {
      String text = path.getText();
      result = textComparator.computeHeadingStrength(text);
    }
    return result;
  }
  
  public int compare(PathGroup group, Path path)
  {
    // if ignoreBreaks is turned on, get the last non break path
    /**
    Path last1 = group.getLastPath();
    Path last2 = group.getLastPath(!ignoreBreaks);

    boolean inlinePaths = false;
    if(last1 != null)
      inlinePaths = last1.equals(last2);
    return compare(last2, path, inlinePaths);
    */

    int result = 0;
    if(group.isEmpty() || group.getLastPath(false) == null)
      return result;
    if(ignoreBreaks && PathHelper.isBreak(path))
      return result;

    Path last = group.getLastPath();
    if(last != null && PathHelper.inlinePaths(last,path))
      return result;

    double strength1 = computeHeadingStrength(group, false);
    double strength2 = computeHeadingStrength(path, false);
    result = Double.compare(strength1, strength2);

    // if equivalent heading, use the text
    if(result == 0 && textComparator != null)
    {
      String text1 = group.getText();
      String text2 = path.getText();
      result = textComparator.compare(text1, text2);
    }

    return result;
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

    double strength1 = computeHeadingStrength(path1, false);
    double strength2 = computeHeadingStrength(path2, false);
    result = Double.compare(strength1, strength2);

    // if equivalent heading, use the text
    if(result == 0 && textComparator != null)
    {
      String text1 = path1.getText();
      String text2 = path2.getText();
      result = textComparator.compare(text1, text2);
    }

    return result;
  }
}
