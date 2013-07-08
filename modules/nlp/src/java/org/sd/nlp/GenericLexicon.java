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


import org.sd.io.FileUtil;
import org.sd.util.SdnUtil;
import org.sd.util.tree.Tree;
import org.sd.xml.XmlLite;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.HashMap;

/**
 * Lexicon for classifying terms from a generic rote list.
 * <p>
 * @author Dave Barney, Spence Koehler
 */
public class GenericLexicon extends AbstractLexicon {
  
  /**
   * Convenience method for loading a lexicon from a resource.
   */
  public static final GenericLexicon loadGenericLexicon(Class clazz, String resourceName,
                                                        AbstractNormalizer normalizer, Category category,
                                                        boolean caseSensitive, boolean isDefinitive,
                                                        boolean hasAttributes, String commonAttributes) throws IOException {
    return new GenericLexicon(FileUtil.getInputStream(clazz, resourceName), normalizer,
                              category, caseSensitive, isDefinitive, hasAttributes, commonAttributes);
  }

  /**
   * Convenience method for loading a lexicon from a resource.
   */
  public static final GenericLexicon loadGenericLexicon(InputStream lexiconInputStream,
                                                        AbstractNormalizer normalizer, Category category,
                                                        boolean caseSensitive, boolean isDefinitive,
                                                        boolean hasAttributes, String commonAttributes) throws IOException {
    return new GenericLexicon(lexiconInputStream, normalizer,
                              category, caseSensitive, isDefinitive, hasAttributes, commonAttributes);
  }

  /**
   * Convenience method for loading a lexicon from a resource.
   */
  public static final GenericLexicon loadGenericLexicon(InputStream lexiconInputStream,
                                                        AbstractNormalizer normalizer,
                                                        CategoryFactory categoryFactory,
                                                        String categoryString,
                                                        boolean caseSensitive, boolean isDefinitive,
                                                        boolean hasAttributes,
                                                        String commonAttributes) throws IOException {
    return new GenericLexicon(lexiconInputStream, normalizer,
                              categoryFactory.getCategory(categoryString), caseSensitive, isDefinitive,
                              hasAttributes, commonAttributes);
  }


  private Category category;

  private Map<String, Map<String, String>> caseSensitiveTerms;     // normalized term -> attribute/value pairs
  private Map<String, Map<String, String>> caseInsensitiveTerms;   // normalized & common-cased term -> attribute/value pairs
  private boolean isDefinitive;
  private boolean hasAttributes;
  private Map<String, String> commonAttributes;

  /**
   * Construct with the given rote-list stream, where each line holds a term.
   * <p>
   * @param roteListStream    The stream from which terms will be read.
   * @param normalizer        The normalizer to apply to the terms.
   * @param category          The category to assign to input that matches a term.
   * @param caseSensitive     True if matching is case-sensitive; otherwise, false.
   * @param isDefinitive      True if when a term matches, no more categories are to be assigned to the input.
   * @param hasAttributes     When true, attributes are parsed from terms of the form "term,att1=val1,att2=val2,...";
   *                          otherwise, terms are not parsed.
   * @param commonAttributes  attributes of the form "att1=val1,att2=val2,..." to attach with every term.
   */
  public GenericLexicon(InputStream roteListStream, AbstractNormalizer normalizer, Category category,
                        boolean caseSensitive, boolean isDefinitive, boolean hasAttributes, String commonAttributes) throws IOException {
    super(normalizer);
    init(category, caseSensitive, isDefinitive, hasAttributes, commonAttributes);
    initializeTerms(roteListStream, caseSensitive);
  }

  /**
   * Construct with the given terms.
   */
  public GenericLexicon(String[] terms, AbstractNormalizer normalizer, Category category, boolean caseSensitive, boolean isDefinitive, boolean hasAttributes, String commonAttributes) {
    super(normalizer);
    init(category, caseSensitive, isDefinitive, hasAttributes, commonAttributes);
    initializeTerms(terms, caseSensitive);
  }

  /**
   * Construct with the given attributes.
   */
  public GenericLexicon(Tree<XmlLite.Data> lexiconNode, CategoryFactory categoryFactory, AbstractNormalizer normalizer) throws IOException {
    super(lexiconNode, categoryFactory, normalizer);

    final Category category = getNamedCategory(getAttribute("category"));
    if (category == null) {
      throw new IllegalArgumentException("Can't define GenericLexicon without 'category' attribute!");
    }
    final boolean caseSensitive = "true".equals(getAttribute("caseSensitive"));
    final boolean isDefinitive = "true".equals(getAttribute("isDefinitive"));
    final boolean hasAttributes = "true".equals(getAttribute("hasAttributes"));
    final String commonAttributes = getAttribute("commonAttributes");

    init(category, caseSensitive, isDefinitive, hasAttributes, commonAttributes);
    
    doLoadTerms(getAttribute("terms"), caseSensitive);
    doLoadTerms(getAttribute("caseSensitiveTerms"), true);
    doLoadTerms(getAttribute("caseInsensitiveTerms"), false);
                
    doLoadFile(getAttribute("file"), caseSensitive);
    doLoadFile(getAttribute("caseSensitiveFile"), true);
    doLoadFile(getAttribute("caseInsensitiveFile"), false);
  }

  private final void init(Category category, boolean caseSensitive, boolean isDefinitive, boolean hasAttributes, String commonAttributes) {
    this.category = category;
    this.caseSensitiveTerms = new HashMap<String, Map<String, String>>();
    this.caseInsensitiveTerms = new HashMap<String, Map<String, String>>();
    this.isDefinitive = isDefinitive;
    this.hasAttributes = hasAttributes;
    this.commonAttributes = (commonAttributes != null) ? parseAttributes(commonAttributes.split("\\s*,\\s*"), 0) : null;
  }

  private final void doLoadTerms(String termsString, boolean caseSensitive) {
    if (termsString != null && termsString.length() > 0) {
      final String[] terms = termsString.split("\\s*,\\s*");
      initializeTerms(terms, caseSensitive);
    }
  }

  private final void doLoadFile(String fileName, boolean caseSensitive) throws IOException {
    if (fileName != null && fileName.length() > 0) {
      final String absolute = SdnUtil.getSdnResourcePath(fileName);
      final InputStream roteListStream = FileUtil.getInputStream(absolute);
      initializeTerms(roteListStream, caseSensitive);
    }
  }

  public Category getCategory() {
    return category;
  }

  public void addTerms(InputStream inputStream, boolean caseSensitive) throws IOException {
    initializeTerms(inputStream, caseSensitive);
  }

  public void addTerms(Class clazz, String resourceName, boolean caseSensitive) throws IOException {
    initializeTerms(FileUtil.getInputStream(clazz, resourceName), caseSensitive);
  }

  public void addTerms(String[] terms, boolean caseSensitive) {
    initializeTerms(terms, caseSensitive);
  }

  /**
   * Determine whether the categories container already has category type(s)
   * that this lexicon would add.
   * <p>
   * NOTE: this is used to avoid calling "define" when categories already exist
   *       for the substring.
   *
   * @return true if this lexicon's category type(s) are already present.
   */
  protected boolean alreadyHasTypes(Categories categories) {
    return categories.hasType(category);
  }

  private final void initializeTerms(InputStream roteListStream, boolean caseSensitive) throws IOException {
    BufferedReader roteReader = FileUtil.getReader(roteListStream);
    String term;
    while ( (term = roteReader.readLine()) != null ) {
      if (caseSensitive) addCaseSensitiveTerm(term); else addCaseInsensitiveTerm(term);
    }
  }

  private final void initializeTerms(String[] terms, boolean caseSensitive) {
    for (String term : terms) {
      if (caseSensitive) addCaseSensitiveTerm(term); else addCaseInsensitiveTerm(term);
    }
  }

  public final void addCaseInsensitiveTerm(String term) {
    doAddTerm(term, caseInsensitiveTerms, true);
  }

  public final void addCaseSensitiveTerm(String term) {
    doAddTerm(term, caseSensitiveTerms, false);
  }

  private final void doAddTerm(String term, Map<String, Map<String, String>> terms, boolean commonCase) {
    if (hasAttributes) {
      final String[] pieces = term.split(",");

      // normalize
      term = normalize(pieces[0]);
      if (commonCase) term = term.toLowerCase();

      final Map<String, String> newAttributes = parseAttributes(pieces, 1);
      if (newAttributes != null) {
        Map<String, String> storedAttributes = terms.get(term);
        if (storedAttributes == null) {
          storedAttributes = new HashMap<String, String>(newAttributes);
          terms.put(term, storedAttributes);
        }
        else {
          for (Map.Entry<String, String> newEntry : newAttributes.entrySet()) {
            storedAttributes.put(newEntry.getKey(), newEntry.getValue());
          }
        }
      }
      else {
        terms.put(term, newAttributes);
      }
    }
    else {
      term = normalize(term);
      if (commonCase) term = term.toLowerCase();

      if (!terms.containsKey(term)) terms.put(term, null);
    }

    if (commonAttributes != null) {
      Map<String, String> attributes = terms.get(term);
      if (attributes == null) {
        attributes = new HashMap<String, String>(commonAttributes);
        terms.put(term, attributes);
      }
      else {
        for (Map.Entry<String, String> commonAttribute : commonAttributes.entrySet()) {
          attributes.put(commonAttribute.getKey(), commonAttribute.getValue());
        }
      }
    }
  }

  private final Map<String, String> parseAttributes(String[] pieces, int startIndex) {
    if (pieces.length <= startIndex) return null;
    final Map<String, String> result = new HashMap<String, String>();
    for (int i = startIndex; i < pieces.length; ++i) {
      final String attVal = pieces[i];
      final String[] parts = attVal.split("=");
      result.put(parts[0], parts[1]);
    }
    return result;
  }

  /**
   * Define applicable categories in the subString.
   *
   * @param subString   The substring to define.
   * @param normalizer  The normalizer to use.
   */
  protected void define(StringWrapper.SubString subString, AbstractNormalizer normalizer) {
    // run string through the same normalizer as used on construction.
    final String string = subString.getNormalizedString(normalizer);
    Definition definition = doBaseLookup(string);
    if (definition != null) {
      subString.addCategory(category);
      subString.setAttribute(NORMALIZED_ATTRIBUTE, definition.normalized);
      if (isDefinitive) subString.setDefinitive(isDefinitive);

      if (definition.attributes != null) {
        for (Map.Entry<String, String> attributeEntry : definition.attributes.entrySet()) {
          subString.addAttribute(attributeEntry.getKey(), attributeEntry.getValue());
        }
      }
    }
  }
  
  private final Definition doBaseLookup(String normalized) {
    Definition result = null;
    boolean gotit = caseSensitiveTerms.containsKey(normalized);
    if (gotit) {  // found case sensitive match
      result = new Definition(normalized, caseSensitiveTerms.get(normalized));
    }
    else { // try caseInsensitive
      normalized = normalized.toLowerCase();
      gotit = caseInsensitiveTerms.containsKey(normalized);
      if (gotit) {
        result = new Definition(normalized, caseInsensitiveTerms.get(normalized));
      }
    }

    return result;
  }

  private final class Definition {
    public final String normalized;
    public final Map<String, String> attributes;

    public Definition(String normalized, Map<String, String> attributes) {
      this.normalized = normalized;
      this.attributes = attributes;
    }
  }
}
