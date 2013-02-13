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

  public void add(Path path) {
    if (path == null) return;
    this.commonPathIndex = computeCommonPathIndex(path);
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
    Path result = null;

    int size = paths.size();
    if (size > 0) {
      result = paths.get(size - 1);
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

    final int total = wordCounts.getN();
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
    for (Path path : paths) {
      addText(path);
    }
  }

  public String toString() {
    final StringBuilder result = new StringBuilder();

    if (commonPathIndex < 0) {
      result.append("<empty>");
    }
    else {
      final List<XmlLite.Tag> tags = getFirstPath().getTagStack().getTags();
      for (int i = 0; i <= commonPathIndex; ++i) {
        final XmlLite.Tag tag = tags.get(i);
        if (result.length() > 0) result.append('.');
        result.append(tag.name);
      }
    }

    result.
      append('(').
      append(paths.size()).
      append(")=").
      append(text);

    return result.toString();
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
