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

import org.sd.nlp.GeneralNormalizer;
import org.sd.nlp.Normalizer;
import org.sd.nlp.NormalizingTokenizer;
import org.sd.nlp.RbiNormalizer;

/**
 * JUnit Tests for the MatchAligner class.
 * <p>
 * @author Spence Koehler
 */
public class TestMatchAligner extends TestCase {

  public TestMatchAligner(String name) {
    super(name);
  }
  
  private final void doAlignmentTest(Normalizer normalizer, ConceptModel conceptModel,
                                     String inputString, String expectedAlignedString,
                                     String expectedNormalizedAlignedString,
                                     String expectedExplanation,
                                     double expectedScore) {
    final MatchAligner aligner =
      new MatchAligner(normalizer,
                       new NormalizingTokenizer(normalizer, inputString).getTokens(),
                       conceptModel);

    final MatchAligner.MatchResult matchResult = aligner.getMatchResult();

    if (expectedAlignedString == null && expectedNormalizedAlignedString == null && expectedExplanation == null) {
      System.out.println(inputString);
      System.out.println(matchResult.getAlignedString(false));
      System.out.println(matchResult.getAlignedString(true));
      System.out.println(matchResult.getExplanation());
      System.out.println(matchResult.getScore());
    }
    else {
      assertEquals(expectedAlignedString, matchResult.getAlignedString(false));
      assertEquals(expectedNormalizedAlignedString, matchResult.getAlignedString(true));
      assertEquals("got: " + matchResult.getExplanation(), expectedExplanation, matchResult.getExplanation());
      assertEquals(expectedScore, matchResult.getScore(), 0.005);
    }
  }

  public void testAlignment1() {
    final Normalizer normalizer = new GeneralNormalizer(true);

    // Concept: Valves, Butterfly, Ethylene Propylene Diene Monomer (EPDM) Seat, Lugged and Tapped
    final ConceptModel conceptModel = TestConceptModel.buildTestModel();


    doAlignmentTest(normalizer, conceptModel,
                    "Lugged and Tapped EPDM Seat Butterfly Valves",
                    "Valves Butterfly EPDM Seat Lugged and Tapped",
                    "valves butterfly epdm seat lugged and tapped",
                    "((+Valves[2/2] +Butterfly[2/2] +EPDM[2/2] +Seat[2/2] +Lugged[2/2] +and[0/0] +Tapped[2/2]))",
                    1.0);

    doAlignmentTest(normalizer, conceptModel,
                    "Marvelously lugged and tapped EPDM seat Butterfly Valves",
                    "Valves Butterfly EPDM seat lugged and tapped",
                    "valves butterfly epdm seat lugged and tapped",
                    "((+Valves[2/2] +Butterfly[2/2] +EPDM[2/2] +seat[2/2] +lugged[2/2] +and[0/0] +tapped[2/2] *Marvelously[0/1]))",
                    0.923);

    doAlignmentTest(normalizer, conceptModel,
                    "lugged, tapped EPDM seat butterfly valves",  // NOTE: leaving 'and' out doesn't hurt
                    "valves butterfly EPDM seat lugged tapped",
                    "valves butterfly epdm seat lugged tapped",
                    "((+valves[2/2] +butterfly[2/2] +EPDM[2/2] +seat[2/2] +lugged[2/2]) (-and[0/0]) (+tapped[2/2]))",
                    1.0);

    doAlignmentTest(normalizer, conceptModel,
                    "lugged, tapped epdm seat butterfly valves",  // NOTE: epdm no longer looks like an acronym
                    "valves butterfly epdm seat lugged tapped",
                    "valves butterfly epdm seat lugged tapped",
                    "((+valves[2/2] +butterfly[2/2] +epdm[0/2] +seat[2/2] +lugged[2/2]) (-and[0/0]) (+tapped[2/2]))",
                    0.833);

    doAlignmentTest(normalizer, conceptModel,
                    "tapped EPDM butterfly valves",
                    "valves butterfly EPDM tapped",
                    "valves butterfly epdm tapped",
                    "((+valves[2/2] +butterfly[2/2] +EPDM[2/2]) (-seat[0/2]) (+tapped[2/2]))",
                    0.8);

    doAlignmentTest(normalizer, conceptModel,
                    "tapped epdm butterfly valves",  // NOTE: epdm no longer looks like an acronym
                    "valves butterfly epdm tapped",
                    "valves butterfly epdm tapped",
                    "((+valves[2/2] +butterfly[2/2] +epdm[0/2]) (-seat[0/2]) (+tapped[2/2]))",
                    0.6);
  }

  public void testAlignment2() {
    final Normalizer normalizer = RbiNormalizer.getInstance();

    final ConceptModel conceptModel = new ConceptModel();
    final String treeString = "(C195561 (F2 (T1 (S0 (V0 W0|dictionaries))) (T2 (S0 (V0 W0|english W2|to W0|german))) (T15 (S0 (V0 W0|publications)))))";
    conceptModel.loadWithTreeString(treeString);

    doAlignmentTest(normalizer, conceptModel,
                    "German",
                    "German",
                    "german",
                    "((-dictionaries[0/16]) (-english[0/4] -to[0/0]) (+German[4/4]) (-publications[0/2]))",
                    0.154);
  }

  public void testAlignment3() {
    final Normalizer normalizer = RbiNormalizer.getInstance();

    final ConceptModel conceptModel = new ConceptModel();
    final String treeString = "(C43700 (F2 (T1 (S0 (V0 W0|gauges))) (T2 (S0 (V0 W0|electronic))) (T9 (S0 (V0 W0|boiler W0|drum W2|and W0|feedwater W0|heater)))) (F2 (T1 (S0 (V0 W0|gauges))) (T2 (S0 (V0 W0|electronic))) (T9 (S0 (V0 W0|boiler W0|drum W2|and W0|feed W0|water W0|heating W0|equipment)))) (F2 (T1 (S0 (V0 W0|gauges))) (T2 (S0 (V0 W0|electronic))) (T9 (S0 (V0 W0|boiler W0|drum W2|and W0|feed W0|water W0|heater)))) (F2 (T1 (S0 (V0 W0|gauges))) (T2 (S0 (V0 W0|electronic))) (T9 (S0 (V0 W0|boiler W0|drum W2|and W0|feedwater W0|heating W0|equipment)))) (F2 (T1 (S0 (V0 W0|gages))) (T2 (S0 (V0 W0|electronic))) (T9 (S0 (V0 W0|boiler W0|drum W2|and W0|feedwater W0|heater)))) (F2 (T1 (S0 (V0 W0|gages))) (T2 (S0 (V0 W0|electronic))) (T9 (S0 (V0 W0|boiler W0|drum W2|and W0|feed W0|water W0|heating W0|equipment)))) (F2 (T1 (S0 (V0 W0|gages))) (T2 (S0 (V0 W0|electronic))) (T9 (S0 (V0 W0|boiler W0|drum W2|and W0|feed W0|water W0|heater)))) (F2 (T1 (S0 (V0 W0|gages))) (T2 (S0 (V0 W0|electronic))) (T9 (S0 (V0 W0|boiler W0|drum W2|and W0|feedwater W0|heating W0|equipment)))) (F0 (T0 (S0 (V0 W0|gauges))) (T0 (S0 (V0 W0|electronic))) (T0 (S0 (V0 W0|boiler W0|drums W2|and W0|feedwater W0|heater)))) (F1 (T0 (S0 (V0 W0|gages))) (T0 (S0 (V0 W0|electronic))) (T0 (S0 (V0 W0|boiler W0|drums W2|and W0|feedwater W0|heater)))) (F4 (T0 (S0 (V0 W0|kalibers))) (T0 (S0 (V0 W0|elektronisch))) (T0 (S0 (V0 W0|ketel W0|trommels W2|en W0|voedingswater W0|heater)))))";
    conceptModel.loadWithTreeString(treeString);

    doAlignmentTest(normalizer, conceptModel,
                    "Heater fans",
                    "Heater",
                    "heater",
                    "((-gauges[0/2]) (-electronic[0/2]) (-boiler[0/2] -drums[0/2] -and[0/0] -feedwater[0/2]) (+Heater[2/2] *fans[0/1]))",
                    0.154);
  }

  public void testAlignment4() {
    final Normalizer normalizer = RbiNormalizer.getInstance();

    ConceptModel conceptModel = null;
    String treeString = null;

    conceptModel = new ConceptModel();
    treeString = "(C12826 (F2 (T1 (S0 (V0 W0|cables))) (T2 (S0 (V0 W0|steel W0|reinforced))) (T3 (S0 (V0 W0|pvc W0|covered))) (T6 (S0 (V0 W0|aluminium)))) (F2 (T1 (S0 (V0 W0|cables))) (T2 (S0 (V0 W0|steel W0|reinforced))) (T3 (S0 (V0 W0|pvc W0|covered))) (T6 (S0 (V0 W0|aluminum)))) (F2 (T1 (S0 (V0 W0|cables))) (T2 (S0 (V0 W0|steel W0|reinforced))) (T3 (S0 (V0 W0|polyvinyl W0|chloride W0|covered))) (T6 (S0 (V0 W0|aluminium)))) (F2 (T1 (S0 (V0 W0|cables))) (T2 (S0 (V0 W0|steel W0|reinforced))) (T3 (S0 (V0 W0|polyvinyl W0|chloride W0|covered))) (T6 (S0 (V0 W0|aluminum)))) (F2 (T1 (S0 (V0 W0|cables))) (T2 (S0 (V0 W0|steel W0|reenforced))) (T3 (S0 (V0 W0|pvc W0|covered))) (T6 (S0 (V0 W0|aluminium)))) (F2 (T1 (S0 (V0 W0|cables))) (T2 (S0 (V0 W0|steel W0|reenforced))) (T3 (S0 (V0 W0|pvc W0|covered))) (T6 (S0 (V0 W0|aluminum)))) (F2 (T1 (S0 (V0 W0|cables))) (T2 (S0 (V0 W0|steel W0|reenforced))) (T3 (S0 (V0 W0|polyvinyl W0|chloride W0|covered))) (T6 (S0 (V0 W0|aluminium)))) (F2 (T1 (S0 (V0 W0|cables))) (T2 (S0 (V0 W0|steel W0|reenforced))) (T3 (S0 (V0 W0|polyvinyl W0|chloride W0|covered))) (T6 (S0 (V0 W0|aluminum)))) (F4 (T0 (S0 (V0 W0|kabels))) (T0 (S0 (V0 W0|aluminium))) (T0 (S0 (V0 W0|staalversterkt))) (T0 (S0 (V0 W0|pvc W0|bekleed)))))";
    conceptModel.loadWithTreeString(treeString);

    doAlignmentTest(normalizer, conceptModel,
                    "Aluminium Chloride",
                    "Chloride Aluminium",
                    "chloride aluminium",
                    "((-cables[0/16]) (-steel[0/8] -reinforced[0/8]) (-polyvinyl[0/4]) (+Chloride[4/4]) (-covered[0/4]) (+Aluminium[4/4]))",
                    0.2);

    conceptModel = new ConceptModel();
    treeString = "(C4173 (F2 (T1 (S0 (V0 W0|coating W0|services))) (T2 (S0 (V0 W0|pvc W0|plastisol))) (T9 (S0 (V0 W0|aluminium W0|coil))) (T10 (S0 (V0 W0|marine)))) (F2 (T1 (S0 (V0 W0|coating W0|services))) (T2 (S0 (V0 W0|pvc W0|plastisol))) (T9 (S0 (V0 W0|aluminum W0|coil))) (T10 (S0 (V0 W0|marine)))) (F2 (T1 (S0 (V0 W0|coating W0|services))) (T2 (S0 (V0 W0|polyvinyl W0|chloride W0|plastisol))) (T9 (S0 (V0 W0|aluminium W0|coil))) (T10 (S0 (V0 W0|marine)))) (F2 (T1 (S0 (V0 W0|coating W0|services))) (T2 (S0 (V0 W0|polyvinyl W0|chloride W0|plastisol))) (T9 (S0 (V0 W0|aluminum W0|coil))) (T10 (S0 (V0 W0|marine)))) (F2 (T1 (S0 (V0 W0|coating))) (T2 (S0 (V0 W0|pvc W0|plastisol))) (T9 (S0 (V0 W0|aluminium W0|coil))) (T10 (S0 (V0 W0|marine)))) (F2 (T1 (S0 (V0 W0|coating))) (T2 (S0 (V0 W0|pvc W0|plastisol))) (T9 (S0 (V0 W0|aluminum W0|coil))) (T10 (S0 (V0 W0|marine)))) (F2 (T1 (S0 (V0 W0|coating))) (T2 (S0 (V0 W0|polyvinyl W0|chloride W0|plastisol))) (T9 (S0 (V0 W0|aluminium W0|coil))) (T10 (S0 (V0 W0|marine)))) (F2 (T1 (S0 (V0 W0|coating))) (T2 (S0 (V0 W0|polyvinyl W0|chloride W0|plastisol))) (T9 (S0 (V0 W0|aluminum W0|coil))) (T10 (S0 (V0 W0|marine)))) (F0 (T0 (S0 (V0 W0|aluminium W0|coil W0|coating))) (T0 (S0 (V0 W0|pvc W0|plastisol))) (T0 (S0 (V0 W0|marine W0|applications)))) (F1 (T0 (S0 (V0 W0|aluminum W0|coil W0|coating))) (T0 (S0 (V0 W0|pvc W0|plastisol))) (T0 (S0 (V0 W0|marine W0|applications)))) (F4 (T0 (S0 (V0 W0|aluminiumspoelcoatings))) (T0 (S0 (V0 W0|pvc W0|plastisol))) (T0 (S0 (V0 W0|zeevaarttoepassingen)))))";
    conceptModel.loadWithTreeString(treeString);

    doAlignmentTest(normalizer, conceptModel,
                    "Aluminium Chloride",
                    "Chloride Aluminium",
                    "chloride aluminium",
                    "((-coating[0/16]) (-polyvinyl[0/4]) (+Chloride[4/4]) (-plastisol[0/4]) (+Aluminium[4/4]) (-coil[0/4]) (-marine[0/4]))",
                    0.2);

    conceptModel = new ConceptModel();
    treeString = "(C111065 (F2 (T1 (S0 (V0 W0|aluminium W0|chloride))) (T2 (S0 (V0 W0|anhydrous)))) (F2 (T1 (S0 (V0 W0|aluminum W0|chloride))) (T2 (S0 (V0 W0|anhydrous)))) (F4 (T0 (S0 (V0 W0|aluminiumchloride))) (T0 (S0 (V0 W0|watervrij)))))";
    conceptModel.loadWithTreeString(treeString);

    doAlignmentTest(normalizer, conceptModel,
                    "Aluminium Chloride",
                    "Aluminium Chloride",
                    "aluminium chloride",
                    "((+Aluminium[16/16] +Chloride[16/16]) (-anhydrous[0/4]))",
                    0.889);
  }

  public void testAlignment5() {
    final Normalizer normalizer = RbiNormalizer.getInstance();

    final ConceptModel conceptModel = new ConceptModel();
    final String treeString = "(C99960 (F2 (T1 (S0 (V0 W0|valves))) (T2 (S0 (V0 W0|butterfly)))) (F3 (T0 (S0 (V0 W0|vlinderkleppen)))) (F4 (T0 (S0 (V0 W4|vlinderkleppen) (V0 W5|vlinder W5|kleppen)))))";
    conceptModel.loadWithTreeString(treeString);

    doAlignmentTest(normalizer, conceptModel,
                    "Afsluiters, vlinder",
                    "vlinder",
                    "vlinder",
                    "((+vlinder[2/2]) (-kleppen[0/2]) (*Afsluiters[0/1]))",
                    0.4);
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestMatchAligner.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
