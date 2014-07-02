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


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sd.util.StatsAccumulator;
import org.sd.util.tree.Tree;

/**
 * Container for a group of paths.
 * <p>
 * @author Spence Koehler
 */
public class PathGroup {
  
  private List<Path> paths;
  private int commonPathIndex;
  private StringBuilder text;
  private StatsAccumulator wordCounts;
  private boolean terminated = false;

//collect or access attributes, tag names, etc.; compute deepest common tag; intersection with another PathGroup

  public PathGroup() {
    this.paths = new ArrayList<Path>();
    this.commonPathIndex = -1;
    this.text = new StringBuilder();
    this.wordCounts = new StatsAccumulator("wordCounts");
  }

  public boolean isEmpty() {
    return paths.size() == 0;
  }

  public int size() {
    return paths.size();
  }

  public int getCommonPathIndex() {
    return commonPathIndex;
  }

  public boolean isTerminated(){ return terminated; }
  public void terminate(){ terminated = true; }

  public void add(Path path) {
    if (path == null) return;
    int idx = computeCommonPathIndex(path);
    if(commonPathIndex >= 0)
      this.commonPathIndex = Math.min(idx, this.commonPathIndex);
    else
      this.commonPathIndex = idx;
    this.paths.add(path);
    addText(path);
  }

  public int computeCommonPathIndex(Path path) {
    int result = -1;

    if (path != null && path.hasTagStack()) {
      final Path lastPath = getLastPath();

      if (lastPath == null) {
        result = path.getTagStack().depth() - 1;
      }
      else {
        if (lastPath.hasTagStack()) {
          result = lastPath.getTagStack().findFirstDivergentTag(path.getTagStack());
          if (result >= 0) --result;
        }
      }
    }

    return result;
  }

  public List<Path> getPaths() {
    return paths;
  }

  public Path getFirstPath() {
    Path result = null;

    if (paths.size() > 0) {
      result = paths.get(0);
    }

    return result;
  }

  public Path getLastPath() {
    return getLastPath(true);
  }
  public Path getLastPath(boolean includeEmpties) {
    Path result = null;

    for(int i = paths.size() - 1; i >= 0 && result == null; i--)
    {
      Path path = paths.get(i);
      if(includeEmpties || path.hasText())
        result = path;
    }

    return result;
  }

  public String getText() {
    return text.toString();
  }

  public boolean hasTag(String tagName) {
    boolean result = false;

    for (Path path : paths) {
      if (path.hasTagStack() && path.getTagStack().hasTag(tagName) >= 0) {
        result = true;
        break;
      }
    }

    return result;
  }

  /**
   * Determine whether any of these tags are present in this group.
   */
  public boolean hasTag(String[] tagNames) {
    final Set<String> tagSet = new HashSet<String>();
    for (String tagName : tagNames) tagSet.add(tagName);
    return hasTag(tagSet);
  }

  /**
   * Determine whether any of these tags are present in this group.
   */
  public boolean hasTag(Set<String> tagNames) {
    boolean result = false;

    for (Path path : paths) {
      if (path.hasTagStack() && path.getTagStack().hasTag(tagNames) >= 0) {
        result = true;
        break;
      }
    }

    return result;
  }

  public double getBulk() {
    double result = 0.0;

    final long total = wordCounts.getN();
    if (total > 0) {
      result = wordCounts.getMean();
    }

    return result;
  }

  public StatsAccumulator getWordCounts() {
    return wordCounts;
  }

  public void rebuildText() {
    wordCounts.clear();
    this.text.setLength(0);

    this.commonPathIndex = -1;
    for (Path path : paths) {
      int idx = computeCommonPathIndex(path);
      if(commonPathIndex >= 0)
        this.commonPathIndex = Math.min(idx, this.commonPathIndex);
      else
        this.commonPathIndex = idx;
      addText(path);
    }
  }

  public String getCommonPathString(boolean includeIndex)
  {
    final StringBuilder result = new StringBuilder();

    if (commonPathIndex < 0) {
      result.append("<empty>");
    }
    else {
      Path firstPath = getFirstPath();
      TagStack tstack = firstPath.getTagStack();
      if(includeIndex)
        result.append(tstack.getPathKey(commonPathIndex+1, true));
      else
      {
        final List<XmlLite.Tag> tags = tstack.getTags();
        for (int i = 0; i <= commonPathIndex; ++i) {
          final XmlLite.Tag tag = tags.get(i);
          if (result.length() > 0) 
            result.append('.');
          result.append(tag.name);
        }
      }
    }

    return result.toString();
  }

  public String toString() {
    final StringBuilder result = new StringBuilder();

    result.
      append(getCommonPathString(true)).
      append('(').
      append(paths.size()).
      append(")=").
      append(text);

    return result.toString();
  }

  public DomElement asXml() 
  {
    final XmlStringBuilder result = new XmlStringBuilder();
    StringBuilder tag = new StringBuilder();
    tag.append("pathGroup paths='").append(paths.size()).append("'");
    result.addTag(tag.toString());
    if (commonPathIndex >= 0)
    {
      Path path = getFirstPath();
      result.addTagAndText("commonPath", path.toString(commonPathIndex));
      result.addTagAndText("text", text.toString());
    }
    result.addEndTag("pathGroup");
    return result.getXmlElement();
  }

  private final void addText(Path path) {
    if (path.hasText()) {
      final String pathText = path.getText();
      if (this.text.length() > 0) this.text.append(' ');
      this.text.append(pathText);

      final int pathWordCount = computeWordCount(pathText);
      wordCounts.add(pathWordCount);
    }
  }

  private final int computeWordCount(String text) {
    if ("".equals(text)) return 0;

    int result = 1;

    final int len = text.length();
    for (int i = 0; i < len; ++i) {
      if (text.charAt(i) == ' ') ++result;
    }

    return result;
  }
}
