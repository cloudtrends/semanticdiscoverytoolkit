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
package org.sd.text.segment;


import org.sd.nlp.Parser;
import org.sd.nlp.ParserWrapper;
import org.sd.nlp.StringWrapper;
import org.sd.util.LRU;

import java.util.List;

/**
 * A boolean feature computation to find a parse in text.
 * <p>
 * @author Spence Koehler
 */
public class HasParseFeature implements BooleanFeatureComputation {

  private String featureType;
  private ParserWrapper parser;
  private LRU<String, List<Parser.Parse>> cache = new LRU<String, List<Parser.Parse>>(100);

  public HasParseFeature(String featureType, ParserWrapper parser) {
    this.featureType = featureType;
    this.parser = parser;
  }

  /**
   * Get the feature type (or key).
   */
  public String getFeatureType() {
    return featureType;
  }

  /**
   * Compute the boolean feature value for the text.
   *
   * @return true, false, or null if this computation does not apply to the text.
   */
  public Boolean computeFeature(String text) {
    final List<Parser.Parse> parses = getParses(text);
    return parses != null;
  }

  /**
   * Function to split the text in a way meaningul to an instance's
   * computation.
   * <p>
   * For example, term finders might split the text into the data before
   * a key term, the key term, and the data after the key term.
   *
   * @return the segment sequence of the split text or null if unable or
   *         meaningless to split.
   */
  public SegmentSequence split(String text) {
    SegmentSequence result = null;

    final List<Parser.Parse> parses = getParses(text);
    if (parses != null) {
      final Parser.Parse parse = parses.get(0);
      final StringWrapper.SubString prior = parse.getUnparsedPriorInput();
      final StringWrapper.SubString parsed = parse.getParsedInput();
      final StringWrapper.SubString post = parse.getUnparsedPostInput();

      result = new SegmentSequence();
      if (prior != null && prior.length() > 0) {
        result.add(prior.originalSubString);
        result.add(" ").setManualFeature(CommonFeatures.WHITESPACE, true);
      }
      result.add(parsed.originalSubString).setManualFeature(featureType, true);
      if (post != null && post.length() > 0) {
        result.add(" ").setManualFeature(CommonFeatures.WHITESPACE, true);
        result.add(post.originalSubString);
      }
    }

    return result;
  }
  
  private final List<Parser.Parse> getParses(String text) {
    List<Parser.Parse> result = null;

    if (cache.containsKey(text)) {
      result = cache.get(text);
    }
    else {
      result = parser.parse(text);
      cache.put(text, result);
    }

    return result;
  }

}
