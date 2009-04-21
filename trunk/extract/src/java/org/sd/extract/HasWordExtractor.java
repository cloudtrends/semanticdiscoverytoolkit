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


import org.sd.io.FileUtil;
import org.sd.nlp.BreakStrategy;
import org.sd.nlp.NormalizedString;
import org.sd.nlp.Normalizer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An extractor to recognize text nodes that contain one of a set of
 * (already normalized) words.
 * <p>
 * @author Spence Koehler
 */
public class HasWordExtractor extends AbstractExtractor {
  
  private boolean haltPipelineWhenMatch;
  private Set<String> words;

  private int maxStringLength;
  private int maxNumWords;

  public HasWordExtractor(String extractionType, File hasWordFile, boolean haltPipelineWhenMatch, Normalizer normalizer) throws IOException {
    this(extractionType, hasWordFile, null, null, false, false, normalizer, null, null, haltPipelineWhenMatch);
  }

  public HasWordExtractor(String extractionType, Set<String> words, boolean haltPipelineWhenMatch, Normalizer normalizer) {
    this(extractionType, words, null, null, false, false, normalizer, null, null, haltPipelineWhenMatch);
  }

  public HasWordExtractor(String extractionType, File hasWordFile,
                          TextAcceptor textAcceptor, TextSplitter textSplitter,
                          boolean needsDocTextCache, boolean stopAtFirst,
                          Normalizer normalizer, BreakStrategy breakStrategy,
                          Disambiguator disambiguator, boolean haltPipelineWhenMatch) throws IOException {
    this(extractionType, textAcceptor, textSplitter, needsDocTextCache, stopAtFirst,
         normalizer, breakStrategy, disambiguator, haltPipelineWhenMatch);

    init(hasWordFile);
  }

  public HasWordExtractor(String extractionType, Set<String> words,
                          TextAcceptor textAcceptor, TextSplitter textSplitter,
                          boolean needsDocTextCache, boolean stopAtFirst,
                          Normalizer normalizer, BreakStrategy breakStrategy,
                          Disambiguator disambiguator, boolean haltPipelineWhenMatch) {
    this(extractionType, textAcceptor, textSplitter, needsDocTextCache, stopAtFirst,
         normalizer, breakStrategy, disambiguator, haltPipelineWhenMatch);

    init(words);
  }

  private HasWordExtractor(String extractionType,
                           TextAcceptor textAcceptor, TextSplitter textSplitter,
                           boolean needsDocTextCache, boolean stopAtFirst,
                           Normalizer normalizer, BreakStrategy breakStrategy,
                           Disambiguator disambiguator, boolean haltPipelineWhenMatch) {
    super(extractionType, textAcceptor, textSplitter, needsDocTextCache, stopAtFirst, normalizer, breakStrategy, disambiguator);

    this.haltPipelineWhenMatch = haltPipelineWhenMatch;
    this.maxStringLength = -1;
    this.maxNumWords = -1;
  }

  /**
   * Initialize with words from the given file, one word per line.
   * Ignore blanks and lines starting with '#'.
   * <p>
   * Assume the words are already properly normalized.
   */
  private final void init(File hasWordFile) throws IOException {
    this.words = new HashSet<String>();
    FileUtil.readStrings(this.words, hasWordFile, null, null, "#", true, false);
  }

  /**
   * Initialize with the given set of words.
   * <p>
   * Assume the words are already properly normalized.
   */
  private final void init(Set<String> words) {
    this.words = words;
  }

  public void setMaxStringLength(int maxStringLength) {
    this.maxStringLength = maxStringLength;
  }

  public void setMaxNumWords(int maxNumWords) {
    this.maxNumWords = maxNumWords;
  }

  /**
   * Perform the extraction on the doc text.
   * <p>
   * If the normalized text has one of this extractor's (full) words, then the
   * Extraction will contain the first word found as an ExtractionStringData.
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
    final Normalizer normalizer = getNormalizer();

    // perform the extraction
    final String string = docText.getString();
    if (string != null && !"".equals(string)) {
      if (maxStringLength > 0 && string.length() > maxStringLength) return result;

      final NormalizedString nString = normalizer.normalize(string);

      if (preQualify(nString)) {
        int numTokens = 0;
        for (NormalizedString.Token token = nString.getToken(0, true); token != null; token = token.getNext(true)) {
          final String normalizedWord = token.getNormalized();
          if (words.contains(normalizedWord)) {
            // found one!
            result = new ArrayList<Extraction>();
            result.add(new Extraction(getExtractionType(), docText, 1.0, new ExtractionStringData(normalizedWord)));
            break;
          }
          if (die != null && die.get()) break;
          ++numTokens;

          if (maxNumWords > 0 && numTokens >= maxNumWords) break;
        }
      }
    }

    if (result != null && haltPipelineWhenMatch) {
      setIsFinishedWithDocText(docText);
    }

    return result;
  }

  /**
   * Hook to prequalify a normalized string for searching for a word.
   * <p>
   * Extenders may override. Default behavior prequalifies all normalized
   * strings.
   */
  protected boolean preQualify(NormalizedString nString) {
    return true;
  }
}
