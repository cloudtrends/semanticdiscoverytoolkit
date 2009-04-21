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
package org.sd.wn;


import org.sd.nlp.AbstractLexicon;
import org.sd.nlp.Categories;
import org.sd.nlp.Category;
import org.sd.nlp.CategoryFactory;
import org.sd.nlp.GeneralNormalizer;
import org.sd.nlp.Normalizer;
import org.sd.nlp.StringWrapper;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A lexicon for recognizing conjugated verbs using simple rules and WordNet's
 * verb exception file.
 * <p>
 * @author Spence Koehler
 */
public class ConjugationLexicon extends AbstractLexicon {

  private static final RegularConjugations REGULAR_CONJUGATIONS = RegularConjugations.getInstance();


  private Category nounCategory;
  private Category verbCategory;
  private Category adjCategory;
  private Category advCategory;
  private IrregularInflections irregularInflections;
  private Map<POS, Category> pos2cat;

  public ConjugationLexicon(Normalizer normalizer, Category nounCategory, Category verbCategory, Category adjCategory, Category advCategory) {
    super(normalizer);

    this.nounCategory = nounCategory;
    this.verbCategory = verbCategory;
    this.adjCategory = adjCategory;
    this.advCategory = advCategory;

    try {
      this.irregularInflections = IrregularInflections.getInstance((File)null);
    }
    catch (IOException e) {
      throw new IllegalStateException(e);
    }

    this.pos2cat = new HashMap<POS, Category>();
    pos2cat.put(POS.NOUN, nounCategory);
    pos2cat.put(POS.VERB, verbCategory);
    pos2cat.put(POS.ADJ, adjCategory);
    pos2cat.put(POS.ADV, advCategory);
  }

  /**
   * Define applicable categories in the subString.
   *
   * @param subString   The substring to define.
   * @param normalizer  The normalizer to use.
   */
  protected void define(StringWrapper.SubString subString, Normalizer normalizer) {
//todo: define appropriately
    // run string through the same normalizer as used on construction.
    final String string = subString.getNormalizedString(normalizer);

    final List<TaggedWord> bases = irregularInflections.getBases(string);

    boolean isDefined = false;

    if (bases != null) {
      // is an irregular inflection
      for (TaggedWord base : bases) {
        for (POS pos : base.partsOfSpeech) {
          subString.addCategory(pos2cat.get(pos));
        }
      }
//      subString.setDefinitive(true);
      isDefined = true;
    }
    else {
      if (string.indexOf(' ') < 0) {  // only try this if it is a single word
        // try standard conjugations
        final List<RegularConjugations.Word> cwords = REGULAR_CONJUGATIONS.getPotentialWords(string);

        if (cwords != null) {
          for (RegularConjugations.Word cword : cwords) {
//todo: verify cword.base (w/POS=cword.basePos) exists?
            subString.addCategory(pos2cat.get(cword.wordPos));  // use actual POS not base's
          }
        }
      }
    }

    if (isDefined) {
      subString.setAttribute(NORMALIZED_ATTRIBUTE, string);
    }
  }

  /**
   * Determine whether the categories container already has category type(s)
   * that this lexicon would add.
   * <p>
   * NOTE: this is used to avoid calling "define" when categories already exist
   *       for the substring.
   *
   * @return true if this lexicon's category type(s) are already present.
   */
  protected boolean alreadyHasTypes(Categories categories) {
    return
      categories.hasType(nounCategory) &&
      categories.hasType(verbCategory) &&
      categories.hasType(adjCategory) &&
      categories.hasType(advCategory);
  }


  public static final void main(String[] args) {
    final CategoryFactory cf = new CategoryFactory();
    cf.addCategories("n,v,adj,adv");
    final ConjugationLexicon lex =
      new ConjugationLexicon(
        GeneralNormalizer.getCaseInsensitiveInstance(),
        cf.getCategory("N"),
        cf.getCategory("V"),
        cf.getCategory("ADJ"),
        cf.getCategory("ADV"));

    for (String arg : args) {
      final StringWrapper sw = new StringWrapper(arg);
      final StringWrapper.SubString ss = sw.getSubString(0);
      lex.lookup(ss);

      System.out.println(arg + " -> " + ss.getCategories());
    }
  }
}
