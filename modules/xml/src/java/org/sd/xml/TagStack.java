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


import java.util.List;
import java.util.Set;
import java.util.Map;

/**
 * Interface for representing and accessing a stack of xml tags.
 * <p>
 * @author Spence Koehler
 */
public interface TagStack {

  /**
   * Get a copy of the current stack from the top (root) to the bottom
   * (farthest from root).
   * <p>
   * NOTE: The result is a copy of the current stack and will not be effected
   *       by subsequent stack operations.
   */
  public List<XmlLite.Tag> getTags();

  /**
   * Get the tag at the given index or null if the index is out of range.
   */
  public XmlLite.Tag getTag(int index);

  /**
   * Get the names of the tags in this stack in the form t1.t2....tN, where
   * t1 is the root (top) of the stack and tN is the bottom of the stack.
   */
  public String getPathKey();

  /**
   * Get the names of the tags in this stack from the root (at index 0) down
   * to but not including the given index.
   */

  public String getPathKey(int index);
  /**
   * Get the names of the tags in this stack from the root (at index 0) down
   * to but not including the given index.
   * If useIndex flag is set, the child number will be printed for all tags
   */
  public String getPathKey(int index, boolean useIndex);

  /**
   * Get this stack's current depth.
   */
  public int depth();

  /**
   * Determine the position at which the current stack has the given tag name.
   *
   * @param tagName  an already lowercased tag name.
   *
   * @return the position of the tag name in the stack (where 0 is 'root')
   *         or -1.
   */
  public int hasTag(String tagName);

  /**
   * Determine the position at which the current stack has any of the given
   * tag names.
   *
   * @param tagNames already lowercased tag names.
   *
   * @return the position of the tag name in the stack (where 0 is 'root')
   *         or -1.
   */
  public int hasTag(Set<String> tagNames);

  /**
   * Determine the position at which the current stack has the given tag instance.
   *
   * @param tag  the tag instance to locate.
   *
   * @return the position of the tag in the stack (where 0 is 'root') or -1.
   */
  public int hasTag(XmlLite.Tag tag);

  /**
   * Determine the position at which the current stack has the any tag which has
   * an attribute with the specified value.
   * @param name attribute name
   * @param value attribute value
   * @return the position of the tag in the stack (where 0 is 'root') or -1.
   */
  public int hasTagAttribute(String name, String value);

  /**
   * Determine the position at which the current stack has the any tag which has
   * an attribute with any of the specified values.
   * @param name attribute name
   * @param values set of attribute value
   * @return the position of the tag in the stack (where 0 is 'root') or -1.
   */
  public int hasTagAttribute(String name, Set<String> values);

  /**
   * Determine the position at which the current stack has the any tag which has
   * any of the specified attributes with any of the specified values.
   * @param attrs a map of valid attribute names and the attribute values they may contain
   * @return the position of the tag in the stack (where 0 is 'root') or -1.
   */
  public int hasTagAttribute(Map<String,Set<String>> attrs);

  /**
   * Find the deepest tag this stack has in common with the other.
   */
  public XmlLite.Tag getDeepestCommonTag(TagStack other);

  /**
   * Find the index of the first divergent tag between this and the other stack.
   *
   * @return the index or -1 if the stacks don't intersect or depth() if they
   *         don't diverge.
   */
  public int findFirstDivergentTag(TagStack other);

  /**
   * Find the deepest index of the tag in this stack.
   *
   * @param tagName  the already lowercased tag name to find.
   *
   * @return the deepest position of the tag name in the stack (where 0 is 'root')
   *         or -1.
   */
  public int findDeepestTag(String tagName);

  /**
   * Find the deepest index of the tag in this stack which has the specified attribute name
   * @param tagName  the already lowercased tag name to find.
   * @param attr  attribute name
   * @return the deepest position of the tag name in the stack (where 0 is 'root')
   *         or -1.
   */
  public int findDeepestTag(String tagName, String attr);

  /**
   * Find the deepest index of any of the tags in the specified set in this stack.
   *
   * @param tagNames  the set of already lowercased tag names to find.
   *
   * @return the deepest position of the first matching tag name in the stack (where 0 is 'root')
   *         or -1.
   */
  public int findDeepestTag(Set<String> tagNames);

  /**
   * Find the deepest index of any of the tag in this stack which have the specified attribute name
   * @param tagNames  the set of already lowercased tag name to find.
   * @param attr attribute name
   * @return the deepest position of the tag name in the stack (where 0 is 'root')
   *         or -1.
   */
  public int findDeepestTag(Set<String> tagNames, String attr);

  /**
   * Get tags that have been saved with this tagstack.
   */
  public List<XmlLite.Tag> getSavedTags();
}
