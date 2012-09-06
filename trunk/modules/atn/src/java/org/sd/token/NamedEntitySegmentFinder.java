/*
    Copyright 2011 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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
package org.sd.token;


import java.util.ArrayList;
import java.util.List;
import org.sd.xml.DataProperties;

/**
 * A segment pointer finder for segmenting potential named entities in a single
 * sentence from non-named entities.
 * <p>
 * The idea here is not to necessarily catch all named entities, but to mark
 * runs of text that may be or may contain one or more multi-token named
 * entities. Single-token named entities at the beginning of an input string
 * will not be identified through this class.
 *
 * @author Spence Koehler
 */
public class NamedEntitySegmentFinder extends WordFinder {
  
  public static final String ENTITY_LABEL = "entity";
  public static final String NON_ENTITY_LABEL = "nonEntity";

  public enum SegmentType { ENTITY, NON_ENTITY, BLOCK };


  public static final StandardTokenizerOptions DEFAULT_TOKENIZER_OPTIONS = new StandardTokenizerOptions();
  static {
    DEFAULT_TOKENIZER_OPTIONS.setRevisionStrategy(TokenRevisionStrategy.SO);
    DEFAULT_TOKENIZER_OPTIONS.setLowerUpperBreak(Break.ZERO_WIDTH_SOFT_BREAK);
    DEFAULT_TOKENIZER_OPTIONS.setUpperLowerBreak(Break.NO_BREAK);
    DEFAULT_TOKENIZER_OPTIONS.setUpperDigitBreak(Break.ZERO_WIDTH_SOFT_BREAK);
    DEFAULT_TOKENIZER_OPTIONS.setLowerDigitBreak(Break.ZERO_WIDTH_SOFT_BREAK);
    DEFAULT_TOKENIZER_OPTIONS.setDigitUpperBreak(Break.ZERO_WIDTH_SOFT_BREAK);
    DEFAULT_TOKENIZER_OPTIONS.setDigitLowerBreak(Break.ZERO_WIDTH_SOFT_BREAK);
    DEFAULT_TOKENIZER_OPTIONS.setNonEmbeddedDoubleDashBreak(Break.SINGLE_WIDTH_HARD_BREAK);
    DEFAULT_TOKENIZER_OPTIONS.setEmbeddedDoubleDashBreak(Break.SINGLE_WIDTH_HARD_BREAK);
    DEFAULT_TOKENIZER_OPTIONS.setEmbeddedDashBreak(Break.SINGLE_WIDTH_SOFT_BREAK);
    DEFAULT_TOKENIZER_OPTIONS.setLeftBorderedDashBreak(Break.SINGLE_WIDTH_HARD_BREAK);
    DEFAULT_TOKENIZER_OPTIONS.setRightBorderedDashBreak(Break.SINGLE_WIDTH_HARD_BREAK);
    DEFAULT_TOKENIZER_OPTIONS.setFreeStandingDashBreak(Break.SINGLE_WIDTH_HARD_BREAK);
    DEFAULT_TOKENIZER_OPTIONS.setWhitespaceBreak(Break.SINGLE_WIDTH_SOFT_BREAK);
    DEFAULT_TOKENIZER_OPTIONS.setQuoteAndParenBreak(Break.SINGLE_WIDTH_SOFT_BREAK);
    DEFAULT_TOKENIZER_OPTIONS.setSymbolBreak(Break.SINGLE_WIDTH_HARD_BREAK);
    DEFAULT_TOKENIZER_OPTIONS.setSlashBreak(Break.SINGLE_WIDTH_HARD_BREAK);
    DEFAULT_TOKENIZER_OPTIONS.setEmbeddedApostropheBreak(Break.NO_BREAK);
    DEFAULT_TOKENIZER_OPTIONS.setEmbeddedPunctuationBreak(Break.SINGLE_WIDTH_HARD_BREAK);
  }

  // segment finder factory with flag to turn off caps change triggering new entity
  public static final 
    SegmentPointerFinderFactory NAMED_ENTITY_SEGMENT_FINDER_FACTORY =
    new NamedEntitySegmentFinderFactory(false);
  public static final 
    SegmentPointerFinderFactory NAMED_ENTITY_SEGMENT_FINDER_IGNORE_CAPS_CHANGE_FACTORY =
    new NamedEntitySegmentFinderFactory(true);

  public static SegmentPointerFinderFactory getFactory() { return getFactory(false); }
  public static SegmentPointerFinderFactory getFactory(boolean ignoreCapsChange) {
    return (ignoreCapsChange ? 
            NAMED_ENTITY_SEGMENT_FINDER_IGNORE_CAPS_CHANGE_FACTORY : 
            NAMED_ENTITY_SEGMENT_FINDER_FACTORY);
  }

  public static SegmentTokenizer getSegmentTokenizer(String input, 
                                                     boolean ignoreCapsChange,
                                                     DataProperties dataProperties) 
  {
    return new SegmentTokenizer(new NamedEntitySegmentFinder(input, ignoreCapsChange), 
                                DEFAULT_TOKENIZER_OPTIONS, dataProperties);
  }

  private final boolean ignoreCapsChange;
  public NamedEntitySegmentFinder(String input) {
    this(input, false);
  }
  public NamedEntitySegmentFinder(String input, boolean ignoreCapsChange) {
    super(input);
    this.ignoreCapsChange = ignoreCapsChange;
  }

  /**
   * Partition into entity/non-entity segments. Note that we are assuming
   * single-sentence inputs and are ignoring potential single-word entities
   * that occur at the beginning of the input.
   * <p>
   * NOTES:
   * <ul>
   * <li>Segments will be labeled as ENTITY or NON_ENTITY.</li>
   * <li>Segments will be numbered in the order requested by findStartPtr.</li>
   * <li>Potential named entities are identified as runs of capitalized words,
   *     possibly interrupted by parenthetical or quoted blocks.</li>
   * <li>Single capitalized word at the beginning of the string are not
   *     labeled as an ENTITY but become part of a NON_ENTITY result.</li>
   * <li>Multi-token parenthetical and quoted blocks that aren't found after
   *     capitalized word(s) are marked as NON_ENTITY segments.</li>
   * </ul>
   */
  public SegmentPointer findSegmentPointer(int startPtr) {
    SegmentPointer result = null;

    final BlockRecognizer blockRecognizer = buildBlockRecognizer();

    SegmentPointer priorSegment = null;
    SegmentType priorType = null;
    String label = NON_ENTITY_LABEL;
    int entityCount = 0;
    NamedEntityCollector collector = null;

    for (SegmentPointer curWordSegment = super.findSegmentPointer(startPtr);
         curWordSegment != null;
         curWordSegment = super.findSegmentPointer(skipToNonWhite(curWordSegment.getEndPtr() + 1))) {

      final SegmentType curType = determineSegmentType(curWordSegment, blockRecognizer);

//System.out.println("\tconsidering curWord: " + curWordSegment + " curType=" + curType + " priorType=" + priorType);

      if (curType == SegmentType.ENTITY) {
        ++entityCount;
      }

      if (priorType == null) {
        priorType = curType;

        if (curType == SegmentType.ENTITY) {
          label = ENTITY_LABEL;
        }
      }
      else {
        if (curType != priorType) {  // current has changed from prior
          boolean endsWithPrior = false;
          switch (priorType) {
            case ENTITY : // switching from ENTITY
              switch (curType) {
                case BLOCK :  // to BLOCK
                  // block is to be part of entity, retaining priorType as ENTITY
                  if (collector == null) {
                    collector = new NamedEntityCollector(priorSegment, false, ignoreCapsChange);
                  }
                  collector.add(curWordSegment, true);
                  break;

                case NON_ENTITY :
                  if (entityCount == 1 && startPtr == 0) {
                    // 2nd word of sentence went from capitalized to non
                    // don't mark potential but unlikely single-word entity
                    priorType = curType;
                    label = NON_ENTITY_LABEL;
                  }
                  else {
                    // switching from ENTITY to NON
                    endsWithPrior = true;
                    label = ENTITY_LABEL;
                  }
                  break;
              }
              break;

            case BLOCK : // switching from BLOCK
              switch (curType) {
                case ENTITY :
                  endsWithPrior = true;
                  label = NON_ENTITY_LABEL;
                  break;

                default :
                  // seeing a non-block now establishes the concrete priorType
                  priorType = curType;
                  break;
              }
              break;

            case NON_ENTITY :  // switching from NON_ENTITY
              switch (curType) {
                case BLOCK :  // to BLOCK
                  // nothing to do -- allows block to be part of non-entity, retaining priorType as NON_ENTITY
                  break;

                case ENTITY :
                  // all other cases mean current non-entity ends with prior segment
                  endsWithPrior = true;
                  label = NON_ENTITY_LABEL;
                  break;
              }
              break;
          }


          if (endsWithPrior) {
            break;  // end scanning words
          }
        }
        else {  // priorType == curType
          if (priorType == SegmentType.ENTITY) {
            // continuing entity -- detect inner segment boundaries
            if (collector == null) {
              collector = new NamedEntityCollector(priorSegment, false, ignoreCapsChange);
            }
            collector.add(curWordSegment);
          }
        }
      }

      priorSegment = curWordSegment;
    }
    
    if (priorSegment != null) {
      result = new SegmentPointer(input, label, getSeqNum(), startPtr, priorSegment.getEndPtr());
      if (collector != null) {
        collector.setInnerSegments(result);
      }
    }

    return result;
  }

  protected BlockRecognizer buildBlockRecognizer() {
    return new BlockRecognizer();
  }

  protected SegmentType determineSegmentType(SegmentPointer wordSegment, BlockRecognizer blockRecognizer) {
    SegmentType result = SegmentType.NON_ENTITY;

    final String word = wordSegment.getText();
    final WordCharacteristics wc = wordSegment.getWordCharacteristics();
    boolean inBlock = !blockRecognizer.stackIsEmpty();
    int numOthers = wc.len() - wc.getNumLetters();

    // record potential start block chars from front of word
    WordCharacteristics.Type frontType = wc.getFirstType(false);
    if (frontType == WordCharacteristics.Type.OTHER) {
      final int max = wc.skip(WordCharacteristics.Type.OTHER, 0);
      numOthers += max;
      for (int idx = 0; idx < max; ++idx) {
        final char c = word.charAt(idx);
        if (blockRecognizer.isPushCandidate(c)) {
          // check for corresponding end
          if (blockRecognizer.hasPopCandidate(wordSegment.getInput(), wordSegment.getStartPtr() + idx, c)) {
            blockRecognizer.push(c);
            --numOthers;
          }
        }
      }
      frontType = wc.getFirstType(true);
    }

    // record potental end block chars from end of word
    final WordCharacteristics.Type backType = wc.getLastType(false);
    if (backType == WordCharacteristics.Type.OTHER) {
      // work back to balance out anything at the end of this word
      final int min = wc.skipBack(WordCharacteristics.Type.OTHER);
      for (int idx = min + 1; idx < wc.len(); ++idx) {
        final char c = word.charAt(idx);
        if (blockRecognizer.updateStack(c, false, true)) {
          --numOthers;
        }
      }
    }

    if (inBlock || !blockRecognizer.stackIsEmpty()) {
      result = SegmentType.BLOCK;
    }
    else {
      if (frontType == WordCharacteristics.Type.UPPER && numOthers < 3) {
        result = SegmentType.ENTITY;
      }
    }

    return result;
  }


  public static void main(String[] args) {
    final SegmentPointerFinderFactory ptrFactory = NamedEntitySegmentFinder.getFactory();
    final SequenceSegmenter segmenter = new SequenceSegmenter();

    for (String arg : args) {
      final SegmentPointer[] segments = segmenter.segment(ptrFactory, arg);
      System.out.println("Input (" + segments.length + " segments): " + arg);
      for (SegmentPointer segment : segments) {
        System.out.println("\t" + segment);
      }
    }
  }
}
