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


import org.sd.nlp.AbstractNormalizer;
import org.sd.nlp.BreakStrategy;
import org.sd.nlp.StringWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A simple regex extractor.
 * <p>
 * @author Spence Koehler
 */
public class RegexExtractor extends AbstractExtractor {
  
  private Pattern pattern;

  /**
   * Construct a default instance.
   */
  public RegexExtractor(String extractionType, String pattern) {
    this(extractionType, pattern, null, null, false, false, null, null, null);
  }

  /**
   * Construct an instance with the given parameters.
   */
  public RegexExtractor(String extractionType, String pattern,
                        TextAcceptor textAcceptor, TextSplitter textSplitter,
                        boolean needsDocTextCache, boolean stopAtFirst,
                        AbstractNormalizer normalizer, BreakStrategy breakStrategy,
                        Disambiguator disambiguator) {
    super(extractionType, textAcceptor, textSplitter, needsDocTextCache, stopAtFirst, normalizer, breakStrategy, disambiguator);

    this.pattern = Pattern.compile(pattern);
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
        final Matcher matcher = pattern.matcher(stringWrapper.string);
        if (matcher.matches()) {
          if (result == null) result = new ArrayList<Extraction>();
          result.add(new Extraction(getExtractionType(), docText, 1.0, new ExtractionMatcherData(matcher)));
        }
      }
    }

    return result;
  }
}
