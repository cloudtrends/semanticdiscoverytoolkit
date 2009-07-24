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
import org.sd.nlp.Normalizer;
import org.sd.nlp.StringWrapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An extractor to recognize literal (normalized) strings.
 * <p>
 * @author Spence Koehler
 */
public abstract class LiteralExtractor extends AbstractExtractor {
  
  /**
   * Add mapping(s) for a line from the literalFile. Extractions will
   * hold a StringExtractionData whose string is the mapping value for
   * literal keys that match.
   * <p>
   * Implementers should call addMapping(key, value).
   */
  protected abstract void addMapping(String line);


  private boolean haltPipelineWhenMatch;
  private Map<String, String> mappings;

  public LiteralExtractor(String extractionType, File literalFile, boolean haltPipelineWhenMatch) throws IOException {
    this(extractionType, literalFile, null, null, false, false, null, null, null, haltPipelineWhenMatch);
  }

  public LiteralExtractor(String extractionType, File literalFile,
                          TextAcceptor textAcceptor, TextSplitter textSplitter,
                          boolean needsDocTextCache, boolean stopAtFirst,
                          Normalizer normalizer, BreakStrategy breakStrategy,
                          Disambiguator disambiguator, boolean haltPipelineWhenMatch) throws IOException {
    super(extractionType, textAcceptor, textSplitter, needsDocTextCache, stopAtFirst, normalizer, breakStrategy, disambiguator);

    this.haltPipelineWhenMatch = haltPipelineWhenMatch;
    this.mappings = new HashMap<String, String>();
    loadLiterals(literalFile);
  }

  private final void loadLiterals(File literalFile) throws IOException {
    if (literalFile != null && literalFile.exists()) {
      final BufferedReader reader = FileUtil.getReader(literalFile);

      String line = null;
      while ((line = reader.readLine()) != null) {
        addMapping(line);
      }
      
      reader.close();
    }
  }

  /**
   * Get the value for the given key.
   * <p>
   * NOTE: The normalizer will be applied to the key.
   */
  protected final String getMapping(String key) {
    final Normalizer normalizer = getNormalizer();
    if (normalizer != null) {
      key = normalizer.normalize(key).getNormalized();
    }
    return mappings.get(key);
  }

  /**
   * Set the value for the given key.
   * <p>
   * NOTE: The normalizer will be applied to the key.
   */
  protected final void addMapping(String key, String value) {
    final Normalizer normalizer = getNormalizer();
    if (normalizer != null) {
      key = normalizer.normalize(key).getNormalized();
    }
    mappings.put(key, value);
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
    final Normalizer normalizer = getNormalizer();

    // perform the extraction
    final StringWrapper[] textToParse = extractTextStrings(docText);
    if (textToParse != null) {
      for (StringWrapper stringWrapper : textToParse) {
        final String string = normalizer != null ? stringWrapper.getNormalizedString(0, stringWrapper.length(), normalizer) : stringWrapper.string;

        final String value = mappings.get(string);
        if (value != null) {
          if (result == null) result = new ArrayList<Extraction>();
          result.add(new Extraction(getExtractionType(), docText, 1.0, new ExtractionStringData(value)));
        }
      }
    }

    if (result != null && haltPipelineWhenMatch) {
      setIsFinishedWithDocText(docText);
    }

    return result;
  }
}
