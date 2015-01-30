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
import org.sd.token.Feature;
import org.sd.token.Normalizer;
import org.sd.token.Token;
import org.sd.token.TokenClassifierHelper;
import org.sd.util.Usage;
import org.sd.xml.DomElement;
import org.sd.xml.DomNamedNodeMap;
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
       "      <feature name='...' type='interp|parse|string|classpath' when='exists|equals|matches-prev|matches-next'>text-to-equal|feature-name</feature>\n" +
       "    </classifiers>\n" +
       "  </stopwords>\n" +
       "</roteListType>"
  )
public class RoteListClassifier extends AbstractAtnStateTokenClassifier {
  
  public static final String TYPED_FEATURE_KEY = "*";
  private static final Map<String, String> EMPTY_ATTRIBUTES = new HashMap<String, String>();

  //
  // <roteListType name='optionalName' caseSensitive='defaultCaseSensitivity' classFeature='...'>
  //   <jclass>org.sd.atn.RoteListClassifier</jclass>
  //   <terms caseSensitive='...' classFeature='...' pluralize='true|false' genitivize='true|false' ...collective term attributes...>
  //     <term ...single term attributes...>...</term>
  //     ...
  //   </terms>
  //   ...
  //   <textfile caseSensitive='...' classFeature='...' _keyFeature='...' pluralize='true|false' genitivize='true|false' ...collective term attributes...>
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
  //       <feature ...>...</feature>
  //       ...
  //     </classifiers>
  //     <predelim>...</predelim>
  //     <postdelim>...</postdelim>
  //     <test>...</test>
  //   </stopwords>
  // </roteListType>
  //

  private String roteListType;
  private TermsWithStopwords termsAndStopwords;
  private boolean defaultCaseSensitivity;
  private String classFeature;
  private ResourceManager resourceManager;
  private boolean trace;

  public RoteListClassifier(DomElement classifierIdElement, ResourceManager resourceManager, Map<String, Normalizer> id2Normalizer) {
    super(classifierIdElement, id2Normalizer);
    init(classifierIdElement, resourceManager);
  }

  private final void init(DomElement classifierIdElement, ResourceManager resourceManager) {
    this.resourceManager = resourceManager;
    this.roteListType = classifierIdElement.getLocalName();
    this.termsAndStopwords = null;
    this.defaultCaseSensitivity = true;
    this.classFeature = null;

    doSupplement(classifierIdElement);
  }

  protected final void doSupplement(DomNode classifierIdElement) {
    this.defaultCaseSensitivity = classifierIdElement.getAttributeBoolean("caseSensitive", this.defaultCaseSensitivity);
    this.classFeature = classifierIdElement.getAttributeValue("classFeature", this.classFeature);
    this.trace = classifierIdElement.getAttributeBoolean("trace", false);

    this.termsAndStopwords = loadTermsAndStopwords(this.termsAndStopwords, classifierIdElement, false, getClassifiersNode(classifierIdElement));
  }

  /**
   * Scan up the DOM tree to find the (shallowest) "classifiers" node.
   */
  private final DomNode getClassifiersNode(DomNode refElement) {
    DomNode result = null;

    for (DomNode parentNode = (DomNode)refElement.getParentNode();
         parentNode != null;
         parentNode = (DomNode)parentNode.getParentNode()) {
      final String nodeName = parentNode.getLocalName();
      if ("classifiers".equals(nodeName)) {
        result = parentNode;
        //NOTE: don't break so we can find shallowest!
      }
    }

    return result;
  }

  private final TermsWithStopwords loadTermsAndStopwords(TermsWithStopwords termsWithStopwords, DomNode classifierIdElement, boolean isStopwords, DomNode allClassifiersParentNode) {
    TermsWithStopwords result = termsWithStopwords == null ? new TermsWithStopwords() : termsWithStopwords;

    final boolean reset = classifierIdElement.getAttributeBoolean("reset", false);
    if (reset) result.reset();

    final boolean pluralize = classifierIdElement.getAttributeBoolean("pluralize", false);
    if (pluralize) result.pluralize();

    final boolean genitivize = classifierIdElement.getAttributeBoolean("genitivize", false);
    if (genitivize) result.genitivize();

    final NodeList childNodes = classifierIdElement.getChildNodes();
    final int numChildNodes = childNodes.getLength();
    for (int childNodeNum = 0; childNodeNum < numChildNodes; ++childNodeNum) {
      final Node childNode = childNodes.item(childNodeNum);
      if (childNode.getNodeType() != Node.ELEMENT_NODE) continue;

      final DomElement childElement = (DomElement)childNode;
      final String childNodeName = childNode.getLocalName();

      if ("terms".equalsIgnoreCase(childNodeName)) {
        result.setTerms(loadTerms(result.getTerms(), childElement, isStopwords));
      }
      else if ("textfile".equalsIgnoreCase(childNodeName)) {
        result.setTerms(loadTextFile(result.getTerms(), childElement, isStopwords, classifierIdElement.getLocalName()));
      }
      else if ("regexes".equalsIgnoreCase(childNodeName)) {
        result.setTerms(loadRegexes(result.getTerms(), childElement, isStopwords));
      }
      else if ("classifiers".equalsIgnoreCase(childNodeName)) {
        result.setTerms(loadClassifiers(result.getTerms(), childElement, isStopwords, allClassifiersParentNode));
      }
      else if ("classifier".equalsIgnoreCase(childNodeName)) {
        result.setTerms(loadClassifier(result.getTerms(), childElement, isStopwords));
      }
      else if ("feature".equalsIgnoreCase(childNodeName)) {
        result.setTerms(loadFeature(result.getTerms(), childElement, isStopwords));
      }
      else if ("predelim".equalsIgnoreCase(childNodeName)) {
        result.setTerms(loadStepTest(result.getTerms(), childElement, isStopwords));
      }
      else if ("postdelim".equalsIgnoreCase(childNodeName)) {
        result.setTerms(loadStepTest(result.getTerms(), childElement, isStopwords));
      }
      else if ("test".equalsIgnoreCase(childNodeName)) {
        result.setTerms(loadStepTest(result.getTerms(), childElement, isStopwords));
      }
      else if ("stopwords".equalsIgnoreCase(childNodeName)) {
        result.setStopwords(loadTermsAndStopwords(result.getStopwords(), childElement, true, allClassifiersParentNode));
      }
    }

    if (result.getTerms() != null) {
      // adjust user-defined down to finite observed if warranted
      final int maxWordCount = result.getTerms().getMaxWordCount();
      final int curMaxWordCount = super.getMaxWordCount();
      getTokenClassifierHelper().setMaxWordCount(adjustMaxWordCount(maxWordCount, curMaxWordCount));
    }

    return result;
  }

  public String getRoteListType() {
    return roteListType;
  }

  public boolean isEmpty() {
    return termsAndStopwords == null || termsAndStopwords.isEmpty();
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
  public boolean doClassifyStopword(Token token, AtnState atnState) {
    return termsAndStopwords == null ? false : termsAndStopwords.doClassifyStopword(token, atnState);
  }

  /**
   * Determine whether the text is a stopword.
   */
  public Map<String, String> doClassifyStopword(String text) {
    return termsAndStopwords == null ? null : termsAndStopwords.doClassifyStopword(text);
  }

  /**
   * Determine whether the token is a term.
   */
  public boolean doClassifyTerm(Token token, AtnState atnState) {
    return termsAndStopwords == null ? false : termsAndStopwords.doClassifyTerm(token, atnState);
  }

  /**
   * Determine whether the text is a term.
   */
  public Map<String, String> doClassifyTerm(String text) {
    return termsAndStopwords == null ? null : termsAndStopwords.doClassifyTerm(text);
  }

  /**
   * Determine whether the token is a valid term, and not a stopword.
   */
  public boolean doClassify(Token token, AtnState atnState) {
    if (trace) {
      System.out.println("RoteListClassifier(" + getName() + ").classify(" + token + "," + atnState + ")...");
    }

    final boolean result = termsAndStopwords.doClassify(token, atnState);

    if (trace) {
      System.out.println("\tRoteListClassifier(" + getName() + ").classify(" + token + ").result=" + result);
    }

    return result;
  }

  /**
   * Determine whether the text is a valid term, and not a stopword.
   */
  public Map<String, String> doClassify(String text) {
    if (trace) {
      System.out.println("RoteListClassifier(" + getName() + ").classify(" + text + ")...");
    }

    final Map<String, String> result = termsAndStopwords.doClassify(text);

    if (trace) {
      System.out.println("RoteListClassifier(" + getName() + ").classify(" + text + ").result=" + result);
    }

    return result;
  }

  protected boolean getDefaultCaseSensitivity() {
    return defaultCaseSensitivity;
  }

  protected String getClassFeature() {
    return classFeature;
  }

  protected TermsWithStopwords getTermsWithStopwords() {
    return termsAndStopwords;
  }

  protected final TermsBundle loadTerms(TermsBundle termsBundle, DomElement termsElement, boolean isStopwords) {
    if (termsElement == null) return termsBundle;

    if (termsBundle == null) termsBundle = new TermsBundle(isStopwords, super.getTokenClassifierHelper());
    termsBundle.loadTerms(termsElement, this.defaultCaseSensitivity, isStopwords ? null : this.classFeature);
    return termsBundle;
  }

  protected final TermsBundle loadTextFile(TermsBundle termsBundle, DomElement textfileElement, boolean isStopwords, String classifierName) {
    if (textfileElement == null) return termsBundle;

    if (termsBundle == null) termsBundle = new TermsBundle(isStopwords, super.getTokenClassifierHelper());
    termsBundle.loadTextFile(textfileElement, this.defaultCaseSensitivity, isStopwords ? null : this.classFeature, resourceManager, classifierName);
    return termsBundle;
  }

  protected final TermsBundle loadRegexes(TermsBundle termsBundle, DomElement regexesElement, boolean isStopwords) {
    if (regexesElement == null) return termsBundle;

    if (termsBundle == null) termsBundle = new TermsBundle(isStopwords, super.getTokenClassifierHelper());
    termsBundle.loadRegexes(regexesElement, this.defaultCaseSensitivity, isStopwords ? null : this.classFeature);
    return termsBundle;
  }

  protected final TermsBundle loadClassifiers(TermsBundle termsBundle, DomElement classifiersElement, boolean isStopwords, DomNode classifiersParentNode) {
    if (classifiersElement == null) return termsBundle;

    if (termsBundle == null) termsBundle = new TermsBundle(isStopwords, super.getTokenClassifierHelper());
    termsBundle.loadClassifiers(classifiersElement, this.defaultCaseSensitivity, isStopwords ? null : this.classFeature, resourceManager, classifiersParentNode);
    return termsBundle;
  }

  protected final TermsBundle loadClassifier(TermsBundle termsBundle, DomElement classifierElement, boolean isStopwords) {
    if (classifierElement == null) return termsBundle;

    if (termsBundle == null) termsBundle = new TermsBundle(isStopwords, super.getTokenClassifierHelper());
    termsBundle.loadClassifier(classifierElement, this.defaultCaseSensitivity, isStopwords ? null : this.classFeature, resourceManager);
    return termsBundle;
  }

  protected final TermsBundle loadFeature(TermsBundle termsBundle, DomElement featureElement, boolean isStopwords) {
    if (featureElement == null) return termsBundle;

    if (termsBundle == null) termsBundle = new TermsBundle(isStopwords, super.getTokenClassifierHelper());
    termsBundle.loadFeature(featureElement, this.defaultCaseSensitivity, isStopwords ? null : this.classFeature, resourceManager);
    return termsBundle;
  }

  protected final TermsBundle loadStepTest(TermsBundle termsBundle, DomElement stepTestElement, boolean isStopwords) {
    if (stepTestElement == null) return termsBundle;

    if (termsBundle == null) termsBundle = new TermsBundle(isStopwords, super.getTokenClassifierHelper());
    termsBundle.loadStepTest(stepTestElement, this.defaultCaseSensitivity, isStopwords ? null : this.classFeature, resourceManager);
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
    private List<ClassifierContainer> classifiers;
    private Map<String, FeatureContainer> features;
    private StepTestContainer testContainer;
    private boolean trace;
    private TokenClassifierHelper tokenClassifierHelper;
    private boolean hasOnlyTests;
    private boolean pluralize;
    private boolean genitivize;

    public Terms(boolean caseSensitive, String classFeature, boolean isStopwords, int userDefinedMaxWordCount, TokenClassifierHelper tokenClassifierHelper) {
      this.caseSensitive = caseSensitive;
      this.classFeature = "".equals(classFeature) ? null : classFeature;
      this.isStopwords = isStopwords;
      this.userDefinedMaxWordCount = userDefinedMaxWordCount;
      this.maxWordCount = 0;
      this.term2attributes = new HashMap<String, Map<String, String>>();
      this.regexes = null;
      this.classifiers = null;
      this.features = null;
      this.testContainer = null;
      this.trace = false;
      this.tokenClassifierHelper = tokenClassifierHelper;
      this.hasOnlyTests = true;
      this.pluralize = false;
      this.genitivize = false;
    }

    public void pluralize() {
      this.pluralize = true;
    }

    public void genitivize() {
      this.genitivize = true;
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

      if (classifiers != null) {
        for (ClassifierContainer classifier : classifiers) {
          classifier.setTrace(trace);
        }
      }
    }

    public void reset() {
      if (term2attributes != null) term2attributes.clear();
      this.regexes = null;
      this.classifiers = null;
      this.features = null;
      this.testContainer = null;
      this.hasOnlyTests = true;
    }

    public boolean isEmpty() {
      return
        (term2attributes == null || term2attributes.size() == 0) &&
        (this.regexes == null || this.regexes.size() == 0) &&
        (this.classifiers == null || this.classifiers.size() == 0) &&
        (this.features == null || this.features.size() == 0) &&
        (this.testContainer == null || this.testContainer.isEmpty());
    }

    public Map<String, Map<String, String>> getTerm2Attributes() {
      return term2attributes;
    }

    public RegexDataContainer getRegexes() {
      return regexes;
    }

    public List<ClassifierContainer> getClassifiers() {
      return classifiers;
    }

    public Map<String, FeatureContainer> getFeatures() {
      return features;
    }

    public StepTestContainer getTestContainer() {
      return testContainer;
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

    public boolean doClassify(Token token, AtnState atnState) {
      boolean result = false;

      boolean exceedsMaxWordCount = false;

      if (maxWordCount > 0 && token.getWordCount() > maxWordCount) {
        // local maxWordCount may be stricter than global
        exceedsMaxWordCount = true;
      }

      boolean hasClassAttribute = false;
      String key = tokenClassifierHelper.getNormalizedText(token);

      if (!exceedsMaxWordCount) {
        if (!caseSensitive) {
          key = key.toLowerCase();
        }

        if (term2attributes.containsKey(key)) {
          result = true;
        }

        if (!result && pluralize) {
          final String dkey = depluralize(key);
          if (dkey != null && term2attributes.containsKey(dkey)) {
            key = dkey;
            result = true;
            token.setFeature("pluralized", "true", this);
          }
        }

        if (!result && genitivize) {
          final String dkey = degenitivize(key);
          if (dkey != null && term2attributes.containsKey(dkey)) {
            key = dkey;
            result = true;
            token.setFeature("genitivized", "true", this);
          }
        }

        if (result) {
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
          for (ClassifierContainer classifier : classifiers) {
            final boolean curResult = classifier.doClassify(token, atnState);
            if (curResult) {
              if (trace) {
                System.out.println("\tfound '" + key + "' in classifier '" + classifier.getName() + "'");
              }
              result = true;
              // keep going to add features from further matches
            }
          }
        }

        if (!result && features != null) {
          for (Map.Entry<String, FeatureContainer> featureEntry : features.entrySet()) {
            final FeatureContainer featureContainer = featureEntry.getValue();
            result = featureContainer.doClassify(token, atnState);
            if (result && trace) {
              final String feature = featureContainer.getFeatureName();
              System.out.println("\tfound feature '" + feature +
                                 "' (value=" + token.getFeatureValue(
                                   featureContainer.getFeatureName(),
                                   null,
                                   featureContainer.getType()) +
                                 ") on token '" + token + "'");
            }
          }
        }

        // NOTE: token tests (predelim/postdelim/test) are only applied to verify (or reverse) a match
        if ((result || hasOnlyTests) && testContainer != null && !testContainer.isEmpty()) {
          result = testContainer.verify(token, atnState).accept();

          if (trace) {
            System.out.println("\ttokenTests.verify(" + token + "," + atnState + ")=" + result);
          }
        }

        if (result && classFeature != null && !hasClassAttribute) {
          token.setFeature("class", classFeature, this);
        }

        if (trace && !result) {
          System.out.println("\tdidn't find '" + key + "'");
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

      boolean matched = false;

      // go ahead and get attributes for both terms and stopwords
      if (term2attributes.containsKey(key)) {
        matched = true;
        result = term2attributes.get(key);
      }

      if (!matched && pluralize) {
        final String dkey = depluralize(key);
        if (dkey != null && term2attributes.containsKey(dkey)) {
          matched = true;
          key = dkey;
          result = new HashMap<String, String>(term2attributes.get(dkey));
          result.put("pluralized", "true");
        }
      }

      if (!matched && genitivize) {
        final String dkey = degenitivize(key);
        if (dkey != null && term2attributes.containsKey(dkey)) {
          matched = true;
          key = dkey;
          result = new HashMap<String, String>(term2attributes.get(dkey));
          result.put("genitivized", "true");
        }
      }

      if (!matched && regexes != null) {
        result = regexes.matches(key);
        if (result != null) {
          matched = true;
        }
      }

      //NOTE: matching raw text against classifiers can fail
      if ((classifiers != null) && (!isStopwords || !matched)) {
        for (ClassifierContainer classifier : classifiers) {
          final Map<String, String> curResult = classifier.doClassify(key);
          if (curResult != null) {
            if (result == null) result = new HashMap<String, String>();
            result.putAll(curResult);
            matched = true;
            //keep going to add features from further matches
          }
        }
      }

      //NOTE: unable to match raw text against features
      //NOTE: unable to match raw text against testContainer
//TODO: remove this (and related) text matching method by fixing infrastructure for free-text matching cases

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

    private final String depluralize(String key) {
      String result = null;

      final int keylen = key.length();
      if (keylen > 0) {
        final char lastchar = key.charAt(keylen - 1);
        if ((lastchar == 's' || lastchar == 'S') && keylen > 1) {
          final char penultimatechar = key.charAt(keylen - 2);
          if (penultimatechar != '\'') {
            result = key.substring(0, keylen - 1);
          }
        }
      }

      return result;
    }

    private final String degenitivize(String key) {
      String result = null;

      final int keylen = key.length();
      if (keylen > 2) {
        final char lastchar = key.charAt(keylen - 1);
        final char penultimatechar = key.charAt(keylen - 2);
        if ((lastchar == 's' || lastchar == 'S') && (penultimatechar == '\'')) {
          result = key.substring(0, keylen - 2);
        }
      }

      return result;
    }

    protected final void loadTerms(DomElement termsElement) {

      // get global attributes to apply to each term (ignoring 'caseSensitive', 'pluralize', 'genitivize' attributes)
      Map<String, String> globalAttributes = null;
      if (termsElement.hasAttributes()) {
        final Map<String, String> termsElementAttributes = termsElement.getDomAttributes().getAttributes();
        int minAttrCount = 1;
        if (termsElementAttributes.containsKey("caseSensitive")) ++minAttrCount;
        if (termsElementAttributes.containsKey("pluralize")) ++minAttrCount;
        if (termsElementAttributes.containsKey("genitivize")) ++minAttrCount;

        if (termsElementAttributes.size() >= minAttrCount) {
          globalAttributes = new HashMap<String, String>(termsElementAttributes);

          // ignore caseSensitive, pluralize, genitivize attributes
          globalAttributes.remove("caseSensitive");
          globalAttributes.remove("pluralize");
          globalAttributes.remove("genitivize");
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
            final DomNamedNodeMap domNodeMap = termElement.getDomAttributes();

            // apply global attributes, but don't clobber term-level overrides
            if (globalAttributes != null) {
              for (Map.Entry<String, String> entry : globalAttributes.entrySet()) {
                if (!termAttributes.containsKey(entry.getKey())) {
                  domNodeMap.setAttribute(entry.getKey(), entry.getValue());
                }
              }
            }
            termAttributes = domNodeMap.getAttributes();
          }
          else if (globalAttributes != null) {
            termAttributes = new HashMap<String, String>(globalAttributes);
          }
          else {
            termAttributes = null;
          }
        }
        addTermAttributes(term, termAttributes);
      }

      if (term2attributes != null && term2attributes.size() > 0) {
        hasOnlyTests = false;
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

      if (term2attributes != null && term2attributes.size() > 0) {
        hasOnlyTests = false;
      }
    }

    protected final void loadRegexes(DomElement regexesElement) {
      this.regexes = new RegexDataContainer(regexesElement);

      if (regexes.size() > 0) {
        hasOnlyTests = false;
      }
    }

    protected final void loadClassifiers(DomElement classifiersElement, ResourceManager resourceManager, DomNode classifiersParentNode) {
      final NodeList classifierNodes = classifiersElement.getChildNodes();
      for (int i = 0; i < classifierNodes.getLength(); ++i) {
        final Node curNode = classifierNodes.item(i);
        if (curNode.getNodeType() != Node.ELEMENT_NODE) continue;

        final String nodeName = curNode.getNodeName();
        final String classifierName = curNode.getTextContent().trim();

        if ("classifier".equals(nodeName)) {
          loadClassifier((DomNode)curNode, resourceManager);
        }
        else if ("feature".equals(nodeName)) {
          loadFeature((DomNode)curNode);
        }
        else if ("allNamedClassifiers".equals(nodeName)) {
          // look at each classifiersParentNode child elts w/"name" attribute
          final NodeList childNodes = classifiersParentNode.getChildNodes();
          final int numChildNodes = childNodes.getLength();
          for (int childNodeNum = 0; childNodeNum < numChildNodes; ++childNodeNum) {
            final Node childNode = childNodes.item(childNodeNum);
            if (childNode == classifiersElement ||
                childNode.getNodeType() != Node.ELEMENT_NODE ||
                !childNode.hasAttributes())
              continue;

            final DomElement childElement = (DomElement)childNode;
            final boolean verbose = childElement.getAttributeBoolean("verbose", false);
            final boolean trace = childElement.getAttributeBoolean("trace", verbose);
            final String cName = childElement.getAttributeValue("name", null);
            retrieveClassifier(cName, resourceManager, trace);
          }
        }
        else {
          if (GlobalConfig.verboseLoad()) {
            System.out.println(new Date() + ": WARNING : Unrecognized 'classifiers' sub-element '" +
                               nodeName + "'. Expecting 'classifier' or 'feature'.");
          }
        }
      }
    }

    protected final void loadClassifier(DomNode classifierElement, ResourceManager resourceManager) {
      String classifierName = classifierElement.hasAttributes() ? classifierElement.getAttributeValue("cat", null) : null;
      if (classifierName == null) {
        classifierName = classifierElement.getTextContent().trim();
      }
      final boolean verbose = classifierElement.getAttributeBoolean("verbose", false);
      final boolean trace = classifierElement.getAttributeBoolean("trace", verbose);
                                                                  
      retrieveClassifier(classifierName, resourceManager, trace);

      if (classifiers != null && classifiers.size() > 0) {
        hasOnlyTests = false;
      }
    }

    private final void retrieveClassifier(String classifierName, ResourceManager resourceManager, boolean trace) {
      if (this.classifiers == null) this.classifiers = new ArrayList<ClassifierContainer>();
      final ClassifierContainer classifier = new ClassifierContainer(classifierName, resourceManager);
      if (trace) classifier.setTrace(trace);
      this.classifiers.add(classifier);
    }

    protected final void loadFeature(DomNode featureElement) {
      final FeatureContainer featureContainer = new FeatureContainer(featureElement);
      final String featureKey = featureContainer.getFeatureKey();

      if (featureKey != null) {
        if (this.features == null) this.features = new HashMap<String, FeatureContainer>();
        this.features.put(featureKey, featureContainer);
      }

      if (features != null && features.size() > 0) {
        hasOnlyTests = false;
      }
    }

    protected final void loadStepTest(DomElement stepTestElement, ResourceManager resourceManager) {
      if (testContainer == null) testContainer = new StepTestContainer();
      final StepTestWrapper testWrapper = new StepTestWrapper(stepTestElement, resourceManager);
      testContainer.addTestWrapper(testWrapper);
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
    private TokenClassifierHelper tokenClassifierHelper;
    private boolean pluralize;
    private boolean genitivize;

    public TermsBundle(boolean isStopwords, TokenClassifierHelper tokenClassifierHelper) {
      this.isStopwords = isStopwords;
      this.caseSensitiveTerms = null;
      this.caseInsensitiveTerms = null;
      this.maxWordCount = 0;
      this.tokenClassifierHelper = tokenClassifierHelper;
      this.pluralize = false;
      this.genitivize = false;
    }

    void setTrace(boolean trace) {
      if (caseSensitiveTerms != null) caseSensitiveTerms.setTrace(trace);
      if (caseInsensitiveTerms != null) caseInsensitiveTerms.setTrace(trace);
    }

    public void pluralize() {
      this.pluralize = true;

      if (caseSensitiveTerms != null) caseSensitiveTerms.pluralize();
      if (caseInsensitiveTerms != null) caseInsensitiveTerms.pluralize();
    }

    public void genitivize() {
      this.genitivize = true;

      if (caseSensitiveTerms != null) caseSensitiveTerms.genitivize();
      if (caseInsensitiveTerms != null) caseInsensitiveTerms.genitivize();
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

    public boolean doClassify(Token token, AtnState atnState) {
      boolean result = false;

      if (caseSensitiveTerms != null) {
        result = caseSensitiveTerms.doClassify(token, atnState);
      }

      if ((caseInsensitiveTerms != null) && (!isStopwords || !result)) {
        // even if prior result succeeded, try again here to get any new token features (unless stopwords)
        result |= caseInsensitiveTerms.doClassify(token, atnState);
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

    protected final void loadClassifiers(DomElement classifiersElement, boolean defaultCaseSensitivity, String classFeature, ResourceManager resourceManager, DomNode classifiersParentNode) {
      final Terms theTerms = getTheTerms(classifiersElement, defaultCaseSensitivity, classFeature);
      theTerms.loadClassifiers(classifiersElement, resourceManager, classifiersParentNode);
      adjustMaxWordCount(theTerms.getMaxWordCount());
    }

    protected final void loadClassifier(DomElement classifierElement, boolean defaultCaseSensitivity, String classFeature, ResourceManager resourceManager) {
      final Terms theTerms = getTheTerms(classifierElement, defaultCaseSensitivity, classFeature);
      theTerms.loadClassifier(classifierElement, resourceManager);
      adjustMaxWordCount(theTerms.getMaxWordCount());
    }

    protected final void loadFeature(DomElement featureElement, boolean defaultCaseSensitivity, String classFeature, ResourceManager resourceManager) {
      final Terms theTerms = getTheTerms(featureElement, defaultCaseSensitivity, classFeature);
      theTerms.loadFeature(featureElement);
      adjustMaxWordCount(theTerms.getMaxWordCount());
    }

    protected final void loadStepTest(DomElement stepTestElement, boolean defaultCaseSensitivity, String classFeature, ResourceManager resourceManager) {
      final Terms theTerms = getTheTerms(stepTestElement, defaultCaseSensitivity, classFeature);
      theTerms.loadStepTest(stepTestElement, resourceManager);
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
        if (this.caseSensitiveTerms == null) this.caseSensitiveTerms = new Terms(true, classFeature, isStopwords, maxWordCount, tokenClassifierHelper);
        theTerms = this.caseSensitiveTerms;
      }
      else {
        if (this.caseInsensitiveTerms == null) this.caseInsensitiveTerms = new Terms(false, classFeature, isStopwords, maxWordCount, tokenClassifierHelper);
        theTerms = this.caseInsensitiveTerms;
      }

      if (pluralize) {
        theTerms.pluralize();
      }
      else {
        this.pluralize = theElement.getAttributeBoolean("pluralize", false);
        if (pluralize) theTerms.pluralize();
      }

      if (genitivize) {
        theTerms.genitivize();
      }
      else {
        this.genitivize = theElement.getAttributeBoolean("genitivize", false);
        if (genitivize) theTerms.genitivize();
      }

      return theTerms;
    }
  }

  protected static final class TermsWithStopwords {
    private TermsBundle terms;
    private TermsWithStopwords stopwords;
    private boolean pluralize;
    private boolean genitivize;

    public TermsWithStopwords() {
      this.terms = null;
      this.stopwords = null;
      this.pluralize = false;
      this.genitivize = false;
    }

    public void reset() {
      if (terms != null) terms.reset();
      if (stopwords != null) stopwords.reset();
    }

    public void pluralize() {
      this.pluralize = true;

      if (terms != null) terms.pluralize();
      if (stopwords != null) stopwords.pluralize();
    }

    public void genitivize() {
      this.genitivize = true;

      if (terms != null) terms.genitivize();
      if (stopwords != null) stopwords.genitivize();
    }

    public void setTrace(boolean trace) {
      if (terms != null) {
        terms.setTrace(trace);
      }
      if (stopwords != null) {
        stopwords.setTrace(trace);
      }
    }

    public TermsBundle getTerms() {
      return terms;
    }

    public void setTerms(TermsBundle terms) {
      this.terms = terms;
      if (pluralize && terms != null) {
        terms.pluralize();
      }
      if (genitivize && terms != null) {
        terms.genitivize();
      }
    }

    public TermsWithStopwords getStopwords() {
      return stopwords;
    }

    public void setStopwords(TermsWithStopwords stopwords) {
      this.stopwords = stopwords;
      if (pluralize && stopwords != null) {
        stopwords.pluralize();
      }
      if (genitivize && stopwords != null) {
        stopwords.genitivize();
      }
    }

    public boolean hasStopwords() {
      return stopwords != null && !stopwords.isEmpty();
    }

    public boolean isEmpty() {
      return (terms == null || terms.isEmpty()) && (stopwords == null || stopwords.isEmpty());
    }

    public boolean doClassify(Token token, AtnState atnState) {
      boolean result = false;

      if ((stopwords == null || (stopwords != null && !stopwords.doClassify(token, atnState))) &&
          (terms != null)) {
        result = terms.doClassify(token, atnState);
      }

      return result;
    }

    public Map<String, String> doClassify(String text) {
      Map<String, String> result = null;

      if ((stopwords == null || (stopwords != null && stopwords.doClassify(text) == null)) &&
          (terms != null)) {
        result = terms.doClassify(text);
      }

      return result;
    }

    public boolean doClassifyTerm(Token token, AtnState atnState) {
      boolean result = false;

      if (terms != null && token != null) {
        result = terms.doClassify(token, atnState);
      }

      return result;
    }

    public Map<String, String> doClassifyTerm(String text) {
      Map<String, String> result = null;

      if (terms != null && text != null) {
        result = terms.doClassify(text);
      }

      return result;
    }

    public boolean doClassifyStopword(Token token, AtnState atnState) {
      boolean result = false;

      if (stopwords != null && token != null) {
        result = stopwords.doClassify(token, atnState);
      }

      return result;
    }

    public Map<String, String> doClassifyStopword(String text) {
      Map<String, String> result = null;

      if (stopwords != null && text != null) {
        result = stopwords.doClassify(text);
      }

      return result;
    }
  }

  protected static final class ClassifierContainer {
    private String classifierName;
    private AtnStateTokenClassifier namedResource;      // temp storage
    private List<AtnStateTokenClassifier> _classifiers; // delayed load
    private boolean literalMatch;                       // delayed load
    private boolean ruleMatch;                          // delayed load
    private boolean trace;

    private Object classifiersMutex = new Object();

    public ClassifierContainer(String classifierName, ResourceManager resourceManager) {
      this.classifierName = classifierName;
      this.namedResource = findClassifierResource(classifierName, resourceManager);
      this._classifiers = null;
      this.literalMatch = false;
      this.ruleMatch = false;
    }

    public boolean getTrace() {
      return trace;
    }

    public void setTrace(boolean trace) {
      this.trace = trace;
    }

    public String getName() {
      return classifierName;
    }

    private final AtnStateTokenClassifier findClassifierResource(String classifierName, ResourceManager resourceManager) {
      AtnStateTokenClassifier result = null;

      final Object classifierObject = classifierName != null && !"".equals(classifierName) ? resourceManager.getResource(classifierName) : null;
      if (classifierObject == null) {
        if (GlobalConfig.verboseLoad()) {
          System.out.println(new Date() + ": WARNING : RoteListClassifier unknown classifier '" + classifierName + "'");
        }
      }
      else {
        if (classifierObject instanceof AtnStateTokenClassifier) {
//TODO: adjust maxWordCount in containing Terms instance?
          result = (AtnStateTokenClassifier)classifierObject;
        }
        else {
          if (GlobalConfig.verboseLoad()) {
            System.out.println(new Date() + ": WARNING : classifier '" +
                               classifierName + "' is *NOT* an AtnStateTokenClassifier (" +
                               classifierObject.getClass().getName() + ")");
          }
        }
      }

      return result;
    }

    public boolean doClassify(Token token, AtnState atnState) {
      final List<AtnStateTokenClassifier> classifiers = getClassifiers(atnState);

      boolean result = false;

      // classify by resource and grammar classifiers
      for (AtnStateTokenClassifier classifier : classifiers) {
        result = classifier.classify(token, atnState).matched();

        if (result) {
          if (trace) {
            System.out.println("\tclassifier(" + classifierName + ").classify(" + token + "," + atnState + ")=true");
          }
          break;
        }
      }

      // check for literal token match
      if (!result && literalMatch) {
        result = classifierName != null && classifierName.equals(token.getText());

        if (result && trace) {
          System.out.println("\tliteralMatch(" + classifierName + ")=true");
        }
      }

      // check for rule match
      if (!result && ruleMatch) {
        result = atnState.getRuleStep().getLabel().equals(classifierName);

        for (AtnState theState = atnState.getPushState(); !result && theState != null; theState = theState.getPushState()) {
          final AtnRule rule = theState.getRule();
          final String ruleName = rule.getRuleName();
          final String ruleId = rule.getRuleId();

          result = ruleName.equals(classifierName);
          if (!result && ruleId != null) {
            result = ruleId.equals(classifierName);
          }
        }
      }

      // check for a token feature that matches the category
      if (!result) {
        final Feature feature = token.getFeature(classifierName, null, true); //todo: parameterize "broaden"?
        result = (feature != null);

        if (result && trace) {
          System.out.println("\ttokenFeature(" + classifierName + ")=" + feature);
        }
      }

      return result;
    }

    private final List<AtnStateTokenClassifier> getClassifiers(AtnState atnState) {
      List<AtnStateTokenClassifier> result = null;

      synchronized (classifiersMutex) {
        if (_classifiers == null) {
          loadClassifiers(atnState);
        }
        result = _classifiers;
      }

      return result;
    }

    public Map<String, String> doClassify(String text) {
      Map<String, String> result = null;

      if (this._classifiers == null) {
        if (trace) {
          System.out.println("\tWARNING: ClassifierContainer(" + classifierName + ") not fully initialized for doClassify(" + text + ")");
        }

        if (namedResource != null) {
          final Map<String, String> curResult = namedResource.classify(text);
          if (curResult != null) {
            if (result == null) new HashMap<String, String>();
            result.putAll(curResult);

            if (trace) {
              System.out.println("\tclassifier(" + classifierName + ").classify(" + text + ")=" + curResult);
            }
          }
        }
      }
      else {
        // classify by resource and grammar classifiers
        for (AtnStateTokenClassifier classifier : _classifiers) {
          final Map<String, String> curResult = classifier.classify(text);
          if (curResult != null) {
            if (result == null) new HashMap<String, String>();
            result.putAll(curResult);

            if (trace) {
              System.out.println("\tclassifier(" + classifierName + ").classify(" + text + ")=" + curResult);
            }
          }
        }
      }

      return result;
    }

    private final synchronized void loadClassifiers(AtnState atnState) {
      this._classifiers = new ArrayList<AtnStateTokenClassifier>();

      if (namedResource != null) {
        this._classifiers.add(namedResource);
      }

      final AtnGrammar grammar = atnState.getRule().getGrammar();
      final List<AtnStateTokenClassifier> tokenClassifiers = grammar.getClassifiers(classifierName);
      if (tokenClassifiers != null) {
//TODO: adjust maxWordCount in containing Terms instance?
        this._classifiers.addAll(tokenClassifiers);
      }
      else {
        if (grammar.getCat2Rules().containsKey(classifierName)) {
          ruleMatch = true;
        }
        else {
          literalMatch = true;
        }
      }
    }
  }

  private static final int WHEN_EXISTS = 0;
  private static final int WHEN_EQUALS = 1;
  private static final int WHEN_MATCHES_PREV = 2;
  private static final int WHEN_MATCHES_NEXT = 3;

  protected static final class FeatureContainer {

    private String featureName;
    private Class type;
    private int when;
    private String text;

    public FeatureContainer(DomNode featureElement) {
      this.featureName = getFeatureName(featureElement);
      this.type = getType(featureElement); //NOTE: may adjust featureName
      this.when = getWhen(featureElement);
      this.text = featureElement.getTextContent().trim();
    }

    public String getFeatureKey() {
      return (featureName == null && type != null) ? TYPED_FEATURE_KEY : featureName;
    }

    public String getFeatureName() {
      return featureName;
    }

    public Class getType() {
      return type;
    }

    public boolean doClassify(Token token, AtnState atnState) {
      boolean result = false;

      switch (when) {
        case WHEN_EQUALS :
          // true when text equals feature's value (.toString)
          result = token.hasFeatureValue(featureName, null, type, text);
          break;
        case WHEN_MATCHES_PREV :
          // true when previous state's token's feature value matches this token's feature value
          final Token prevToken = TokenTest.getPrevToken(token);
          // assume that if this is the first token that the prev "would" match
          result = prevToken == null ? true : token.hasMatchingFeatureValue(prevToken, featureName, null, type);
          break;
        case WHEN_MATCHES_NEXT :
          // true when next state's token's feature value matches this token's feature value
          final Token nextToken = token.getNextToken();
          // assume that if this is the last token that the next "would" match
          result = nextToken == null ? true : token.hasMatchingFeatureValue(nextToken, featureName, null, type);
          break;
        default : // WHEN_EXISTS
          // true when feature exists on token
          if (token.getFeatureValue(featureName, null, type) != null) {
            result = true;
          }
          break;
      }

      return result;
    }

    private final String getFeatureName(DomNode featureElement) {
      String result  = featureElement.hasAttributes() ? featureElement.getAttributeValue("name", null) : null;
      if (result == null) {
        result = featureElement.getTextContent().trim();
        if ("".equals(result)) result = null;
      }
      return result;
    }

    private final Class getType(DomNode featureElement) {
      Class result = null;
      final String typeString = featureElement.getAttributeValue("type", null);
      if (typeString != null && !"".equals(typeString)) {
        if ("interp".equals(typeString)) {
          result = ParseInterpretation.class;
        }
        else if ("parse".equals(typeString)) {
          //NOTE: this requires an adjustment the featureName
          this.featureName = AtnParseBasedTokenizer.SOURCE_PARSE;
          result = Parse.class;
        }
        else if ("string".equals(typeString)) {
          result = String.class;
        }
        else {
          try {
            result = Class.forName(typeString);
          }
          catch (ClassNotFoundException e) {
            System.err.println(new Date() +
                               ": RoteListClassifier bad feature type '" +
                               typeString + "' IGNORED");
            result = null;
          }
        }
      }
      return result;
    }

    private final int getWhen(DomNode featureElement) {
      int result = WHEN_EXISTS;

      // exists | equals | matches-prev | matches-next

      String when = featureElement.hasAttributes() ? featureElement.getAttributeValue("when", null) : null;
      if (when != null && !"".equals(when)) {
        when = when.toLowerCase();
        if ("equals".equals(when)) {
          result = WHEN_EQUALS;
        }
        else if (when.startsWith("matches")) {
          if (when.endsWith("prev")) {
            result = WHEN_MATCHES_PREV;
            
          }
          else if (when.endsWith("next")) {
            result = WHEN_MATCHES_NEXT;
          }
        }
      }
      //else, default to WHEN_EXISTS

      return result;
    }
  }  
}
