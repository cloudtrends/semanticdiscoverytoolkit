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


import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Enumeration of WordNet pointer symbols.
 * <p>
 * See WordNet-3.0/doc/pdf/wninput.5.pdf
 *
 * @author Spence Koehler
 */
public enum PointerSymbol {
  
  ANTONYM("!", new POS[]{POS.NOUN, POS.VERB, POS.ADJ, POS.ADV}),
  HYPERNYM("@", new POS[]{POS.NOUN, POS.VERB}),
  INSTANCE_HYPERNYM("@i", new POS[]{POS.NOUN}),
  HYPONYM("~", new POS[]{POS.NOUN, POS.VERB}),
  INSTANCE_HYPONYM("~i", new POS[]{POS.NOUN}),
  MEMBER_HOLONYM("#m", new POS[]{POS.NOUN}),
  SUBSTANCE_HOLONYM("#s", new POS[]{POS.NOUN}),
  PART_HOLONYM("#p", new POS[]{POS.NOUN}),
  MEMBER_MERONYM("%m", new POS[]{POS.NOUN}),
  SUBSTANCE_MERONYM("%s", new POS[]{POS.NOUN}),
  PART_MERONYM("%p", new POS[]{POS.NOUN}),
  ATTRIBUTE("=", new POS[]{POS.NOUN, POS.ADJ}),
  DERIVATIONALLY_RELATED_FORM("+", new POS[]{POS.NOUN, POS.VERB, POS.ADJ}),
  DOMAIN_OF_SYNSET_TOPIC(";c", new POS[]{POS.NOUN, POS.VERB, POS.ADJ, POS.ADV}),
  MEMBER_OF_THIS_DOMAIN_TOPIC("-c", new POS[]{POS.NOUN}),
  DOMAIN_OF_SYNSET_REGION(";r", new POS[]{POS.NOUN, POS.VERB, POS.ADJ, POS.ADV}),
  MEMBER_OF_THIS_DOMAIN_REGION("-r", new POS[]{POS.NOUN}),
  DOMAIN_OF_SYNSET_USAGE(";u", new POS[]{POS.NOUN, POS.VERB, POS.ADJ, POS.ADV}),
  MEMBER_OF_THIS_DOMAIN_USAGE("-u", new POS[]{POS.NOUN}),

  ENTAILMENT("*", new POS[]{POS.VERB}),
  CAUSE(">", new POS[]{POS.VERB}),
  ALSO_SEE("^", new POS[]{POS.VERB, POS.ADJ}),
  VERB_GROUP("$", new POS[]{POS.VERB}),

  SIMILAR_TO("&", new POS[]{POS.ADJ}),
  PARTICIPLE_OF_VERB("<", new POS[]{POS.ADJ}),
  PERTAINYM("\\", new POS[]{POS.ADJ}),  // (pertains to Noun)
  DERIVED_FROM_ADJ("\\", new POS[]{POS.ADV}),

  UNKNOWN(null, new POS[]{POS.NOUN, POS.VERB, POS.ADJ, POS.ADV}),

  //
  // NOTE: Always add more types before this. (Used for testing/integrity purposes.)
  //
  //   *** REMEMBER to always add new types to the set below!
  //
  _FINAL_(null, new POS[]{});


  public final String symbol;
  public final POS[] partsOfSpeech;

  PointerSymbol(String symbol, POS[] partsOfSpeech) {
    this.symbol = symbol;
    this.partsOfSpeech = partsOfSpeech;
  }

  private static final Set<PointerSymbol> pointerSymbols = new LinkedHashSet<PointerSymbol>();
  private static final Map<POS, Map<String, PointerSymbol>> pos2symbol2pointer = new HashMap<POS, Map<String, PointerSymbol>>();
  private static final Map<PointerSymbol, PointerSymbol> reflect = new HashMap<PointerSymbol, PointerSymbol>();

  static {
    pointerSymbols.add(ANTONYM);
    pointerSymbols.add(HYPERNYM);
    pointerSymbols.add(INSTANCE_HYPERNYM);
    pointerSymbols.add(HYPONYM);
    pointerSymbols.add(INSTANCE_HYPONYM);
    pointerSymbols.add(MEMBER_HOLONYM);
    pointerSymbols.add(SUBSTANCE_HOLONYM);
    pointerSymbols.add(PART_HOLONYM);
    pointerSymbols.add(MEMBER_MERONYM);
    pointerSymbols.add(SUBSTANCE_MERONYM);
    pointerSymbols.add(PART_MERONYM);
    pointerSymbols.add(ATTRIBUTE);
    pointerSymbols.add(DERIVATIONALLY_RELATED_FORM);
    pointerSymbols.add(DOMAIN_OF_SYNSET_TOPIC);
    pointerSymbols.add(MEMBER_OF_THIS_DOMAIN_TOPIC);
    pointerSymbols.add(DOMAIN_OF_SYNSET_REGION);
    pointerSymbols.add(MEMBER_OF_THIS_DOMAIN_REGION);
    pointerSymbols.add(DOMAIN_OF_SYNSET_USAGE);
    pointerSymbols.add(MEMBER_OF_THIS_DOMAIN_USAGE);

    pointerSymbols.add(ENTAILMENT);
    pointerSymbols.add(CAUSE);
    pointerSymbols.add(ALSO_SEE);
    pointerSymbols.add(VERB_GROUP);
    pointerSymbols.add(DERIVATIONALLY_RELATED_FORM);

    pointerSymbols.add(SIMILAR_TO);
    pointerSymbols.add(PARTICIPLE_OF_VERB);
    pointerSymbols.add(PERTAINYM);
    pointerSymbols.add(DERIVED_FROM_ADJ);

    pointerSymbols.add(UNKNOWN);

    for (PointerSymbol pointerSymbol : pointerSymbols) {
      for (POS partOfSpeech : pointerSymbol.partsOfSpeech) {
        Map<String, PointerSymbol> symbol2pointer = pos2symbol2pointer.get(partOfSpeech);
        if (symbol2pointer == null) {
          symbol2pointer = new HashMap<String, PointerSymbol>();
          pos2symbol2pointer.put(partOfSpeech, symbol2pointer);
        }
        symbol2pointer.put(pointerSymbol.symbol, pointerSymbol);
      }
    }

    reflect.put(ANTONYM, ANTONYM);
    reflect.put(HYPONYM, HYPERNYM);
    reflect.put(HYPERNYM, HYPONYM);
    reflect.put(MEMBER_HOLONYM, MEMBER_MERONYM);
    reflect.put(MEMBER_MERONYM, MEMBER_HOLONYM);
    reflect.put(SUBSTANCE_HOLONYM, SUBSTANCE_MERONYM);
    reflect.put(SUBSTANCE_MERONYM, SUBSTANCE_HOLONYM);
    reflect.put(PART_HOLONYM, PART_MERONYM);
    reflect.put(PART_MERONYM, PART_HOLONYM);
    reflect.put(SIMILAR_TO, SIMILAR_TO);
    reflect.put(ATTRIBUTE, ATTRIBUTE);
    reflect.put(VERB_GROUP, VERB_GROUP);
    reflect.put(DERIVATIONALLY_RELATED_FORM, DERIVATIONALLY_RELATED_FORM);
    reflect.put(DOMAIN_OF_SYNSET_TOPIC, MEMBER_OF_THIS_DOMAIN_TOPIC);
    reflect.put(MEMBER_OF_THIS_DOMAIN_TOPIC, DOMAIN_OF_SYNSET_TOPIC);
    reflect.put(DOMAIN_OF_SYNSET_REGION, MEMBER_OF_THIS_DOMAIN_REGION);
    reflect.put(MEMBER_OF_THIS_DOMAIN_REGION, DOMAIN_OF_SYNSET_REGION);
    reflect.put(DOMAIN_OF_SYNSET_USAGE, MEMBER_OF_THIS_DOMAIN_USAGE);
    reflect.put(MEMBER_OF_THIS_DOMAIN_USAGE, DOMAIN_OF_SYNSET_USAGE);
  }

  /**
   * Get this pointer symbol's reflexive symbol.
   */
  public PointerSymbol reflect() {
    return reflect.get(this);
  }

  /**
   * Get the pointerSymbol for the part of speech and symbol.
   */
  public static final PointerSymbol getPointerSymbol(POS partOfSpeech, String symbol) {
    PointerSymbol result = null;

    final Map<String, PointerSymbol> symbol2pointer = pos2symbol2pointer.get(partOfSpeech);
    if (symbol2pointer != null) {
      result = symbol2pointer.get(symbol);
    }

    return result;
  }
}
