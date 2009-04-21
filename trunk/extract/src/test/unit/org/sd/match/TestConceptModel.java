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


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.List;

/**
 * JUnit Tests for the ConceptModel class.
 * <p>
 * @author Spence Koehler
 */
public class TestConceptModel extends TestCase {

  public TestConceptModel(String name) {
    super(name);
  }
  
  public static final ConceptModel buildTestModel() {
    // Concept: Valves, Butterfly, Ethylene Propylene Diene Monomer (EPDM) Seat, Lugged and Tapped

    final ConceptModel result = new ConceptModel();
    final ConceptModel.MatchConcept concept = result.setConceptId(123);

    ConceptModel.ConceptForm form = null;
    ConceptModel.ConceptTerm term = null;
    ConceptModel.TermSynonym syn = null;
    ConceptModel.OrthographicVariant var = null;
    ConceptModel.WordData word = null;

    /////
    // Valves
    form = concept.addConceptForm(Form.Type.FULL_UK_DESC);
    term = form.addConceptTerm(Decomp.Type.MISCELLANEOUS);
    syn = term.addTermSynonym(Synonym.Type.PRIMARY);
    var = syn.addOrthographicVariant(Variant.Type.NORMAL);
    word = var.addWordData(Word.Type.NORMAL, "valves");

    /////
    // Butterfly
    term = form.addConceptTerm(Decomp.Type.MISCELLANEOUS);
    syn = term.addTermSynonym(Synonym.Type.PRIMARY);
    var = syn.addOrthographicVariant(Variant.Type.NORMAL);
    word = var.addWordData(Word.Type.NORMAL, "butterfly");

    /////
    // Ethylene Propylene Diene Monomer (EPDM) Seat
    term = form.addConceptTerm(Decomp.Type.MISCELLANEOUS);

    // Ethylene Propylene Diene Monomer Seat
    syn = term.addTermSynonym(Synonym.Type.PRIMARY);
    var = syn.addOrthographicVariant(Variant.Type.NORMAL);
    word = var.addWordData(Word.Type.NORMAL, "ethylene");
    word = var.addWordData(Word.Type.NORMAL, "propylene");
    word = var.addWordData(Word.Type.NORMAL, "diene");
    word = var.addWordData(Word.Type.NORMAL, "monomer");
    word = var.addWordData(Word.Type.NORMAL, "seat");

    // EPDM Seat
    syn = term.addTermSynonym(Synonym.Type.ACRONYM);
    var = syn.addOrthographicVariant(Variant.Type.NORMAL);
    word = var.addWordData(Word.Type.ACRONYM, "epdm");
    word = var.addWordData(Word.Type.NORMAL, "seat");

    /////
    // Lugged and Tapped
    term = form.addConceptTerm(Decomp.Type.MISCELLANEOUS);
    syn = term.addTermSynonym(Synonym.Type.PRIMARY);
    var = syn.addOrthographicVariant(Variant.Type.NORMAL);
    word = var.addWordData(Word.Type.NORMAL, "lugged");
    word = var.addWordData(Word.Type.FUNCTIONAL, "and");
    word = var.addWordData(Word.Type.NORMAL, "tapped");

    // Lugged
    syn = term.addTermSynonym(Synonym.Type.CONJ);
    var = syn.addOrthographicVariant(Variant.Type.NORMAL);
    word = var.addWordData(Word.Type.NORMAL, "lugged");

    // Tapped
    syn = term.addTermSynonym(Synonym.Type.CONJ);
    var = syn.addOrthographicVariant(Variant.Type.NORMAL);
    word = var.addWordData(Word.Type.NORMAL, "tapped");

    return result;
  }

  public void testWordIndex() {
    final ConceptModel model = buildTestModel();
    
    final ConceptModel.MatchConcept concept = model.getMatchConcept();
    final ConceptModel.ConceptForm form = concept.getNode().getChildren().get(0).getData().asConceptForm();

    List<ConceptModel.WordData> wordNodes = form.getWordDatas("lugged");
    assertEquals(2, wordNodes.size());
    assertEquals(3, wordNodes.get(0).getNumWords());  // "lugged and tapped"
    assertEquals(1, wordNodes.get(1).getNumWords());  // "lugged"
    
    wordNodes = form.getWordDatas("seat");
    assertEquals(2, wordNodes.size());
    assertEquals(5, wordNodes.get(0).getNumWords());  // ethylene propylene diene monomer seat
    assertEquals(2, wordNodes.get(1).getNumWords());  // epdm seat
  }

  public void testBuildDumpRebuild() {
    // build
    final ConceptModel model1 = buildTestModel();

    // dump
    final String modelString1 = model1.getTreeString();
    assertNotNull(modelString1);
    assertTrue(modelString1.length() > 0);

    System.out.println("\n" + modelString1);

    // rebuild
    final ConceptModel model2 = new ConceptModel(modelString1);

    // re-dump
    final String modelString2 = model2.getTreeString();
    assertEquals(modelString1, modelString2);
  }

  public void testReconstructModel() {
    final ConceptModel model = new ConceptModel();
    final String treeString = "(C195561 (F2 (T1 (S0 (V0 W0|dictionaries))) (T2 (S0 (V0 W0|english W2|to W0|german))) (T15 (S0 (V0 W0|publications)))))";
    model.loadWithTreeString(treeString);

    assertEquals(treeString, model.getTreeString());
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestConceptModel.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
