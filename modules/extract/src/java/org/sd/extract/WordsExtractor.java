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


import org.sd.nlp.GeneralNormalizer;
import org.sd.nlp.NormalizedString;
import org.sd.nlp.Normalizer;
//import org.sd.rbi.tools.RbiNormalizer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A simple extractor that just grabs words from text.
 * <p>
 * @author Spence Koehler
 */
public class WordsExtractor extends AbstractExtractor {

  private static final WordAcceptor DEFAULT_ACCEPTOR = new StopwordsBasedAcceptor(GeneralNormalizer.getCaseInsensitiveInstance(), null);


  private Extractor pluginExtractor;
  private WordAcceptor wordAcceptor;
  private Integer n;  // extract n words at a time. If null (default), just 1 word at a time.

  public WordsExtractor(String extractionType) {
    this(extractionType, DEFAULT_ACCEPTOR);
  }

  public WordsExtractor(String extractionType, Integer n) {
    this(extractionType, DEFAULT_ACCEPTOR, n);
  }

  public WordsExtractor(String extractionType, WordAcceptor wordAcceptor) {
    super(extractionType, null, null, false, false, null, null, null);

    this.wordAcceptor = wordAcceptor == null ? StopwordsBasedAcceptor.getDefaultInstance() : wordAcceptor;
    this.n = null;
  }

  public WordsExtractor(String extractionType, WordAcceptor wordAcceptor, Integer n) {
    this(extractionType, wordAcceptor);
    this.n = n;
  }

  public WordsExtractor(Extractor pluginExtractor, WordAcceptor wordAcceptor) {
    super(pluginExtractor.getExtractionType(), null, null, false, false, null, null, null);

    this.pluginExtractor = pluginExtractor;
    this.wordAcceptor = wordAcceptor == null ? StopwordsBasedAcceptor.getDefaultInstance() : wordAcceptor;
    this.n = null;
  }

  public WordsExtractor(Extractor pluginExtractor, WordAcceptor wordAcceptor, Integer n) {
    this(pluginExtractor, wordAcceptor);
    this.n = n;
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

    if (pluginExtractor != null) {
      result = applyPluginExtractor(docText, die);
    }
    else {
      result = extractWords(docText, die);
    }

    return result;
  }

  
  private final List<Extraction> applyPluginExtractor(DocText docText, AtomicBoolean die) {
    List<Extraction> result = new ArrayList<Extraction>();

    final List<Extraction> extracted = pluginExtractor.extract(docText, die);
    if (extracted != null) {
      for (Extraction extraction : extracted) {
        final String string = extraction.asString();
        buildExtractions(docText, die, string, result);
      }
    }

    return result.size() == 0 ? null : result;
  }

  private final List<Extraction> extractWords(DocText docText, AtomicBoolean die) {
    List<Extraction> result = new ArrayList<Extraction>();

    // perform the extraction
    final String string = docText.getStringWrapper().string;
    buildExtractions(docText, die, string, result);

    return result.size() == 0 ? null : result;
  }

  private final void buildExtractions(DocText docText, AtomicBoolean die, String string, List<Extraction> result) {
    if (n == null) {
      doBuildExtractions(docText, die, string, result);
    }
    else {
      doBuildExtractions(docText, die, n, string, result);
    }
  }

  private final void doBuildExtractions(DocText docText, AtomicBoolean die, String string, List<Extraction> result) {
    // get each of the words as an extraction.
    final String[] words = wordAcceptor.split(string, false);

    for (String word : words) {
      if (wordAcceptor.accept(word, true, true)) {
        result.add(new Extraction(getExtractionType(), docText, 1.0, new ExtractionStringData(word)));
      }
    }
  }

  private final void doBuildExtractions(DocText docText, AtomicBoolean die, int n, String string, List<Extraction> result) {
    // get each of the words as an extraction.
    final String[] words = wordAcceptor.split(string, false);

    final Set<String> strings = new HashSet<String>();

    for (int i = 0; i < words.length; ++i) {
      final String word = words[i];

      if (wordAcceptor.accept(word, true, true)) {
        // word passes, create extractions for each position of this word in the N words
        final int start = Math.max(i - (n - 1), 0);
        for (int j = start; j <= i && j + n <= words.length; ++j) {
          final StringBuilder curWords = new StringBuilder();
          for (int k = 0; k < n; ++k) {
            curWords.append(words[k + j]);
            if (k + 1 < n) curWords.append('_');
          }
          strings.add(curWords.toString());
        }
      }

      for (String curString : strings) {
        result.add(new Extraction(getExtractionType(), docText, 1.0, new ExtractionStringData(curString)));
      }
    }
  }
}
