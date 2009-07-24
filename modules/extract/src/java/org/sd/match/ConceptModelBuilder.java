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
package org.sd.match;


/**
 * Utility class to build a concept model through a builder strategy.
 * <p>
 * @author Spence Koehler
 */
public class ConceptModelBuilder {
  
  private ConceptModelBuilderStrategy strategy;

  public ConceptModelBuilder(ConceptModelBuilderStrategy strategy) {
    this.strategy = strategy;
  }

  public ConceptModelBuilderStrategy getStrategy() {
    return strategy;
  }

  public ConceptModel buildNextModel() {
    ConceptModel model = null;

    final Integer conceptId = strategy.nextMatchConcept();

    if (conceptId == null) return null;
    
    model = new ConceptModel();
    model.setInfoObject(strategy.getCurrentInfoObject());
    final ConceptModel.MatchConcept concept = model.setConceptId(conceptId);

    for (Form.Type formType = strategy.nextConceptForm(); formType != null; formType = strategy.nextConceptForm()) {
      ConceptModel.ConceptForm form = null;

      for (Decomp.Type decompType = strategy.nextConceptTerm(); decompType != null; decompType = strategy.nextConceptTerm()) {
        ConceptModel.ConceptTerm term = null;

        for (Synonym.Type synonymType = strategy.nextTermSynonym(); synonymType != null; synonymType = strategy.nextTermSynonym()) {
          ConceptModel.TermSynonym syn = null;

          for (Variant.Type variantType = strategy.nextVariantType(); variantType != null; variantType = strategy.nextVariantType()) {
            ConceptModel.OrthographicVariant var = null;

            for (TypedWord typedWord = strategy.nextTypedWord(); typedWord != null; typedWord = strategy.nextTypedWord()) {

              // lazily build model nodes so we don't have incomplete paths.
              if (form == null) {
                form = concept.addConceptForm(formType);
              }
              if (term == null) {
                term = form.addConceptTerm(decompType);
              }
              if (syn == null) {
                syn = term.addTermSynonym(synonymType);
              }
              if (var == null) {
                var = syn.addOrthographicVariant(variantType);
              }

              // add word data
              var.addWordData(typedWord.wordType, typedWord.word);
            }
          }
        }
      }
    }

    return model;
  }
}
