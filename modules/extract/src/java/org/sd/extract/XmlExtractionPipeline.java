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


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An extraction pipeline operating over xml files.
 * <p>
 * @author Spence Koehler
 */
public class XmlExtractionPipeline extends ExtractionPipeline {
  
  private boolean isHtml;
  private Set<String> ignoreTags;

  public XmlExtractionPipeline(boolean isHtml, Set<String> ignoreTags, boolean keepDocTexts, boolean keepEmpties) {
    super(keepDocTexts, keepEmpties);
    this.isHtml = isHtml;
    this.ignoreTags = ignoreTags;
  }

  /**
   * Build a text container for the file.
   */
  protected TextContainer buildTextContainer(String streamId, InputStream inputStream, boolean keepDocTexts, boolean keepEmpties, AtomicBoolean die) throws IOException {
    return new XmlTextContainer(streamId, inputStream, isHtml, ignoreTags, keepDocTexts, keepEmpties);
  }
}
