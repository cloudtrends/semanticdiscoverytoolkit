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
package org.sd.text.textcat;


import org.sd.io.FileUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Port of TextCat implementation from perl to java.
 * <p>
 * Original perl version created by:
 * <p>
 * © Gertjan van Noord, 1997.
 * mailto:vannoord@let.rug.nl
 * http://odur.let.rug.nl/~vannoord/TextCat/index.html
 * <p>
 * @author Spence Koehler
 */
public class TextCat {

  private static final String LANGUAGE_MODULE_RESOURCE_DIR = "resources";
  private static final String LANGUAGE_RULES_FILE = "resources/rules.txt";

  private int opt_a;    // limit on number of languages to return (default 10)
  private double opt_u; // how much worse a result must be to not be mentioned as an alternative (usually 1.05 or 1.1)
  private int opt_f;    // lower ngram frequency limit to improve sorting speed/memory consumption (usually 0 for short texts)
  private int opt_t;    // number of sorted ngrams to retain (default 400)

  private Map<String, Map<String, NGram>> language2ngrams;
  private Set<Map.Entry<String, Map<String, NGram>>> lnEntries;
  private Set<String> languages;

  /**
   * Default constructor.
   */
  public TextCat() throws IOException {
    this(null);
  }

  /**
   * Construct a classifier for the given languages.
   */
  public TextCat(String[] languages) throws IOException {
    this.opt_a = 10;
    this.opt_u = 1.05;
    this.opt_f = 0;
    this.opt_t = 400;

    LanguageRules rules = null;

    if (languages == null) {
      rules = new LanguageRules(FileUtil.getResourceFile(this.getClass(), LANGUAGE_RULES_FILE));
    }

    this.language2ngrams = new TreeMap<String, Map<String, NGram>>();
    loadLanguageModules(language2ngrams, FileUtil.getResourceFile(this.getClass(), LANGUAGE_MODULE_RESOURCE_DIR), languages, rules);
    this.lnEntries = language2ngrams.entrySet();
    this.languages = buildLanguages(language2ngrams);
  }

  public Set<String> languages() {
    return languages;
  }

  public boolean hasLanguage(String language) {
    final String[] pieces = splitLanguage(language);
    return languages.contains(pieces[0]);
  }

  /**
   * Classify the language of the given text.
   * <p>
   * Build a list of possibilities with the most likely languages first.
   */
  public List<ClassificationResult> classify(String text) {
    final List<ClassificationResult> result = new ArrayList<ClassificationResult>();
    final List<NGram> unknown = createLanguageModule(text);

    for (Map.Entry<String, Map<String, NGram>> lnEntry : lnEntries) {
      final String language = lnEntry.getKey();
      final Map<String, NGram> ngrams = lnEntry.getValue();

      int i = 0;
      long p = 0;

      for (NGram textNGram : unknown) {
        final NGram ngram = ngrams.get(textNGram.chars);
        if (ngram != null) {
          p += Math.abs(ngram.rank - i);
        }
        else {
          p += opt_t;  // maxp
        }
        ++i;
      }

      result.add(new ClassificationResult(language, p));
    }

    Collections.sort(result);

    // keep just those results that are close to each other
    boolean didFirst = false;
    long firstP = -1;
    int index = 0;
    for (ClassificationResult cr : result) {
      ++index;
      if (!didFirst) {
        firstP = cr.p;
        didFirst = true;
      }
      else {
        if (cr.p < (opt_u * firstP)) break;
      }
    }

    return result.subList(0, index);
  }

  public static boolean hasLanguage(List<ClassificationResult> results, String language) {
    return hasLanguage(results, language, results.size());
  }
  /**
   * Determine whether the given language is a possibility in the results.
   */
  public static boolean hasLanguage(List<ClassificationResult> results, String language, int limit) {
    final String[] pieces = splitLanguage(language);
    for (int i=0; i < limit && i < results.size(); i++) {
      ClassificationResult result = results.get(i);
      if (pieces[0].equals(result.language)) return true;
    }
    return false;
  }

  /**
   * Determine whether any of the given languages is a possibility in the results.
   */
  public static boolean hasLanguage(List<ClassificationResult> results, Set<String> languages) {
    for (ClassificationResult result : results) {
      if (languages.contains(result.language)) return true;
    }
    return false;
  }

  /**
   * Determine whether the top of the given languages is a possibility in the results.
   */
  public static boolean hasLanguageAsTop(List<ClassificationResult> results, Set<String> languages) {
    return languages.contains(results.get(0).language);
  }


  /**
   * Convert the results to a string.
   */
  public static String asString(List<ClassificationResult> languages) {
    final StringBuilder result = new StringBuilder();

    for (ClassificationResult language : languages) {
      if (result.length() > 0) result.append(',');
      result.append(language.language);
    }

    return result.toString();
  }

  /**
   * Load readable resources ending in .lm from the given directory.
   * <p>
   * Note that modules can be safely disabled by removing read permissions.
   */
  private final void loadLanguageModules(Map<String, Map<String, NGram>> language2ngrams, File dir, String[] languages, final LanguageRules rules) throws IOException {

    final Set<String> languageFiles = (languages == null) ? null : new HashSet<String>();
    if (languages != null) for (String language : languages) languageFiles.add(language.toLowerCase() + ".lm");

    final File[] modules = dir.listFiles(new FileFilter() {
        public final boolean accept(File pathname) {
          boolean result = false;

          final String name = pathname.getName().toLowerCase();
          if (name.endsWith(".lm")) {
            result = (languageFiles != null) ? languageFiles.contains(name) : rules != null ? (rules.include(name) && !rules.exclude(name)) : true;
          }

          return result;
        }
      });

    for (File module : modules) {
      if (module.canRead()) {
        loadLanguageModule(language2ngrams, module);
      }
    }
  }

  private final void loadLanguageModule(Map<String, Map<String, NGram>> language2ngrams, File module) throws IOException {
    final String language = getLanguage(module);
    final Map<String, NGram> ngrams = new HashMap<String, NGram>();
    language2ngrams.put(language, ngrams);

    // use lines starting with an appropriate character. others are ignored.
    final BufferedReader reader = FileUtil.getReader(module);

    int rank = 1;
    String line = null;
    while ((line = reader.readLine()) != null) {
      if (line.length() > 0) {
        final char firstChar = line.charAt(0);

        // accept if doesn't start with non_word_characters, which are "0-9\s"
        if (!((firstChar >= '0' && firstChar <= '9') || Character.isWhitespace(firstChar))) {
          final String[] pieces = line.split("\\s+");
          ngrams.put(pieces[0], new NGram(pieces[0], rank++));
        }
      }
    }

    reader.close();
  }

  protected final String getLanguage(File module) {
    final String filename = module.getName();

    // strip of suffix (.lm)
    final int dotPos = filename.lastIndexOf('.');
    return filename.substring(0, dotPos);
  }

  private final List<NGram> createLanguageModule(String text) {
    final Map<String, NGram> chars2ngram = new HashMap<String, NGram>();

    // add ngrams
    addNGrams(chars2ngram, text);

    // sort and keep top ngrams
    return processNGrams(chars2ngram, opt_t);
  }

  static final List<NGram> processNGrams(Map<String, NGram> chars2ngram, int opt_t) {
    final List<NGram> ngrams = new ArrayList<NGram>(chars2ngram.values());
    Collections.sort(ngrams);

    // just keep the top 'opt_t' ngrams
    final List<NGram> result = (ngrams.size() > opt_t) ? ngrams.subList(0, opt_t) : ngrams;

    return result;
  }

  static final void addNGrams(Map<String, NGram> chars2ngram, String text) {
    final String[] words = text.split("[0-9\\s]+");
    for (String word : words) {
      word = "_" + word + "_";
      int len = word.length();
      final int flen = len;
      for (int i = 0; i < flen; ++i) {
        if (len > 4) addNGram(chars2ngram, word, i, 5);
        if (len > 3) addNGram(chars2ngram, word, i, 4);
        if (len > 2) addNGram(chars2ngram, word, i, 3);
        if (len > 1) addNGram(chars2ngram, word, i, 2);
        addNGram(chars2ngram, word, i, 1);
        --len;
      }
    }
  }

  private static final void addNGram(Map<String, NGram> chars2ngram, String word, int startIndex, int length) {
    final String substr = word.substring(startIndex, startIndex + length);
    NGram ngram = chars2ngram.get(substr);
    if (ngram == null) {
      ngram = new NGram(substr);
      chars2ngram.put(substr, ngram);
    }
    else {
      ngram.incCount();
    }
  }

  private final Set<String> buildLanguages(Map<String, Map<String, NGram>> language2ngrams) {
    final Set<String> result = new LinkedHashSet<String>();

    final Set<String> languages = language2ngrams.keySet();
    for (String language : languages) {
      final String[] pieces = splitLanguage(language);
      result.add(pieces[0]);
    }

    return result;
  }

  private static final String[] splitLanguage(String le) {
    final String[] result = new String[2];

    final int dashPos = le.indexOf("-");
    if (dashPos >= 0) {
      result[0] = le.substring(0, dashPos);
      result[1] = le.substring(dashPos + 1);
    }
    else {
      result[0] = le;
      result[1] = "utf8";
    }

    return result;
  }

  static final class NGram implements Comparable<NGram> {
    public final String chars;
    public final int rank;
    private int count;

    public NGram(String chars, int rank) {
      this.chars = chars;
      this.rank = rank;
    }

    public NGram(String chars) {
      this.chars = chars;
      this.rank = 0;
      this.count = 1;
    }

    public void incCount() {
      ++count;
    }

    public int compareTo(NGram other) {
      // primary sort on frequency (highest to lowest); secondary sort on chars (increasing alphabetical)
      int result = (other.count - count);
      if (result == 0) {
        result = chars.compareTo(other.chars);
      }
      return result;
    }

    public String toString() {
      final StringBuilder result = new StringBuilder();

      result.append(chars).append("\t ").append(count);

      return result.toString();
    }
  }

  public static final class ClassificationResult implements Comparable<ClassificationResult> {
    public final String language;
    public final String encoding;
    public final long p;

    public ClassificationResult(String language, long p) {
      final String[] pieces = splitLanguage(language);
      this.language = pieces[0];
      this.encoding = pieces[1];
      this.p = p;
    }

    public boolean matches(String language) {
      final String[] pieces = splitLanguage(language);
      return this.language.equals(pieces[0]);
    }

    /**
     * Compare to another result such that a result with a higher confidence
     * (lower p) is less than another with a lower confidence (higher p).
     */
    public int compareTo(ClassificationResult other) {
      return (p < other.p) ? -1 : (p > other.p) ? 1 : 0;
    }

    public String toString() {
      final StringBuilder result = new StringBuilder();

      result.append(language).append('(').append(p).append(')');

      return result.toString();
    }
  }
}
