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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sd.io.FileUtil;
import org.sd.atn.ResourceManager;
import org.sd.token.Normalizer;
import org.sd.token.Token;
import org.sd.util.Usage;
import org.sd.xml.DomElement;
import org.sd.xml.DomNode;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Classifier that takes a list of terms in the defining xml or reads a flat
 * file with terms.
 * <p>
 * Flat file names are specified under 'textfile caseSensitive="true|false"
 * _keyFeature="..." classFeature="..."' nodes and have contents of the form:
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
 * <p>
 * If classFeature is non-empty, then all terms will receive a feature with
 * "class" as the the key and the classifer's name as the value.
 * 
 * @author Spence Koehler
 */
@Usage(notes =
       "This is an org.sd.atn.AbstractAtnStateTokenClassifier used as a Classifier\n" +
       "within an org.sd.atn.AtnGrammar based on matching terms by dictionary or\n" +
       "regular expression matching.\n" +
       "\n" +
       "Terms are loaded from in-line lists (terms), text files (textfile), or regular\n" +
       "expressions (regexes). Stopword terms can also be specified and are applied\n" +
       "before term matches.  Supplements can mask base terms using stopwords and/or add\n" +
       "terms to the classifier. Stopwords can also be triggered based on other classifier\n" +
       "classifications. The XML format for specifying the terms is:\n" +
       "\n" +
       "<roteListType name='optionalName' caseSensitive='defaultCaseSensitivity' classFeature='...'>\n" +
       "  <jclass>org.sd.atn.RoteListClassifier</jclass>\n" +
       "  <terms caseSensitive='...' classFeature='...' ...collective term attributes...>\n" +
       "    <term ...single term attributes...>...</term>\n" +
       "    ...\n" +
       "  </terms>\n" +
       "  ...\n" +
       "  <textfile caseSensitive='...' classFeature='...' _keyFeature='...' ...collective term attributes...>\n" +
       "  ...\n" +
       "  <regexes ...>   see RegexData\n" +
       "    <regex ...>...</regex>\n" +
       "    ...\n" +
       "  </regexes>\n" +
       "  ...\n" +
       "  <stopwords>\n" +
       "    <terms.../>\n" +
       "    ...\n" +
       "    <textfile.../>\n" +
       "    ...\n" +
       "    <regexes.../>\n" +
       "    ...\n" +
       "    <classifiers>\n" +
       "      <classifier>...</classifier>\n" +
       "      ...\n" +
       "    </classifiers>\n" +
       "  </stopwords>\n" +
       "</roteListType>"
  )
public class RoteListClassifier extends AbstractAtnStateTokenClassifier {
  
  private static final Map<String, String> EMPTY_ATTRIBUTES = new HashMap<String, String>();

  //
  // <roteListType name='optionalName' caseSensitive='defaultCaseSensitivity' classFeature='...'>
  //   <jclass>org.sd.atn.RoteListClassifier</jclass>
  //   <terms caseSensitive='...' classFeature='...' ...collective term attributes...>
  //     <term ...single term attributes...>...</term>
  //     ...
  //   </terms>
  //   ...
  //   <textfile caseSensitive='...' classFeature='...' _keyFeature='...' ...collective term attributes...>
  //   ...
  //   <regexes ...>   // see RegexData
  //     <regex ...>...</regex>
  //     ...
  //   </regexes>
  //   ...
  //   <stopwords>
  //     <terms.../>
  //     ...
  //     <textfile.../>
  //     ...
  //     <regexes.../>
  //     ...
  //     <classifiers>
  //       <classifier>...</classifier>
  //       ...
  //     </classifiers>
  //   </stopwords>
  // </roteListType>
  //

  private String roteListType;
  private TermsBundle terms;
  private TermsBundle stopwords;
  private boolean defaultCaseSensitivity;
  private String classFeature;
  private ResourceManager resourceManager;
  private String name;
  private boolean trace;

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
    this.defaultCaseSensitivity = true;
    this.classFeature = null;

    doSupplement(classifierIdElement);
  }

  protected final void doSupplement(DomNode classifierIdElement) {
    this.defaultCaseSensitivity = classifierIdElement.getAttributeBoolean("caseSensitive", this.defaultCaseSensitivity);
    this.classFeature = classifierIdElement.getAttributeValue("classFeature", this.classFeature);
    this.trace = classifierIdElement.getAttributeBoolean("trace", false);

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
      else if ("regexes".equalsIgnoreCase(childNodeName)) {
        this.terms = loadRegexes(this.terms, childElement, false);
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
          else if ("regexes".equalsIgnoreCase(stopNodeName)) {
            this.stopwords = loadRegexes(this.stopwords, stopElement, true);
          }
          else if ("classifiers".equalsIgnoreCase(stopNodeName)) {
            this.stopwords = loadClassifiers(this.stopwords, stopElement, true);
          }
        }
      }
    }

    if (this.terms != null) {
      this.terms.setTrace(trace);
      // adjust user-defined down to finite observed if warranted
      final int maxWordCount = terms.getMaxWordCount();
      final int curMaxWordCount = super.getMaxWordCount();
      super.setMaxWordCount(adjustMaxWordCount(maxWordCount, curMaxWordCount));
    }
    if (this.stopwords != null) this.stopwords.setTrace(trace);
  }

  public String getName() {
    return name;
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

    if (trace) {
      System.out.println("RoteListClassifier(" + name + ").classify(" + token + ")...");
    }

    if ((stopwords == null || (stopwords != null && !stopwords.doClassify(token))) &&
        (terms != null)) {
      result = terms.doClassify(token);
    }

    if (trace) {
      System.out.println("\tRoteListClassifier(" + name + ").classify(" + token + ").result=" + result);
    }

    return result;
  }

  /**
   * Determine whether the text is a valid term, and not a stopword.
   */
  public Map<String, String> doClassify(String text) {
    Map<String, String> result = null;

    if (trace) {
      System.out.println("RoteListClassifier(" + name + ").classify(" + text + ")...");
    }

    if ((stopwords == null || (stopwords != null && stopwords.doClassify(text) == null)) &&
        (terms != null)) {
      result = terms.doClassify(text);
    }

    if (trace) {
      System.out.println("RoteListClassifier(" + name + ").classify(" + text + ").result=" + result);
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

  protected String getClassFeature() {
    return classFeature;
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
    termsBundle.loadTerms(termsElement, this.defaultCaseSensitivity, isStopwords ? null : this.classFeature);
    return termsBundle;
  }

  protected final TermsBundle loadTextFile(TermsBundle termsBundle, DomElement textfileElement, boolean isStopwords, String classifierName) {
    if (textfileElement == null) return termsBundle;

    if (termsBundle == null) termsBundle = new TermsBundle(isStopwords);
    termsBundle.loadTextFile(textfileElement, this.defaultCaseSensitivity, isStopwords ? null : this.classFeature, resourceManager, classifierName);
    return termsBundle;
  }

  protected final TermsBundle loadRegexes(TermsBundle termsBundle, DomElement regexesElement, boolean isStopwords) {
    if (regexesElement == null) return termsBundle;

    if (termsBundle == null) termsBundle = new TermsBundle(isStopwords);
    termsBundle.loadRegexes(regexesElement, this.defaultCaseSensitivity, isStopwords ? null : this.classFeature);
    return termsBundle;
  }

  protected final TermsBundle loadClassifiers(TermsBundle termsBundle, DomElement classifiersElement, boolean isStopwords) {
    if (classifiersElement == null) return termsBundle;

    if (termsBundle == null) termsBundle = new TermsBundle(isStopwords);
    termsBundle.loadClassifiers(classifiersElement, this.defaultCaseSensitivity, isStopwords ? null : this.classFeature, resourceManager);
    return termsBundle;
  }

  public static final int adjustMaxWordCount(int localMaxWordCount, int globalMaxWordCount) {
    int result = globalMaxWordCount;

    if (localMaxWordCount > 0) {
      // bring max down to finite observed if warranted
      if (result == 0 || result > localMaxWordCount) {
        result = localMaxWordCount;
      }
    }

    return result;
  }


  /**
   * Container for a set of case sensitive or case insensitive terms
   * with attributes.
   */
  protected static final class Terms {
    private boolean caseSensitive;
    private String classFeature;
    private boolean isStopwords;
    private int userDefinedMaxWordCount;
    private int maxWordCount;
    private Map<String, Map<String, String>> term2attributes;
    private RegexDataContainer regexes;
    private List<RoteListClassifier> classifiers;
    private Map<String, Class> features;
    private boolean trace;

    public Terms(boolean caseSensitive, String classFeature, boolean isStopwords, int userDefinedMaxWordCount) {
      this.caseSensitive = caseSensitive;
      this.classFeature = "".equals(classFeature) ? null : classFeature;
      this.isStopwords = isStopwords;
      this.userDefinedMaxWordCount = userDefinedMaxWordCount;
      this.maxWordCount = 0;
      this.term2attributes = new HashMap<String, Map<String, String>>();
      this.regexes = null;
      this.classifiers = null;
      this.features = null;
      this.trace = false;
    }

    public void setMaxWordCount(int maxWordCount) {
      this.maxWordCount = maxWordCount;
    }

    public int getMaxWordCount() {
      // adjust user-defined down to finite observed if warranted
      return adjustMaxWordCount(maxWordCount, userDefinedMaxWordCount);
    }

    void setTrace(boolean trace) {
      this.trace = trace;
    }

    public void reset() {
      if (term2attributes != null) term2attributes.clear();
      this.regexes = null;
      this.classifiers = null;
      this.features = null;
    }

    public boolean isEmpty() {
      return
        (term2attributes == null || term2attributes.size() == 0) &&
        (this.regexes == null || this.regexes.size() == 0) &&
        (this.classifiers == null || this.classifiers.size() == 0) &&
        (this.features == null || this.features.size() == 0);
    }

    public Map<String, Map<String, String>> getTerm2Attributes() {
      return term2attributes;
    }

    public RegexDataContainer getRegexes() {
      return regexes;
    }

    public List<RoteListClassifier> getClassifiers() {
      return classifiers;
    }

    public Map<String, Class> getFeatures() {
      return features;
    }

    public boolean isCaseSensitive() {
      return caseSensitive;
    }

    public String getClassFeature() {
      return classFeature;
    }

    public boolean isStopwords() {
      return isStopwords;
    }

    public boolean doClassify(Token token) {
      boolean result = false;

      boolean exceedsMaxWordCount = false;

      if (maxWordCount > 0 && token.getWordCount() > maxWordCount) {
        // local maxWordCount may be stricter than global
        exceedsMaxWordCount = true;
      }

      boolean hasClassAttribute = false;
      String key = token.getText();

      if (!exceedsMaxWordCount) {
        if (!caseSensitive) {
          key = key.toLowerCase();
        }

        if (term2attributes.containsKey(key)) {
          result = true;

          if (trace) {
            System.out.println("\tfound '" + key + "' in term2attributes");
          }

          // only add token attributes for non stopwords
          if (!isStopwords) {
            final Map<String, String> attributes = term2attributes.get(key);

            if (attributes != null) {
              hasClassAttribute = attributes.containsKey("class");
              for (Map.Entry<String, String> kvPair : attributes.entrySet()) {
                token.setFeature(kvPair.getKey(), kvPair.getValue(), this);
              }
            }
          }
        }

        if (!result && regexes != null) {
          if (regexes.matches(key, token, !isStopwords)) {
            if (trace) {
              System.out.println("\tfound '" + key + "' in regexData");
            }

            result = true;
          }
        }

        if ((classifiers != null) && (!isStopwords || !result)) {
          for (RoteListClassifier classifier : classifiers) {
            final boolean curResult = classifier.doClassify(token);
            if (curResult) {
              if (trace) {
                System.out.println("\tfound '" + key + "' in classifier '" + classifier.getName() + "'");
              }
              result = true;
              // keep going to add features from further matches
            }
          }
        }
      }

      if (!result && features != null) {
        for (Map.Entry<String, Class> featureEntry : features.entrySet()) {
          final String feature = featureEntry.getKey();
          final Class type = featureEntry.getValue();

          if (token.getFeatureValue(feature, null, type) != null) {
            result = true;
            if (trace) {
              System.out.println("\tfound '" + feature + "' (value=" + token.getFeatureValue(feature, null, type) + ")");
            }
          }
        }
      }

      if (result && classFeature != null && !hasClassAttribute) {
        token.setFeature("class", classFeature, this);
      }

      if (trace && !result) {
        System.out.println("\tdidn't find '" + key + "'");
      }

      return result;
    }

    public Map<String, String> doClassify(String text) {
      Map<String, String> result = null;

      String key = text;

      if (!caseSensitive) {
        key = key.toLowerCase();
      }

      boolean matched = false;

      // go ahead and get attributes for both terms and stopwords
      if (term2attributes.containsKey(key)) {
        matched = true;
        result = term2attributes.get(key);
      }

      if (!matched && regexes != null) {
        result = regexes.matches(key);
        if (result != null) {
          matched = true;
        }
      }

      if ((classifiers != null) && (!isStopwords || !matched)) {
        for (RoteListClassifier classifier : classifiers) {
          final Map<String, String> curResult = classifier.doClassify(key);
          if (curResult != null) {
            if (result == null) {
              result = curResult;
            }
            else {
              result.putAll(curResult);
            }
            matched = true;
            //keep going to add features from further matches
          }
        }
      }

      //NOTE: unable to match raw text against features

      if (matched && classFeature != null) {
        boolean hasClassAttribute = false;
        if (result != null) {
          hasClassAttribute = result.containsKey("class");
        }
        if (!hasClassAttribute) {
          if (result == null) result = new HashMap<String, String>();
          result.put("class", classFeature);
        }
      }

      if (matched && result == null) {
        result = EMPTY_ATTRIBUTES;
      }

      return result;
    }

    protected final void loadTerms(DomElement termsElement) {

      // get global attributes to apply to each term (ignoring 'caseSensitive' attribute)
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
        addTermAttributes(term, termAttributes);
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
            addTermAttributes(term, termAttributes);
          }
        }

        reader.close();
      }
      catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }

    protected final void loadRegexes(DomElement regexesElement) {
      this.regexes = new RegexDataContainer(regexesElement);
    }

    protected final void loadClassifiers(DomElement classifiersElement, ResourceManager resourceManager) {
      final NodeList classifierNodes = classifiersElement.getChildNodes();
      for (int i = 0; i < classifierNodes.getLength(); ++i) {
        final Node curNode = classifierNodes.item(i);
        if (curNode.getNodeType() != Node.ELEMENT_NODE) continue;

        final String nodeName = curNode.getNodeName();
        final String classifierName = curNode.getTextContent().trim();

        if ("classifier".equals(nodeName)) {
          final Object classifierObject = resourceManager.getResource(classifierName);
          if (classifierObject == null) {
            System.out.println(new Date() + ": WARNING : RoteListClassifier unknown classifier '" + classifierName + "'");
          }
          else {
            if (classifierObject instanceof RoteListClassifier) {
              if (this.classifiers == null) this.classifiers = new ArrayList<RoteListClassifier>();
              this.classifiers.add((RoteListClassifier)classifierObject);
            }
            else {
              System.out.println(new Date() + ": WARNING : RoteListClassifier classifier '" +
                                 classifierName + "' is *NOT* an RoteListClassifier (" +
                                 classifierObject.getClass().getName() + ")");
            }
          }
        }
        else if ("feature".equals(nodeName)) {
          if (this.features == null) this.features = new HashMap<String, Class>();

          Class type = null;
          final String typeString = ((DomElement)curNode).getAttributeValue("type", null);
          if (typeString != null) {
            if ("interp".equals(typeString)) {
              type = ParseInterpretation.class;
            }
          }

          final String classifierNameString = "".equals(classifierName) ? null : classifierName;
          this.features.put(classifierNameString, type);
        }
        else {
          System.out.println(new Date() + ": WARNING : Unrecognized 'classifiers' sub-element '" +
                             nodeName + "'. Expecting 'classifier' or 'feature'.");
        }
      }
    }

    private final void addTermAttributes(String term, Map<String, String> attributes) {
      Map<String, String> curAttributes = term2attributes.get(term);
      if (curAttributes == null) {
        term2attributes.put(term, attributes);
      }
      else {
        if (attributes != null) {
          curAttributes.putAll(attributes);
        }
      }

      // update max word count
      final int curWordCount = computeWordCount(term);
      if (curWordCount > maxWordCount) {
        maxWordCount = curWordCount;
      }
    }

    private final int computeWordCount(String term) {
      //NOTE: we don't have a tokenizer to use for this here, so we're just counting spaces.
      int result = 1;
      
      for (int spos = term.indexOf(' '); spos >= 0; spos = term.indexOf(' ', spos + 1)) {
        ++result;
      }

      return result;
    }
  }

  /**
   * Container for bundling case sensitive and insensitive terms.
   */
  protected static final class TermsBundle {
    private boolean isStopwords;
    private Terms caseSensitiveTerms;
    private Terms caseInsensitiveTerms;
    private int maxWordCount;

    public TermsBundle(boolean isStopwords) {
      this.isStopwords = isStopwords;
      this.caseSensitiveTerms = null;
      this.caseInsensitiveTerms = null;
      this.maxWordCount = 0;
    }

    void setTrace(boolean trace) {
      if (caseSensitiveTerms != null) caseSensitiveTerms.setTrace(trace);
      if (caseInsensitiveTerms != null) caseInsensitiveTerms.setTrace(trace);
    }

    public int getMaxWordCount() {
      return maxWordCount;
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
        (caseSensitiveTerms == null || caseSensitiveTerms.isEmpty()) &&
        (caseInsensitiveTerms == null || caseInsensitiveTerms.isEmpty());
    }

    public boolean doClassify(Token token) {
      boolean result = false;

      if (caseSensitiveTerms != null) {
        result = caseSensitiveTerms.doClassify(token);
      }

      if ((caseInsensitiveTerms != null) && (!isStopwords || !result)) {
        // even if prior result succeeded, try again here to get any new token features (unless stopwords)
        result |= caseInsensitiveTerms.doClassify(token);
      }

      return result;
    }

    public Map<String, String> doClassify(String text) {
      Map<String, String> result = null;

      if (caseSensitiveTerms != null) {
        result = caseSensitiveTerms.doClassify(text);
      }

      if ((caseInsensitiveTerms != null) && (!isStopwords || result == null)) {
        // even if prior result succeeded, try again here to get any new token features (unless stopwords)
        final Map<String, String> ciResult = caseInsensitiveTerms.doClassify(text);
        if (result == null) {
          result = ciResult;
        }
        else {
          if (ciResult != null) {
            result.putAll(ciResult);
          }
        }
      }

      return result;
    }

    protected final void loadTerms(DomElement termsElement, boolean defaultCaseSensitivity, String classFeature) {
      final Terms theTerms = getTheTerms(termsElement, defaultCaseSensitivity, classFeature);
      theTerms.loadTerms(termsElement);
      adjustMaxWordCount(theTerms.getMaxWordCount());
    }

    protected final void loadTextFile(DomElement textfileElement, boolean defaultCaseSensitivity, String classFeature, ResourceManager resourceManager, String classifierName) {
      final Terms theTerms = getTheTerms(textfileElement, defaultCaseSensitivity, classFeature);
      theTerms.loadTextFile(textfileElement, resourceManager, classifierName);
      adjustMaxWordCount(theTerms.getMaxWordCount());
    }

    protected final void loadRegexes(DomElement regexesElement, boolean defaultCaseSensitivity, String classFeature) {
      final Terms theTerms = getTheTerms(regexesElement, defaultCaseSensitivity, classFeature);
      theTerms.loadRegexes(regexesElement);
      adjustMaxWordCount(theTerms.getMaxWordCount());
    }

    protected final void loadClassifiers(DomElement classifiersElement, boolean defaultCaseSensitivity, String classFeature, ResourceManager resourceManager) {
      final Terms theTerms = getTheTerms(classifiersElement, defaultCaseSensitivity, classFeature);
      theTerms.loadClassifiers(classifiersElement, resourceManager);
      adjustMaxWordCount(theTerms.getMaxWordCount());
    }

    private final void adjustMaxWordCount(int termsMaxWordCount) {
      // for a bundle, we want to preserve the "largest" maxWordCount
      if (termsMaxWordCount > 0 && termsMaxWordCount > this.maxWordCount) {
        this.maxWordCount = termsMaxWordCount;
      }
    }

    /** Get the correct (case -sensitive or -insensitive) terms */
    private final Terms getTheTerms(DomElement theElement, boolean defaultCaseSensitivity, String defaultClassFeature) {
      Terms theTerms = null;

      final boolean caseSensitive = theElement.getAttributeBoolean("caseSensitive", defaultCaseSensitivity);
      final String classFeature = theElement.getAttributeValue("classFeature", defaultClassFeature);
      final int maxWordCount = theElement.getAttributeInt("maxWordCount", 0);

      // Get a handle on theTerms based on case sensitivity
      if (caseSensitive) {
        if (this.caseSensitiveTerms == null) this.caseSensitiveTerms = new Terms(true, classFeature, isStopwords, maxWordCount);
        theTerms = this.caseSensitiveTerms;
      }
      else {
        if (this.caseInsensitiveTerms == null) this.caseInsensitiveTerms = new Terms(false, classFeature, isStopwords, maxWordCount);
        theTerms = this.caseInsensitiveTerms;
      }

      return theTerms;
    }
  }
}
