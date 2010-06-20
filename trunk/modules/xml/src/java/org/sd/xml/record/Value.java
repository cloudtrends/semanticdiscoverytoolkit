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
package org.sd.xml.record;


import org.sd.util.tree.Tree;
import org.sd.xml.XmlLite;

/**
 * A value is a terminal record holding data.
 * <p>
 * @author Spence Koehler
 */
public class Value extends Record {

  private XmlLite.Text data;

  /**
   * Construct a new value.
   * <p>
   * Note that the same instance of a value may be used in multiple
   * records.
   */
  public Value(String data) {
    this("", data);
  }

  /**
   * Construct a new named value.
   */
  public Value(String name, String data) {
    super(name);

    this.data = new XmlLite.Text(data);
    this.data.setProperty("_record", this);
  }

  public Value(XmlLite.Text data) {
    super("");
    this.data = data;
    this.data.setProperty("_record", this);
  }

  /**
   * Safely downcast this record to a value if it is one.
   */
  public Value asValue() {
    return this;
  }

  /**
   * Get this value's data.
   */
  public String getData() {
    return data.text;
  }

  /**
   * Get this value as a tree.
   */
  public Tree<XmlLite.Data> asTree() {
    Tree<XmlLite.Data> result = new Tree<XmlLite.Data>(data);
    data.setContainer(result);
    return result;
  }

  /**
   * Set this value's path id.
   */
  public void setPathId(int pathId) {
    data.setProperty("_pathId", pathId);
  }

  /**
   * Get this value's path id.
   */
  public int getPathId() {
    final Object result = data.getProperty("_pathId");
    return (result == null) ? -1 : (Integer)result;
  }

  /**
   * Set this value's path num.
   */
  public void setPathNum(int pathNum) {
    data.setProperty("_pathNum", pathNum);
  }

  /**
   * Get this value's path num.
   */
  public int getPathNum() {
    final Object result = data.getProperty("_pathNum");
    return (result == null) ? -1 : (Integer)result;
  }
}
