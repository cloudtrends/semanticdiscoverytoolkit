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
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Properties;
import org.sd.io.FileUtil;
import org.sd.util.tree.Tree;
import org.sd.util.PropertiesParser;
import org.sd.util.StringUtil;
import org.sd.util.WordIterator;
import org.sd.util.TextHeadingComparator;

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
  private HtmlDivHeadingComparator htmlDivComparator;
  private HtmlDivBlockComparator htmlBlockComparator;
  private boolean skipNonAlphaNum = false;

  private String lastPathId = null;

  public HtmlDivRipper(File htmlFile) throws IOException {
    this(htmlFile, true, true, true);
  }
  public HtmlDivRipper(File htmlFile, 
                       boolean useFullHeadings,
                       boolean useCapitalization,
                       boolean correctEncoding) 
    throws IOException 
  {
    this.leafRipper = new XmlLeafNodeRipper(FileUtil.getInputStream(htmlFile), 
                                            true, true, true, null, 
                                            true, null);
    this.htmlHelper = new HtmlHelper(useFullHeadings);
    this.inProgress = null;

    this.htmlDivComparator = new HtmlDivHeadingComparator(useFullHeadings, useCapitalization);
    this.htmlBlockComparator = new HtmlDivBlockComparator(new String[] {"center"});

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

  private boolean shouldSkip(Path path)
  {
    if (path.hasText())
    {
      String text = path.getText().trim();
      if(skipNonAlphaNum && 
         text.length() <= 3 && 
         StringUtil.firstLetterOrDigitPos(text, 0) > 0)
        return true;
      else
        return false;
    }
    else
    {
      if(PathHelper.isBreak(path))
        return false;
      else
        return true;
    }
  }

  private PathGroup getNextPathGroup() {
    PathGroup result = this.inProgress;
    this.inProgress = null;

    while (leafRipper.hasNext()) {
      final Tree<XmlLite.Data> leaf = leafRipper.next();
      final TagStack tagStack = leafRipper.getTagStack();
      final Path path = new Path(leaf, tagStack);

      if (shouldSkip(path)) 
        continue;

      if (result == null) result = new PathGroup();
      if (belongs(path, result)) {
        if(htmlDivComparator.shouldTerminateGroup(path))
        {
          result.terminate();
          this.inProgress = new PathGroup();
          break;
        }
        else
          result.add(path);
      }
      else {
        this.inProgress = new PathGroup();
        if(htmlDivComparator.shouldTerminateGroup(path))
          result.terminate();
        else
          this.inProgress.add(path);
        break;
      }
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

  /**
   * A path belongs in the pathGroup if it shares the deepest div or if
   * there is no div in either the path or the group.
   */
  protected boolean belongs(Path path, PathGroup pathGroup) 
  {
    boolean result = false;

    if (pathGroup == null || pathGroup.isEmpty())
      result = true;
    else {
      if(htmlBlockComparator.compare(pathGroup, path) >= 0)
        result = true;
      if(result && htmlDivComparator.compare(pathGroup, path) != 0)
        result = false;
    }

    return result;
  }

  public double getPathGroupHeadingStrength(PathGroup group)
  {
    /**
    double total = 0.0;
    int count = 0;
    int length = group.getText().length();
    for (Path path : group.getPaths())
    {
      if(PathHelper.isBreak(path) || shouldTerminateGroup(path))
        continue;

      count++;
      double weight = (double)path.getText().length() / (double)length;
      int strength = htmlHelper.computeHeadingStrength(path, true);
      total += strength * weight;
    }
    return total;
    */
    /**
    int result = -1;
    for (Path path : group.getPaths())
    {
      if(PathHelper.isBreak(path) || shouldTerminateGroup(path))
        continue;

      int strength = htmlHelper.computeHeadingStrength(path, true);
      if(result == -1 || strength < result) 
        result = strength;
    }

    return (result < 0 ? 0 : result);
    */
    double result = htmlDivComparator.computeHeadingStrength(group);
    return (result < 0.0 ? 0.0 : result);
  }

  public static void main(String[] args) throws IOException {
    final PropertiesParser pp = new PropertiesParser(args);
    final Properties properties = pp.getProperties();
    args = pp.getArgs();
    final boolean fullHeadings = 
      "true".equalsIgnoreCase(properties.getProperty("fullHeadings", "true"));
    final boolean capitalization = 
      "true".equalsIgnoreCase(properties.getProperty("capitalization", "true"));
    final boolean correctEncoding = 
      "true".equalsIgnoreCase(properties.getProperty("correctEncdoing", "true"));

    DecimalFormat df = new DecimalFormat("#.##");
    final HtmlDivRipper ripper = new HtmlDivRipper(new File(args[0]), fullHeadings, capitalization, correctEncoding);
    int groupNum = 1;
    while (ripper.hasNext()) {
      final PathGroup pathGroup = ripper.next();
      final double strength = ripper.getPathGroupHeadingStrength(pathGroup);
      System.out.println(groupNum + "("+df.format(strength)+"): " + pathGroup);
      ++groupNum;
    }

    ripper.close();
  }
}
