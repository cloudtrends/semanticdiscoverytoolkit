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
package org.sd.xml;


import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Map;

/**
 * Abstract base implementation of the tag stack interface.
 * <p>
 * @author Spence Koehler
 */
public abstract class BaseTagStack implements TagStack {

  private String _pathKey = null;
  private List<XmlLite.Tag> savedTags;
  private boolean useTagEquivalents = false;

  /**
   * Get a handle on the instance's tags list ordered from the (top) root
   * to the bottom.
   */
  protected abstract List<XmlLite.Tag> getTagsList();

  protected BaseTagStack() {
    this(false);
  }
  protected BaseTagStack(boolean useTagEquivalents) {
    this.useTagEquivalents = useTagEquivalents;
  }
  protected BaseTagStack(List<XmlLite.Tag> savedTags) {
    this(savedTags, false);
  }
  protected BaseTagStack(List<XmlLite.Tag> savedTags, boolean useTagEquivalents) {
    this.savedTags = savedTags;
    this.useTagEquivalents = useTagEquivalents;
  }

  /**
   * Get the tag at the given index or null if the index is out of range.
   */
  public XmlLite.Tag getTag(int index) {
    XmlLite.Tag result = null;

    final List<XmlLite.Tag> tagsList = getTagsList();
    if (index >= 0 && index < tagsList.size()) {
      result = tagsList.get(index);
    }

    return result;
  }

  /**
   * Get the names of the tags in this stack in the form t1.t2....tN, where
   * t1 is the root (top) of the stack and tN is the bottom of the stack.
   */
  public String getPathKey() {
    if (_pathKey == null) {
      final PathKeyBuilder result = new PathKeyBuilder();
      result.addAll(getTagsList());
      _pathKey = result.getPathKey();
    }
    return _pathKey;
  }

  /**
   * Get the names of the tags in this stack from the root (at index 0) down
   * to but not including the given index.
   */
  public String getPathKey(int index) {
    return getPathKey(index, false);
  }
  public String getPathKey(int index, boolean useIndex) {
    if (_pathKey == null) {
      final PathKeyBuilder result = new PathKeyBuilder(useIndex);
      
      for (int i = 0; i < index; ++i) {
        final XmlLite.Tag tag = getTag(i);
        if (tag != null) result.add(tag);
      }
      _pathKey = result.getPathKey();
    }
    return _pathKey;
  }

  /**
   * Get this stack's current depth.
   */
  public int depth() {
    return getTagsList().size();
  }

  /**
   * Determine the position at which the current stack has the given tag name.
   *
   * @param tagName  an already lowercased tag name.
   *
   * @return the position of the tag name in the stack (where 0 is 'root')
   *         or -1.
   */
  public int hasTag(String tagName) {
    if (tagName == null) return -1;

    int result = 0;
    final List<XmlLite.Tag> tags = getTagsList();
    for (XmlLite.Tag tag : tags) {
      if (tagName.equals(tag.name)) break;
      ++result;
    }
    return result >= tags.size() ? -1 : result;
  }

  /**
   * Determine the position at which the current stack has any of the given
   * tag names.
   *
   * @param tagNames an already lowercased tag name.
   *
   * @return the position of the tag name in the stack (where 0 is 'root')
   *         or -1.
   */
  public int hasTag(Set<String> tagNames) {
    if (tagNames == null) return -1;

    int result = 0;
    final List<XmlLite.Tag> tags = getTagsList();
    for (XmlLite.Tag tag : tags) {
      if (tagNames.contains(tag.name)) break;
      ++result;
    }
    return result >= tags.size() ? -1 : result;
  }

  /**
   * Determine the position at which the current stack has the given tag instance.
   *
   * @param tag  the tag instance to locate.
   *
   * @return the position of the tag in the stack (where 0 is 'root') or -1.
   */
  public int hasTag(XmlLite.Tag tag) {
    if (tag == null) return -1;

    int result = 0;
    final List<XmlLite.Tag> tags = getTagsList();
    for (XmlLite.Tag curTag : tags) {
      if (curTag == tag) break;
      ++result;
    }
    return result >= tags.size() ? -1 : result;
  }

  /**
   * Determine the position at which the current stack has the any tag which has
   * an attribute with the specified value.
   * @param name attribute name
   * @param value attribute value
   * @return the position of the tag in the stack (where 0 is 'root') or -1.
   */
  public int hasTagAttribute(String name, String value) {
    if (name == null || value == null) return -1;

    int result = 0;
    final List<XmlLite.Tag> tags = getTagsList();
    for (XmlLite.Tag tag : tags) {
      String val = tag.getAttribute(name);
      if(value.equals(val)) break;
      ++result;
    }
    return result >= tags.size() ? -1 : result;
  }

  /**
   * Determine the position at which the current stack has the any tag which has
   * an attribute with any of the specified values.
   * @param name attribute name
   * @param values set of attribute value
   * @return the position of the tag in the stack (where 0 is 'root') or -1.
   */
  public int hasTagAttribute(String name, Set<String> values) {
    if (name == null || values == null || values.size() == 0) return -1;

    int result = 0;
    final List<XmlLite.Tag> tags = getTagsList();
    for (XmlLite.Tag tag : tags) {
      String val = tag.getAttribute(name);
      if(values.contains(val)) break;
      ++result;
    }
    return result >= tags.size() ? -1 : result;
  }

  /**
   * Determine the position at which the current stack has the any tag which has
   * any of the specified attributes with any of the specified values.
   * @param attrs a map of valid attribute names and the attribute values they may contain
   * @return the position of the tag in the stack (where 0 is 'root') or -1.
   */
  public int hasTagAttribute(Map<String,Set<String>> attrs) {
    if (attrs.size() == 0) return -1;

    int result = 0;
    final List<XmlLite.Tag> tags = getTagsList();
    for (XmlLite.Tag tag : tags) {
      boolean found = false;
      for(Map.Entry<String,Set<String>> entry : attrs.entrySet())
      {
        String name = entry.getKey();
        Set<String> values = entry.getValue();
        String val = tag.getAttribute(name);
        if(values.contains(val))
        {
          found = true;
          break;
        }
      }
      
      if(found) break;
      
      ++result;
    }
    return result >= tags.size() ? -1 : result;
  }

  private boolean equivalentTags(XmlLite.Tag a, XmlLite.Tag b) 
  {
    if(useTagEquivalents)
    {
      if(a == b) 
        return true;
      else if(a == null && b != null)
        return false;
      
      boolean result = false;
      if (a != null && a.equals(b))
        result = a.getChildNum() == b.getChildNum();
      return result;
    }      
    else
      return a == b;
  }

  /**
   * Find the deepest tag this stack has in common with the other.
   */
  public XmlLite.Tag getDeepestCommonTag(TagStack other) {
    if (other == null) return null;

    final Iterator<XmlLite.Tag> myTagsIter = getTagsList().iterator();
    final Iterator<XmlLite.Tag> otherTagsIter = ((BaseTagStack)other).getTagsList().iterator();

    XmlLite.Tag result = null;

    while (myTagsIter.hasNext() && otherTagsIter.hasNext()) {
      final XmlLite.Tag myTag = myTagsIter.next();
      final XmlLite.Tag otherTag = otherTagsIter.next();

      if (!equivalentTags(myTag, otherTag)) {
        break;
      }
      else {
        result = myTag;
      }
    }

    return result;
  }

  /**
   * Find the index of the first divergent tag between this and the other stack.
   *
   * @return the index or -1 if the stacks don't intersect or depth() if they
   *         don't diverge.
   */
  public int findFirstDivergentTag(TagStack other) {
    if (other == null) return -1;

    final Iterator<XmlLite.Tag> myTagsIter = getTagsList().iterator();
    final Iterator<XmlLite.Tag> otherTagsIter = ((BaseTagStack)other).getTagsList().iterator();

    int result = -1;
    boolean match = true;
    while (myTagsIter.hasNext() && otherTagsIter.hasNext()) {
      final XmlLite.Tag myTag = myTagsIter.next();
      final XmlLite.Tag otherTag = otherTagsIter.next();

      ++result;

      if (!equivalentTags(myTag, otherTag)) {
        match = false;
        break;
      }
    }

    // if we run out of tags, these are common blocks
    if(match) result++;
    return result;
  }

  /**
   * Find the deepest index of the tag in this stack.
   *
   * @param tagName  the already lowercased tag name to find.
   *
   * @return the deepest position of the tag name in the stack (where 0 is 'root')
   *         or -1.
   */
  public int findDeepestTag(String tagName) {
    int result = -1;

    if (tagName != null) {
      final List<XmlLite.Tag> tags = getTagsList();
      for (result = tags.size() - 1; result >= 0; --result) {
        if (tagName.equals(tags.get(result).name)) {
          break;
        }
      }
    }

    return result;
  }

  /**
   * Find the deepest index of the tag in this stack which has the specified attribute name
   * @param tagName  the already lowercased tag name to find.
   * @param attr  attribute name
   * @return the deepest position of the tag name in the stack (where 0 is 'root')
   *         or -1.
   */
  public int findDeepestTag(String tagName, String attr) {
    int result = -1;

    if (tagName != null) {
      final List<XmlLite.Tag> tags = getTagsList();
      for (result = tags.size() - 1; result >= 0; --result) {
        if (tagName.equals(tags.get(result).name) && 
            tags.get(result).getAttribute(attr) != null) {
          break;
        }
      }
    }

    return result;
  }

  /**
   * Find the deepest index of any of the tags in the specified set in this stack.
   * @param tagNames  the set of already lowercased tag names to find.
   * @return the deepest position of the first matching tag name in the stack (where 0 is 'root')
   *         or -1.
   */
  public int findDeepestTag(Set<String> tagNames) {
    int result = -1;

    if (tagNames != null && tagNames.size() > 0) {
      final List<XmlLite.Tag> tags = getTagsList();
      for (result = tags.size() - 1; result >= 0; --result) {
        if (tagNames.contains(tags.get(result).name)) {
          break;
        }
      }
    }

    return result;
  }

  /**
   * Find the deepest index of any of the tag in this stack which have the specified attribute name
   * @param tagNames  the set of already lowercased tag name to find.
   * @param attr attribute name
   * @return the deepest position of the tag name in the stack (where 0 is 'root')
   *         or -1.
   */
  public int findDeepestTag(Set<String> tagNames, String attr) {
    int result = -1;

    if (tagNames != null && tagNames.size() > 0) {
      final List<XmlLite.Tag> tags = getTagsList();
      for (result = tags.size() - 1; result >= 0; --result) {
        if (tagNames.contains(tags.get(result).name) && 
            tags.get(result).getAttribute(attr) != null) {
          break;
        }
      }
    }

    return result;
  }

  /**
   * Get tags that have been saved with this tagstack.
   */
  public List<XmlLite.Tag> getSavedTags() {
    return savedTags;
  }

  /**
   * Get a string representation of this tag stack.
   */
  public String toString() {
    return getPathKey();
  }

  protected void clearPathKey() {
    _pathKey = null;
  }
}
