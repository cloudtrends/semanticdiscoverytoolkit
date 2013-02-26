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
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with The Semantic Discovery Toolkit.  If not, see <http://www.gnu.org/licenses/>.
*/
package org.sd.xml;


import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import org.sd.io.FileUtil;
import org.sd.util.tree.Tree;

/**
 * A ripper that grabs text blocks under (deepest) div nodes.
 * <p>
 * @author Spence Koehler
 */
public class HtmlDivRipper implements Iterator<PathGroup> {
  
  private XmlLeafNodeRipper leafRipper;
  private PathGroup inProgress;
  private PathGroup next;

  public HtmlDivRipper(File htmlFile) throws IOException {
    this.leafRipper = new XmlLeafNodeRipper(FileUtil.getInputStream(htmlFile), true, null, false, null);
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
      if (commonPos >= 0) {
        final int deepestDiv = findDeepestBlockElement(pathGroup.getLastPath());
        //if ("text size:".equals(path.getText())) {
        //  final boolean stopHere = true;
        //}

        if (deepestDiv < 0) {  // pathGroup has no div
          // path belongs if it has no div as well
          if (!path.hasTagStack() || 
              path.getTagStack().hasTag(HtmlHelper.DEFAULT_BLOCK_TAGS) < 0) {
            result = true;
          }
        }
        else if (deepestDiv <= commonPos) {
          // since tag stack is common, this is a div in path, too
          result = true;
        }
        //else, fail when commonPos < pathGroup's deepest div
      }
    }

    return result;
  }

  protected int findDeepestBlockElement(Path path) {
    int result = -1;

    if (path != null && path.hasTagStack()) {
      result = path.getTagStack().findDeepestTag(HtmlHelper.DEFAULT_BLOCK_TAGS);
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
