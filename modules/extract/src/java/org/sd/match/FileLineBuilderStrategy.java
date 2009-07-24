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


import org.sd.io.FileUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.LinkedList;

/**
 * Abstract implementation of the concept model builder strategy for files
 * where each line is a concept.
 * <p>
 * @author Spence Koehler
 */
public abstract class FileLineBuilderStrategy implements ConceptModelBuilderStrategy {

  protected abstract boolean setLine(String line);  // return true to continue with line; false to skip
  protected abstract Integer getConceptId();
  protected abstract void fillFormDatas(Integer conceptId, LinkedList<FormData> formDatas);
  protected abstract void fillTermDatas(FormData formData, LinkedList<TermData> termDatas);
  protected abstract void fillSynDatas(TermData termData, LinkedList<SynonymData> synDatas);
  protected abstract void fillVarDatas(SynonymData synData, LinkedList<VariantData> varDatas);
  protected abstract void fillWordDatas(VariantData varData, LinkedList<WordData> wordDatas);


  private final LinkedList<FormData> formDatas = new LinkedList<FormData>();
  private final LinkedList<TermData> termDatas = new LinkedList<TermData>();
  private final LinkedList<SynonymData> synDatas = new LinkedList<SynonymData>();
  private final LinkedList<VariantData> varDatas = new LinkedList<VariantData>();
  private final LinkedList<WordData> wordDatas = new LinkedList<WordData>();
  
  private BufferedReader reader;
  private String firstLine;

  protected FileLineBuilderStrategy(String filename) throws IOException {
    this.reader = FileUtil.getReader(filename);
    this.firstLine = reader.readLine();
  }
  
  protected final String getFirstLine() {
    return firstLine;
  }

  /**
   * Get the conceptId for the next match concept.
   * <p>
   * Note that state will be reset for all lower types.
   * 
   * @return the conceptId or null if there are no more match concepts.
   */
  public Integer nextMatchConcept() {
    Integer result = null;

    String line = null;

    while (true) {
      try {
        line = reader.readLine();
      }
      catch (IOException e) {
        throw new IllegalStateException(e);
      }

      if (line == null) {
        System.err.println("no more lines.");
        return null;
      }

      if (setLine(line)) {
        break;
      }
    }

    formDatas.clear();
    termDatas.clear();
    synDatas.clear();
    varDatas.clear();
    wordDatas.clear();

    result = getConceptId();

    if (result != null) {
      fillFormDatas(result, formDatas);
    }
    else {
      System.err.println("no conceptId for line '" + line + "'!");
    }

    return result;
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

      fillTermDatas(formData, termDatas);
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

      fillSynDatas(termData, synDatas);
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

      fillVarDatas(synData, varDatas);
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

      fillWordDatas(variantData, wordDatas);
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

  protected static abstract class FormData {
    public final Form.Type formType;

    protected FormData(Form.Type formType) {
      this.formType = formType;
    }
  }

  protected static abstract class TermData {
    public final Decomp.Type decompType;

    protected TermData(Decomp.Type decompType) {
      this.decompType = decompType;
    }
  }

  protected static abstract class SynonymData {
    public final Synonym.Type synonymType;

    protected SynonymData(Synonym.Type synonymType) {
      this.synonymType = synonymType;
    }
  }

  protected static abstract class VariantData {
    public final Variant.Type variantType;

    protected VariantData(Variant.Type variantType) {
      this.variantType = variantType;
    }
  }

  protected static abstract class WordData {
    public final TypedWord typedWord;

    protected WordData(TypedWord typedWord) {
      this.typedWord = typedWord;
    }
  }
}
