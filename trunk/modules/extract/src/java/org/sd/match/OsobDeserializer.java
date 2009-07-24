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


import java.util.TreeSet;

/**
 * Utility to deserialize a serialized osob.
 * <p>
 * @author Spence Koehler
 */
public class OsobDeserializer implements ConceptDeserializer {
  
  public OsobDeserializer() {
  }

  public ConceptModel buildConceptModel(String encodedOsob) {
    final Osob osob = Osob.decode(encodedOsob);
    return buildConceptModel(osob);
  }

  public ConceptModel buildConceptModel(Osob osob) {
    final ConceptModel result = new ConceptModel();
    final ConceptModel.MatchConcept concept = result.setConceptId(osob.getConceptId());
    
    ConceptModel.ConceptForm form = null;
    ConceptModel.ConceptTerm term = null;
    ConceptModel.TermSynonym syn = null;
    ConceptModel.OrthographicVariant var = null;

    Form.Type formType = null;
    Decomp.Type termType = null;
    Synonym.Type synType = null;
    Variant.Type varType = null;
    Word.Type wordType = null;

    final TreeSet<NumberedWord> numberedWords = new TreeSet<NumberedWord>(OsobSerializer.getInputOrderComparator());

    for (Osob.Pointer pointer = osob.firstPointer(); pointer.hasData(); pointer.increment()) {

      if ((formType = pointer.getFormType()) != null) {
        form = concept.addConceptForm(formType);
      }
      if ((termType = pointer.getTermType()) != null) {
        term = form.addConceptTerm(termType);
      }
      if ((synType = pointer.getSynonymType()) != null) {
        syn = term.addTermSynonym(synType);
      }
      if ((varType = pointer.getVariantType()) != null) {
        if (numberedWords.size() > 0) {
          // flush words
          for (NumberedWord numberedWord : numberedWords) {
            var.addWordData(numberedWord.word.wordType, numberedWord.word.word);
          }
          numberedWords.clear();
        }

        var = syn.addOrthographicVariant(varType);
      }
      if ((wordType = pointer.getWordType()) != null) {
        numberedWords.add(pointer.getNumberedWord());
      }
    }

    if (numberedWords.size() > 0) {
      // flush words
      for (NumberedWord numberedWord : numberedWords) {
        var.addWordData(numberedWord.word.wordType, numberedWord.word.word);
      }
    }

    return result;
  }
}
