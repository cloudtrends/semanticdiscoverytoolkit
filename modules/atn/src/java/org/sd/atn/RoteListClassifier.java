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
 * _keyFeature="..." nodes and have contents of the form:
 * <p>
 * term \t key=val \t key=val \t ...
 * <p>
 * Xml terms are specified under 'terms caseSensitive="true|false" nodes and
 * have child nodes of the form 'term att=val ...' with each term's text.
 * <p>
 * The '_keyFeature' designates the feature name used in the file that should
 * be changed to mirror the current classifier's name instead of that value
 * used in the file. This is needed to avoid shadowing rules of the same
 * name as the feature in the file, when the rote list is used in a classifier
 * with a different name.
 * 
 * @author Spence Koehler
 */
public class RoteListClassifier extends AbstractAtnStateTokenClassifier {
  
  private static final Map<String, String> EMPTY_ATTRIBUTES = new HashMap<String, String>();

  //
  // <roteListType name='optionalName' caseSensitive='defaultCaseSensitivity'>
  //   <jclass>org.sd.atn.RoteListClassifier</jclass>
  //   <terms caseSensitive='...' ...collective term attributes...>
  //     <term ...single term attributes...>...</term>
  //     ...
  //   </terms>
  //   ...
  //   <textfile caseSensitive='...' _keyFeature='...' ...collective term attributes...>
  //   ...
  //   <stopwords>
  //     <terms.../>
  //     ...
  //     <textfile.../>
  //     ...
  //   </stopwords>
  // </roteListType>
  //

  private String roteListType;
  private TermsBundle terms;
  private TermsBundle stopwords;
  private boolean defaultCaseSensitivity;
  private ResourceManager resourceManager;
  private String name;

  public RoteListClassifier(DomElement classifierIdElement, ResourceManager resourceManager, Map<String, Normalizer> id2Normalizer) {
    super(classifierIdElement, id2Normalizer);
    init(classifierIdElement, resourceManager);
  }

  private final void init(DomElement classifierIdElement, ResourceManager resourceManager) {
    this.resourceManager = resourceManager;
    this.name = classifierIdElement.getAttributeValue("name", "unknown");
    this.roteListType = classifierIdElement.getLocalName();
    this.terms = null;
    this.stopwords = null;

    doSupplement(classifierIdElement);
  }

  protected final void doSupplement(DomNode classifierIdElement) {
    this.defaultCaseSensitivity = classifierIdElement.getAttributeBoolean("caseSensitive", this.defaultCaseSensitivity);

    final NodeList childNodes = classifierIdElement.getChildNodes();
    final int numChildNodes = childNodes.getLength();
    for (int childNodeNum = 0; childNodeNum < numChildNodes; ++childNodeNum) {
      final Node childNode = childNodes.item(childNodeNum);
      if (childNode.getNodeType() != Node.ELEMENT_NODE) continue;

      final DomElement childElement = (DomElement)childNode;
      final String childNodeName = childNode.getLocalName();

      if ("terms".equalsIgnoreCase(childNodeName)) {
        this.terms = loadTerms(this.terms, childElement, false);
      }
      else if ("textfile".equalsIgnoreCase(childNodeName)) {
        this.terms = loadTextFile(this.terms, childElement, false, classifierIdElement.getLocalName());
      }
      else if ("stopwords".equalsIgnoreCase(childNodeName)) {
        final boolean reset = childElement.getAttributeBoolean("reset", false);
        if (reset) {
          if (this.stopwords != null) this.stopwords.reset();
        }

        final NodeList stopChildNodes = childNode.getChildNodes();
        final int numStopChildNodes = stopChildNodes.getLength();
        for (int stopNodeNum = 0; stopNodeNum < numStopChildNodes; ++stopNodeNum) {
          final Node stopNode = stopChildNodes.item(stopNodeNum);
          if (stopNode.getNodeType() != Node.ELEMENT_NODE) continue;

          final DomElement stopElement = (DomElement)stopNode;
          final String stopNodeName = stopNode.getLocalName();

          if ("terms".equalsIgnoreCase(stopNodeName)) {
            this.stopwords = loadTerms(this.stopwords, stopElement, true);
          }
          else if ("textfile".equalsIgnoreCase(stopNodeName)) {
            this.stopwords = loadTextFile(this.stopwords, stopElement, true, classifierIdElement.getLocalName());
          }
        }
      }
    }
  }

  public String getRoteListType() {
    return roteListType;
  }

  public boolean isEmpty() {
    return terms == null || terms.isEmpty();
  }

  /**
   * Supplement this classifier with the given dom node.
   */
  public void supplement(DomNode supplementNode) {
    doSupplement(supplementNode);
  }


  /**
   * Determine whether the token is a stopword.
   */
  public boolean doClassifyStopword(Token token) {
    return doClassify(token, stopwords);
  }

  /**
   * Determine whether the text is a stopword.
   */
  public Map<String, String> doClassifyStopword(String text) {
    return doClassify(text, stopwords);
  }

  /**
   * Determine whether the token is a term.
   */
  public boolean doClassifyTerm(Token token) {
    return doClassify(token, terms);
  }

  /**
   * Determine whether the text is a term.
   */
  public Map<String, String> doClassifyTerm(String text) {
    return doClassify(text, terms);
  }

  /**
   * Determine whether the token is a valid term, and not a stopword.
   */
  public boolean doClassify(Token token) {
    boolean result = false;

    if ((stopwords == null || (stopwords != null && !stopwords.doClassify(token))) &&
        (terms != null)) {
      result = terms.doClassify(token);
    }

    return result;
  }

  /**
   * Determine whether the text is a valid term, and not a stopword.
   */
  public Map<String, String> doClassify(String text) {
    Map<String, String> result = null;

    if ((stopwords == null || (stopwords != null && stopwords.doClassify(text) == null)) &&
        (terms != null)) {
      result = terms.doClassify(text);
    }

    return result;
  }

  private final boolean doClassify(Token token, TermsBundle termsBundle) {
    boolean result = false;

    if (termsBundle != null && token != null) {
      result = termsBundle.doClassify(token);
    }

    return result;
  }

  private final Map<String, String> doClassify(String text, TermsBundle termsBundle) {
    Map<String, String> result = null;

    if (termsBundle != null && text != null) {
      result = termsBundle.doClassify(text);
    }

    return result;
  }

  protected boolean getDefaultCaseSensitivity() {
    return defaultCaseSensitivity;
  }

  protected TermsBundle getTerms() {
    return terms;
  }

  protected TermsBundle getStopwords() {
    return stopwords;
  }

  protected final TermsBundle loadTerms(TermsBundle termsBundle, DomElement termsElement, boolean isStopwords) {
    if (termsElement == null) return termsBundle;

    if (termsBundle == null) termsBundle = new TermsBundle(isStopwords);
    termsBundle.loadTerms(termsElement, this.defaultCaseSensitivity);
    return termsBundle;
  }

  protected final TermsBundle loadTextFile(TermsBundle termsBundle, DomElement textfileElement, boolean isStopwords, String classifierName) {
    if (textfileElement == null) return termsBundle;

    if (termsBundle == null) termsBundle = new TermsBundle(isStopwords);
    termsBundle.loadTextFile(textfileElement, this.defaultCaseSensitivity, resourceManager, classifierName);
    return termsBundle;
  }


  /**
   * Container for a set of case sensitive or case insensitive terms
   * with attributes.
   */
  protected static final class Terms {
    private boolean caseSensitive;
    private boolean isStopwords;
    private Map<String, Map<String, String>> term2attributes;

    public Terms(boolean caseSensitive, boolean isStopwords) {
      this.caseSensitive = caseSensitive;
      this.isStopwords = isStopwords;
      this.term2attributes = new HashMap<String, Map<String, String>>();
    }

    public void reset() {
      if (term2attributes != null) term2attributes.clear();
    }

    public boolean isEmpty() {
      return term2attributes == null || term2attributes.size() == 0;
    }

    public Map<String, Map<String, String>> getTerm2Attributes() {
      return term2attributes;
    }

    public boolean isCaseSensitive() {
      return caseSensitive;
    }

    public boolean isStopwords() {
      return isStopwords;
    }

    public boolean doClassify(Token token) {
      boolean result = false;

      String key = token.getText();

      if (!caseSensitive) {
        key = key.toLowerCase();
      }

      if (term2attributes.containsKey(key)) {
        result = true;

        // only add token attributes for non stopwords
        if (!isStopwords) {
          final Map<String, String> attributes = term2attributes.get(key);

          if (attributes != null) {
            for (Map.Entry<String, String> kvPair : attributes.entrySet()) {
              token.setFeature(kvPair.getKey(), kvPair.getValue(), this);
            }
          }
        }
      }

      return result;
    }

    public Map<String, String> doClassify(String text) {
      Map<String, String> result = null;

      String key = text;

      if (!caseSensitive) {
        key = key.toLowerCase();
      }

      // go ahead and get attributes for both terms and stopwords
      if (term2attributes.containsKey(key)) {
        result = term2attributes.get(key);
        if (result == null) {
          result = EMPTY_ATTRIBUTES;
        }
      }

      return result;
    }

    protected final void loadTerms(DomElement termsElement) {

      // get global attributes to apply to each term (ignoring 'caseSensitive" attribute)
      Map<String, String> globalAttributes = null;
      if (termsElement.hasAttributes()) {
        final Map<String, String> termsElementAttributes = termsElement.getDomAttributes().getAttributes();
        final int minAttrCount = termsElementAttributes.containsKey("caseSensitive") ? 2 : 1;
        if (termsElementAttributes.size() >= minAttrCount) {
          globalAttributes = new HashMap<String, String>(termsElementAttributes);

          // ignore caseSensitive attribute
          globalAttributes.remove("caseSensitive");
        }
      }

      // load each "term" under "terms"
      Map<String, String> termAttributes = null;
      final NodeList termNodes = termsElement.selectNodes("term");
      for (int i = 0; i < termNodes.getLength(); ++i) {
        final Node curNode = termNodes.item(i);
        if (curNode.getNodeType() != Node.ELEMENT_NODE) continue;
        final DomElement termElement = (DomElement)curNode;

        final String termText = termElement.getTextContent();
        final String term = caseSensitive ? termText : termText.toLowerCase();

        // only load attributes for non-stopwords
        if (!isStopwords) {
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
          else if (globalAttributes != null) {
            termAttributes = new HashMap<String, String>(globalAttributes);
          }
        }

        term2attributes.put(term, termAttributes);
      }
    }

    protected final void loadTextFile(DomElement textfileElement, ResourceManager resourceManager, String classifierName) {

      final int minChars = textfileElement.getAttributeInt("minChars", 1);
      final String keyFeature = textfileElement.getAttributeValue("_keyFeature", null); 

      final File textfile = resourceManager.getWorkingFile(textfileElement);

      try {
        final BufferedReader reader = FileUtil.getReader(textfile);

        String line = null;
        while ((line = reader.readLine()) != null) {
          if (!"".equals(line) && line.charAt(0) == '#') continue;

          final String[] lineFields = line.trim().split("\t");
          final String term = caseSensitive ? lineFields[0] : lineFields[0].toLowerCase();
          if (term.length() >= minChars) {
            Map<String, String> termAttributes = null;
            if (lineFields.length > 1) {
              termAttributes = new HashMap<String, String>();
              for (int fieldNum = 1; fieldNum < lineFields.length; ++fieldNum) {
                final String[] attVal = lineFields[fieldNum].split("=");
                if (attVal.length == 2) {
                  String key = attVal[0];
                  final String value = attVal[1];

                  // change the keyFeature to match the classifier name
                  if (keyFeature != null && classifierName != null && keyFeature.equals(key)) {
                    key = classifierName;
                  }

                  termAttributes.put(key, value);
                }
              }
            }
            term2attributes.put(term, termAttributes);
          }
        }

        reader.close();
      }
      catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
  }

  /**
   * Container for bundling case sensitive and insensitive terms.
   */
  protected static final class TermsBundle {
    private boolean isStopwords;
    private Terms caseSensitiveTerms;
    private Terms caseInsensitiveTerms;

    public TermsBundle(boolean isStopwords) {
      this.isStopwords = isStopwords;
      this.caseSensitiveTerms = null;
      this.caseInsensitiveTerms = null;
    }

    public void reset() {
      if (caseSensitiveTerms != null) caseSensitiveTerms.reset();
      if (caseInsensitiveTerms != null) caseInsensitiveTerms.reset();
    }

    public boolean isStopwords() {
      return isStopwords;
    }

    public Terms getCaseSensitiveTerms() {
      return caseSensitiveTerms;
    }

    public Terms getCaseInsensitiveTerms() {
      return caseInsensitiveTerms;
    }

    public boolean isEmpty() {
      return
        (caseSensitiveTerms != null && !caseSensitiveTerms.isEmpty()) ||
        (caseInsensitiveTerms != null && !caseInsensitiveTerms.isEmpty());
    }

    public boolean doClassify(Token token) {
      boolean result = false;

      if (caseSensitiveTerms != null) {
        result = caseSensitiveTerms.doClassify(token);
      }

      if (!result && caseInsensitiveTerms != null) {
        result = caseInsensitiveTerms.doClassify(token);
      }

      return result;
    }

    public Map<String, String> doClassify(String text) {
      Map<String, String> result = null;

      if (caseSensitiveTerms != null) {
        result = caseSensitiveTerms.doClassify(text);
      }

      if (result == null && caseInsensitiveTerms != null) {
        result = caseInsensitiveTerms.doClassify(text);
      }

      return result;
    }

    protected final void loadTerms(DomElement termsElement, boolean defaultCaseSensitivity) {
      Terms theTerms = null;

      final boolean caseSensitive = termsElement.getAttributeBoolean("caseSensitive", defaultCaseSensitivity);

      // Get a handle on theTerms based on case sensitivity
      if (caseSensitive) {
        if (this.caseSensitiveTerms == null) this.caseSensitiveTerms = new Terms(true, isStopwords);
        theTerms = this.caseSensitiveTerms;
      }
      else {
        if (this.caseInsensitiveTerms == null) this.caseInsensitiveTerms = new Terms(false, isStopwords);
        theTerms = this.caseInsensitiveTerms;
      }

      theTerms.loadTerms(termsElement);
    }

    protected final void loadTextFile(DomElement textfileElement, boolean defaultCaseSensitivity, ResourceManager resourceManager, String classifierName) {
      Terms theTerms = null;

      final boolean caseSensitive = textfileElement.getAttributeBoolean("caseSensitive", defaultCaseSensitivity);

      // Get a handle on theTerms based on case sensitivity
      if (caseSensitive) {
        if (this.caseSensitiveTerms == null) this.caseSensitiveTerms = new Terms(true, isStopwords);
        theTerms = this.caseSensitiveTerms;
      }
      else {
        if (this.caseInsensitiveTerms == null) this.caseInsensitiveTerms = new Terms(false, isStopwords);
        theTerms = this.caseInsensitiveTerms;
      }

      theTerms.loadTextFile(textfileElement, resourceManager, classifierName);
    }
  }
}
