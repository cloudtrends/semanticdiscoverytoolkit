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


import java.io.IOException;

/**
 * A hyperlink extractor.
 * <p>
 * @author Spence Koehler
 */
public class LinkExtractor extends TagExtractor {
  
  private static final String[] LINK_TAGS = new String[] {
    "a", "link", "img", "area",
  };

  public LinkExtractor(String extractionType, TagNodeAcceptor linkAcceptor) {
    this(extractionType, linkAcceptor, false, false, null);
  }

  public LinkExtractor(String extractionType, TagNodeAcceptor linkAcceptor,
                       boolean needsCache, boolean stopAtFirst,
                       Disambiguator disambiguator) {
    super(extractionType, LINK_TAGS, linkAcceptor, needsCache, stopAtFirst, disambiguator);
  }


  public static final void main(String[] args) throws IOException {
    // arg0: html file
    final ExtractionPipeline megaExtractor =
      ExtractionPipeline.buildDefaultHtmlPipeline(
        true, false, true,
        new Extractor[] {
          new LinkExtractor("linkExtractor",
                            new DomainLinkAcceptor())
        });

    megaExtractor.mainRunner(args, "linkExtractor");
  }
}
