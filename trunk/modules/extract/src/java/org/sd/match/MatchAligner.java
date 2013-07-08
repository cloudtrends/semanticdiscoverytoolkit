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


import org.sd.nlp.AbstractNormalizer;
import org.sd.nlp.StringWrapper;
import org.sd.util.tree.Tree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Class to align input with a match concept for scoring.
 * <p>
 * @author Spence Koehler
 */
public class MatchAligner {
  
  private AbstractNormalizer normalizer;
  private List<StringWrapper.SubString> inputWords;

  private ConceptModel.MatchConcept matchConcept;
  private MatchResult _matchResult;

  public MatchAligner(AbstractNormalizer normalizer, List<StringWrapper.SubString> inputWords, ConceptModel conceptModel) {
    this.normalizer = normalizer;
    this.inputWords = inputWords;
    this.matchConcept = conceptModel.getMatchConcept();
  }

  public MatchResult getMatchResult() {
    if (_matchResult == null) {
      _matchResult = computeMatchResult();
      matchConcept = null;  // release memory
    }
    return _matchResult;
  }

  private final MatchResult computeMatchResult() {
    MatchResult bestResult = null;

    // find the best match result across the forms under the match concept.
    for (Tree<ConceptModel.Data> formNode : matchConcept.getNode().getChildren()) {
      final ConceptModel.ConceptForm form = formNode.getData().asConceptForm();
      final MatchResult curResult = computeMatchResult(form);

      if (bestResult == null || curResult.getScore() > bestResult.getScore()) {
        bestResult = curResult;

        if (bestResult.getScore() == 1.0) {
          // can't get any better
          break;
        }
      }
    }

    return bestResult;
  }

  private final MatchResult computeMatchResult(ConceptModel.ConceptForm form) {

    final VariantAlignmentContainer variantAlignmentContainer = createVariantAlignmentContainer(form);
    final TermAlignmentContainer termAlignmentContainer = createTermAlignmentContainer(variantAlignmentContainer);

    //
    // across all terms, need
    //  matchedInputWords, missingInputWords, extraInputWords, totalAlignmentScore, totalAlignmentWeight
    //
    final MatchResult result = new MatchResult();

    for (Tree<ConceptModel.Data> termNode : form.getNode().getChildren()) {
      final ConceptModel.ConceptTerm term = termNode.getData().asConceptTerm();
      final ConceptTermAlignment conceptTermAlignment = termAlignmentContainer.getConceptTermAlignment(term);

      if (conceptTermAlignment == null) {
        // for missingInputWords add totalAlignmentWeight in unmatched terms,
        //  use default variant under model with its (minimum) alignment weight.
        result.addUnalignedTerm(term);
      }
      else {
        // access matched words from conceptTermAlignment
        final OrthographicVariantAlignment bestVariant = conceptTermAlignment.getBestVariant();
        result.addAlignedVariant(bestVariant);
      }
    }

    result.addExtraWords(variantAlignmentContainer.getExtraWords());

//todo:
// if matchedInputWords normalized/concatenated == stopWord or under min size, score = 0.
// if (isBad(result.getAlignedString())) result.disqualify();

    return result;
  }

  private final VariantAlignmentContainer createVariantAlignmentContainer(ConceptModel.ConceptForm form) {
    final VariantAlignmentContainer result = new VariantAlignmentContainer();

    for (StringWrapper.SubString inputWord : inputWords) {
      final WordWrapper wordWrapper = new WordWrapper(inputWord);

      final String wordText = inputWord.getNormalizedString(normalizer);
      final List<ConceptModel.WordData> wordDatas = form.getWordDatas(wordText);

      result.add(wordWrapper, wordDatas);
    }

    return result;
  }

  private final TermAlignmentContainer createTermAlignmentContainer(VariantAlignmentContainer variantAlignmentContainer) {
    final TermAlignmentContainer result = new TermAlignmentContainer();

    final Collection<OrthographicVariantAlignment> variantAlignments = variantAlignmentContainer.getVariantAlignments();
    for (OrthographicVariantAlignment variantAlignment : variantAlignments) {
      result.add(variantAlignment);
    }

    return result;
  }


  public static final class WordWrapper implements AlignedWord {
    private StringWrapper.SubString inputWord;  // instantiated if aligned or extra
    private String dataWord;                    // instantiated if aligned or missing
    private int alignmentScore;
    private int alignmentWeight;
    private int variantId;
    private int formOrdinal;

    WordWrapper(StringWrapper.SubString inputWord) {
      this.inputWord = inputWord;
      this.alignmentScore = 0;
      this.alignmentWeight = 1;  // penalty for extra; overridden if/when aligned
    }

    WordWrapper(ConceptModel.WordData wordData, int alignmentWeight) {
      this.dataWord = wordData.getWord();
      this.variantId = wordData.getNode().getParent().getData().asOrthographicVariant().getId();
      this.formOrdinal = wordData.getConceptForm().getFormType().ordinal();
      this.alignmentScore = 0;
      this.alignmentWeight = alignmentWeight;
    }

    void setWordData(ConceptModel.WordData wordData) {
      this.dataWord = wordData.getWord();
    }

    void setAlignmentScore(int alignmentScore) {
      this.alignmentScore = alignmentScore;
    }

    void setAlignmentWeight(int alignmentWeight) {
      this.alignmentWeight = alignmentWeight;
    }

    public boolean isAligned() {
      return dataWord != null && inputWord != null;
    }

    public boolean isExtra() {
      return inputWord != null && dataWord == null;
    }

    public boolean isMissing() {
      return dataWord != null && inputWord == null;
    }

    public StringWrapper.SubString getInputWord() {
      return inputWord;
    }

    public String getDataWord() {
      return dataWord;
    }

    public int getAlignmentScore() {
      return alignmentScore;
    }

    public int getAlignmentWeight() {
      return alignmentWeight;
    }

    public int getVariantId() {
      return variantId;
    }

    public int getFormOrdinal() {
      return formOrdinal;
    }

    public String getForm() {
      return "F" + Integer.toString(formOrdinal);
    }

    public String getDescription() {
      // [+-*]string\[s/w\]
      final StringBuilder result = new StringBuilder();

      // add symbol and original input or concept string
      if (inputWord != null) {
        if (dataWord != null) {
          result.append('+');  // aligned
        }
        else {
          result.append('*');  // extra
        }

        result.append(inputWord.originalSubString);
      }
      else {                   // missing
        result.append('-').append(dataWord);
      }

      // add score info
      result.append('[').
        append(alignmentScore).
        append('/').
        append(alignmentWeight).
        append(']');

      return result.toString();
    }
  }

  public static final class MatchResult implements AlignmentResult {
    private List<WordWrapper> wordWrappers;
    private int totalAlignmentScore;
    private int totalAlignmentWeight;

    private Double _score;
    private String _explanation;
    private String _form;

    MatchResult() {
      this.wordWrappers = new ArrayList<WordWrapper>();
      this.totalAlignmentScore = 0;
      this.totalAlignmentWeight = 0;
    }

    void addUnalignedTerm(ConceptModel.ConceptTerm term) {
      final int alignmentWeight = term.getDefaultAlignmentWeight();

      // add (default) alignment weight [denominator]
      if (alignmentWeight > 0) {
        totalAlignmentWeight += alignmentWeight;

        final ConceptModel.OrthographicVariant variant = term.getDefaultVariant();
        for (Iterator<Tree<ConceptModel.Data>> wordIter = variant.getNode().getChildren().iterator(); wordIter.hasNext(); ) {
          final ConceptModel.WordData wordData = wordIter.next().getData().asWordData();
          wordWrappers.add(new WordWrapper(wordData, alignmentWeight));
        }
      }
    }

    void addAlignedVariant(OrthographicVariantAlignment variantAlignment) {
      for (int i = 0; i < variantAlignment.wordAlignments.length; ++i) {
        final WordDataAlignment wordAlignment = variantAlignment.wordAlignments[i];

        WordWrapper wordWrapper = null;
        if (wordAlignment.hasWordWrapper()) {
          // add the aligned word
          wordWrapper = wordAlignment.getWordWrapper();

          // align. i.e. link this word data alignment to the word wrapper.
          wordWrapper.setWordData(wordAlignment.wordData);

          wordWrappers.add(wordWrapper);
          wordWrapper.setAlignmentWeight(wordAlignment.getAlignmentWeight());
        }
        else {
          // add the unaligned (missed) concept word
          wordWrapper = new WordWrapper(wordAlignment.wordData, wordAlignment.getAlignmentWeight());
          wordWrappers.add(wordWrapper);
        }

        // set alignment score in word wrapper
        wordWrapper.setAlignmentScore(wordAlignment.getAlignmentScore());
        if (_form == null) _form = wordWrapper.getForm();

        // add scoring counts
        totalAlignmentScore += wordAlignment.getAlignmentScore();
        totalAlignmentWeight += wordAlignment.getAlignmentWeight();
      }
    }

    void addExtraWords(List<WordWrapper> extraWords) {
      for (WordWrapper extraWord : extraWords) {
        wordWrappers.add(extraWord);
        totalAlignmentWeight += extraWord.getAlignmentWeight();
      }
    }

    /**
     * Disqualify this result by setting the score to 0.
     * <p>
     * This would apply, for example, if the normalized input string is
     * a stopword.
     */
    public void disqualify() {
      this._score = 0.0;
    }

    /**
     * Get a string for just the aligned text.
     */
    public String getAlignedString(boolean normalized) {
      return createAlignedString(normalized);
    }

    public double getScore() {
      if (_score == null) {
        _score = ((double)totalAlignmentScore) / ((double)totalAlignmentWeight);
      }
      return _score;
    }

//todo: add an accessor to get the extra input terms?

    public String getExplanation() {
      if (_explanation == null) {
        _explanation = createExplanation();
      }
      return _explanation;
    }

    public String getForm() {
      return _form;
    }

    private final String createExplanation() {
      final StringBuilder result = new StringBuilder();

      int variant = -1;

      result.append('(');
      boolean needSpace = false;
      for (WordWrapper wordWrapper : wordWrappers) {
        final int curVariant = wordWrapper.getVariantId();
        if (curVariant != variant) {
          if (variant >= 0) {
            // close off previous section
            result.append(')');
            needSpace = true;
          }
          if (curVariant >= 0) {
            // open a new section
            if (needSpace) result.append(' ');
            result.append('(');
            needSpace = false;
          }
          variant = curVariant;
        }
        if (needSpace) result.append(' ');
        result.append(wordWrapper.getDescription());
        needSpace = true;
      }
      if (variant >= 0) result.append(')');
      result.append(')');

      return result.toString();
    }

    private final String createAlignedString(boolean normalized) {
      final StringBuilder result = new StringBuilder();
      for (WordWrapper wordWrapper : wordWrappers) {
        if (wordWrapper.isAligned()) {
          if (result.length() > 0) result.append(' ');
          if (normalized) {
            result.append(wordWrapper.getDataWord());
          }
          else {
            result.append(wordWrapper.getInputWord().originalSubString);
          }
        }
      }
      return result.toString();
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

  private final class TermAlignmentContainer {
    private Map<ConceptModel.ConceptTerm, ConceptTermAlignment> term2alignment;

    public TermAlignmentContainer() {
      this.term2alignment = new LinkedHashMap<ConceptModel.ConceptTerm, ConceptTermAlignment>();
    }

    public void add(OrthographicVariantAlignment variantAlignment) {
      final ConceptModel.ConceptTerm term = variantAlignment.variant.getNode().getParent().getParent().getData().asConceptTerm();
      ConceptTermAlignment termAlignment = term2alignment.get(term);
      if (termAlignment == null) {
        termAlignment = new ConceptTermAlignment(term);
        term2alignment.put(term, termAlignment);
      }
      termAlignment.incorporate(variantAlignment);
    }

    public ConceptTermAlignment getConceptTermAlignment(ConceptModel.ConceptTerm term) {
      return term2alignment.get(term);
    }

    public double getScore(ConceptModel.ConceptTerm term) {
      double result = 0.0;

      final ConceptTermAlignment termAlignment = term2alignment.get(term);
      if (termAlignment != null) {
        result = termAlignment.getBestVariant().getScore();
      }

      return result;
    }
  }

  private final class ConceptTermAlignment {
    public final ConceptModel.ConceptTerm term;

    private OrthographicVariantAlignment bestVariant;

    public ConceptTermAlignment(ConceptModel.ConceptTerm term) {
      this.term = term;
      this.bestVariant = null;
    }

    public OrthographicVariantAlignment getBestVariant() {
      return bestVariant;
    }

    public void incorporate(OrthographicVariantAlignment variantAlignment) {
      if (bestVariant == null) {
        bestVariant = variantAlignment;
      }
      else {
        final double bestScore = bestVariant.getScore();
        final double variantScore = variantAlignment.getScore();

        if (variantScore > bestScore) {
          bestVariant = variantAlignment;
        }
        else if (variantScore == bestScore) {
          // keep the one that consumes the most input words
          if (variantAlignment.numInputWords() > bestVariant.numInputWords()) {
            bestVariant = variantAlignment;
          }
        }
      }
    }
  }

  /**
   * Container class for a word with its associated concept WordDatas.
   */
  private final class VariantAlignmentContainer {
    private Map<ConceptModel.OrthographicVariant, OrthographicVariantAlignment> variant2alignment;
    private List<WordWrapper> extraWords;

    public VariantAlignmentContainer() {
      this.variant2alignment = new LinkedHashMap<ConceptModel.OrthographicVariant, OrthographicVariantAlignment>();
      this.extraWords = new ArrayList<WordWrapper>();
    }

    public void add(WordWrapper wordWrapper, List<ConceptModel.WordData> wordDatas) {
      if (wordDatas == null) {
        extraWords.add(wordWrapper);
      }
      else {
        for (ConceptModel.WordData wordData : wordDatas) {
          final ConceptModel.OrthographicVariant variant = wordData.getNode().getParent().getData().asOrthographicVariant();
          OrthographicVariantAlignment variantAlignment = variant2alignment.get(variant);

          if (variantAlignment == null) {
            variantAlignment = new OrthographicVariantAlignment(variant);
            variant2alignment.put(variant, variantAlignment);
          }
          variantAlignment.add(wordWrapper, wordData);
        }
      }
    }

    public Collection<OrthographicVariantAlignment> getVariantAlignments() {
      return variant2alignment.values();
    }

    public List<WordWrapper> getExtraWords() {
      return extraWords;
    }
  }

  private final class OrthographicVariantAlignment {
    public final ConceptModel.OrthographicVariant variant;
    public final WordDataAlignment[] wordAlignments;  // one for each variant word, not input word

    private Double _score;
    private Integer _numInputWords;

    OrthographicVariantAlignment(ConceptModel.OrthographicVariant variant) {
      this.variant = variant;

      final List<Tree<ConceptModel.Data>> wordNodes = variant.getNode().getChildren();
      this.wordAlignments = new WordDataAlignment[wordNodes.size()];

      int index = 0;
      for (Tree<ConceptModel.Data> wordNode : wordNodes) {
        final ConceptModel.WordData wordData = wordNode.getData().asWordData();
        wordAlignments[index] = new WordDataAlignment(wordData);
        ++index;
      }
    }

    void add(WordWrapper wordWrapper, ConceptModel.WordData wordData) {
      //NOTE: need to use instance equality, not object (.equals) equality in this impl.

      WordDataAlignment theAlignment = null;
      for (WordDataAlignment wordAlignment : wordAlignments) {
        // if we've already added this input word wrapper, ignore it.
        if (wordWrapper == wordAlignment.getWordWrapper()) {
          theAlignment = null;
          break;
        }
        // find the alignment corresponding to the wordData
        if (theAlignment == null && wordData == wordAlignment.wordData) {
          if (!wordAlignment.hasWordWrapper()) {
            theAlignment = wordAlignment;
            //NOTE: keep looping to make sure we haven't already added this inputWord to another wordData.
          }
          else {
            // already has an inputWord. don't add this combo.
            break;
          }
        }
      }
      if (theAlignment != null) {
        theAlignment.setWordWrapper(wordWrapper);
      }
    }

    int numInputWords() {
      if (_numInputWords == null) {
        int numInputWords = 0;
        for (WordDataAlignment wordAlignment : wordAlignments) {
          if (wordAlignment.hasWordWrapper()) ++numInputWords;
        }
        _numInputWords = numInputWords;
      }
      return _numInputWords;
    }

    double getScore() {
      if (_score == null) {
        int numerator = 0;
        int denominator = 0;

        for (WordDataAlignment wordAlignment : wordAlignments) {
          numerator += wordAlignment.getAlignmentScore();
          denominator += wordAlignment.getAlignmentWeight();
        }

        _score = ((double)numerator) / ((double)denominator);
      }
      return _score;
    }
  }

  private final class WordDataAlignment {
    public final ConceptModel.WordData wordData;
    private WordWrapper wordWrapper;

    private Integer _alignmentWeight;

    public WordDataAlignment(ConceptModel.WordData wordData) {
      this.wordData = wordData;
      this.wordWrapper = null;
      this._alignmentWeight = null;
    }

    public void setWordWrapper(WordWrapper wordWrapper) {
      this.wordWrapper = wordWrapper;
    }

    public WordWrapper getWordWrapper() {
      return wordWrapper;
    }

    public boolean hasWordWrapper() {
      return wordWrapper != null;
    }

    /**
     * Get the weight of this alignment within its orthographic variant as
     * a function of the word, variant, synonym, and decomposition types;
     * take into account whether we have input to match against.
     */
    public int getAlignmentWeight() {
      if (_alignmentWeight == null) {
        _alignmentWeight = computeAlignmentWeight();
      }

      return _alignmentWeight;
    }

    private final int computeAlignmentWeight() {
      // get the base weight, considering the Word.Type's weight (i.e. functional -vs- normal).
      int result = wordData.getWeightMultiplier();

      // If there is an input word to match against, the weight multiplier up the
      // type hierarchy for multiplier, m, is min(1, m); otherwise, the multiplier
      // is m.
      for (Tree<ConceptModel.Data> node = wordData.getNode().getParent();
           node != null; node = node.getParent()) {
        final ConceptModel.Data data = node.getData();
        int m = data.getWeightMultiplier();
        if (wordWrapper != null && m < 1) m = 1;
        result *= m;
      }

      return result;
    }

    /**
     * Get the score of this alignment within its orthographic variant as
     * a function of the quality of the input word's match and the word,
     * variant, synonym, and decomposition types.
     */
    public int getAlignmentScore() {
      if (wordWrapper == null) return 0;

      int result = getAlignmentWeight();

      if (result > 0) {
        switch (wordData.getWordType()) {
          case ACRONYM :  // make sure the input word looks like an acronym.
            if (!MatchUtil.looksLikeAcronym(wordWrapper.getInputWord())) {
              result = 0;
            }
            break;
          case NUMERIC :  // match the input against the number.
            //todo: take ranges and units into account? note that we've already
            //      mapped the input to this word so either the numbers already
            //      match or I got around to putting a numeric stub into place
            //      that got us here.
            break;
        }
      }

      return result;
    }
  }
}
