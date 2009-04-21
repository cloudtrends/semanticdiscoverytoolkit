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


import java.util.concurrent.atomic.AtomicInteger;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * A factory for storing and retrieving valid categories.
 * <p>
 * @author Spence Koehler
 */
public class CategoryFactory {
  
  private static final AtomicInteger NEXT_FACTORY_ID = new AtomicInteger(0);

  private AtomicInteger nextId;
  private Map<Integer, Category> id2type;
  private Map<String, Category> label2type;

  private int factoryId;

  public CategoryFactory() {
    this.factoryId = NEXT_FACTORY_ID.getAndIncrement();

    this.nextId = new AtomicInteger(0);
    this.id2type = new HashMap<Integer, Category>();
    this.label2type = new HashMap<String, Category>();
  }

  public CategoryFactory(String categoriesString) {
    this();
    addCategories(categoriesString);
  }

  public CategoryFactory(String[] categories) {
    this();
    addCategories(categories);
  }

  /**
   * Add categories based on parsing the given categories string.
   * <p>
   * The categories string is a comma-delimitted string of category names, each
   * optionally preceded by a question mark. If the question mark is present,
   * then the 'canGuess' flag is set to true for the category.
   */
  public final void addCategories(String categoriesString) {
    final String[] pieces = categoriesString.split("\\s*,\\s*");
    addCategories(pieces);
  }

  public final void addCategories(String[] pieces) {
    for (String piece : pieces) {
      String categoryString = piece;
      boolean guessable = false;
      if (piece.charAt(0) == '?') {
        guessable = true;
        categoryString = piece.substring(1);
      }
      defineCategory(categoryString, guessable);
    }
  }

  /**
   * Define the category. Throw an IllegalArgumentException if not new.
   */
  public Category defineCategory(String label, boolean canGuess) {
    Category result = null;
    label = label.toUpperCase();
    synchronized (label2type) {
      if (label2type.get(label) != null) {
        throw new IllegalArgumentException("Category '" + label + "' already defined!");
      }

      final int id = nextId.getAndIncrement();
      result = new Category(id, label, canGuess);
      id2type.put(id, result);
      label2type.put(label, result);
    }
    return result;
  }

  /**
   * Get the instance of the category that has the given label.
   */
  public Category getCategory(String label) {
    final Category result = label2type.get(label);
    if (result == null) {
      throw new IllegalArgumentException("Category '" + label + "' is undefined!");
    }
    return result;
  }

  public Category[] getCategories(String[] labels) {
    final Category[] result = new Category[labels.length];
    int index = 0;
    for (String label : labels) {
      result[index++] = getCategory(label);
    }
    return result;
  }

  /** Get all category instances. */
  public Category[] getCategories() {
    Collection<Category> values = id2type.values();
    return values.toArray(new Category[values.size()]);
  }

  public boolean equals(Object o) {
    boolean result = (this == o);

    if (!result && o instanceof CategoryFactory) {
      final CategoryFactory other = (CategoryFactory)o;
      return factoryId == other.factoryId;
    }

    return result;
  }

  public int hashCode() {
    return factoryId;
  }
}
