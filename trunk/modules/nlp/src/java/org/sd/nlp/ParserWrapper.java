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


import org.sd.fsm.Grammar;
import org.sd.io.FileUtil;
import org.sd.util.ReflectUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

/**
 * A wrapper for running a parser.
 * <p>
 * @author Spence Koehler
 */
public class ParserWrapper {

  public static final ParserWrapper buildParserWrapper(Class clazz, String propertiesResource) {
    ParserWrapper result = null;

    try {
      result = buildParserWrapper(FileUtil.getInputStream(clazz, propertiesResource));
    }
    catch (IOException e) {
      throw new IllegalStateException(e);
    }

    return result;
  }

  public static final ParserWrapper buildParserWrapper(InputStream propertiesInputStream) throws IOException {
    final Properties properties = new Properties();
    properties.load(propertiesInputStream);
    return buildParserWrapper(properties);
  }

  public static final ParserWrapper buildParserWrapper(Properties properties) throws IOException {
    // normalizer=classpath[:buildMethod]
    // breakStrategy=classpath[:buildMethod]
    // categories=[?]category1,[?]category2,...
    // grammar=[classname:]path
    // topLevelCategoryNames=category1,category2,...
    // tokenizationStrategyType=type
    // ignoreExtraInput=true/false
    // skipUpTo=NUMBER
    // allowSkipAfterFirst=true/false
    // lexiconBuilder=classpath[:buildMethod]

    final String normalizerClass = properties.getProperty("normalizer");
    final String breakStrategyClass = properties.getProperty("breakStrategy");
    final String categories = properties.getProperty("categories");
    final String[] topLevelCategoryNames = properties.getProperty("topLevelCategoryNames").split("\\s*\\,\\s*");
    final String grammarPath = properties.getProperty("grammar");
    final String tokenizationStrategyTypeName = properties.getProperty("tokenizationStrategyType");
    final boolean ignoreExtraInput = "true".equals(properties.getProperty("ignoreExtraInput", "true"));
    final int skipUpTo = Integer.parseInt(properties.getProperty("skipUpTo", "0"));
    final boolean allowSkipAfterFirst = "true".equals(properties.getProperty("allowSkipAfterFirst", "true"));
    final String lexiconBuilderClass = properties.getProperty("lexiconBuilder");

    if (categories == null || topLevelCategoryNames == null || grammarPath == null || lexiconBuilderClass == null) {
      throw new IllegalArgumentException("Properties for 'categories', 'grammarPath', 'topLevelCategoryNames' and 'lexiconBuilder' must all be defined!\n" +
                                         "  additional properties are: ''normalizer', 'breakStrategy', 'tokenizationStrategyType', 'ignoreExtraInput', 'skipUpTo', and 'allowSkipAfterFirst'.");
    }

    final CategoryFactory categoryFactory = new CategoryFactory(categories);
    final AbstractNormalizer normalizer = normalizerClass == null ? GeneralNormalizer.getCaseInsensitiveInstance() : (AbstractNormalizer)ReflectUtil.buildInstance(normalizerClass, properties);
    final BreakStrategy breakStrategy = breakStrategyClass == null ? GeneralBreakStrategy.getInstance() : (BreakStrategy)ReflectUtil.buildInstance(breakStrategyClass, properties);
    final TokenizationStrategy.Type tokenizationStrategyType = tokenizationStrategyTypeName == null ? TokenizationStrategy.Type.LONGEST_TO_SHORTEST : Enum.valueOf(TokenizationStrategy.Type.class, tokenizationStrategyTypeName);
    final InputStream grammarInputStream = FileUtil.getInputStream(grammarPath);
    final LexiconBuilder lexiconBuilder = (LexiconBuilder)ReflectUtil.buildInstance(lexiconBuilderClass, properties);
    final Lexicon lexicon = lexiconBuilder.buildLexicon(categoryFactory, normalizer);

    return
      new ParserWrapper(lexicon, categoryFactory, topLevelCategoryNames, tokenizationStrategyType,
                        normalizer, breakStrategy, grammarInputStream, ignoreExtraInput, skipUpTo,
                        allowSkipAfterFirst);
  }


  private Lexicon lexicon;
  private CategoryFactory categoryFactory;
  private Category[] topLevelCategories;
  private TokenizationStrategy.Type tokenizationStrategyType;
  private AbstractNormalizer normalizer;
  private BreakStrategy breakStrategy;
  private Grammar grammar;
  private boolean ignoreExtraInput;
  private int skipUpTo;
  private boolean allowSkipAfterFirst;
  private Parser parser;
  private TokenPointerFactory tokenPointerFactory;
  private LexicalTokenizerFactory lexicalTokenizerFactory;

  public ParserWrapper(Lexicon lexicon,
                       CategoryFactory categoryFactory,
                       String[] topLevelCategoryNames,
                       TokenizationStrategy.Type tokenizationStrategyType,
                       AbstractNormalizer normalizer,
                       BreakStrategy breakStrategy,
                       InputStream grammarInputStream,
                       boolean ignoreExtraInput,
                       int skipUpTo,
                       boolean allowSkipAfterFirst) throws IOException {
    this.lexicon = lexicon;
    this.categoryFactory = categoryFactory;
    this.topLevelCategories = categoryFactory.getCategories(topLevelCategoryNames);
    this.tokenizationStrategyType = tokenizationStrategyType;
    this.normalizer = normalizer;
    this.breakStrategy = breakStrategy;
    this.grammar = GrammarFactory.loadGrammar(categoryFactory, grammarInputStream);
    this.ignoreExtraInput = ignoreExtraInput;
    this.skipUpTo = skipUpTo;
    this.allowSkipAfterFirst = allowSkipAfterFirst;
    this.parser = new Parser(grammar, lexicon, ignoreExtraInput, skipUpTo);
    this.tokenPointerFactory = TokenizationStrategy.getStrategy(tokenizationStrategyType, lexicon);
    this.lexicalTokenizerFactory = new LexicalTokenizerFactory(tokenPointerFactory, skipUpTo);
  }

  public List<Parser.Parse> parse(String text) {
    final LexicalTokenizer lexicalTokenizer = buildLexicalTokenizer(getStringWrapper(text));
    return parser.parse(lexicalTokenizer, topLevelCategories, allowSkipAfterFirst);
  }

  public StringWrapper getStringWrapper(String text) {
    return new StringWrapper(text, breakStrategy);
  }

  public List<Parser.Parse> parse(StringWrapper stringWrapper) {
    final LexicalTokenizer lexicalTokenizer = buildLexicalTokenizer(stringWrapper);
    return parser.parse(lexicalTokenizer, topLevelCategories, allowSkipAfterFirst);
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

  public final Category[] getTopLevelCategories() {
    return topLevelCategories;
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

  public final Grammar getGrammar() {
    return grammar;
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

  public final Parser getParser() {
    return parser;
  }
}
