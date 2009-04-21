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
package org.sd.nlp;


import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A container for categories for a String.
 * <p>
 * @author Spence Koehler
 */
public class Categories {

  private Set<Category> categories;

  public Categories() {
    this.categories = new LinkedHashSet<Category>();
  }

  public Categories(Category[] categories) {
    this();
    for (Category type : categories) {
      this.categories.add(type);
    }
  }

  public Categories(Set<Category> categories) {
    this();
    if (categories != null) this.categories.addAll(categories);
  }

  /**
   * Copy constructor.
   */
  public Categories(Categories other) {
    this();
    if (other != null && other.categories != null) this.categories.addAll(other.categories);
  }

  public int size() {
    return (categories == null) ? 0 : categories.size();
  }

  public boolean hasType(Category type) {
    return categories.contains(type);
  }

  public Set<Category> getTypes() {
    return categories;
  }

  public boolean addType(Category type) {
    return categories.add(type);
  }

  public void addAllTypes(Categories other) {
    if (other != null && other.categories != null) {
      if (categories == null) this.categories = new LinkedHashSet<Category>();
      categories.addAll(other.categories);
    }
  }

  public String toString() {
    return categories == null ? "*undefined*" : categories.toString();
  }

  public boolean isEmpty() {
    return ((categories == null) || categories.isEmpty());
  }
}
