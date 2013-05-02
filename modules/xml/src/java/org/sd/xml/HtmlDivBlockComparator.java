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
import java.util.Set;
import java.util.HashSet;

/**
 */
public class HtmlDivBlockComparator 
  implements Comparator<Path> 
{
  private final Set<String> blockTags;

  public HtmlDivBlockComparator(String[] additionalBlockTags) 
  {
    this.blockTags = new HashSet<String>();
    for(String tag : HtmlHelper.DEFAULT_BLOCK_TAGS)
      blockTags.add(tag);
    if(additionalBlockTags != null)
    {
      for(String tag : additionalBlockTags)
        blockTags.add(tag);
    }
  }

  public int compare(PathGroup group, Path path) 
  {
    Path last = group.getLastPath();
    return compare(last, path);
  }
  public int compare(Path path1, Path path2)
  {
    int result = 0;
    if(path1 == null)
      return 0;

    // compute common path for path1 and path2
    int commonPos = computeCommonPathIndex(path1, path2);
    if (commonPos >= 0) {
      int leafDiv1 = getLeafDivIndex(path1);
      if (leafDiv1 < 0) {
        int rootDiv2 = path2.indexOfTag(blockTags);
        if (rootDiv2 < 0) result = 0;
      }
      else
        result = commonPos - leafDiv1;
    }

    return result;
  }

  private int getLeafDivIndex(Path path)
  {
    int result = -1;
    if(path != null)
      result = path.lastIndexOfTag(blockTags);
    return result;
  }

  private int computeCommonPathIndex(Path path1, Path path2) 
  {
    int result = -1;

    if(path2 == null || !path2.hasTagStack())
      return result;

    TagStack stack2 = path2.getTagStack();
    if (path1 == null)
      result = stack2.depth() - 1;
    else if (path1.hasTagStack()) {
      TagStack stack1 = path1.getTagStack();
      result = stack1.findFirstDivergentTag(stack2);
      if (result >= 0) --result;
    }
    
    return result;
  }
}
