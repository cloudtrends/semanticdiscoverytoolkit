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

import org.sd.nlp.AbstractNormalizer;
import org.sd.nlp.GeneralNormalizer;
import org.sd.nlp.NormalizingTokenizer;
import org.sd.nlp.StringWrapper;

import java.util.List;

/**
 * JUnit Tests for the OsobAligner class.
 * <p>
 * @author Spence Koehler
 */
public class TestOsobAligner extends TestCase {

  public TestOsobAligner(String name) {
    super(name);
  }
  
  private final InputWrapper getInputWrapper(AbstractNormalizer normalizer, String inputString) {
    final List<StringWrapper.SubString> inputWords = new NormalizingTokenizer(normalizer, inputString).getTokens();
    return new InputWrapper(normalizer, inputWords);
  }

  private final Osob getOsob(String treeString) {
    final ConceptModel model = new ConceptModel();
    model.loadWithTreeString(treeString);
    final OsobSerializer serializer = new OsobSerializer(true);
    model.serialize(serializer);
    return serializer.getOsob();
  }

  private final void verifyAlignmentResult(AlignmentResult result, double expectedScore, String expectedForm, String expectedExplanation) {
    if (expectedForm == null && expectedExplanation == null) {
      System.out.println("score=" + result.getScore());
      System.out.println("form=" + result.getForm());
      System.out.println("explanation=" + result.getExplanation());
    }
    else {
      assertEquals(expectedScore, result.getScore(), 0.005);
      assertEquals(expectedForm, result.getForm());
      assertEquals(expectedExplanation, result.getExplanation());
    }
  }

  public void testAlignment1() {
    final OsobAligner aligner = new OsobAligner();
    final AbstractNormalizer normalizer = new GeneralNormalizer(true);

    Osob osob = null;
    InputWrapper inputWrapper = null;
    AlignmentResult result = null;

    osob = getOsob("(C100001 (F2 (T1 (S0 (V0 W0|valves))) (T2 (S0 (V0 W0|butterfly))) (T3 (S0 (V0 W0|ethylene W0|propylene W0|diene W0|monomer W0|seat))) (T4 (S0 (V0 W0|lugged W2|and W0|tapped)))) (F2 (T1 (S0 (V0 W0|valves))) (T2 (S0 (V0 W0|butterfly))) (T3 (S0 (V0 W1|epdm W0|seat))) (T4 (S0 (V0 W0|lugged W2|and W0|tapped)))) (F2 (T1 (S0 (V0 W0|valves))) (T2 (S0 (V0 W0|butterfly))) (T3 (S0 (V0 W0|ethylene W0|propylene W0|diene W0|monomer W0|chair))) (T4 (S0 (V0 W0|lugged W2|and W0|tapped)))) (F2 (T1 (S0 (V0 W0|valves))) (T2 (S0 (V0 W0|butterfly))) (T3 (S0 (V0 W1|epdm W0|chair))) (T4 (S0 (V0 W0|lugged W2|and W0|tapped)))) (F3 (T0 (S0 (V0 W0|vlinderkleppen W0|ethylene W0|propylene W0|diene W0|monomer W0|seat W0|lugged W2|and W0|tapped)))) (F3 (T0 (S0 (V0 W0|vlinderkleppen W1|epdm W0|seat W0|lugged W2|and W0|tapped)))) (F4 (T0 (S0 (V0 W4|vlinderkleppen W0|ethyleenpropyleendieenmonomeer W0|zitting W0|getrokken W2|en W0|met W0|kraan)))) (F4 (T0 (S0 (V0 W4|vlinderkleppen W1|epdm W0|zitting W0|getrokken W2|en W0|met W0|kraan)))) (F4 (T0 (S0 (V0 W5|vlinder W5|kleppen W0|ethyleenpropyleendieenmonomeer W0|zitting W0|getrokken W2|en W0|met W0|kraan)))) (F4 (T0 (S0 (V0 W5|vlinder W5|kleppen W1|epdm W0|zitting W0|getrokken W2|en W0|met W0|kraan)))) (F5 (T0 (S0 (V0 W4|absperrklappen W0|sitz W0|aus W0|ethylen W0|propylen W0|elastomer W0|gewindeaugen)))) (F5 (T0 (S0 (V0 W4|absperrklappen W1|epdm W0|gewindeaugen)))) (F5 (T0 (S0 (V0 W5|absperr W5|klappen W0|sitz W0|aus W0|ethylen W0|propylen W0|elastomer W0|gewindeaugen)))) (F5 (T0 (S0 (V0 W5|absperr W5|klappen W1|epdm W0|gewindeaugen)))))");

    // perfect match
    inputWrapper = getInputWrapper(normalizer, "Lugged and Tapped EPDM Seat Butterfly Valves");
    result = aligner.align(osob, inputWrapper, 1);
    verifyAlignmentResult(result, 1.0, "F2", "F2 T1 S0 V0 W0 +valves[16/16] T2 S0 V0 W0 +butterfly[4/4] T3 S0 V0 W1 +epdm[4/4] W0 +seat[4/4] T4 S0 V0 W2 +and[0/0] W0 +lugged[4/4] W0 +tapped[4/4]");

    // add an adverb
    inputWrapper = getInputWrapper(normalizer, "Marvelously Lugged and Tapped EPDM Seat Butterfly Valves");
    result = aligner.align(osob, inputWrapper, 1);
    verifyAlignmentResult(result, 0.973, "F2", "F2 T1 S0 V0 W0 +valves[16/16] T2 S0 V0 W0 +butterfly[4/4] T3 S0 V0 W1 +epdm[4/4] W0 +seat[4/4] T4 S0 V0 W2 +and[0/0] W0 +lugged[4/4] W0 +tapped[4/4] *Marvelously[0/1]");

    // leaving out "and" shouldn't hurt
    inputWrapper = getInputWrapper(normalizer, "lugged, tapped EPDM seat butterfly valves");
    result = aligner.align(osob, inputWrapper, 1);
    verifyAlignmentResult(result, 1.0, "F2", "F2 T1 S0 V0 W0 +valves[16/16] T2 S0 V0 W0 +butterfly[4/4] T3 S0 V0 W1 +epdm[4/4] W0 +seat[4/4] T4 S0 V0 W2 -and[0/0] W0 +lugged[4/4] W0 +tapped[4/4]");

    // epdm no longer looks like an acronym
    inputWrapper = getInputWrapper(normalizer, "lugged, tapped epdm seat butterfly valves");
    result = aligner.align(osob, inputWrapper, 1);
    verifyAlignmentResult(result, 0.889, "F2", "F2 T1 S0 V0 W0 +valves[16/16] T2 S0 V0 W0 +butterfly[4/4] T3 S0 V0 W1 +epdm[0/4] W0 +seat[4/4] T4 S0 V0 W2 -and[0/0] W0 +lugged[4/4] W0 +tapped[4/4]");

    // leave out some terms
    inputWrapper = getInputWrapper(normalizer, "tapped EPDM butterfly valves");
    result = aligner.align(osob, inputWrapper, 1);
    verifyAlignmentResult(result, 0.778, "F2", "F2 T1 S0 V0 W0 +valves[16/16] T2 S0 V0 W0 +butterfly[4/4] T3 S0 V0 W1 +epdm[4/4] W0 -seat[0/4] T4 S0 V0 W2 -and[0/0] W0 -lugged[0/4] W0 +tapped[4/4]");

    // leave out some terms and epdm no longer looks like an acronym
    inputWrapper = getInputWrapper(normalizer, "tapped epdm butterfly valves");
    result = aligner.align(osob, inputWrapper, 1);
    verifyAlignmentResult(result, 0.667, "F2", "F2 T1 S0 V0 W0 +valves[16/16] T2 S0 V0 W0 +butterfly[4/4] T3 S0 V0 W1 +epdm[0/4] W0 -seat[0/4] T4 S0 V0 W2 -and[0/0] W0 -lugged[0/4] W0 +tapped[4/4]");

    // match to another form
    inputWrapper = getInputWrapper(normalizer, "absperr klappen EPDM gewindeaugen");
    result = aligner.align(osob, inputWrapper, 1);
    verifyAlignmentResult(result, 1.0, "F5", "F5 T0 S0 V0 W5 +absperr[2/2] W1 +epdm[2/2] W0 +gewindeaugen[2/2] W5 +klappen[2/2]");
  }

  public void testAlignment2() {
    final OsobAligner aligner = new OsobAligner();
    final AbstractNormalizer normalizer = new GeneralNormalizer(true);

    Osob osob = null;
    InputWrapper inputWrapper = null;
    AlignmentResult result = null;

    osob = getOsob("(C195561 (F2 (T1 (S0 (V0 W0|dictionaries))) (T2 (S0 (V0 W0|english W2|to W0|german))) (T15 (S0 (V0 W0|publications)))))");

    // perfect match
    inputWrapper = getInputWrapper(normalizer, "German");
    result = aligner.align(osob, inputWrapper, 1);
    verifyAlignmentResult(result, 0.154, "F2", "F2 T1 S0 V0 W0 -dictionaries[0/16] T2 S0 V0 W0 -english[0/4] W0 +german[4/4] W2 -to[0/0] T15 S0 V0 W0 -publications[0/2]");
  }

  public void testAlignment3() {
    final OsobAligner aligner = new OsobAligner();
    final AbstractNormalizer normalizer = new GeneralNormalizer(true);

    Osob osob = null;
    InputWrapper inputWrapper = null;
    AlignmentResult result = null;

    osob = getOsob("(C43700 (F2 (T1 (S0 (V0 W0|gauges))) (T2 (S0 (V0 W0|electronic))) (T9 (S0 (V0 W0|boiler W0|drum W2|and W0|feedwater W0|heater)))) (F2 (T1 (S0 (V0 W0|gauges))) (T2 (S0 (V0 W0|electronic))) (T9 (S0 (V0 W0|boiler W0|drum W2|and W0|feed W0|water W0|heating W0|equipment)))) (F2 (T1 (S0 (V0 W0|gauges))) (T2 (S0 (V0 W0|electronic))) (T9 (S0 (V0 W0|boiler W0|drum W2|and W0|feed W0|water W0|heater)))) (F2 (T1 (S0 (V0 W0|gauges))) (T2 (S0 (V0 W0|electronic))) (T9 (S0 (V0 W0|boiler W0|drum W2|and W0|feedwater W0|heating W0|equipment)))) (F2 (T1 (S0 (V0 W0|gages))) (T2 (S0 (V0 W0|electronic))) (T9 (S0 (V0 W0|boiler W0|drum W2|and W0|feedwater W0|heater)))) (F2 (T1 (S0 (V0 W0|gages))) (T2 (S0 (V0 W0|electronic))) (T9 (S0 (V0 W0|boiler W0|drum W2|and W0|feed W0|water W0|heating W0|equipment)))) (F2 (T1 (S0 (V0 W0|gages))) (T2 (S0 (V0 W0|electronic))) (T9 (S0 (V0 W0|boiler W0|drum W2|and W0|feed W0|water W0|heater)))) (F2 (T1 (S0 (V0 W0|gages))) (T2 (S0 (V0 W0|electronic))) (T9 (S0 (V0 W0|boiler W0|drum W2|and W0|feedwater W0|heating W0|equipment)))) (F0 (T0 (S0 (V0 W0|gauges W0|electronic W0|boiler W0|drums W2|and W0|feedwater W0|heater)))) (F1 (T0 (S0 (V0 W0|gages W0|electronic W0|boiler W0|drums W2|and W0|feedwater W0|heater)))) (F4 (T0 (S0 (V0 W0|kalibers W0|elektronisch W0|ketel W0|trommels W2|en W0|voedingswater W0|heater)))) (F5 (T0 (S0 (V0 W0|messgeraete W0|elektronische W4|kesseltrommeln W2|und W4|speisewasservorwaermer)))) (F5 (T0 (S0 (V0 W0|messgeraete W0|elektronische W4|kesseltrommeln W2|und W5|speisewasser W5|vorwaermer)))) (F5 (T0 (S0 (V0 W0|messgeraete W0|elektronische W5|kessel W5|trommeln W2|und W4|speisewasservorwaermer)))) (F5 (T0 (S0 (V0 W0|messgeraete W0|elektronische W5|kessel W5|trommeln W2|und W5|speisewasser W5|vorwaermer)))))");

    // perfect match
    inputWrapper = getInputWrapper(normalizer, "Heater fans");
    result = aligner.align(osob, inputWrapper, 1);
    verifyAlignmentResult(result, 0.154, "F0", "F0 T0 S0 V0 W2 -and[0/0] W0 -boiler[0/2] W0 -drums[0/2] W0 -electronic[0/2] W0 -feedwater[0/2] W0 -gauges[0/2] W0 +heater[2/2] *fans[0/1]");
  }

  public void testAlignment4() {
    final OsobAligner aligner = new OsobAligner();
    final AbstractNormalizer normalizer = new GeneralNormalizer(true);

    Osob osob = null;
    InputWrapper inputWrapper = null;
    AlignmentResult result = null;

    osob = getOsob("(C12826 (F2 (T1 (S0 (V0 W0|cables))) (T2 (S0 (V0 W0|steel W0|reinforced))) (T3 (S0 (V0 W0|pvc W0|covered))) (T6 (S0 (V0 W0|aluminium)))) (F2 (T1 (S0 (V0 W0|cables))) (T2 (S0 (V0 W0|steel W0|reinforced))) (T3 (S0 (V0 W0|pvc W0|covered))) (T6 (S0 (V0 W0|aluminum)))) (F2 (T1 (S0 (V0 W0|cables))) (T2 (S0 (V0 W0|steel W0|reinforced))) (T3 (S0 (V0 W0|polyvinyl W0|chloride W0|covered))) (T6 (S0 (V0 W0|aluminium)))) (F2 (T1 (S0 (V0 W0|cables))) (T2 (S0 (V0 W0|steel W0|reinforced))) (T3 (S0 (V0 W0|polyvinyl W0|chloride W0|covered))) (T6 (S0 (V0 W0|aluminum)))) (F2 (T1 (S0 (V0 W0|cables))) (T2 (S0 (V0 W0|steel W0|reenforced))) (T3 (S0 (V0 W0|pvc W0|covered))) (T6 (S0 (V0 W0|aluminium)))) (F2 (T1 (S0 (V0 W0|cables))) (T2 (S0 (V0 W0|steel W0|reenforced))) (T3 (S0 (V0 W0|pvc W0|covered))) (T6 (S0 (V0 W0|aluminum)))) (F2 (T1 (S0 (V0 W0|cables))) (T2 (S0 (V0 W0|steel W0|reenforced))) (T3 (S0 (V0 W0|polyvinyl W0|chloride W0|covered))) (T6 (S0 (V0 W0|aluminium)))) (F2 (T1 (S0 (V0 W0|cables))) (T2 (S0 (V0 W0|steel W0|reenforced))) (T3 (S0 (V0 W0|polyvinyl W0|chloride W0|covered))) (T6 (S0 (V0 W0|aluminum)))) (F4 (T0 (S0 (V0 W0|kabels W0|aluminium W0|staalversterkt W0|pvc W0|bekleed)))) (F5 (T0 (S0 (V0 W0|kabel W0|aluminium W0|stahlverstaerkte W0|pvc W0|beschichtete)))))");

    // perfect match
    inputWrapper = getInputWrapper(normalizer, "Aluminium Chloride");
    result = aligner.align(osob, inputWrapper, 1);
    verifyAlignmentResult(result, 0.2, "F2", "F2 T1 S0 V0 W0 -cables[0/16] T2 S0 V0 W0 -reinforced[0/4] W0 -steel[0/4] T3 S0 V0 W0 +chloride[4/4] W0 -covered[0/4] W0 -polyvinyl[0/4] T6 S0 V0 W0 +aluminium[4/4]");

    osob = getOsob("(C4173 (F2 (T1 (S0 (V0 W0|coating W0|services))) (T2 (S0 (V0 W0|pvc W0|plastisol))) (T9 (S0 (V0 W0|aluminium W0|coil))) (T10 (S0 (V0 W0|marine)))) (F2 (T1 (S0 (V0 W0|coating W0|services))) (T2 (S0 (V0 W0|pvc W0|plastisol))) (T9 (S0 (V0 W0|aluminum W0|coil))) (T10 (S0 (V0 W0|marine)))) (F2 (T1 (S0 (V0 W0|coating W0|services))) (T2 (S0 (V0 W0|polyvinyl W0|chloride W0|plastisol))) (T9 (S0 (V0 W0|aluminium W0|coil))) (T10 (S0 (V0 W0|marine)))) (F2 (T1 (S0 (V0 W0|coating W0|services))) (T2 (S0 (V0 W0|polyvinyl W0|chloride W0|plastisol))) (T9 (S0 (V0 W0|aluminum W0|coil))) (T10 (S0 (V0 W0|marine)))) (F2 (T1 (S0 (V0 W0|coating))) (T2 (S0 (V0 W0|pvc W0|plastisol))) (T9 (S0 (V0 W0|aluminium W0|coil))) (T10 (S0 (V0 W0|marine)))) (F2 (T1 (S0 (V0 W0|coating))) (T2 (S0 (V0 W0|pvc W0|plastisol))) (T9 (S0 (V0 W0|aluminum W0|coil))) (T10 (S0 (V0 W0|marine)))) (F2 (T1 (S0 (V0 W0|coating))) (T2 (S0 (V0 W0|polyvinyl W0|chloride W0|plastisol))) (T9 (S0 (V0 W0|aluminium W0|coil))) (T10 (S0 (V0 W0|marine)))) (F2 (T1 (S0 (V0 W0|coating))) (T2 (S0 (V0 W0|polyvinyl W0|chloride W0|plastisol))) (T9 (S0 (V0 W0|aluminum W0|coil))) (T10 (S0 (V0 W0|marine)))) (F0 (T0 (S0 (V0 W0|aluminium W0|coil W0|coating W0|pvc W0|plastisol W0|marine W0|applications)))) (F1 (T0 (S0 (V0 W0|aluminum W0|coil W0|coating W0|pvc W0|plastisol W0|marine W0|applications)))) (F4 (T0 (S0 (V0 W0|aluminiumspoelcoatings W0|pvc W0|plastisol W0|zeevaarttoepassingen)))) (F5 (T0 (S0 (V0 W4|bandlackierung W0|von W0|aluminium W0|pvc W0|plastisol W4|hochseeanwendungen)))) (F5 (T0 (S0 (V0 W4|bandlackierung W0|von W0|aluminium W0|pvc W0|plastisol W5|hoch W5|see W5|anwendungen)))) (F5 (T0 (S0 (V0 W5|band W5|lackierung W0|von W0|aluminium W0|pvc W0|plastisol W4|hochseeanwendungen)))) (F5 (T0 (S0 (V0 W5|band W5|lackierung W0|von W0|aluminium W0|pvc W0|plastisol W5|hoch W5|see W5|anwendungen)))) (F5 (T0 (S0 (V0 W1|coil W1|coating W0|von W0|aluminium W0|pvc W0|plastisol W4|hochseeanwendungen)))) (F5 (T0 (S0 (V0 W1|coil W1|coating W0|von W0|aluminium W0|pvc W0|plastisol W5|hoch W5|see W5|anwendungen)))))");

    result = aligner.align(osob, inputWrapper, 1);
    verifyAlignmentResult(result, 0.2, "F2", "F2 T1 S0 V0 W0 -coating[0/16] T2 S0 V0 W0 +chloride[4/4] W0 -plastisol[0/4] W0 -polyvinyl[0/4] T9 S0 V0 W0 +aluminium[4/4] W0 -coil[0/4] T10 S0 V0 W0 -marine[0/4]");

    osob = getOsob("(C111065 (F2 (T1 (S0 (V0 W0|aluminium W0|chloride))) (T2 (S0 (V0 W0|anhydrous)))) (F2 (T1 (S0 (V0 W0|aluminum W0|chloride))) (T2 (S0 (V0 W0|anhydrous)))) (F4 (T0 (S0 (V0 W0|aluminiumchloride W0|watervrij)))))");

    result = aligner.align(osob, inputWrapper, 1);
    verifyAlignmentResult(result, 0.889, "F2", "F2 T1 S0 V0 W0 +aluminium[16/16] W0 +chloride[16/16] T2 S0 V0 W0 -anhydrous[0/4]");
  }

  public void testAlignment5() {
    final OsobAligner aligner = new OsobAligner();
    final AbstractNormalizer normalizer = new GeneralNormalizer(true);

    Osob osob = null;
    InputWrapper inputWrapper = null;
    AlignmentResult result = null;

    osob = getOsob("(C99960 (F2 (T1 (S0 (V0 W0|valves))) (T2 (S0 (V0 W0|butterfly)))) (F3 (T0 (S0 (V0 W0|vlinderkleppen)))) (F4 (T0 (S0 (V0 W5|vlinder W5|kleppen)))) (F4 (T0 (S0 (V0 W0|vlinder W0|valves)))) (F4 (T0 (S0 (V0 W0|vlinder W0|hijskranen)))) (F4 (T0 (S0 (V0 W0|vlinder W0|ventielen)))) (F4 (T0 (S0 (V0 W0|vlinder W0|kranen)))) (F4 (T0 (S0 (V0 W0|vlinder W0|afsluiters)))) (F5 (T0 (S0 (V0 W0|schmetterlings W0|ventile)))) (F5 (T0 (S0 (V0 W4|schmetterlingsventile)))) (F5 (T0 (S0 (V0 W5|schmetterling W5|ventile)))))");

    // perfect match
    inputWrapper = getInputWrapper(normalizer, "Afsluiters, vlinder");
    result = aligner.align(osob, inputWrapper, 1);
    verifyAlignmentResult(result, 1.0, "F4", "F4 T0 S0 V0 W0 +afsluiters[2/2] W0 +vlinder[2/2]");
  }

  public void testIsSubset() {
    final OsobAligner aligner = new OsobAligner();
    Osob osob1 = null;
    Osob osob2 = null;
    boolean isSubset = false;

    // 2514: Adhesive Tapes, Sealing, PVC Foam
    osob1 = getOsob("(C2514 (F2 (T1 (S0 (V0 W0|adhesive W0|tapes))) (T2 (S0 (V0 W0|sealing))) (T3 (S0 (V0 W0|pvc W0|foam)))) (F2 (T1 (S0 (V0 W0|adhesive W0|tapes))) (T2 (S0 (V0 W0|sealing))) (T3 (S0 (V0 W0|polyvinyl W0|chloride W0|foam)))) (F2 (T1 (S0 (V0 W0|cohesive W0|tapes))) (T2 (S0 (V0 W0|sealing))) (T3 (S0 (V0 W0|pvc W0|foam)))) (F2 (T1 (S0 (V0 W0|cohesive W0|tapes))) (T2 (S0 (V0 W0|sealing))) (T3 (S0 (V0 W0|polyvinyl W0|chloride W0|foam)))) (F2 (T1 (S0 (V0 W0|glue W0|tapes))) (T2 (S0 (V0 W0|sealing))) (T3 (S0 (V0 W0|pvc W0|foam)))) (F2 (T1 (S0 (V0 W0|glue W0|tapes))) (T2 (S0 (V0 W0|sealing))) (T3 (S0 (V0 W0|polyvinyl W0|chloride W0|foam)))))");  // 2514
    // 153717: PVC
    osob2 = getOsob("(C153717 (F2 (T1 (S0 (V0 W0|pvc)))) (F2 (T1 (S0 (V0 W0|polyvinyl W0|chloride)))))");  // 153717

    isSubset = aligner.isSubset(osob2, osob1);
    assertFalse(isSubset);  // pvc isn't at the end of its term

    // 2299: Adhesive Tapes, Foam
    osob2 = getOsob("(C2299 (F2 (T1 (S0 (V0 W0|adhesive W0|tapes))) (T2 (S0 (V0 W0|foam)))) (F2 (T1 (S0 (V0 W0|cohesive W0|tapes))) (T2 (S0 (V0 W0|foam)))) (F2 (T1 (S0 (V0 W0|glue W0|tapes))) (T2 (S0 (V0 W0|foam)))))");  // 2299

    isSubset = aligner.isSubset(osob2, osob1);
    assertTrue(isSubset);  // foam is at the end of its term
  }

  public void testSingleWordAlignment() {
    final OsobAligner aligner = new OsobAligner();
    final AbstractNormalizer normalizer = new GeneralNormalizer(true);

    Osob osob = null;
    InputWrapper inputWrapper = null;
    AlignmentResult result = null;

    osob = getOsob("(C152584 (F2 (T1 (S0 (V0 W0|truncheons)))) (F2 (T1 (S0 (V0 W0|batons)))) (F2 (T1 (S0 (V0 W0|night W0|sticks)))) (F2 (T1 (S0 (V0 W0|nightsticks)))) (F5 (T0 (S0 (V0 W4|schlagstoecke)))) (F5 (T0 (S0 (V0 W5|schlag W5|stoecke)))) (F5 (T0 (S0 (V0 W0|mechanischer W0|stoSs W0|stoecke)))))");

    // perfect match
    inputWrapper = getInputWrapper(normalizer, "Batons");
    result = aligner.align(osob, inputWrapper, 1);
    verifyAlignmentResult(result, 1.0, "F2", "F2 T1 S0 V0 W0 +batons[16/16]");
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestOsobAligner.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
