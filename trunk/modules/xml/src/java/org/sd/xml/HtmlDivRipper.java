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


import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;
import org.sd.io.FileUtil;
import org.sd.util.tree.Tree;
import org.sd.util.StringUtil;
import org.sd.util.WordIterator;

/**
 * A ripper that grabs text blocks under (deepest) div nodes.
 * <p>
 * @author Spence Koehler
 */
public class HtmlDivRipper implements Iterator<PathGroup> {
  
  private XmlLeafNodeRipper leafRipper;
  private HtmlHelper htmlHelper;
  private PathGroup inProgress;
  private PathGroup next;

  public HtmlDivRipper(File htmlFile) throws IOException {
    this.leafRipper = new XmlLeafNodeRipper(FileUtil.getInputStream(htmlFile), true, null, false, null);
    this.htmlHelper = new HtmlHelper(true /* use full headings map */);
    this.inProgress = null;
    this.next = getNextPathGroup();
  }

  public boolean hasNext() {
    return (this.next != null);
  }

  public void close() {
    this.leafRipper.close();
  }

  public PathGroup next() {
    PathGroup result = next;
    this.next = getNextPathGroup();
    return result;
  }

  public void remove() {
    //do nothing.
  }

  private PathGroup getNextPathGroup() {
    PathGroup result = this.inProgress;
    this.inProgress = null;

    while (leafRipper.hasNext()) {
      final Tree<XmlLite.Data> leaf = leafRipper.next();
      final TagStack tagStack = leafRipper.getTagStack();
      final Path path = new Path(leaf, tagStack);
      if (!path.hasText()) continue;

      if (result == null) result = new PathGroup();
      if (belongs(path, result)) {
        result.add(path);
      }
      else {
        this.inProgress = new PathGroup();
        this.inProgress.add(path);
        break;
      }
    }

    return result;
  }

  /**
   * A path belongs in the pathGroup if it shares the deepest div or if
   * there is no div in either the path or the group.
   */
  protected boolean belongs(Path path, PathGroup pathGroup) {
    boolean result = false;

    if (pathGroup == null || pathGroup.isEmpty()) {
      result = true;
    }
    else {
      int commonPos = pathGroup.computeCommonPathIndex(path);
      Path lastPath = pathGroup.getLastPath();
      if (commonPos >= 0) {
        final int lastDeepestDiv = findDeepestBlockElement(lastPath);
        // todo: if the current path's deepest div is deeper than the last
        //       path's, it is possible that the current div may be nested
        //       in such a case, it might make sense to skip this path
        //       when comparing the next path for common pos, 
        //       i.e. the header div is considered to be the header for both
        //       the current block and the next block

        if (lastDeepestDiv < 0) {  // pathGroup has no div
          // path belongs if it has no div as well
          if (!path.hasTagStack() || 
              path.getTagStack().hasTag(HtmlHelper.DEFAULT_BLOCK_TAGS) < 0) {
            result = true;
          }
        }
        // since tag stack is common, this is a div in path, too
        else if (lastDeepestDiv <= commonPos) {
          result = true;
        }
        //else, fail when commonPos < pathGroup's deepest div
      }
      
      final int lastPathStrength = getPathHeadingStrength(lastPath);
      final int pathStrength = getPathHeadingStrength(path);
      // todo: check the path strength if the tag is not inline
      //       even with a tag which is not a block element,
      //       the tag may not share a common root element
      if(!lastPath.isInline(path))
      {
        if(lastPathStrength <= 1 && pathStrength <= 1 && isAllCapsPrefix(path))
          result = false;
        else if(pathStrength > lastPathStrength)
          result = false;
      }
    }

    return result;
  }
  
  public boolean isAllCapsPrefix(Path path)
  {
    boolean result = false;
    int count = 0;
    if(path.hasText())
    {
      String text = path.getText();
      for(WordIterator it = new WordIterator(text); it.hasNext();)
      {
        String word = it.next();
        // ignore short words
        if(word.length() < 3)
          continue;
        else if((word.length() > 4 || !StringUtil.isLikelyAbbreviation(word))
                && StringUtil.allCaps(word))
        {
          result = true;
          break;
        }
        else
        {
          result = false;
          break;
        }
      }
    }
    return result;
  }

  public int getPathGroupHeadingStrength(PathGroup group)
  {
    int result = 0;
    for (Path path : group.getPaths())
    {
      int strength = getPathHeadingStrength(path);
      if(strength != HtmlHelper.MAX_STRENGTH && strength > result)
        result = strength;
    }

    return result;
  }

  public int getPathHeadingStrength(Path path)
  {
    int result = 0;

    if(!path.hasTagStack())
      return result;
    
    for(XmlLite.Tag tag : path.getTagStack().getTags())
    {
      int strength = htmlHelper.computeHeadingStrength(tag);
      if(strength > result)
        result = strength;
    }
    
    // if the heading strength == 0
    //  and the text starts with all caps,
    //  set heading strength to 1
    if(result < 1 && isAllCapsPrefix(path))
      result = 1;

    return result;
  }
  
  protected int findDeepestBlockElement(Path path) {
    int result = -1;

    if (path != null && path.hasTagStack()) {
      TagStack tstack = path.getTagStack();
      result = tstack.findDeepestTag(HtmlHelper.DEFAULT_BLOCK_TAGS);
    }

    return result;
  }

  protected int findDeepestDiv(Path path) {
    int result = -1;

    if (path != null && path.hasTagStack()) {
      result = path.getTagStack().findDeepestTag("div");
    }

    return result;
  }


  public static void main(String[] args) throws IOException {
    final HtmlDivRipper ripper = new HtmlDivRipper(new File(args[0]));

    int groupNum = 1;
    while (ripper.hasNext()) {
      final PathGroup pathGroup = ripper.next();
      System.out.println(groupNum + ": " + pathGroup);
      ++groupNum;
    }

    ripper.close();
  }
}
