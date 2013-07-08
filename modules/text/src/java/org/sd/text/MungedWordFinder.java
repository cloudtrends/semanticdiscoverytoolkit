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
package org.sd.text;

import org.sd.io.FileUtil;
import org.sd.nlp.AbstractLexicon;
import org.sd.nlp.AbstractNormalizer;
import org.sd.nlp.Break;
import org.sd.nlp.BreakStrategy;
import org.sd.nlp.Categories;
import org.sd.nlp.Category;
import org.sd.nlp.CategoryFactory;
import org.sd.nlp.GeneralNormalizer;
import org.sd.nlp.GenericLexicon;
import org.sd.nlp.Lexicon;
import org.sd.nlp.LexiconPipeline;
import org.sd.nlp.TokenizationStrategy;
import org.sd.nlp.TokenizerWrapper;
import org.sd.nlp.StringWrapper;
import org.sd.util.PropertiesParser;
import org.sd.util.tree.Tree;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Utility to find meaningful words in strings.
 * <p>
 * @author Spence Koehler
 */
public class MungedWordFinder {

  private static final String UNDETERMINED_LETTER = "X";
  private static final String UNDETERMINED_WORD = "U";
  private static final String WORD = "W";
  private static final String NUMBER = "N";
  private static final String SEQUENCE = "S";
  private static final String CONSTITUENT = "C";

  private static final String[] CATEGORIES = new String[] {
    UNDETERMINED_LETTER,
    UNDETERMINED_WORD,
    WORD,
    NUMBER,
    SEQUENCE,
    CONSTITUENT,
  };

  private static final AbstractNormalizer NORMALIZER = GeneralNormalizer.getCaseInsensitiveInstance();
  private static final BreakStrategy BREAK_STRATEGY = new MyBreakStrategy();
  private static final Comparator<WordSequence> SEQUENCE_COMPARATOR = new Comparator<WordSequence>() {
    public int compare(WordSequence ws1, WordSequence ws2) {
      return ws1.size() - ws2.size();
    }
    public boolean equals(Object o) {
      return this == o;
    }
  };


  private CategoryFactory categoryFactory;
  private Map<String, File> wordFiles;
  private Map<String, String[]> wordSets;
  private List<Lexicon> lexicons;

  private TokenizerWrapper _tokenizerWrapper;
  
  /**
   * Construct without any dictionaries.
   * <p>
   * Add dictionaries to use for finding munged words using the
   * addWordFile, addWordSet, and addLexicon methods.
   */
  public MungedWordFinder() {
    this.categoryFactory = new CategoryFactory(CATEGORIES);
    this.wordFiles = new HashMap<String, File>();
    this.wordSets = new HashMap<String, String[]>();
    this.lexicons = new ArrayList<Lexicon>();
    this._tokenizerWrapper = null;
  }

  /**
   * Add a word file defining words to recognize.
   */
  public final void addWordFile(String name, File wordFile) {
    this.wordFiles.put(name, wordFile);
    this._tokenizerWrapper = null;  // force rebuild.
  }

  /**
   * Add a word set defining words to recognize.
   */
  public final void addWordSet(String name, String[] words) {
    this.wordSets.put(name, words);
    this._tokenizerWrapper = null;  // force rebuild.
  }

  /**
   * Add a lexicon defining words to recognize.
   */
  public final void addLexicon(Lexicon lexicon) {
    this.lexicons.add(lexicon);
    this._tokenizerWrapper = null;  // force rebuild.
  }

  protected final TokenizerWrapper getTokenizerWrapper() {
    if (_tokenizerWrapper == null) {
      try {
        final Lexicon lexicon = new LexiconPipeline(buildLexicons());
    
        _tokenizerWrapper = new TokenizerWrapper(
          lexicon, categoryFactory,
          TokenizationStrategy.Type.LONGEST_TO_SHORTEST,
          NORMALIZER, BREAK_STRATEGY,
          false, 0, false);  //NOTE: lexicon should classify every token
      }
      catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
    return _tokenizerWrapper;
  }

  private final Lexicon[] buildLexicons() throws IOException {
    final List<Lexicon> result = new ArrayList<Lexicon>();

    final Category wordCategory = categoryFactory.getCategory(WORD);

    // add a lexicon that identifies/recognizes number sequences.
    result.add(new NumberLexicon(categoryFactory.getCategory(NUMBER)));
      
    // add a lexicon for each wordSet
    for (Map.Entry<String, String[]> wordSetEntry : wordSets.entrySet()) {
      final String name = wordSetEntry.getKey();
      final String[] terms = wordSetEntry.getValue();
      final GenericLexicon genericLexicon =
        new GenericLexicon(terms, NORMALIZER, wordCategory, false, true, false,
                           "name=" + name);
      genericLexicon.setMaxNumWords(0);  // disable "word" limit. each char counts as a word with our break strategy.
      result.add(genericLexicon);
    }

    // add a lexicon for each wordFile
    for (Map.Entry<String, File> wordFileEntry : wordFiles.entrySet()) {
      final String name = wordFileEntry.getKey();
      final File wordFile = wordFileEntry.getValue();
      final GenericLexicon genericLexicon =
        new GenericLexicon(FileUtil.getInputStream(wordFile),
                           NORMALIZER, wordCategory, false, true, false,
                           "name=" + name);
      genericLexicon.setMaxNumWords(0);  // disable "word" limit. each char counts as a word with our break strategy.
      result.add(genericLexicon);
    }

    // add each lexicon
    for (Lexicon lexicon : lexicons) {
      result.add(lexicon);
    }

    // add a lexicon that identifies a single letter as unknown.
    result.add(new UnknownLexicon(categoryFactory.getCategory(UNDETERMINED_LETTER)));

    return result.toArray(new Lexicon[result.size()]);

//todo: what about conjugations?
  }


  public List<WordSequence> getBestSplits(String string) {
    List<WordSequence> result = new ArrayList<WordSequence>();

    final TokenizerWrapper tokenizerWrapper = getTokenizerWrapper();
    final Tree<StringWrapper.SubString> tokenTree = tokenizerWrapper.tokenize(string);
    final List<Tree<StringWrapper.SubString>> leaves = tokenTree.gatherLeaves();

    int maxScore = -1;
    for (Tree<StringWrapper.SubString> leaf : leaves) {
      final WordSequence wordSequence = new WordSequence(leaf);
      final int curScore = wordSequence.getScore();
      if (curScore >= maxScore) {
        if (curScore > maxScore) {
          result.clear();
          maxScore = curScore;
        }
        result.add(wordSequence);
      }
    }

    if (result.size() > 0) {
      Collections.sort(result, SEQUENCE_COMPARATOR);
    }

    return result;
  }

  public List<WordSequence> getBestDomainSplits(String domain) {
    final int dotPos = domain.indexOf('.');
    if (dotPos > 0 && dotPos < domain.length() - 1) {
      final DetailedUrl dUrl = new DetailedUrl(domain);
      domain = dUrl.getHost(false, false, false);
    }
    return getBestSplits(domain);
  }

  public String getSplitsAsString(List<WordSequence> splits) {
    final StringBuilder result = new StringBuilder();

    if (splits != null) {
      for (Iterator<WordSequence> splitIter = splits.iterator(); splitIter.hasNext(); ) {
        final WordSequence split = splitIter.next();
        result.append(split.toString());
        if (splitIter.hasNext()) result.append('|');
      }
    }

    return result.toString();
  }

  // symbols are hard breaks... letters are soft breaks... consecutive digits aren't (don't break numbers)
  private static final class MyBreakStrategy implements BreakStrategy {

    public Break[] computeBreaks(int[] codePoints) {
      final Break[] result = new Break[codePoints.length];

      boolean inNumber = false;
      for (int i = 0; i < codePoints.length; ++i) {
        Break curBreak = Break.NONE;
        final int cp = codePoints[i];
        if (Character.isDigit(cp)) {
          if (i > 0 && !inNumber && result[i - 1] != Break.HARD) {
            curBreak = Break.SOFT_SPLIT;
          }
          else {
            curBreak = Break.NONE;
          }
          inNumber = true;
        }
        else if (Character.isLetter(cp)) {
          // first letter after a hard break (or at start) isn't a break
          if (i == 0 || result[i - 1] == Break.HARD) {
            curBreak = Break.NONE;
          }
          else {
            curBreak = Break.SOFT_SPLIT;
          }
          inNumber = false;
        }
        else {
          // if following a digit or with-digit-symbol, then consider the number parts as a whole
          if (inNumber) {
            curBreak = Break.NONE;
          }
          // else if a digit or digit-symbol follows, then consider the number parts as a whole
          else if (i + 1 < codePoints.length &&
                   (Character.isDigit(codePoints[i + 1]) ||
                    (i + 2 < codePoints.length && Character.isDigit(codePoints[i + 2])))) {
            curBreak = Break.NONE;
            inNumber = true;
          }
          else {
            curBreak = Break.HARD;
            inNumber = false;
          }
        }
        result[i] = curBreak;
      }

      return result;
    }
  }

  private static final class NumberLexicon extends AbstractLexicon {
    
    private Category category;

    public NumberLexicon(Category category) {
      super(null);

      this.category = category;
    }

    /**
     * Define applicable categories in the subString.
     * <p>
     * In this case, if a digit is found in the subString, then the NUMBER category is added.
     *
     * @param subString   The substring to define.
     * @param normalizer  The normalizer to use.
     */
    protected void define(StringWrapper.SubString subString, AbstractNormalizer normalizer) {
      if (!subString.hasDefinitiveDefinition()) {
        boolean isNumber = true;
        int lastNumberPos = -1;
        for (int i = subString.startPos; i < subString.endPos; ++i) {
          final int cp = subString.stringWrapper.getCodePoint(i);
          if (cp > '9' || cp < '0') {
            isNumber = false;
            break;
          }
          lastNumberPos = i;
        }

        if (!isNumber) {
          // check for "text" numbers
          if (lastNumberPos >= 0 && lastNumberPos == subString.endPos - 3) {
            // check for number suffix
            isNumber = TextNumber.isNumberEnding(subString.originalSubString.substring(lastNumberPos + 1).toLowerCase());
          }
          else if (lastNumberPos < 0) {
            // check for number word
            isNumber = TextNumber.isNumber(subString.originalSubString.toLowerCase());
          }
        }
        
        if (isNumber) {
          subString.setAttribute("name", NUMBER);
          subString.addCategory(category);
          subString.setDefinitive(true);
        }
      }
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
  }

  private static final class UnknownLexicon extends AbstractLexicon {
    
    private Category category;

    public UnknownLexicon(Category category) {
      super(null);

      this.category = category;
    }

    /**
     * Define applicable categories in the subString.
     * <p>
     * In this case, if a digit is found in the subString, then the NUMBER category is added.
     *
     * @param subString   The substring to define.
     * @param normalizer  The normalizer to use.
     */
    protected void define(StringWrapper.SubString subString, AbstractNormalizer normalizer) {
      if (!subString.hasDefinitiveDefinition() && subString.length() == 1) {
        subString.addCategory(category);
        subString.setAttribute("name", UNDETERMINED_WORD);
      }
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
  }


  /**
   * Container class for a word.
   */
  public static final class Word {
    public final StringWrapper.SubString word;
    public final String type;

    private Integer _score;

    public Word(StringWrapper.SubString word, String type) {
      this.word = word;
      this.type = type;

      this._score = null;
    }

    /**
     * Get this word's score.
     * <p>
     * A word's score is its number of defined letters.
     */
    public int getScore() {
      if (_score == null) {
        _score = UNDETERMINED_WORD.equals(type) ? 0 : word.length() * word.length();
      }
      return _score;
    }

    public String toString() {
      final StringBuilder result = new StringBuilder();
      result.append(type).append('=').append(word.originalSubString);
      return result.toString();
    }
  }

  /**
   * Container for a sequence of words for a string.
   */
  public static final class WordSequence {
    public final String string;
    public final List<Word> words;

    private int score;

    public WordSequence(Tree<StringWrapper.SubString> leaf) {
      final LinkedList<Word> theWords = new LinkedList<Word>();
      this.words = theWords;
      this.score = 0;
      String theString = null;

      while (leaf != null) {
        final StringWrapper.SubString subString = leaf.getData();
        if (subString == null) break;  // hit root.
        final String nameValue = subString.getAttribute("name");
        final Word word = new Word(subString, nameValue == null ? UNDETERMINED_WORD : nameValue);
        theWords.addFirst(word);
        score += word.getScore();
        if (theString == null) theString = subString.stringWrapper.string;
        leaf = leaf.getParent();
      }

      this.string = theString;
    }

    /**
     * Get this sequence's score.
     * <p>
     * The score is the total number of defined letters in the sequence's words.
     */
    public int getScore() {
      return score;
    }

    public int size() {
      return words.size();
    }

    public String toString() {
      return asString(",");
    }

    public String asString(String delim) {
      final StringBuilder result = new StringBuilder();

      for (Iterator<Word> wordIter = words.iterator(); wordIter.hasNext(); ) {
        final Word word = wordIter.next();
        result.append(word.toString());
        if (wordIter.hasNext()) result.append(delim);
      }
//      result.append('(').append(getScore()).append(')');

      return result.toString();
    }
  }


  //java -Xmx640m org.sd.nlp.MungedWordFinder wordSet="foo,bar,baz" wordSetName="fbb" foobarbaz "foo-bar-baz" "foobArbaz" gesundheit
  public static final void main(String[] args) throws IOException {
    // properties:
    //    wordFiles="file1,file2,..."
    //    wordSet="word1,word2,..."
    //    wordSetName="name"
    // non-property arguments are munged strings to split.
    // STDIN has other munged strings or domains to split.
    final PropertiesParser propertiesParser = new PropertiesParser(args);
    final Properties properties = propertiesParser.getProperties();

    final String wordFileNames = properties.getProperty("wordFiles");
    final String wordSet = properties.getProperty("wordSet");
    final String wordSetName = properties.getProperty("wordSetName");

    final MungedWordFinder mungedWordFinder = new MungedWordFinder();
    if (wordFileNames != null) {
      for (String wordFile : wordFileNames.split("\\s*,\\s*")) {
        final File file = new File(wordFile);
        mungedWordFinder.addWordFile(file.getName().split("\\.")[0], file);
      }
    }
    if (wordSet != null) {
      mungedWordFinder.addWordSet(wordSetName, wordSet.split("\\s*,\\s*"));
    }

    final String[] mungedWords = propertiesParser.getArgs();
    for (String mungedWord : mungedWords) {
      final List<WordSequence> splits = mungedWordFinder.getBestDomainSplits(mungedWord);
      final String splitsString = mungedWordFinder.getSplitsAsString(splits);
      System.out.println(mungedWord + "|" + splitsString);
    }

    final BufferedReader reader = FileUtil.getReader(System.in);
    String line = null;
    while ((line = reader.readLine()) != null) {
      final List<WordSequence> splits = mungedWordFinder.getBestDomainSplits(line);
      final String splitsString = mungedWordFinder.getSplitsAsString(splits);
      System.out.println(line + "|" + splitsString);
    }
    reader.close();
  }
}
