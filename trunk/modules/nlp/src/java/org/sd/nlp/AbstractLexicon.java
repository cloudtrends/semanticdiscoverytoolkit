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


import org.sd.util.ReflectUtil;
import org.sd.util.tree.Tree;
import org.sd.xml.XmlLite;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Abstract implementation of the lexicon interface.
 * <p>
 * Currently, this manages the stringDelims for the lexicon.
 *
 * @author Spence Koehler
 */
public abstract class AbstractLexicon implements Lexicon {
  
  public static final String NORMALIZED_ATTRIBUTE = "NORMALIZED";


  private AbstractNormalizer normalizer;
  private AtomicInteger maxNumWords;
  private Tree<XmlLite.Data> lexiconNode;
  private Map<String, Category> namedCategories;

  protected AbstractLexicon() {
    this.normalizer = null;
    this.maxNumWords = new AtomicInteger(0);
    this.lexiconNode = null;
    this.namedCategories = null;
  }

  protected AbstractLexicon(AbstractNormalizer normalizer) {
    this();
    this.normalizer = normalizer;
  }

  protected AbstractLexicon(Tree<XmlLite.Data> lexiconNode, CategoryFactory categoryFactory, AbstractNormalizer normalizer) {
    this();
    this.lexiconNode = lexiconNode;

    final boolean normalize = "true".equals(getAttribute("normalize"));
    final String normalizerClasspath = getAttribute("normalizer");
    if (normalize && normalizerClasspath != null && normalizerClasspath.length() > 0) {
      normalizer = (AbstractNormalizer)ReflectUtil.buildInstance(normalizerClasspath);
    }
    this.normalizer = normalize ? normalizer : null;

    setLexiconNode(lexiconNode);

    addNamedCategories(categoryFactory, getAttribute("category"));
    addNamedCategories(categoryFactory, getAttribute("categories"));
  }

  private final void addNamedCategories(CategoryFactory categoryFactory, String categories) {
    if (categories != null && categories.length() > 0) {
      final String[] pieces = categories.split("[,;\\|]");
      for (String piece : pieces) {
        piece = piece.trim();
        if (piece.length() > 0) {
          final Category category = categoryFactory.getCategory(piece);
          if (category != null) {
            if (namedCategories == null) namedCategories = new HashMap<String, Category>();
            namedCategories.put(piece, category);
          }
        }
      }
    }
  }

  protected final void setLexiconNode(Tree<XmlLite.Data> lexiconNode) {
    this.lexiconNode = lexiconNode;

    final String mnwString= getAttribute("maxNumWords");
    if (mnwString != null) {
      setMaxNumWords(Integer.parseInt(mnwString));
    }
  }

  protected Category getNamedCategory(String categoryName) {
    return (namedCategories == null) ? null : namedCategories.get(categoryName);
  }

  /**
   * Get the named attributed from the lexicon node.
   */
  protected String getAttribute(String name) {
    return (lexiconNode == null) ? null : lexiconNode.getData().asTag().getAttribute(name);
  }

  /**
   * Get the normalizer that applies to this lexicon.
   *
   * @return this lexicon's normalizer. null when no normalization occurs.
   */
  public AbstractNormalizer getNormalizer() {
    return normalizer;
  }

  /**
   * Get the maximum number of words in terms defined by this lexicon.
   *
   * @return the maximum number of words or 0 for unlimited.
   */
  public int maxNumWords() {
    return maxNumWords.get();
  }

  /**
   * Set the maximum number of words to be the given value.
   */
  public final void setMaxNumWords(int value) {
    this.maxNumWords.set(value);
  }

  /**
   * Normalize a string. This is usually called to make sure items added to a
   * lexicon are properly normalized for later matching against substrings.
   */
  protected final String normalize(String string) {
    String result = string;

    final StringWrapper stringWrapper = new StringWrapper(string);
    if (normalizer != null) result = normalizer.normalize(stringWrapper.getSubString(0)).getNormalized();

    final int curNumWords = stringWrapper.numWords();
    if (curNumWords > maxNumWords.get()) maxNumWords.set(curNumWords);

    return result;
  }

  /**
   * Look up the categories for a subString, adding definition that apply.
   *
   * @param subString  The subString to lookup categories for.
   */
  public void lookup(StringWrapper.SubString subString) {
    boolean result = false;

    int max = maxNumWords.get();
    if (max > 0 && subString.getNumWords() > max) return;

    if (!alreadyHasCategories(subString)) define(subString, normalizer);
  }

  private final boolean alreadyHasCategories(StringWrapper.SubString subString) {
    boolean result = false;

    final Categories categories = subString.getCategories();
    if (categories != null) {
      result = alreadyHasTypes(categories);
    }

    return result;
  }

  /**
   * Define applicable categories in the subString.
   *
   * @param subString   The substring to define.
   * @param normalizer  The normalizer to use.
   */
  protected abstract void define(StringWrapper.SubString subString, AbstractNormalizer normalizer);

  /**
   * Determine whether the categories container already has category type(s)
   * that this lexicon would add.
   * <p>
   * NOTE: this is used to avoid calling "define" when categories already exist
   *       for the substring.
   *
   * @return true if this lexicon's category type(s) are already present.
   */
  protected abstract boolean alreadyHasTypes(Categories categories);
}
