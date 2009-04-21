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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Wrapper class for running a pipeline.
 * <p>
 * @author Spence Koehler
 */
public abstract class ExtractionRunner {

  /**
   * Build a default extraction runner.
   * <p>
   * Note that the default extraction runner does nothing to close resources.
   */
  public static final ExtractionRunner buildExtractionRunner(boolean extractHeadings, boolean keepDocTexts,
                                                             boolean keepEmpties, Extractor[] extractors) {
    return new ExtractionRunner(ExtractionPipeline.buildDefaultHtmlPipeline(extractHeadings, keepDocTexts, keepEmpties, extractors)) {
        public void close() throws IOException {
          // no-op.
        }
      };
  }


  private ExtractionPipeline megaExtractor;

  public ExtractionRunner(ExtractionPipeline megaExtractor) {
    this.megaExtractor = megaExtractor;
  }
  
  /**
   * Run the extraction pipeline and disambiguate the results over the file.
   */
  public ExtractionResults run(File file, AtomicBoolean die, boolean turnOffCaching) throws IOException {
    return run(file.getAbsolutePath(), FileUtil.getInputStream(file), die, turnOffCaching);
  }

  /**
   * Run the extraction pipeline and disambiguate the results over the xml string.
   */
  public ExtractionResults run(String stringId, String xmlString, AtomicBoolean die, boolean turnOffCaching) throws IOException {
    return run(stringId, new ByteArrayInputStream(xmlString.getBytes()), die, turnOffCaching);
  }

  /**
   * Run the extraction pipeline and disambiguate the results over the input stream.
   */
  public ExtractionResults run(String streamId, InputStream inputStream, AtomicBoolean die, boolean turnOffCaching) throws IOException {
    final TextContainer textContainer = megaExtractor.runExtractors(streamId, inputStream, die, turnOffCaching);
    return run(textContainer);
  }

  /**
   * Run the extraction pipeline and disambiguate the results over the text container.
   */
  private final ExtractionResults run(TextContainer textContainer) {
    final ExtractionResults extractionResults = (textContainer != null) ? textContainer.getExtractionResults(false) : null;

    if (extractionResults != null) {
      extractionResults.disambiguate();
    }

    return extractionResults;
  }


  /**
   * Get this runner's extraction pipeline.
   */
  public ExtractionPipeline getMegaExtractor() {
    return megaExtractor;
  }

  /**
   * Close resources associated with this runner.
   */
  public abstract void close() throws IOException;
}
