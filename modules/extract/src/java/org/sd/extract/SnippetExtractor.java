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


//import org.sd.cluster.io.FileResponse;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Class for extracting snippets from a document.
 * <p>
 * @author Spence Koehler
 */
public abstract class SnippetExtractor {

  /**
   * Get the extraction group from the results from which snippets are to be
   * extracted.
   */
  protected abstract ExtractionGroup getExtractionGroup(ExtractionResults results);


  private ExtractionRunner runner;

  protected SnippetExtractor(Extractor[] extractors) {
    this.runner = ExtractionRunner.buildExtractionRunner(true, true, true, extractors);
  }

  public List<ExtractionSnippet> getSnippets(String stringId, String xmlString) throws IOException {
    final ExtractionResults results = runner.run(stringId, xmlString, null, false);
    return getSnippets(results);
  }

  public List<ExtractionSnippet> getSnippets(File htmlFile) throws IOException {
    final ExtractionResults results = runner.run(htmlFile, null, false);
    return getSnippets(results);
  }

//   public List<ExtractionSnippet> getSnippets(FileResponse fileResponse) throws IOException {
//     List<ExtractionSnippet> result = null;
//     if (fileResponse.hasFile()) {
//       final ExtractionResults results = runner.run(fileResponse.getFilename(), fileResponse.getInputStream(), null, false);
//       result = getSnippets(results);
//     }
//     return result;
//   }

  private final List<ExtractionSnippet> getSnippets(ExtractionResults results) {
    List<ExtractionSnippet> result = null;

    if (results != null) {
      final ExtractionGroup group = getExtractionGroup(results);

      if (group != null) {
        result = group.getSnippets(results);
      }
    }

    return result;
  }

}
