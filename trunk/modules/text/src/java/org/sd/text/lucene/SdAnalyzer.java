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
package org.sd.text.lucene;


import org.sd.nlp.Normalizer;

import java.io.IOException;
import java.io.Reader;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.util.Version;

import org.sd.text.IndexingNormalizer;

/**
 * An analyzer that uses a Normalizer to build token strings.
 * <p>
 * @author Spence Koehler
 */
public class SdAnalyzer extends Analyzer {

  private Normalizer normalizer;
  private Set<String> stopwords;
  private int positionIncrementGap;

  public SdAnalyzer() {
    this(null, null, true);
  }

  public SdAnalyzer(Normalizer normalizer, Set<String> stopwords, boolean treatMultipleAddsAsContinuous) {
    this.normalizer = normalizer != null ? normalizer : IndexingNormalizer.getInstance(IndexingNormalizer.ALL_OPTIONS);
    this.stopwords = stopwords;
    this.positionIncrementGap = treatMultipleAddsAsContinuous ? 0 : 1;
  }

  public TokenStream tokenStream(String fieldName, Reader reader) {
    return buildSavedStreams(fieldName, reader).result;
  }

  public TokenStream reusableTokenStream(String fieldName, Reader reader) throws IOException {
    SavedStreams streams = (SavedStreams)getPreviousTokenStream();
    if (streams == null) {
      streams = buildSavedStreams(fieldName, reader);
      setPreviousTokenStream(streams);
    }
    else {
      streams.source.reset(reader);
    }
    return streams.result;
  }

  public int getPositionIncrementGap(String fieldName) {
    return positionIncrementGap;
  }


  private final SavedStreams buildSavedStreams(String fieldName, Reader reader) {
    final SdTokenStream sdTokenStream = new SdTokenStream(reader, normalizer);

    final TokenStream result = (stopwords != null) ?
      new StopFilter(StopFilter.getEnablePositionIncrementsVersionDefault(Version.LUCENE_CURRENT), sdTokenStream, stopwords) :
      sdTokenStream;

    return new SavedStreams(sdTokenStream, result);
  }


  private class SavedStreams {
    public final SdTokenStream source;
    public final TokenStream result;

    SavedStreams(SdTokenStream source, TokenStream result) {
      this.source = source;
      this.result = result;
    }
  };
}
