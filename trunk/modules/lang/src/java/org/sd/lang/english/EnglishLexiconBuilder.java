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
package org.sd.lang.english;


import org.sd.nlp.Category;
import org.sd.nlp.CategoryFactory;
import org.sd.nlp.GenericLexicon;
import org.sd.nlp.Lexicon;
import org.sd.nlp.LexiconBuilder;
import org.sd.nlp.LexiconPipeline;
import org.sd.nlp.Normalizer;
//import org.sd.qtag.QtagLexicon;
import org.sd.wn.ConjugationLexicon;
import org.sd.wn.POS;
import org.sd.wn.WordNetLexicon;

import java.io.IOException;
import java.util.Properties;

/**
 * Lexicon builder for english.
 * <p>
 * @author Spence Koehler
 */
public class EnglishLexiconBuilder implements LexiconBuilder {
  
  public static final EnglishLexiconBuilder getInstance(Properties properties) {
    return new EnglishLexiconBuilder(properties);
  }


  private Properties properties;

  private EnglishLexiconBuilder(Properties properties) {
    this.properties = properties;
  }

  public Lexicon buildLexicon(CategoryFactory categoryFactory, Normalizer normalizer) throws IOException {

    final Category noun = categoryFactory.getCategory("N");
    final Category verb = categoryFactory.getCategory("V");
    final Category adj = categoryFactory.getCategory("ADJ");
    final Category adv = categoryFactory.getCategory("ADV");
    final Category prep = categoryFactory.getCategory("PREP");
    final Category det = categoryFactory.getCategory("DET");
    final Category conj = categoryFactory.getCategory("CONJ");

//numbers (=ADJ)  include units "i.e. 30 feet/sec"
//numbersWithAlpha (=N)
//initials and acronyms (take care not to collide with determiner/article "a")
//handle conjunctions within lexicons? (and without at high-levels only in grammar)

    final Lexicon[] lexicons = new Lexicon[] {

//      // let's see how qtag works as a primary means of defining terms
//      new QtagLexicon(null, noun, verb, adj, adv, prep, det, conj),

      // Numbers, AlphaNumerics
      //todo: ...

//todo: make WordNet dbMaps use case insensitive general normalizer! ...ignore 'normalizer' here and use general normalizer in WordNetLexicon construction.

      // N: WordNet nouns, Proper nouns, Onomasticon/Full Name forms, Acronyms
      new WordNetLexicon(null, POS.NOUN, noun, normalizer),
      //todo: Proper nouns, Onomasticon/Full Name forms, Acronmyms

      // V: WordNet verbs
      new WordNetLexicon(null, POS.VERB, verb, normalizer),

      // ADJ
      new WordNetLexicon(null, POS.ADJ, adj, normalizer),

      // ADV: Recognize by endings?
      //todo: ...

      // PREP
      GenericLexicon.loadGenericLexicon(EnglishLexiconBuilder.class, "resources/EnglishSentence.preps.txt", normalizer, prep, false, true, false, null),

      // xtra NOUNs (i.e. dets that can be nouns, like "this", "that", etc)
      GenericLexicon.loadGenericLexicon(EnglishLexiconBuilder.class, "resources/EnglishSentence.xtra-nouns.txt", normalizer, noun, false, false, false, null),

      // DET: This needs to be after Initials, and Acronyms so as not to confuse article "a" with others?
      GenericLexicon.loadGenericLexicon(EnglishLexiconBuilder.class, "resources/EnglishSentence.dets.txt", normalizer, det, false, true, false, null),

      // N,V,ADJ,ADV: Conjugations
      new ConjugationLexicon(normalizer, noun, verb, adj, adv),
    };

    return new LexiconPipeline(lexicons);
  }

//todo: include classifying "to V" as a verb
}
