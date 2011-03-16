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
package org.sd.atn;


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.sd.io.FileUtil;
import org.sd.atn.ResourceManager;
import org.sd.token.Normalizer;
import org.sd.token.Token;
import org.sd.xml.DomElement;
import org.sd.xml.DomNode;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Classifier that takes a list of terms in the defining xml or reads a flat
 * file with terms.
 * <p>
 * Flat file names are specified under 'textfile caseSensitive="true|false"'
 * nodes and have contents of the form:
 * <p>
 * term \t key=val \t key=val \t ...
 * <p>
 * Xml terms are specified under 'terms caseSensitive="true|false" nodes and
 * have child nodes of the form 'term att=val ...' with each term's text.
 * 
 * @author Spence Koehler
 */
public class RoteListClassifier extends AbstractAtnStateTokenClassifier {
  
  private static final Map<String, String> EMPTY_ATTRIBUTES = new HashMap<String, String>();


  private String roteListType;
  public String getRoteListType() {
    return roteListType;
  }

  private Map<String, Map<String, String>> term2attributes;
  private Map<String, Map<String, String>> term2attributesLC;
  private boolean caseSensitive;
  private ResourceManager resourceManager;

  public RoteListClassifier(DomElement classifierIdElement, ResourceManager resourceManager, Map<String, Normalizer> id2Normalizer) {
    super(classifierIdElement, id2Normalizer);
    init(classifierIdElement, resourceManager);
  }

  private final void init(DomElement classifierIdElement, ResourceManager resourceManager) {
    this.resourceManager = resourceManager;
    this.roteListType = classifierIdElement.getLocalName();
    this.term2attributes = new HashMap<String, Map<String, String>>();
    this.term2attributesLC = new HashMap<String, Map<String, String>>();
    this.caseSensitive = false;

    doSupplement(classifierIdElement);
  }

  private final void doSupplement(DomNode classifierIdElement) {
    this.caseSensitive = classifierIdElement.getAttributeBoolean("caseSensitive", this.caseSensitive);

    final DomElement termsNode = (DomElement)classifierIdElement.selectSingleNode("terms");

    if (termsNode != null) {
      loadTerms(termsNode, null, null, term2attributes, term2attributesLC);
    }

    final DomElement textfileNode = (DomElement)classifierIdElement.selectSingleNode("textfile");

    if (textfileNode != null) {
      loadTextFile(textfileNode, null, null, term2attributes, term2attributesLC);
    }
  }

  public boolean isEmpty() {
    return term2attributes == null || term2attributes.size() == 0;
  }

  /**
   * Supplement this classifier with the given dom node.
   */
  public void supplement(DomNode supplementNode) {
    doSupplement(supplementNode);
  }


  public boolean doClassify(Token token) {
    
    boolean result = false;

    Map<String, Map<String, String>> t2a = term2attributes;
    String key = token.getText();

    if (!caseSensitive) {
      t2a = term2attributesLC;
      key = key.toLowerCase();
    }

    if (t2a.containsKey(key)) {
      result = true;

      final Map<String, String> attributes = t2a.get(key);

      if (attributes != null) {
        for (Map.Entry<String, String> kvPair : attributes.entrySet()) {
          token.setFeature(kvPair.getKey(), kvPair.getValue(), this);
        }
      }
    }

    return result;
  }

  public Map<String, String> doClassify(String text) {
    Map<String, String> result = null;

    Map<String, Map<String, String>> t2a = term2attributes;
    String key = text;

    if (!caseSensitive) {
      t2a = term2attributesLC;
      key = key.toLowerCase();
    }

    if (t2a.containsKey(key)) {
      result = t2a.get(key);
      if (result == null) {
        result = EMPTY_ATTRIBUTES;
      }
    }

    return result;
  }

  protected boolean caseSensitive() {
    return this.caseSensitive;
  }

  protected Map<String, Map<String, String>> getTerm2Attributes() {
    return term2attributes;
  }

  protected Map<String, Map<String, String>> getTerm2AttributesLC() {
    return term2attributesLC;
  }

  protected final void loadTerms(DomElement termsNode,
                                 Set<String> terms, Set<String> termsLC,
                                 Map<String, Map<String, String>> term2attributes,
                                 Map<String, Map<String, String>> term2attributesLC) {
    if (termsNode == null) return;

    // currently caseSensitivity is applied globally
    this.caseSensitive = termsNode.getAttributeBoolean("caseSensitive", this.caseSensitive);

    // get global attributes to apply to each term
    Map<String, String> globalAttributes = null;
    if (termsNode.hasAttributes()) {
      final Map<String, String> termsNodeAttributes = termsNode.getDomAttributes().getAttributes();
      final int minAttrCount = termsNodeAttributes.containsKey("caseSensitive") ? 2 : 1;
      if (termsNodeAttributes.size() >= minAttrCount) {
        globalAttributes = new HashMap<String, String>(termsNodeAttributes);

        // ignore caseSensitive attribute
        globalAttributes.remove("caseSensitive");
      }
    }

    // load each "term" under "terms"
    Map<String, String> termAttributes = null;
    final NodeList termNodes = termsNode.selectNodes("term");
    for (int i = 0; i < termNodes.getLength(); ++i) {
      final Node curNode = termNodes.item(i);
      if (curNode.getNodeType() != Node.ELEMENT_NODE) continue;
      final DomElement termElement = (DomElement)curNode;

      final String termText = termElement.getTextContent();
      final String termTextLC = termText.toLowerCase();

      if (terms != null) {
        addTerms(termText, terms);
      }
      if (termsLC != null) {
        addTerms(termTextLC, termsLC);
      }

      if (term2attributes != null || term2attributesLC != null) {
        if (termElement.hasAttributes()) {
          termAttributes = termElement.getDomAttributes().getAttributes();

          // apply global attributes, but don't clobber term-level overrides
          if (globalAttributes != null) {
            for (Map.Entry<String, String> entry : globalAttributes.entrySet()) {
              if (!termAttributes.containsKey(entry.getKey())) {
                termAttributes.put(entry.getKey(), entry.getValue());
              }
            }
          }
        }
        else termAttributes = null;

        if (term2attributes != null) term2attributes.put(termText, termAttributes);
        if (term2attributesLC != null) term2attributesLC.put(termTextLC, termAttributes);
      }
    }
  }

  protected final void loadTextFile(DomElement textfileElement,
                                    Set<String> terms, Set<String> termsLC,
                                    Map<String, Map<String, String>> term2attributes,
                                    Map<String, Map<String, String>> term2attributesLC) {
    if (textfileElement == null) return;

    this.caseSensitive = textfileElement.getAttributeBoolean("caseSensitive", this.caseSensitive);
    final int minChars = textfileElement.getAttributeInt("minChars", 1);

    final File textfile = resourceManager.getWorkingFile(textfileElement);

    try {
      final BufferedReader reader = FileUtil.getReader(textfile);

      String line = null;
      while ((line = reader.readLine()) != null) {
        if (!"".equals(line) && line.charAt(0) == '#') continue;

        final String[] lineFields = line.trim().split("\t");
        final String term = lineFields[0];
        final String termLC = lineFields[0].toLowerCase();
        if (term.length() >= minChars) {
          if (terms != null) addTerms(term, terms);
          if (termsLC != null) addTerms(termLC, termsLC);
          if (term2attributes != null || term2attributesLC != null) {
            Map<String, String> termAttributes = null;
            if (lineFields.length > 1) {
              termAttributes = new HashMap<String, String>();
              for (int fieldNum = 1; fieldNum < lineFields.length; ++fieldNum) {
                final String[] attVal = lineFields[fieldNum].split("=");
                if (attVal.length == 2) {
                  termAttributes.put(attVal[0], attVal[1]);
                }
              }
            }
            if (term2attributes != null) term2attributes.put(term, termAttributes);
            if (term2attributesLC != null) term2attributesLC.put(termLC, termAttributes);
          }
        }
      }

      reader.close();
    }
    catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private final void addTerms(String termText, Set<String> terms) {
    terms.add(termText);
    if (termText.indexOf(' ') > 0) {
      final String[] pieces = termText.split(" ");
      for (String piece : pieces) {
        terms.add(piece);
      }
    }
  }
}
