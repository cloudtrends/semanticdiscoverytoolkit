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
package org.sd.extract.datetime;


import org.sd.extract.AbstractExtractorFSM;
import org.sd.nlp.AbstractNormalizer;
import org.sd.nlp.Category;
import org.sd.nlp.CategoryFactory;
import org.sd.nlp.DateTimeBreakStrategy;
import org.sd.nlp.DateTimeLexiconBuilder;
import org.sd.nlp.GeneralNormalizer;
import org.sd.nlp.LexicalTokenizer;
import org.sd.nlp.Lexicon;
import org.sd.nlp.Parser;
import org.sd.nlp.StringWrapper;
import org.sd.nlp.StringWrapperLexicalTokenizer;
import org.sd.nlp.TokenizationStrategy;

import java.io.IOException;
import java.util.List;

/**
 * Extractor for DateTime entities built under the 'doc' extraction framework.
 * <p>
 * @author Spence Koehler
 */
public class DateTimeExtractor extends AbstractExtractorFSM {
  
  public static final String EXTRACTION_TYPE = "DateTime";
  private static final AbstractNormalizer NORMALIZER = GeneralNormalizer.getCaseInsensitiveInstance();

  private int minYear;

  public DateTimeExtractor(int minYear) {
    this(minYear, false, 0);
  }

  public DateTimeExtractor(int minYear, boolean ignoreExtraInput, int skipUpTo) {
    super(EXTRACTION_TYPE, DateTimeTextAcceptor.getInstance(), null, false, false,
          NORMALIZER, DateTimeBreakStrategy.getInstance(),
          DateTimeExtractor.class, "resources/datetime_grammar.txt",
          ignoreExtraInput, skipUpTo, false, DateTimeDisambiguator.getInstance());

    this.minYear = minYear;
  }

  protected CategoryFactory buildCategoryFactory() {
    final CategoryFactory result = new CategoryFactory();

    result.defineCategory("DATE_TIME", false);
    result.defineCategory("DATE", false);
    result.defineCategory("TIME", false);

    result.defineCategory("DAY", false);
    result.defineCategory("MONTH", false);
    result.defineCategory("YEAR", false);
    result.defineCategory("WEEKDAY", false);

    result.defineCategory("HOUR", false);
    result.defineCategory("MINUTE", false);
    result.defineCategory("SECOND", false);
    result.defineCategory("AMPM", false);
    result.defineCategory("TZ", false);

    result.defineCategory("PREP", false);

    return result;
  }

  protected Lexicon buildLexicon(CategoryFactory categoryFactory, AbstractNormalizer normalizer) throws IOException {
    return DateTimeLexiconBuilder.buildLexicon(categoryFactory, normalizer, minYear);
  }

  protected Category[] buildTopLevelCategories(CategoryFactory categoryFactory) {
    return new Category[] {
      categoryFactory.getCategory("DATE_TIME"),
      categoryFactory.getCategory("DATE"),
      categoryFactory.getCategory("TIME"),
    };
  }

  protected LexicalTokenizer buildLexicalTokenizer(StringWrapper stringWrapper, Lexicon lexicon, int skipUpTo) {
    return new StringWrapperLexicalTokenizer(
      stringWrapper,
      TokenizationStrategy.getStrategy(TokenizationStrategy.Type.LONGEST_TO_SHORTEST_TO_LONGEST, lexicon),
      skipUpTo);
  }
}
