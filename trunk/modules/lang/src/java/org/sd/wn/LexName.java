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
 * Enumeration of lexicographer file names.
 * <p>
 * @author Spence Koehler
 */
public enum LexName {
  
  ADJ_ALL(0, "adj.all", "all adjective clusters"),
  ADJ_PERT(1, "adj.pert", "relational adjectives (pertainyms)"),
  ADV_ALL(2, "adv.all", "all adverbs"),
  NOUN_TOPS(3, "noun.Tops", "unique beginners for nouns"),
  NOUN_ACT(4, "noun.act", "nouns denoting acts or actions"),
  NOUN_ANIMAL(5, "noun.animal", "nouns denoting animals"),
  NOUN_ARTIFACT(6, "noun.artifact", "nouns denoting man-made objects"),
  NOUN_ATTRIBUTE(7, "noun.attribute", "nouns denoting attributes of people and objects"),
  NOUN_BODY(8, "noun.body", "nouns denoting body parts"),
  NOUN_COGNITION(9, "noun.cognition", "nouns denoting cognitive processes and contents"),
  NOUN_COMMUNICATION(10, "noun.communication", "nouns denoting communicative processes and contents"),
  NOUN_EVENT(11, "noun.event", "nouns denoting natural events"),
  NOUN_FEELING(12, "noun.feeling", "nouns denoting feelings and emotions"),
  NOUN_FOOD(13, "noun.food", "nouns denoting foods and drinks"),
  NOUN_GROUP(14, "noun.group", "nouns denoting groupings of people or objects"),
  NOUN_LOCATION(15, "noun.location", "nouns denoting spatial position"),
  NOUN_MOTIVE(16, "noun.motive", "nouns denoting goals"),
  NOUN_OBJECT(17, "noun.object", "nouns denoting natural objects (not man-made)"),
  NOUN_PERSON(18, "noun.person", "nouns denoting people"),
  NOUN_PHENOMENON(19, "noun.phenomenon", "nouns denoting natural phenomena"),
  NOUN_PLANT(20, "noun.plant", "nouns denoting plants"),
  NOUN_POSSESSION(21, "noun.possession", "nouns denoting possession and transfer of possession"),
  NOUN_PROCESS(22, "noun.process", "nouns denoting natural processes"),
  NOUN_QUANTITY(23, "noun.quantity", "nouns denoting quantities and units of measure"),
  NOUN_RELATION(24, "noun.relation", "nouns denoting relations between people or things or ideas"),
  NOUN_SHAPE(25, "noun.shape", "nouns denoting two and three dimensional shapes"),
  NOUN_STATE(26, "noun.state", "nouns denoting stable states of affairs"),
  NOUN_SUBSTANCE(27, "noun.substance", "nouns denoting substances"),
  NOUN_TIME(28, "noun.time", "nouns denoting time and temporal relations"),
  VERB_BODY(29, "verb.body", "verbs of grooming, dressing and bodily care"),
  VERB_CHANGE(30, "verb.change", "verbs of size, temperature change, intensifying, etc."),
  VERB_COGNITION(31, "verb.cognition", "verbs of thinking, judging, analyzing, doubting"),
  VERB_COMMUNICATION(32, "verb.communication", "verbs of telling, asking, ordering, singing"),
  VERB_COMPETITION(33, "verb.competition", "verbs of fighting, athletic activities"),
  VERB_CONSUMPTION(34, "verb.consumption", "verbs of eating and drinking"),
  VERB_CONTACT(35, "verb.contact", "verbs of touching, hitting, tying, digging"),
  VERB_CREATION(36, "verb.creation", "verbs of sewing, baking, painting, performing"),
  VERB_EMOTION(37, "verb.emotion", "verbs of feeling"),
  VERB_MOTION(38, "verb.motion", "verbs of walking, flying, swimming"),
  VERB_PERCEPTION(39, "verb.perception", "verbs of seeing, hearing, feeling"),
  VERB_POSSESSION(40, "verb.possession", "verbs of buying, selling, owning"),
  VERB_SOCIAL(41, "verb.social", "verbs of political and social activities and events"),
  VERB_STATIVE(42, "verb.stative", "verbs of being, having, spatial relations"),
  VERB_WEATHER(43, "verb.weather", "verbs of raining, snowing, thawing, thundering"),
  ADJ_PPL(44, "adj.ppl", "participial adjectives");

  public final int fileNum;
  public final String fileName;
  public final String description;

  LexName(int fileNum, String fileName, String description) {
    this.fileNum = fileNum;
    this.fileName = fileName;
    this.description = description;
  }

  public String getName() {
    return fileName.substring(fileName.indexOf('.') + 1);
  }

  private static final Set<LexName> lexNames = new LinkedHashSet<LexName>();
  private static final Map<Integer, LexName> fileNum2lexName = new HashMap<Integer, LexName>();

  static {
    lexNames.add(ADJ_ALL);
    lexNames.add(ADJ_PERT);
    lexNames.add(ADV_ALL);
    lexNames.add(NOUN_TOPS);
    lexNames.add(NOUN_ACT);
    lexNames.add(NOUN_ANIMAL);
    lexNames.add(NOUN_ARTIFACT);
    lexNames.add(NOUN_ATTRIBUTE);
    lexNames.add(NOUN_BODY);
    lexNames.add(NOUN_COGNITION);
    lexNames.add(NOUN_COMMUNICATION);
    lexNames.add(NOUN_EVENT);
    lexNames.add(NOUN_FEELING);
    lexNames.add(NOUN_FOOD);
    lexNames.add(NOUN_GROUP);
    lexNames.add(NOUN_LOCATION);
    lexNames.add(NOUN_MOTIVE);
    lexNames.add(NOUN_OBJECT);
    lexNames.add(NOUN_PERSON);
    lexNames.add(NOUN_PHENOMENON);
    lexNames.add(NOUN_PLANT);
    lexNames.add(NOUN_POSSESSION);
    lexNames.add(NOUN_PROCESS);
    lexNames.add(NOUN_QUANTITY);
    lexNames.add(NOUN_RELATION);
    lexNames.add(NOUN_SHAPE);
    lexNames.add(NOUN_STATE);
    lexNames.add(NOUN_SUBSTANCE);
    lexNames.add(NOUN_TIME);
    lexNames.add(VERB_BODY);
    lexNames.add(VERB_CHANGE);
    lexNames.add(VERB_COGNITION);
    lexNames.add(VERB_COMMUNICATION);
    lexNames.add(VERB_COMPETITION);
    lexNames.add(VERB_CONSUMPTION);
    lexNames.add(VERB_CONTACT);
    lexNames.add(VERB_CREATION);
    lexNames.add(VERB_EMOTION);
    lexNames.add(VERB_MOTION);
    lexNames.add(VERB_PERCEPTION);
    lexNames.add(VERB_POSSESSION);
    lexNames.add(VERB_SOCIAL);
    lexNames.add(VERB_STATIVE);
    lexNames.add(VERB_WEATHER);
    lexNames.add(ADJ_PPL);

    for (LexName lexName : lexNames) {
      fileNum2lexName.put(lexName.fileNum, lexName);
    }
  }

  /**
   * Get the lexName for the fileNum.
   */
  public static final LexName getLexName(int fileNum) {
    return fileNum2lexName.get(fileNum);
  }

  /**
   * Get the lexName for the fileName.
   */
  public static final LexName getLexName(String fileName) {
    fileName = fileName.toUpperCase().replaceAll("\\.", "_");
    return Enum.valueOf(LexName.class, fileName);
  }
}
