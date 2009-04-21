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


import org.sd.nlp.StringWrapper;
import org.sd.util.tree.Tree;

import java.util.List;

/**
 * General matching utility methods.
 * <p>
 * @author Spence Koehler
 */
public class MatchUtil {

  /**
   * Determine whether the input looks like an acronym. For now, the rule is
   * that the subString begins and ends with upperCase letters in the original
   * unnormalized input.
   */
  public static final boolean looksLikeAcronym(StringWrapper.SubString subString) {
    return
      Character.isUpperCase(subString.stringWrapper.getCodePoint(subString.startPos)) &&
      Character.isUpperCase(subString.stringWrapper.getCodePoint(subString.endPos - 1));
  }

  /**
   * Utility to merge the given concept models into a single concept model.
   * <p>
   * NOTE: The new concept model's id and info object will be that of the first
   *       concept model.
   * <p>
   * NOTE: The argument concept model instances will be invalidated!
   */
  public static final ConceptModel mergeConceptModels(ConceptModel[] conceptModels) {
    final ConceptModel result = new ConceptModel();

    result.setConceptId(conceptModels[0].getConceptId());
    result.setInfoObject(conceptModels[0].getInfoObject());

    final Tree<ConceptModel.Data> conceptTree = result.getTree();
    final ConceptModel.MatchConcept matchConcept = conceptTree.getData().asMatchConcept();

    for (ConceptModel conceptModel : conceptModels) {
      final Tree<ConceptModel.Data> curTree = conceptModel.getTree();
      final List<Tree<ConceptModel.Data>> formNodes = curTree.getChildren();
      for (Tree<ConceptModel.Data> formNode : formNodes) {
        final ConceptModel.ConceptForm formData = formNode.getData().asConceptForm();
        matchConcept.addConceptForm(formData);
      }
    }

    return result;
  }
}
