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


import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Abstract implementation of the ConceptModelBuilderStrategy.
 * <p>
 * @author Spence Koehler
 */
public abstract class AbstractConceptBuilderStrategy implements ConceptModelBuilderStrategy {
  
  protected abstract List<WordData> buildWordDatas(ConceptForm conceptForm);

  private final LinkedList<FormData> formDatas = new LinkedList<FormData>();
  private final LinkedList<TermData> termDatas = new LinkedList<TermData>();
  private final LinkedList<SynonymData> synDatas = new LinkedList<SynonymData>();
  private final LinkedList<VariantData> varDatas = new LinkedList<VariantData>();
  private final LinkedList<WordData> wordDatas = new LinkedList<WordData>();
  
  private IdGenerator idGenerator;
  private ConceptId currentConceptId;

  protected AbstractConceptBuilderStrategy(IdGenerator idGenerator) {
    this.idGenerator = idGenerator;
  }
  
  protected final IdGenerator getIdGenerator() {
    return idGenerator;
  }

  protected ConceptId getCurrentConceptId() {
    return currentConceptId;
  }

  /**
   * Get the conceptId for the next match concept.
   * <p>
   * Note that state will be reset for all lower types.
   * 
   * @return the conceptId or null if there are no more match concepts.
   */
  public Integer nextMatchConcept() {

    final ConceptId conceptId = idGenerator.getNextId();
    this.currentConceptId = conceptId;
    final Integer result = (conceptId == null) ? null : conceptId.getId();

    formDatas.clear();
    termDatas.clear();
    synDatas.clear();
    varDatas.clear();
    wordDatas.clear();

    if (result != null) {
      fillFormDatas(conceptId, formDatas);
    }

    return result;
  }

  private final void fillFormDatas(ConceptId conceptId, LinkedList<FormData> formDatas) {
    for (Iterator<ConceptForm> iter = conceptId.getConceptForms(); iter.hasNext(); ) {
      final ConceptForm conceptForm = iter.next();

      final List<WordData> wordDatas = buildWordDatas(conceptForm);
      if (wordDatas != null && wordDatas.size() > 0) {
        final FormData formData = new FormData(conceptForm.formType, wordDatas);
        formDatas.add(formData);
      }
    }
  }

//   /**
//    * Get the current match concept's info object.
//    * <p>
//    * NOTE: the current match concept is that whose conceptId was last
//    *       returned from nextMatchConcept.
//    */
//   public Object getCurrentInfoObject() {
//   }

  /**
   * Get the next concept form for the current match concept.
   * <p>
   * Note that state will be reset for all lower types.
   * 
   * @return the form type or null if there are no more concept forms
   *         for the current match concept.
   */
  public Form.Type nextConceptForm() {
    Form.Type result = null;

    termDatas.clear();
    synDatas.clear();
    varDatas.clear();
    wordDatas.clear();

    if (formDatas.size() > 0) {
      final FormData formData = formDatas.removeFirst();
      result = formData.formType;
      formData.fillTermDatas(termDatas);
    }

    return result;
  }

  /**
   * Get the next concept term's decomp type for the current concept form.
   * <p>
   * Note that state will be reset for all lower types.
   * 
   * @return the concept term's decomp type or null if there are no more
   *         concept terms for the current concept form.
   */
  public Decomp.Type nextConceptTerm() {
    Decomp.Type result = null;

    synDatas.clear();
    varDatas.clear();
    wordDatas.clear();

    if (termDatas.size() > 0) {
      final TermData termData = termDatas.removeFirst();
      result = termData.decompType;
      termData.fillSynDatas(synDatas);
    }

    return result;
  }

  /**
   * Get the next synonym type for the current concept term.
   * <p>
   * Note that state will be reset for all lower types.
   * 
   * @return the synonym type or null if there are no more synonyms for the
   *         current concept term.
   */
  public Synonym.Type nextTermSynonym() {
    Synonym.Type result = null;

    varDatas.clear();
    wordDatas.clear();

    if (synDatas.size() > 0) {
      final SynonymData synData = synDatas.removeFirst();
      result = synData.synonymType;
      synData.fillVarDatas(varDatas);
    }

    return result;
  }

  /**
   * Get the next variant type for the current synonym.
   * <p>
   * Note that state will be reset for all lower types.
   * 
   * @return the variant type or null if there are no more variants for the
   *         current synonym
   */
  public Variant.Type nextVariantType() {
    Variant.Type result = null;

    wordDatas.clear();

    if (varDatas.size() > 0) {
      final VariantData variantData = varDatas.removeFirst();
      result = variantData.variantType;
      variantData.fillWordDatas(wordDatas);
    }

    return result;
  }

  /**
   * Get the next typed word for the current variant.
   * 
   * @return the typedWord or null if there are no more words for the current
   *         variant.
   */
  public TypedWord nextTypedWord() {
    TypedWord result = null;

    if (wordDatas.size() > 0) {
      final WordData wordData = wordDatas.removeFirst();
      result = wordData.typedWord;
    }

    return result;
  }

  protected static class FormData {
    public final Form.Type formType;
    private List<TermData> termDatas;

    protected FormData(Form.Type formType, List<WordData> wordDatas) {
      this.formType = formType;
      this.termDatas = new ArrayList<TermData>();

      Decomp.Type decompType = null;
      TermData termData = null;

      for (WordData wordData : wordDatas) {
        final Decomp.Type curDecompType = wordData.decompType;
        if (decompType != curDecompType) {
          termData = new TermData(curDecompType);
          decompType = curDecompType;
          termDatas.add(termData);
        }
        termData.add(wordData);
      }
    }

    protected void fillTermDatas(LinkedList<TermData> termDatas) {
      termDatas.addAll(this.termDatas);
    } 
  }

  protected static class TermData {
    public final Decomp.Type decompType;
    private List<SynonymData> synDatas;

    private Synonym.Type synType;
    private SynonymData synData;

    protected TermData(Decomp.Type decompType) {
      this.decompType = decompType;
      this.synDatas = new ArrayList<SynonymData>();

      this.synType = null;
      this.synData = null;
    }

    private final void add(WordData wordData) {
      final Synonym.Type curSynType = wordData.synonymType;
      if (synType != curSynType) {
        synData = new SynonymData(curSynType);
        synType = curSynType;
        synDatas.add(synData);
      }
      synData.add(wordData);
    }

    protected void fillSynDatas(LinkedList<SynonymData> synDatas) {
      synDatas.addAll(this.synDatas);
    }
  }

  protected static class SynonymData {
    public final Synonym.Type synonymType;
    private List<VariantData> varDatas;

    private Variant.Type varType;
    private VariantData varData;


    protected SynonymData(Synonym.Type synonymType) {
      this.synonymType = synonymType;
      this.varDatas = new ArrayList<VariantData>();

      this.varType = null;
      this.varData = null;
    }

    private final void add(WordData wordData) {
      final Variant.Type curVarType = wordData.variantType;
      if (varType != curVarType) {
        varData = new VariantData(curVarType);
        varType = curVarType;
        varDatas.add(varData);
      }
      varData.add(wordData);
    }

    protected void fillVarDatas(LinkedList<VariantData> varDatas) {
      varDatas.addAll(this.varDatas);
    }
  }

  protected static class VariantData {
    public final Variant.Type variantType;
    private List<WordData> wordDatas;

    protected VariantData(Variant.Type variantType) {
      this.variantType = variantType;
      this.wordDatas = new ArrayList<WordData>();
    }

    private final void add(WordData wordData) {
      wordDatas.add(wordData);
    }

    protected void fillWordDatas(LinkedList<WordData> wordDatas) {
      wordDatas.addAll(this.wordDatas);
    }
  }

  public static class WordData {
    public final TypedWord typedWord;

    public final Variant.Type variantType;
    public final Synonym.Type synonymType;
    public final Decomp.Type decompType;
    public final Form.Type formType;

    public WordData(Word.Type wordType, String word,
                       Variant.Type variantType, Synonym.Type synonymType,
                       Decomp.Type decompType, Form.Type formType) {
      this(new TypedWord(word, wordType), variantType,
           synonymType, decompType, formType);
    }

    public WordData(TypedWord typedWord,
                       Variant.Type variantType, Synonym.Type synonymType,
                       Decomp.Type decompType, Form.Type formType) {
      this.typedWord = typedWord;

      this.variantType = variantType;
      this.synonymType = synonymType;
      this.decompType = decompType;
      this.formType = formType;
    }
  }
}
