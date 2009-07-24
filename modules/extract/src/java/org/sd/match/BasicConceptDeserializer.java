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
 * Utility to deserialize a serialized basic concept.
 * <p>
 * @author Spence Koehler
 */
public class BasicConceptDeserializer implements ConceptDeserializer {
  
  public ConceptModel buildConceptModel(String basicConcept) {
    final ConceptModel result = new ConceptModel();

    final String[] pieces = basicConcept.split(" ");

    final int conceptId = Integer.parseInt(pieces[0]);
    final ConceptModel.MatchConcept concept = result.setConceptId(conceptId);
    
    ConceptModel.ConceptForm form = null;
    ConceptModel.ConceptTerm term = null;
    ConceptModel.TermSynonym syn = null;
    ConceptModel.OrthographicVariant var = null;

    for (int i = 1; i < pieces.length; ++i) {
      final char c = pieces[i].charAt(0);

      switch (c) {
        case 'F' :
          form = concept.addConceptForm(Form.getType(pieces[++i]));
          break;
        case 'D' :
          term = form.addConceptTerm(Decomp.getType(pieces[++i]));
          break;
        case 'S' :
          syn = term.addTermSynonym(Synonym.getType(pieces[++i]));
          break;
        case 'V' :
          var = syn.addOrthographicVariant(Variant.getType(pieces[++i]));
          break;
        case 'W' :
          var.addWordData(Word.getType(pieces[++i]), pieces[++i]);
          break;
      }
    }

    return result;
  }
}
