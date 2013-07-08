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
package org.sd.extract;


import org.sd.fsm.Grammar;
import org.sd.fsm.impl.GrammarImpl;
import org.sd.io.FileUtil;
import org.sd.nlp.AbstractNormalizer;
import org.sd.nlp.BreakStrategy;
import org.sd.nlp.Category;
import org.sd.nlp.CategoryFactory;
import org.sd.nlp.ExtendedGrammarTokenFactory;
import org.sd.nlp.GenericLexicon;
import org.sd.nlp.LexicalTokenizer;
import org.sd.nlp.Lexicon;
import org.sd.nlp.ParseStateDecoder;
import org.sd.nlp.Parser;
import org.sd.nlp.StringWrapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Abstract implementation of the Extractor interface using a Finite State
 * Machine (FSM) parser.
 * <p>
 * @author Spence Koehler
 */
public abstract class AbstractExtractorFSM extends AbstractExtractor {

  protected abstract CategoryFactory buildCategoryFactory();
  protected abstract Lexicon buildLexicon(CategoryFactory categoryFactory, AbstractNormalizer normalizer) throws IOException;
  protected abstract Category[] buildTopLevelCategories(CategoryFactory categoryFactory);
  protected abstract LexicalTokenizer buildLexicalTokenizer(StringWrapper stringWrapper, Lexicon lexicon, int skipUpTo);

  private boolean initialized;
  private CategoryFactory categoryFactory;
  private LexicalTokenizer lexicalTokenizer;
  private Parser parser;
  private Grammar grammar;
  private Lexicon lexicon;
  private Category[] topLevelCategories;

  private Class relativeGrammarClass;
  private String grammarResource;
  private boolean ignoreExtraInput;
  private int skipUpTo;
  private boolean allowSkipAfterFirst;

  protected AbstractExtractorFSM(String extractionType, TextAcceptor textAcceptor, TextSplitter textSplitter,
                                 boolean needsDocTextCache, boolean stopAtFirst, AbstractNormalizer normalizer,
                                 BreakStrategy breakStrategy, Class relativeGrammarClass, String grammarResource,
                                 boolean ignoreExtraInput, int skipUpTo, boolean allowSkipAfterFirst,
                                 Disambiguator disambiguator) {

    super(extractionType, textAcceptor, textSplitter, needsDocTextCache, stopAtFirst,
          normalizer, breakStrategy, disambiguator);

    this.initialized = false;
    this.categoryFactory = null;
    this.lexicalTokenizer = null;
    this.parser = null;
    this.grammar = null;
    this.lexicon = null;
    this.topLevelCategories = null;

    this.relativeGrammarClass = relativeGrammarClass;
    this.grammarResource = grammarResource;
    this.ignoreExtraInput = ignoreExtraInput;
    this.skipUpTo = skipUpTo;
    this.allowSkipAfterFirst = allowSkipAfterFirst;
  }

  private final synchronized void initialize() throws IOException {
    if (initialized) return;
    this.categoryFactory = buildCategoryFactory();
    this.lexicon = buildLexicon(categoryFactory, getNormalizer());
    this.grammar = loadGrammar(categoryFactory, relativeGrammarClass, grammarResource);
    this.parser = new Parser(grammar, lexicon, ignoreExtraInput, skipUpTo);
    this.topLevelCategories = buildTopLevelCategories(categoryFactory);
    this.initialized = true;
  }

  /**
   * Perform the extraction on the doc text.
   * 
   * @param docText  The docText to extract from.
   * @param die      Trigger to halt processing. Needs to be monitored and
   *                 obeyed; but can also be set from within the implementation.
   *                 Use with care!
   *
   * @return one or more extractions or null.
   */
  public List<Extraction> extract(DocText docText, AtomicBoolean die) {
    List<Extraction> result = null;

    // perform the extraction
    final StringWrapper[] textToParse = extractTextStrings(docText);
    if (textToParse != null) {
      for (StringWrapper stringWrapper : textToParse) {
        final List<Parser.Parse> parses = parse(stringWrapper);
        if (parses != null) {
          if (result == null) result = new ArrayList<Extraction>();
          result.add(new Extraction(getExtractionType(), docText, 0.0, new ExtractionParseData(parses)));
        }
      }
    }

    // set stopAtFirst constraint
    if (result != null) {
      super.setDidFirst(docText);
    }

    return result;
  }

  /**
   * For JUnit testing.
   * <p>
   * Parse the first string from the xmlString.
   */
  public List<Parser.Parse> parse(String xmlString) throws IOException {
    List<Parser.Parse> result = null;

    final DocText docText = DocText.makeDocText(xmlString);
    final StringWrapper[] textToParse = extractTextStrings(docText);
    if (textToParse != null && textToParse.length > 0) {
      result = parse(textToParse[0]);
    }

    return result;
  }

  private final Grammar loadGrammar(CategoryFactory categoryFactory, Class relativeGrammarClass, String grammarResource) throws IOException {
    String relative = null;
    InputStream grammarInputStream = null;

    if (relativeGrammarClass == null) {
      grammarInputStream = FileUtil.getInputStream(grammarResource);
    }
    else {
      relative = relativeGrammarClass.getName();
      grammarInputStream = relativeGrammarClass.getResourceAsStream(grammarResource);
    }

    if (grammarInputStream == null) {
      System.out.println("can't find grammar '" + grammarResource + "' from '" + relative + "'!");
      if (relativeGrammarClass == null && relative == null) {
        System.out.println("\tDefine SDN_ROOT environment variable!");
      }
    }

    return GrammarImpl.loadGrammar(grammarInputStream, ExtendedGrammarTokenFactory.getInstance(categoryFactory), ParseStateDecoder.getInstance());
  }

  protected List<Parser.Parse> parse(StringWrapper stringWrapper) {
    try {
      if (!initialized) initialize();
    }
    catch (IOException e) {
      throw new IllegalStateException("IOException in initializing parser!", e);
    }

    final LexicalTokenizer lexicalTokenizer = buildLexicalTokenizer(stringWrapper, lexicon, skipUpTo);
    return parser.parse(lexicalTokenizer, topLevelCategories, allowSkipAfterFirst);
  }

  protected Lexicon getLexicon() throws IOException {
    if (!initialized) initialize();
    return lexicon;
  }

  // load resources relative to this class
  protected GenericLexicon loadGenericLexicon(String resourceName, AbstractNormalizer normalizer,
                                              Category category, boolean caseSensitive,
                                              boolean isDefinitive, boolean hasAttributes) throws IOException {
    return GenericLexicon.loadGenericLexicon(this.getClass(), resourceName, normalizer, category,
                                             caseSensitive, isDefinitive, hasAttributes, null);
  }

  public final List<Parser.Parse> dumpParse(String string, PrintStream out) {
    final StringWrapper stringWrapper = new StringWrapper(string, getBreakStrategy());

    final List<Parser.Parse> parses = parse(stringWrapper);
    out.println("parses(" + string + ")=" + parses);
    return parses;
  }

  public final void processFileLines(String filename, PrintStream verboseOut, PrintStream verboseErr) throws IOException {
    final BufferedReader reader = FileUtil.getReader(filename);
    String line = null;
    final long startTime = System.currentTimeMillis();
    int tcount = 0;
    int gcount = 0;
    while ((line = reader.readLine()) != null) {
      try {
        if (dumpParse(line, verboseOut) != null) ++gcount;
      }
      catch (Throwable t) {
        throw new IllegalStateException("ERROR! (count=" + tcount + ", line=" + line + ")!", t);
      }
      ++tcount;
    }
    final long totalTime = System.currentTimeMillis() - startTime;
    final double totalSeconds = (totalTime / 1000.0);

    if (verboseErr != null) {
      verboseErr.println("processed " + tcount + " lines in " + totalSeconds + " seconds. (" + (tcount / totalSeconds) + " lines/sec)");
      verboseErr.println("\t" + gcount + " of " + tcount + " parses succeeded. (" + (((double)gcount) / (double)tcount) + ").");
    }
  }

  /**
   * Debugging/Testing Utility for accessing the lexical tokenizer.
   */
  public LexicalTokenizer getLexicalTokenizer(String string) throws IOException {
    if (!initialized) initialize();

    final LexicalTokenizer tokenizer = buildLexicalTokenizer(new StringWrapper(string, getBreakStrategy()), lexicon, skipUpTo);

    return tokenizer;
  }
}
