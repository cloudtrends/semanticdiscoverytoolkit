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

import java.util.Collection;
import java.util.TreeSet;

/**
 * Class to encapsulate processes and structures to align input with an osob
 * for scoring.
 * <p>
 * @author Spence Koehler
 */
public class OsobAligner {

  private final FormAligner formAligner;

  /**
   * Create an instance of an osob aligner.
   * <p>
   * NOTE: an instance is NOT thread-safe. Each thread must obtain a new instance!
   */
  public OsobAligner() {
    this.formAligner = new FormAligner();
  }

  /**
   * Align the input with the match concept.
   * <p>
   * The strategy here is to be as tight and fast as possible in finding the
   * best alignment of the input to the match object. Once the best alignment
   * is found, detailed alignment information for an explanation, etc. can be
   * (re-)extracted. The goal is to minimize object creation, etc.
   * <p>
   * It is assumed that the match concept terms have been sorted within each
   * variant and that the tree has been built fully expanded such that all
   * combinatorics have already been applied.
   */
  public AlignmentResult align(Osob osob, InputWrapper inputWrapper, int extraWordPenalty) {

    formAligner.reset(inputWrapper);

    // align across all forms (or until a better alignment cannot be found)
    final Osob.Pointer pointer = osob.firstPointer();
    while (pointer.hasData()) {
      formAligner.alignForm(pointer, false, extraWordPenalty);

      if (formAligner.bestScore >= 0.95) {
        break;  // early-out. don't need to keep looking for a better score.
      }
    }

    // construct an alignment result with the best
    MatchResult result = null;

    if (formAligner.bestFormOffset > 0 && formAligner.bestScore > 0.0) {
      result = new MatchResult(osob, formAligner.inputWrapper, formAligner.bestFormOffset, formAligner.bestScore, extraWordPenalty);
    }

    return result;
  }

  public final boolean isSubset(Osob subOsob, Osob supOsob) {

    final DualPointer dualPointer = new DualPointer(subOsob, supOsob);
      
    do {
      if (isSubForm(dualPointer)) {
        return true;
      }
      else {
        dualPointer.increment(false, false, false, true);
      }
    } while (dualPointer.hasData());

    return false;
  }

  private final boolean isSubForm(DualPointer dualPointer) {

    boolean result = false;

    do {
      final boolean isSubTerm = isSubTerm(dualPointer);

      if (isSubTerm) {
        dualPointer.increment(true, true, false, false);
        result = true;
      }
      else {
        if (dualPointer.increment(false, true, false, false)) {
          result = false;
        }
      }

    } while (!dualPointer.atNextSupForm() && !dualPointer.atNextSubForm());

    if (!dualPointer.atNextSubForm()) result = false;

    return result;
  }

  private final boolean isSubTerm(DualPointer dualPointer) {

    boolean result = false;
    int maxSupIndex = -1;
    int numSupTerms = 0;

    do {
      final int compare = dualPointer.compareWord();

      if (compare > 0) {
        // inc sup word and try again.
        dualPointer.incSupPointer();
        ++numSupTerms;
      }
      else if (compare < 0) {
        // non-match.
        result = false;
        dualPointer.incSupPointer();
        break;
      }
/*
      else {  // compare == 0
        // match! inc both.
        result = true;
        dualPointer.incSupPointer();
        dualPointer.incSubPointer();
      }
    } while (!dualPointer.atNextSupTerm() && !dualPointer.atNextSubTerm());

    if (!dualPointer.atNextSubTerm()) result = false;  // must completely consume the sub term
*/
///*
      else {  // compare == 0
        final int curSupIndex = dualPointer.supPointer.getWordNum();
        if (curSupIndex > maxSupIndex) maxSupIndex = curSupIndex;

        // match! inc both.
        dualPointer.incSupPointer();
        dualPointer.incSubPointer();
        ++numSupTerms;

        if (dualPointer.atNextSubTerm()) {

          // inc to end of sup term to get the total number of terms.
          while (!dualPointer.atNextSupTerm()) {
            dualPointer.incSupPointer();
            ++numSupTerms;
          }


          if (maxSupIndex == numSupTerms - 1) {
            // must completely consume the sub term
            // sub term hasta match the last word in a super term

            result = true;
            break;
          }
        }
      }
    } while (!dualPointer.atNextSupTerm() && !dualPointer.atNextSubTerm());
//*/

    return result;
  }

  private static final class DualPointer {
    private Osob subOsob;
    private Osob supOsob;
    private Osob.Pointer subPointer;
    private Osob.Pointer supPointer;
    private int subOffset;
    private int supOffset;

    DualPointer(Osob subOsob, Osob supOsob) {
      this.subOsob = subOsob;
      this.supOsob = supOsob;

      this.subPointer = subOsob.firstPointer();
      this.supPointer = supOsob.firstPointer();

      this.subOffset = subPointer.getOffset();
      this.supOffset = supPointer.getOffset();
    }

    /**
     * Compare current sub to sup words.
     */
    public int compareWord() {
      return subPointer.compareChars(supPointer);
    }

    public void incSubPointer() {
      subPointer.increment();
    }

    public void incSupPointer() {
      supPointer.increment();
    }

    // true if at next sub term or end
    public boolean atNextSubTerm() {
      return !subPointer.hasData() || ((subPointer.getTermType() != null) && subPointer.getOffset() > subOffset);
    }

    // true if at next sup term or end
    public boolean atNextSupTerm() {
      return !supPointer.hasData() || (supPointer.getTermType() != null);
    }

    // true if at next sub form or end
    public boolean atNextSubForm() {
      return !subPointer.hasData() || ((subPointer.getFormType() != null) && subPointer.getOffset() > subOffset);
    }

    // true if at next sup form or end
    public boolean atNextSupForm() {
      return !supPointer.hasData() || (supPointer.getFormType() != null);
    }

    public boolean hasData() {
      return subPointer.hasData() && supPointer.hasData();
    }

    public boolean increment(boolean incSubTerm, boolean incSupTerm, boolean incSubForm, boolean incSupForm) {
      boolean result = false;

      // inc sup
      if (incSupTerm) {
        while (supPointer.hasData() && supPointer.getTermType() == null) supPointer.increment();
      }
      if (incSupForm) {
        while (supPointer.hasData() && supPointer.getFormType() == null) supPointer.increment();
      }

      // when sup runs out, start sup over and inc sub
      if (!supPointer.hasData() && subPointer.hasData()) {
        supPointer = supOsob.getPointer(supOffset);
        if (!(incSubForm || incSubTerm)) {
          subPointer.increment();
          incSubForm = true;
          result = true;
        }
      }

      // inc sub
      if (incSubTerm) {
        while (subPointer.hasData() && subPointer.getTermType() == null) subPointer.increment();
      }
      if (incSubForm) {
        while (subPointer.hasData() && subPointer.getFormType() == null) subPointer.increment();
      }
      if (!incSubTerm && !incSubForm) {
        // bounce back to beginning of current term.
        subPointer = subOsob.getPointer(subOffset);
      }

      // when sub runs out, we're done.
      subOffset = subPointer.getOffset();

      return result;
    }
  }

  public static final class MatchResult implements AlignmentResult {
    
    private Osob.Pointer pointer;        //note: is released after incremented.
    private InputWrapper inputWrapper;   //note: is released after consumed.
    private double score;
    private String form;
    private int extraWordPenalty;

    private String _normalizedAlignedString;  // lazy load
    private String _originalAlignedString;    // lazy load
    private String _explanation;              // lazy load
    private Collection<InputWrapper.SortableWord> _inputWords;  // lazy load
    private AlignedOsobWord _firstWord;                         // lazy load

    MatchResult(Osob osob, InputWrapper inputWrapper, int variantOffset, double score, int extraWordPenalty) {
      this.pointer = osob.getPointer(variantOffset);
      this.inputWrapper = inputWrapper;
      this.score = score;
      this.form = "F" + Integer.toString(pointer.getFormType().ordinal());
      this.extraWordPenalty = extraWordPenalty;

      final int primaryPenalty = getPrimaryPenalty();
      if (primaryPenalty > 0) {
        score = score / (primaryPenalty + 1);
      }
    }

    /**
     * Disqualify this result by setting the score to 0.
     * <p>
     * This would apply, for example, if the normalized input string is
     * a stopword.
     */
    public void disqualify() {
      this.score = 0.0;
    }

    /**
     * Get a string for just the aligned text.
     */
    public String getAlignedString(boolean normalized) {
      return normalized ? getNormalizedAlignedString() : getOriginalAlignedString();
    }

    public double getScore() {
      return score;
    }
  
    public String getExplanation() {
      if (_explanation == null) {
        _explanation = createExplanation();
      }
      return _explanation;
    }

    // get best-matched form as "F" + form ordinal. (i.e. "F2").
    public String getForm() {
      return form;
    }

    private final String getNormalizedAlignedString() {
      if (_normalizedAlignedString == null) {
        _normalizedAlignedString = createNormalizedAlignedString();
      }
      return _normalizedAlignedString;
    }

    private final String getOriginalAlignedString() {
      if (_originalAlignedString == null) {
        _originalAlignedString = createOriginalAlignedString();
      }
      return _originalAlignedString;
    }

    private final String createNormalizedAlignedString() {
      final StringBuilder result = new StringBuilder();
      final Collection<InputWrapper.SortableWord> inputWords = getInputWords();

      for (InputWrapper.SortableWord inputWord : inputWords) {
        if (result.length() > 0) result.append(' ');
        result.append(inputWord.getNormalizedString());
      }
      return result.toString();
    }

    private final String createOriginalAlignedString() {
      final StringBuilder result = new StringBuilder();
      final Collection<InputWrapper.SortableWord> inputWords = getInputWords();

      for (InputWrapper.SortableWord inputWord : inputWords) {
        if (result.length() > 0) result.append(' ');
        result.append(inputWord.word.originalSubString);
      }
      return result.toString();
    }

    /**
     * Count the number of 'primary decomp' osob words that didn't have matching
     * input. This will be a penalty (less 1) by which the score is divided (if nonzero).
     */
    private final int getPrimaryPenalty() {
      int result = 0;

      for (AlignedOsobWord alignedWord = getFirstWord(); alignedWord != null; alignedWord = alignedWord.next()) {
        if (alignedWord.hasOsobWord() && !alignedWord.hasInputWord()) {
          final NumberedWord numberedWord = alignedWord.getOsobWord();
          final Decomp.Type termType = numberedWord.getTermType();
          if (termType != null && termType.isPrimary()) {
            ++result;
          }
        }
      }

      return result;
    }

    private final String createExplanation() {
      final StringBuilder result = new StringBuilder();

      for (AlignedOsobWord alignedWord = getFirstWord(); alignedWord != null; alignedWord = alignedWord.next()) {

        boolean extra = true;
        String word = null;

        if (alignedWord.hasOsobWord()) {
          extra = false;
          final NumberedWord numberedWord = alignedWord.getOsobWord();

          final Form.Type formType = numberedWord.getFormType();
          final Decomp.Type termType = numberedWord.getTermType();
          final Synonym.Type synType = numberedWord.getSynonymType();
          final Variant.Type varType = numberedWord.getVariantType();
          final Word.Type wordType = numberedWord.getWordType();
          word = numberedWord.getWord();

          // add type changes (note: change is indicated when type is non-null)
          if (formType != null) {
            result.append('F').append(formType.ordinal());
          }
          if (termType != null) {
            result.append(" T").append(termType.ordinal());
          }
          if (synType != null) {
            result.append(" S").append(synType.ordinal());
          }
          if (varType != null) {
            result.append(" V").append(varType.ordinal());
          }
          result.append(" W").append(wordType.ordinal()).append(' ');

          if (alignedWord.hasInputWord()) {
            // mark aligned word ('+')
            result.append('+');
          }
          else {
            // mark unaligned word ('-')
            result.append('-');
          }
        }

        if (extra) {
          // mark extra word ('*')
          result.append(" *");

          word = alignedWord.getInputWord().word.originalSubString;
        }

        // add word
        result.append(word);

        // add weight "[score/weight]" designator
        result.append('[').append(alignedWord.getScore()).append('/').append(alignedWord.getWeight()).append(']');
      }

      return result.toString();
    }

    private final Collection<InputWrapper.SortableWord> getInputWords() {
      if (_inputWords == null) {
        _inputWords = createInputWords();
      }
      return _inputWords;
    }

    private final Collection<InputWrapper.SortableWord> createInputWords() {
      // put input back into original ordering
      final TreeSet<InputWrapper.SortableWord> result = new TreeSet<InputWrapper.SortableWord>(InputWrapper.getInputOrderComparator());
      for (AlignedOsobWord alignedWord = getFirstWord(); alignedWord != null; alignedWord = alignedWord.next()) {
        if (alignedWord.hasInputWord()) {
          result.add(alignedWord.getInputWord());
        }
      }

      return result;
    }

    private final AlignedOsobWord getFirstWord() {
      if (_firstWord == null) {
        _firstWord = createFirstWord();
      }
      return _firstWord;
    }

    private final AlignedOsobWord createFirstWord() {
      final FormAligner formAligner = new FormAligner();
      formAligner.reset(inputWrapper);
      final AlignedOsobWord result = formAligner.alignForm(pointer, true, extraWordPenalty);

      // release handle to pointer; it's no longer needed.
      this.pointer = null;

      return result;
    }

    public final int compareTo(AlignmentResult other) {
      final double thisScore = this.getScore();
      final double otherScore = other.getScore();

      return
        (thisScore > otherScore) ? -1 :
        (thisScore < otherScore) ? 1 :
        0;
    }
  }

  public static final class AlignedOsobWord {

    private InputWrapper.SortableWord inputWord;
    private NumberedWord osobWord;
    private int score;
    private int weight;
    private AlignedOsobWord next;

    AlignedOsobWord(InputWrapper.SortableWord inputWord, NumberedWord osobWord, AlignedOsobWord prev, int score, int weight) {
      this.inputWord = inputWord;
      this.osobWord = osobWord;
      this.score = score;
      this.weight = weight;

      if (prev != null) prev.next = this;
    }


    public AlignedOsobWord next() {
      return next;
    }

    public boolean hasInputWord() {
      return inputWord != null;
    }

    public InputWrapper.SortableWord getInputWord() {
      return inputWord;
    }

    public boolean hasOsobWord() {
      return osobWord != null;
    }

    public NumberedWord getOsobWord() {
      return osobWord;
    }

    public int getScore() {
      return score;
    }

    public int getWeight() {
      return weight;
    }
  }

  static final class FormAligner {

    private InputWrapper inputWrapper;
    private InputWalker inputWalker;

    private int curNumerator;
    private int curDenominator;
    private int curFormOffset;
    private double bestScore;
    private int bestFormOffset;

    private Form.Type formType;
    private Decomp.Type termType;
    private Synonym.Type synType;
    private Variant.Type varType;
    private Word.Type wordType;

    private int curFormMultiplier;
    private int curTermMultiplier;
    private int curSynMultiplier;
    private int curVarMultiplier;

    private AlignedOsobWord first;
    private AlignedOsobWord prev;

    FormAligner() {
    }

    public final void reset(InputWrapper inputWrapper) {

      this.inputWrapper = inputWrapper;
      this.inputWalker = (inputWrapper.numWords() == 0) ? null : new InputWalker(inputWrapper);

      this.curNumerator = 0;
      this.curDenominator = 0;
      this.curFormOffset = 0;
      this.bestScore = 0.0;
      this.bestFormOffset = 0;

      this.formType = null;
      this.termType = null;
      this.synType = null;
      this.varType = null;
      this.wordType = null;

      this.curFormMultiplier = 0;
      this.curTermMultiplier = 0;
      this.curSynMultiplier = 0;
      this.curVarMultiplier = 0;

      this.first = null;
      this.prev = null;
    }

    public final AlignedOsobWord alignForm(Osob.Pointer pointer, boolean buildAlignedWords, int extraWordPenalty) {

      if (inputWalker == null) return null;  // nothing to align!

      first = null;
      prev = null;

      // assume pointer is at a form boundary
      curFormOffset = pointer.getOffset();

      formType = pointer.getFormType();
      termType = pointer.getTermType();
      synType = pointer.getSynonymType();
      varType = pointer.getVariantType();

      curFormMultiplier = formType.getWeightMultiplier();
      curTermMultiplier = curFormMultiplier * termType.getWeightMultiplier();
      curSynMultiplier = curTermMultiplier * synType.getWeightMultiplier();
      curVarMultiplier = curSynMultiplier * varType.getWeightMultiplier();

      while (pointer.hasData()) {

        // NOTE: assume all combinatorics have already been applied, so we don't have
        //       to keep track of a best variant or synonym; just tally the score
        //       across all form terms.
        // compare cur result to best result, keep best.

        wordType = pointer.getWordType();
        final int curWeight = wordType.getWeight() * curVarMultiplier;

        // compare words
        int value = (!inputWalker.isDone()) ? pointer.compareChars(inputWalker.getWord().wordChars) : -1;

        if (value > 0) {
          // input word is less, increment input iterator until equal or greater
          InputWrapper.SortableWord inputWord = null;
          while ((inputWord = inputWalker.increment()) != null) {
            // note: consumed words are automatically skipped
            value = pointer.compareChars(inputWord.wordChars);
            if (value <= 0) {
              // time to quit incrementing input. we'll tally match/mismatch below.
              break;
            }
            // else, we'll keep incrementing over the input. maybe some will match later.
          }
        }

        if (value == 0) {  // words match!
          // double check the match
          boolean matches = false;

          if (wordType == Word.Type.ACRONYM) {
            if (inputWalker.looksLikeAcronym()) {
              matches = true;
            }
          }
          else {
            matches = true;
          }

          if (buildAlignedWords) {
            // add aligned osob/input word
            prev = new AlignedOsobWord(inputWalker.getWord(), pointer.getNumberedWord(), prev, matches ? curWeight : 0, curWeight);
          }

          // add word score to current variant score
          // inc both osob pointer and input iterator
          inputWalker.markAsConsumed();  // mark consumed word
          inputWalker.increment();

          // tally matched osob/input word
          if (matches) curNumerator += curWeight;
          curDenominator += curWeight;
        }
        else {
          // osob word is less (or still greater), either way, increment osob pointer by continuing to loop.

          if (buildAlignedWords) {
            // add unaligned osob word
            prev = new AlignedOsobWord(null, pointer.getNumberedWord(), prev, 0, curWeight);
          }

          // tally unmatched osob word
          curDenominator += curWeight;
        }

        pointer.increment();

        if (!pointer.hasData() || pointer.getFormType() != null) {
          // we're ending the current form
          double curScore = 0.0;
          if (curDenominator > 0 && curNumerator > 0) {
            // factor in here the extra input words
            curDenominator += inputWalker.getUnconsumedWeight(extraWordPenalty);

            curScore = ((double)curNumerator) / ((double)curDenominator);
          }
          if (curScore > bestScore) {
            bestScore = curScore;
            bestFormOffset = curFormOffset;
          }

          curNumerator = 0;
          curDenominator = 0;

          if (prev != null && first == null) first = prev;
          
          if (buildAlignedWords) {
            // add all extra input words (those that weren't consumed).
            final int numExtraWords = inputWalker.getNumExtraWords();
            for (int i = 0; i < numExtraWords; ++i) {
              prev = new AlignedOsobWord(inputWalker.getExtraWord(i), null, prev, 0, inputWalker.getExtraWordWeight(i, extraWordPenalty));
            }
          }

          // reset input walker for next form.
          if (pointer.hasData()) inputWalker.reset(true);

          break;
        }

        if ((termType = pointer.getTermType()) != null) {
          curTermMultiplier = curFormMultiplier * termType.getWeightMultiplier();
        }

        if ((synType = pointer.getSynonymType()) != null) {
          curSynMultiplier = curTermMultiplier * synType.getWeightMultiplier();
        }

        if ((varType = pointer.getVariantType()) != null) {
          // restart iterating over (unconsumed) input words
          inputWalker.reset(false);
          curVarMultiplier = curSynMultiplier * varType.getWeightMultiplier();
        }
        
        if (prev != null && first == null) first = prev;
      } //while

      return first;
    }
  }

  private static final class InputWalker {

    private final InputWrapper inputWrapper;
    private final int numInputWords;
    private final boolean[] consumed;
    private int inputIndex;
    private InputWrapper.SortableWord curInputWord;

    /**
     * Initialize walker, pointing to the first input word.
     */
    InputWalker(InputWrapper inputWrapper) {
      this.inputWrapper = inputWrapper;
      this.numInputWords = inputWrapper.numWords();
      this.consumed = new boolean[numInputWords];
      this.inputIndex = 0;
      this.curInputWord = (inputIndex < numInputWords) ? inputWrapper.getSortableWord(inputIndex) : null;
    }

    /**
     * Reset this walker to the beginning of the (available) input.
     *
     * @param clearConsumed  if true, erase all consumed flags before resetting.
     */
    public final void reset(boolean clearConsumed) {
      if (clearConsumed) {
        for (int i = 0; i < consumed.length; ++i) {
          consumed[i] = false;
        }
      }
      inputIndex = 0;
      while (inputIndex < numInputWords && consumed[inputIndex]) ++inputIndex;
      curInputWord = (inputIndex < numInputWords) ? inputWrapper.getSortableWord(inputIndex) : null;
    }

    /**
     * Mark the current input word as being consumed or unavailable.
     */
    public final void markAsConsumed() {
      if (inputIndex < numInputWords) {
        consumed[inputIndex] = true;
      }
    }

    public final boolean looksLikeAcronym() {
      return curInputWord.looksLikeAcronym();
    }

    /**
     * Get the current input word.
     */
    public final InputWrapper.SortableWord getWord() {
      return curInputWord;
    }

    /**
     * @return true if the end of available input has been reached.
     */
    public final boolean isDone() {
      return (curInputWord == null);
    }

    /**
     * Increment to the and return the next available input word.
     * <p>
     * @return the next input word or null if the input has been consumed.
     */
    public final InputWrapper.SortableWord increment() {
      if (inputIndex < numInputWords) {
        ++inputIndex;
        while (inputIndex < numInputWords && consumed[inputIndex]) ++inputIndex;
      }
      curInputWord = (inputIndex < numInputWords) ? inputWrapper.getSortableWord(inputIndex) : null;
      return curInputWord;
    }

    /**
     * Get the total weight of all unconsumed input words.
     */
    public final int getUnconsumedWeight(int extraWordPenalty) {
      int result = 0;

      for (int i = 0; i < consumed.length; ++i) {
        if (!consumed[i]) {
//todo: penalize 'inputWrapper.getSortableWord(i);' differently for numbers, adjectives, trade names, etc.
          result += extraWordPenalty;
        }
      }

      return result;
    }

    /**
     * Get the number of unconsumed words.
     */
    public final int getNumExtraWords() {
      int result = 0;

      for (boolean isConsumed : consumed) {
        if (!isConsumed) ++result;
      }

      return result;
    }

    /**
     * Get the designated unconsumed word.
     */
    public final InputWrapper.SortableWord getExtraWord(int index) {
      InputWrapper.SortableWord result = null;
      int resultIndex = 0;

      for (int i = 0; i < consumed.length; ++i) {
        if (!consumed[i]) {
          if (resultIndex == index) {
            result = inputWrapper.getSortableWord(i);
            break;
          }
          ++resultIndex;
        }
      }

      return result;
    }

    /**
     * Get the designated unconsumed word's weight.
     */
    public final int getExtraWordWeight(int index, int extraWordPenalty) {
//todo: penalize 'getExtraWord(index)' differently for numbers, adjectives, trade names, etc.
      return extraWordPenalty;
    }
  }
}
