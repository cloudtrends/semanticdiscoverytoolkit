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
import org.sd.util.ReflectUtil;
import org.sd.util.tree.Tree;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

/**
 * A wrapper for running a parser.
 * <p>
 * @author Spence Koehler
 */
public class TokenizerWrapper {

  public static final TokenizerWrapper buildTokenizerWrapper(Class clazz, String propertiesResource) {
    TokenizerWrapper result = null;

    try {
      result = buildTokenizerWrapper(FileUtil.getInputStream(clazz, propertiesResource));
    }
    catch (IOException e) {
      throw new IllegalStateException(e);
    }

    return result;
  }

  public static final TokenizerWrapper buildTokenizerWrapper(InputStream propertiesInputStream) throws IOException {
    final Properties properties = new Properties();
    properties.load(propertiesInputStream);
    return buildTokenizerWrapper(properties);
  }

  public static final TokenizerWrapper buildTokenizerWrapper(Properties properties) throws IOException {
    // normalizer=classpath[:buildMethod]
    // breakStrategy=classpath[:buildMethod]
    // categories=[?]category1,[?]category2,...
    // tokenizationStrategyType=type
    // ignoreExtraInput=true/false
    // skipUpTo=NUMBER
    // allowSkipAfterFirst=true/false
    // lexiconBuilder=classpath[:buildMethod]

    final String normalizerClass = properties.getProperty("normalizer");
    final String breakStrategyClass = properties.getProperty("breakStrategy");
    final String categories = properties.getProperty("categories");
    final String tokenizationStrategyTypeName = properties.getProperty("tokenizationStrategyType");
    final boolean ignoreExtraInput = "true".equals(properties.getProperty("ignoreExtraInput", "true"));
    final int skipUpTo = Integer.parseInt(properties.getProperty("skipUpTo", "0"));
    final boolean allowSkipAfterFirst = "true".equals(properties.getProperty("allowSkipAfterFirst", "true"));
    final String lexiconBuilderClass = properties.getProperty("lexiconBuilder");

    if (categories == null || lexiconBuilderClass == null) {
      throw new IllegalArgumentException("Properties for 'categories' and 'lexiconBuilder' must all be defined!\n" +
                                         "  additional properties are: ''normalizer', 'breakStrategy', 'ignoreExtraInput', 'skipUpTo', 'tokenizationStrategyType', and 'allowSkipAfterFirst'.");
    }

    final CategoryFactory categoryFactory = new CategoryFactory(categories);
    final AbstractNormalizer normalizer = normalizerClass == null ? GeneralNormalizer.getCaseInsensitiveInstance() : (AbstractNormalizer)ReflectUtil.buildInstance(normalizerClass, properties);
    final BreakStrategy breakStrategy = breakStrategyClass == null ? GeneralBreakStrategy.getInstance() : (BreakStrategy)ReflectUtil.buildInstance(breakStrategyClass, properties);
    final TokenizationStrategy.Type tokenizationStrategyType = tokenizationStrategyTypeName == null ? TokenizationStrategy.Type.LONGEST_TO_SHORTEST : Enum.valueOf(TokenizationStrategy.Type.class, tokenizationStrategyTypeName);
    final LexiconBuilder lexiconBuilder = (LexiconBuilder)ReflectUtil.buildInstance(lexiconBuilderClass, properties);
    final Lexicon lexicon = lexiconBuilder.buildLexicon(categoryFactory, normalizer);

    return
      new TokenizerWrapper(lexicon, categoryFactory, tokenizationStrategyType,
                           normalizer, breakStrategy, ignoreExtraInput, skipUpTo,
                           allowSkipAfterFirst);
  }


  private Lexicon lexicon;
  private CategoryFactory categoryFactory;
  private TokenizationStrategy.Type tokenizationStrategyType;
  private AbstractNormalizer normalizer;
  private BreakStrategy breakStrategy;
  private boolean ignoreExtraInput;
  private int skipUpTo;
  private boolean allowSkipAfterFirst;
  private TokenPointerFactory tokenPointerFactory;
  private LexicalTokenizerFactory lexicalTokenizerFactory;

  public TokenizerWrapper(Lexicon lexicon,
                          CategoryFactory categoryFactory,
                          TokenizationStrategy.Type tokenizationStrategyType,
                          AbstractNormalizer normalizer,
                          BreakStrategy breakStrategy,
                          boolean ignoreExtraInput,
                          int skipUpTo,
                          boolean allowSkipAfterFirst) throws IOException {
    this.lexicon = lexicon;
    this.categoryFactory = categoryFactory;
    this.tokenizationStrategyType = tokenizationStrategyType;
    this.normalizer = normalizer;
    this.breakStrategy = breakStrategy;
    this.ignoreExtraInput = ignoreExtraInput;
    this.skipUpTo = skipUpTo;
    this.allowSkipAfterFirst = allowSkipAfterFirst;
    this.tokenPointerFactory = TokenizationStrategy.getStrategy(tokenizationStrategyType, lexicon);
    this.lexicalTokenizerFactory = new LexicalTokenizerFactory(tokenPointerFactory, skipUpTo);
  }


  /**
   * Create a tree with the exhaustive set of definable tokens from text.
   * <p>
   * The child of a node contains words that follow the parent.
   * Siblings are alternative definable words starting at the same position.
   */
  public Tree<StringWrapper.SubString> tokenize(String text) {
    return tokenize(getStringWrapper(text));
  }

  /**
   * Create a tree with the exhaustive set of definable tokens from text.
   * <p>
   * The child of a node contains words that follow the parent.
   * Siblings are alternative definable words starting at the same position.
   */
  public Tree<StringWrapper.SubString> tokenize(StringWrapper stringWrapper) {
    final Tree<StringWrapper.SubString> root = new Tree<StringWrapper.SubString>(null);
    final LexicalTokenizer lexicalTokenizer = buildLexicalTokenizer(stringWrapper);
    
    final LexicalEntry firstEntry = lexicalTokenizer.getFirstEntry();
    buildTokenTree(root, firstEntry);
    
    return root;
  }
  
  /**
   * Recursive auxiliary to build the token tree.
   */
  private final void buildTokenTree(Tree<StringWrapper.SubString> parentNode, LexicalEntry childData) {
    if (childData != null) {
      final StringWrapperTokenPointer tokenPointer = (StringWrapperTokenPointer)childData.getPointer();
      final StringWrapper.SubString subString = tokenPointer.getSubString();
      final Tree<StringWrapper.SubString> childNode = parentNode.addChild(subString);

      // next words are children in tree; smaller words from same start are siblings in tree
      final LexicalEntry grandChildData = childData.next(allowSkipAfterFirst);
      if (grandChildData != null) {
        buildTokenTree(childNode, grandChildData);
      }
      else if (!ignoreExtraInput) {
        // remainder of string is not defined, but isn't to be ignored.
        final StringWrapper.SubString remainingSubString = subString.getRemainingSubStringAfter();
        if (remainingSubString != null) {
          childNode.addChild(remainingSubString);
        }
      }
      buildTokenTree(parentNode, childData.revise());
    }
  }

  public StringWrapper getStringWrapper(String text) {
    return new StringWrapper(text, breakStrategy);
  }

  public final LexicalTokenizer buildLexicalTokenizer(String string) {
    return buildLexicalTokenizer(getStringWrapper(string));
  }

  public final LexicalTokenizer buildLexicalTokenizer(StringWrapper stringWrapper) {
    return lexicalTokenizerFactory.getLexicalTokenizer(stringWrapper);
  }

  public final Lexicon getLexicon() {
    return lexicon;
  }

  public final CategoryFactory getCategoryFactory() {
    return categoryFactory;
  }

  public final TokenizationStrategy.Type getTokenizationStrategyType() {
    return tokenizationStrategyType;
  }

  public final AbstractNormalizer getNormalizer() {
    return normalizer;
  }

  public final BreakStrategy getBreakStrategy() {
    return breakStrategy;
  }

  public final boolean getIgnoreExtraInput() {
    return ignoreExtraInput;
  }

  public final int getSkipUpTo() {
    return skipUpTo;
  }

  public final boolean getAllowSkipAfterFirst() {
    return allowSkipAfterFirst;
  }
}
