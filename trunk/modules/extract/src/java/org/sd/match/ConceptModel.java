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


import org.sd.util.tree.SimpleTreeBuilder;
import org.sd.util.tree.SimpleTreeBuilderStrategy;
import org.sd.util.tree.Tree;
import org.sd.util.tree.TreeBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tree-based model of match concepts.
 * <p>
 * A model is built in either of two ways:
 * <p>
 * <b>(A) Manual piece-wise construction:</b>
 * <p><ol>
 * <li>ConceptModel model = new ConceptModel();</li>
 * <li>ConceptModel.MatchConcept concept = model.setConceptId(conceptId);</li>
 * <li>ConceptModel.ConceptForm form = concept.addConceptForm(formType); // for each form</li>
 * <li>ConceptModel.ConceptTerm term = form.addConceptTerm(decompType);  // for each term</li>
 * <li>ConceptModel.TermSynonym syn = term.addTermSynonym(synonymType);  // for each synonym</li>
 * <li>ConceptModel.OrthographicVariant var = syn.addOrthographicVariant(variantType);  // for each variant</li>
 * <li>var.addWordData(wordType, word);  // for each word</li>
 * </ol>
 * <b>(B) Reconstruction from a tree string:</b>
 * <p><ol>
 * <li>String conceptTreeString = existingModel.getTreeString();</li>
 * <li>ConceptModel newModel = new ConceptModel();</li>
 * <li>newModel.loadWithTreeString(conceptTreeString);</li>
 * </ol>
 * @author Spence Koehler
 */
public class ConceptModel {
  
  static final DataTreeBuilderStrategy TREE_BUILDER_STRATEGY = new DataTreeBuilderStrategy();
  static final TreeBuilder<Data> TREE_BUILDER = new SimpleTreeBuilder<Data>(TREE_BUILDER_STRATEGY);

  private Tree<Data> tree;
  private Object infoObject;
  private Integer conceptId;

  private static final AtomicInteger variantId = new AtomicInteger(0);

  private static int numWarnings = 0;

  /**
   * Construct a new un-initialized instance.
   * <p>
   * Consumer is meant to initialize using loadWithTreeString or setConceptId.
   */
  public ConceptModel() {
    this.tree = null;
    this.infoObject = null;
    this.conceptId = null;
    variantId.set(0);
  }

  /**
   * Convenience (re)constructor, calls loadWithTreeString(treeString).
   */
  public ConceptModel(String treeString) {
    loadWithTreeString(treeString);
  }

  /**
   * Get this model's tree.
   */
  public Tree<Data> getTree() {
    return tree;
  }

  /**
   * Get this model's form strings.
   */
  public String[] getFormPhraseStrings() {
    Set<String> result = null;

    if (tree != null) {
      final List<Tree<Data>> formNodes = tree.getChildren();
      if (formNodes != null && formNodes.size() > 0) {
        result = new LinkedHashSet<String>();
        for (Tree<Data> formNode : formNodes) {
          result.add(buildPhraseString(formNode));
        }
      }
    }

    return result.toArray(new String[result.size()]);
  }

  private final String buildPhraseString(Tree<Data> node) {
    final StringBuilder result = new StringBuilder();

    for (Iterator<Tree<Data>> iter = node.iterator(Tree.Traversal.DEPTH_FIRST); iter.hasNext(); ) {
      final Tree<Data> curNode = iter.next();
      final WordData wordData = curNode.getData().asWordData();
      if (wordData != null) {
        if (result.length() > 0) result.append(' ');
        result.append(wordData.getWord());
      }
    }

    return result.toString();
  }

  /**
   * Determine whether this model has any forms.
   */
  public boolean hasForms() {
    final List<Tree<Data>> formNodes = tree.getChildren();
    return formNodes != null && formNodes.size() > 0;
  }

  /**
   * Convenience accessor for the root match concept.
   */
  public MatchConcept getMatchConcept() {
    return tree.getData().asMatchConcept();
  }

  /**
   * Get the default weights for forms sorted from lowest to highest.
   */
  public int[] getDefaultWeights() {
    int[] result = null;

    final Set<Integer> defaultWeights = new TreeSet<Integer>();
    Tree<Data> conceptNode = tree;
    final List<Tree<Data>> formNodes = conceptNode.getChildren();
    if (formNodes != null) {
      for (Tree<Data> formNode : formNodes) {
        int defaultWeight = 0;

        final List<Tree<Data>> termNodes = formNode.getChildren();
        for (Tree<Data> termNode : termNodes) {
          defaultWeight += termNode.getData().asConceptTerm().getDefaultAlignmentWeight();
        }

        defaultWeights.add(defaultWeight);
      }
    }

    if (defaultWeights.size() > 0) {
      result = new int[defaultWeights.size()];
      int index = 0;
      for (Integer defaultWeight : defaultWeights) {
        result[index++] = defaultWeight;
      }
    }

    return result;
  }

  /**
   * Serialize the current model into the given serializer.
   */
  public void serialize(ConceptSerializer serializer) {
    Tree<Data> conceptNode = tree;
    serializer.setConceptId(conceptNode.getData().asMatchConcept().getConceptId());
    final List<Tree<Data>> formNodes = conceptNode.getChildren();
    if (formNodes != null) {
      for (Tree<Data> formNode : formNodes) {
        serializer.addConceptForm(formNode.getData().asConceptForm().getFormType());
        final List<Tree<Data>> termNodes = formNode.getChildren();
        for (Tree<Data> termNode : termNodes) {
          serializer.addConceptTerm(termNode.getData().asConceptTerm().getDecompType());
          final List<Tree<Data>> synNodes = termNode.getChildren();
          for (Tree<Data> synNode : synNodes) {
            serializer.addTermSynonym(synNode.getData().asTermSynonym().getSynonymType());
            final List<Tree<Data>> varNodes = synNode.getChildren();
            for (Tree<Data> varNode : varNodes) {
              serializer.addVariantType(varNode.getData().asOrthographicVariant().getVariantType());
              final List<Tree<Data>> wordNodes = varNode.getChildren();
              for (Tree<Data> wordNode : wordNodes) {
                serializer.addTypedWord(wordNode.getData().asWordData().getTypedWord());
              }
            }
          }
        }
      }
    }
    else {
      if (numWarnings < 10) {
        System.err.println("***WARNING: empty concept '" + conceptNode.getData().asMatchConcept().getConceptId() + "'!");
      }
      else {
        if (numWarnings % 1000 == 0) {
          System.err.println("...hit " + numWarnings + " warnings!");
        }
      }
      ++numWarnings;
    }
  }

  /**
   * Load with the tree string form, wiping out any existing tree data.
   * <p>
   * NOTE: this will wipe out any existing tree data for this concept!
   *       Typically, construction is accomplished either through this
   *       method or through setConceptId followed by ConceptModel.Data
   *       adders to build the model tree.
   */
  public void loadWithTreeString(String treeString) {
    this.tree = TREE_BUILDER.buildTree(treeString);
    this.conceptId = tree.getData().asMatchConcept().getConceptId();
  }

  public int getConceptId() {
    return conceptId;
  }

  /**
   * Get the tree string for this model.
   *
   * @return the tree string or null if not yet initialized.
   */
  public String getTreeString() {
    return (tree != null) ? TREE_BUILDER.buildString(this.tree) : null;
  }

  public String asString(ConceptSerializerStrategy strategy) {
    final ConceptSerializer serializer = strategy.getSerializer();

    String result = null;
    if (serializer != null) {
      serialize(serializer);
      result = serializer.asString();
    }

    return result;
  }

  /**
   * Set the concept id for this model's tree, wiping out any existing
   * tree data.
   * <p>
   * NOTE: this will wipe out any existing tree data for this concept!
   *       Typically, construction is accomplished either through this
   *       method followed by ConceptModel.Data adder methods to build
   *       the model tree or through loadWithTreeString.
   * <p>
   * @return the new match concept, which is the root data for the model tree.
   */
  public MatchConcept setConceptId(int conceptId) {
    this.conceptId = conceptId;
    final MatchConcept result = new MatchConcept(conceptId);
    this.tree = new Tree<Data>(result);
    result.setNode(this.tree);
    return result;
  }

  public void setInfoObject(Object infoObject) {
    this.infoObject = infoObject;
  }

  public Object getInfoObject() {
    return infoObject;
  }

  /**
   * Collect NORMAL, and COMPOUND_* words in terms, ACRONYM in acronyms, and FUNCTIONAL in functional.
   * Drop NUMERICs.
   */
  public void collectTerms(Set<String> terms, Set<String> acronyms, Set<String> functional) {
    final List<Tree<Data>> leaves = tree.gatherLeaves();

    for (Tree<Data> leaf : leaves) {
      final WordData wordData = leaf.getData().asWordData();
      if (wordData != null) {
        switch (wordData.getWordType()) {
        case ACRONYM :
          acronyms.add(wordData.getWord());
          break;
        case FUNCTIONAL :
          functional.add(wordData.getWord());
          break;
        case NUMERIC :
          break;
        default :
          terms.add(wordData.getWord());
        }
      }
    }
  }

  /**
   * Interface for model tree data.
   */
  public static interface Data {
    public MatchConcept asMatchConcept();
    public ConceptForm asConceptForm();
    public ConceptTerm asConceptTerm();
    public TermSynonym asTermSynonym();
    public OrthographicVariant asOrthographicVariant();
    public WordData asWordData();

    /**
     * Get the weight multiplier for this data.
     */
    public int getWeightMultiplier();

    /**
     * Back-reference to the node containing this data.
     */
    public Tree<Data> getNode();

    /** Safely downcast this Data as AbstractData. Intended for internal use. */
    public AbstractData asAbstractData();
  }

  protected static abstract class AbstractData implements Data {
    private Tree<Data> node;

    protected AbstractData() {
    }

    public MatchConcept asMatchConcept() {return null;}
    public ConceptForm asConceptForm() {return null;}
    public ConceptTerm asConceptTerm() {return null;}
    public TermSynonym asTermSynonym() {return null;}
    public OrthographicVariant asOrthographicVariant() {return null;}
    public WordData asWordData() {return null;}
    public AbstractData asAbstractData() {return this;}

    public Tree<Data> getNode() {return node;}

    /**
     * Set this data's backreference to the node. Package protected to prevent
     * misuse. Should only be called by the DataTreeBuilderStrategy. This
     * method becomes safely available through the asAbstractData downcast.
     */
    void setNode(Tree<Data> node) {
      this.node = node;
    }
  }

  public static final class MatchConcept extends AbstractData {
    private int conceptId;

    /**
     * Package protected because construction occurs through load, set, and add methods.
     */
    MatchConcept(int conceptId) {
      super();
      this.conceptId = conceptId;
    }

    public MatchConcept asMatchConcept() {return this;}
    public int getConceptId() {return conceptId;}

    public int getWeightMultiplier() {return 1;}

    /**
     * Add a concept form to this match concept with the given formType.
     *
     * @return the new concept form to which conceptTerms may be added.
     */
    public ConceptForm addConceptForm(Form.Type formType) {
      final ConceptForm result = new ConceptForm(formType);
      final Tree<Data> conceptFormNode = new Tree<Data>(result);

      // add child through the strategy so all connections are properly made
      TREE_BUILDER_STRATEGY.addChild(getNode(), conceptFormNode);

      return result;
    }

    /**
     * Add the given concept form to this match concept.
     * <p>
     * NOTE: The original tree in which the form resided will be invalidated!
     */
    public ConceptForm addConceptForm(ConceptForm conceptForm) {
      final ConceptForm result = conceptForm;
      final Tree<Data> conceptFormNode = result.getNode();

      // add child through the strategy so all connections are properly made
      TREE_BUILDER_STRATEGY.addChild(getNode(), conceptFormNode);

      return result;
    }

    /**
     * Build a reversible string for this data.
     * <p>
     * Make sure the string distinguishes this data type from others.
     * <p>
     * (See DataTreeBuilderStrategy.constructCoreNodeData)
     *
     * @return C + conceptId
     */
    public String toString() {
      return new StringBuilder().append('C').append(conceptId).toString();
    }
  }

  public static final class ConceptForm extends AbstractData {
    private Form.Type formType;
    private Map<String, List<WordData>> word2nodes;  // word text to wordData nodes.

    ConceptForm(Form.Type formType) {
      super();
      this.formType = formType;
      this.word2nodes = new HashMap<String, List<WordData>>();
    }

    public ConceptForm asConceptForm() {return this;}
    public Form.Type getFormType() {return formType;}

    public int getWeightMultiplier() {return formType.getWeightMultiplier();}

    /**
     * Add a concept term to this concept form with the given decompType.
     *
     * @return the new concept term to which termSynonyms may be added.
     */
    public ConceptTerm addConceptTerm(Decomp.Type decompType) {
      final ConceptTerm result = new ConceptTerm(decompType);
      final Tree<Data> conceptTermNode = new Tree<Data>(result);

      // add child through the strategy so all connections are properly made
      TREE_BUILDER_STRATEGY.addChild(getNode(), conceptTermNode);

      return result;
    }

    /**
     * Index the word data from this match concept.
     * Called by the DataTreeBuilderStrategy.
     */
    void addWordData(WordData wordData) {
      final String wordText = wordData.getWord();
      List<WordData> wordDatas = word2nodes.get(wordText);
      if (wordDatas == null) {
        wordDatas = new ArrayList<WordData>();
        word2nodes.put(wordText, wordDatas);
      }
      wordDatas.add(wordData);
    }

    public List<WordData> getWordDatas(String word) {
      return word2nodes.get(word);
    }

    /**
     * Build a reversible string for this data.
     * <p>
     * Make sure the string distinguishes this data type from others.
     * <p>
     * (See DataTreeBuilderStrategy.constructCoreNodeData)
     *
     * @return F + formType.ordinal
     */
    public String toString() {
      return new StringBuilder().append('F').append(formType.ordinal()).toString();
    }
  }

  public static final class ConceptTerm extends AbstractData {
    private Decomp.Type decompType;

    // default variant to use when computing scores against missing input.
    private Integer _defaultAlignmentWeight;
    private OrthographicVariant _defaultVariant;

    /**
     * Package protected because construction occurs through load, set, and add methods.
     */
    ConceptTerm(Decomp.Type decompType) {
      super();
      this.decompType = decompType;
      this._defaultAlignmentWeight = null;
      this._defaultVariant = null;
    }

    public ConceptTerm asConceptTerm() {return this;}
    public Decomp.Type getDecompType() {return decompType;}

    public int getWeightMultiplier() {return decompType.getWeightMultiplier();}

    /**
     * Add a term synonym to this concept term with the given synonymType.
     *
     * @return the new term synonym to which orthographicVariants may be added.
     */
    public TermSynonym addTermSynonym(Synonym.Type synonymType) {
      final TermSynonym result = new TermSynonym(synonymType);
      final Tree<Data> termSynonymNode = new Tree<Data>(result);

      // add child through the strategy so all connections are properly made
      TREE_BUILDER_STRATEGY.addChild(getNode(), termSynonymNode);

      return result;
    }

    public int getDefaultAlignmentWeight() {
      if (_defaultAlignmentWeight == null) {
        computeDefaults();
      }
      return _defaultAlignmentWeight;
    }

    public OrthographicVariant getDefaultVariant() {
      if (_defaultAlignmentWeight == null) {
        computeDefaults();
      }
      return _defaultVariant;
    }

    /**
     * Build a reversible string for this data.
     * <p>
     * Make sure the string distinguishes this data type from others.
     * <p>
     * (See DataTreeBuilderStrategy.constructCoreNodeData)
     *
     * @return T + decompType.ordinal
     */
    public String toString() {
      return new StringBuilder().append('T').append(decompType.ordinal()).toString();
    }

    /**
     * Compute default alignment weight and default variant for computing scores against missing input.
     */
    private final void computeDefaults() {
      int alignmentWeight = 0;
      OrthographicVariant variant = null;

      final int termWeight = getWeightMultiplier();
      if (termWeight > 0) {
        for (Iterator<Tree<Data>> synonymIter = getNode().getChildren().iterator(); synonymIter.hasNext(); ) {
          final Tree<Data> synonymNode = synonymIter.next();
          final int synonymWeight = synonymNode.getData().getWeightMultiplier();
          if (synonymWeight > 0) {
            final int synMult = termWeight * synonymWeight;
            for (Iterator<Tree<Data>> variantIter = synonymNode.getChildren().iterator(); variantIter.hasNext(); ) {
              final Tree<Data> variantNode = variantIter.next();
              final int variantWeight = variantNode.getData().getWeightMultiplier();
              if (variantWeight > 0) {
                final int varMult = synMult * variantWeight;
                int curWeight = 0;
                for (Iterator<Tree<Data>> wordIter = variantNode.getChildren().iterator(); wordIter.hasNext(); ) {
                  final Tree<Data> wordNode = wordIter.next();
                  final int wordWeight = wordNode.getData().getWeightMultiplier();
                  curWeight += varMult * wordWeight;
                }
                if (curWeight > alignmentWeight) {
                  alignmentWeight = curWeight;
                  variant = variantNode.getData().asOrthographicVariant();
                }
              }
            }
          }
        }
      }

      this._defaultAlignmentWeight = alignmentWeight;
      this._defaultVariant = variant;
    }
  }

  public static final class TermSynonym extends AbstractData {
    private Synonym.Type synonymType;

    /**
     * Package protected because construction occurs through load, set, and add methods.
     */
    TermSynonym(Synonym.Type synonymType) {
      super();
      this.synonymType = synonymType;
    }

    public TermSynonym asTermSynonym() {return this;}
    public Synonym.Type getSynonymType() {return synonymType;}

    public int getWeightMultiplier() {return synonymType.getWeightMultiplier();}

    /**
     * Add an orthographic variant to this term synonym with the given variantType.
     *
     * @return the new orthographic variant to which wordDatas may be added.
     */
    public OrthographicVariant addOrthographicVariant(Variant.Type variantType) {
      final OrthographicVariant result = new OrthographicVariant(variantType);
      final Tree<Data> orthographicVariantNode = new Tree<Data>(result);

      // add child through the strategy so all connections are properly made
      TREE_BUILDER_STRATEGY.addChild(getNode(), orthographicVariantNode);

      return result;
    }

    /**
     * Build a reversible string for this data.
     * <p>
     * Make sure the string distinguishes this data type from others.
     * <p>
     * (See DataTreeBuilderStrategy.constructCoreNodeData)
     *
     * @return S + synonymType.ordinal
     */
    public String toString() {
      return new StringBuilder().append('S').append(synonymType.ordinal()).toString();
    }
  }

  public static final class OrthographicVariant extends AbstractData {
    private Variant.Type variantType;
    private StringBuilder variant;
    private int id;  // used solely to distinguish one variant from another.

    /**
     * Package protected because construction occurs through load, set, and add methods.
     */
    OrthographicVariant(Variant.Type variantType) {
      super();
      this.variantType = variantType;
      this.variant = new StringBuilder();
      this.id = variantId.getAndIncrement();
    }

    public OrthographicVariant asOrthographicVariant() {return this;}
    public Variant.Type getVariantType() {return variantType;}
    public String getVariant() {return variant.toString();}  //todo: is this needed?
    public int getId() {return id;}

    public int getWeightMultiplier() {return variantType.getWeightMultiplier();}

    /**
     * Add the given word to this orthographic variant.
     *
     * @return the new word data.
     */
    public WordData addWordData(Word.Type wordType, String word) {
      final WordData result = new WordData(wordType, word);
      final Tree<Data> wordNode = new Tree<Data>(result);

      // add child through the strategy so all connections are properly made
      TREE_BUILDER_STRATEGY.addChild(getNode(), wordNode);

      return result;
    }

//todo: determine whether "variant" string is necessary.
    /**
     * Append another word to the variant text. Handled by the DataTreeBuilderStrategy.
     */
    void addVariantText(String word) {
      if (variant.length() > 0) variant.append(' ');
      variant.append(word);
    }

    /**
     * Build a reversible string for this data.
     * <p>
     * Make sure the string distinguishes this data type from others.
     * <p>
     * (See DataTreeBuilderStrategy.constructCoreNodeData)
     *
     * @return V + variantType.ordinal
     */
    public String toString() {
      return new StringBuilder().append('V').append(variantType.ordinal()).toString();
    }
  }

  public static final class WordData extends AbstractData {
    private TypedWord typedWord;

    /**
     * Package protected because construction occurs through load, set, and add methods.
     */
    WordData(Word.Type wordType, String word) {
      super();
      this.typedWord = new TypedWord(word, wordType);
    }

    public WordData asWordData() {return this;}
    public Word.Type getWordType() {return typedWord.wordType;}
    public String getWord() {return typedWord.word;}
    public TypedWord getTypedWord() {return typedWord;}

    public int getWeightMultiplier() {return typedWord.wordType.getWeight();}

    /**
     * Get the number of words that occur with this word in this context.
     */
    public int getNumWords() {
      return getNode().getParent().getData().asOrthographicVariant().getNode().getChildren().size();
    }

    public ConceptForm getConceptForm() {
      for (Tree<Data> node = getNode().getParent(); node != null; node = node.getParent()) {
        final ConceptForm result = node.getData().asConceptForm();
        if (result != null) return result;
      }
      return null;
    }

    /**
     * Build a reversible string for this data.
     * <p>
     * Make sure the string distinguishes this data type from others.
     * <p>
     * (See DataTreeBuilderStrategy.constructCoreNodeData)
     *
     * @return W + wordType.ordinal + '|' + word
     */
    public String toString() {
      return new StringBuilder().append('W').append(typedWord.wordType.ordinal()).append('|').append(typedWord.word).toString();
    }
  }


  private static final class DataTreeBuilderStrategy extends SimpleTreeBuilderStrategy <Data> {

    DataTreeBuilderStrategy() {
    }

    /**
     * Construct core node data from its string representation.
     * <p>
     * @param coreDataString The string form of the core node data.
     * <p>
     * @return the core node data.
     */
    public Data constructCoreNodeData(String coreDataString) {
      Data result = null;

      final char marker = coreDataString.charAt(0);
      final String content = coreDataString.substring(1);

      switch (marker) {
        case 'C' : // MatchConcept + conceptId
          result = new MatchConcept(Integer.parseInt(content));
          break;
        case 'F' : // ConceptForm  + formOrdinal
          result = new ConceptForm(Form.getType(Integer.parseInt(content)));
          break;
        case 'T' : // ConceptTerm  + synonymOrdinal
          result = new ConceptTerm(Decomp.getType(Integer.parseInt(content)));
          break;
        case 'S' : // TermSynonym  + synonymOrdinal
          result = new TermSynonym(Synonym.getType(Integer.parseInt(content)));
          break;
        case 'V' : // OrthographicVariant + variantOrdinal
          result = new OrthographicVariant(Variant.getType(Integer.parseInt(content)));
          break;
        case 'W' : // Word + wordOrdinal + | + wordText
          final int separatorPos = content.indexOf('|');
          final int wordOrdinal = Integer.parseInt(content.substring(0, separatorPos));
          final String wordText = content.substring(separatorPos + 1);
          result = new WordData(Word.getType(wordOrdinal), wordText);
          break;
      }
      return result;
    }

    public void addChild(Tree<Data> node, Tree<Data> child) {
      super.addChild(node, child);

      boolean topDown = false;

      if (child.getData().asWordData() == null) {
        topDown = (child.getChildren() == null);
      }
      else {
        topDown = (node.getParent() != null);
      }

      if (topDown) {
        addChildTopDown(node, child);
      }
      else {  // bottom up
        addChildBottomUp(node, child);
      }
    }

    private final void addChildTopDown(Tree<Data> node, Tree<Data> child) {
      // now that nodes are built and connected with data, add backreferences
      if (node.getParent() == null && node.getData().getNode() == null) {
        // haven't back-referenced the root yet.
        node.getData().asAbstractData().setNode(node);
      }

      // back-reference the child
      child.getData().asAbstractData().setNode(child);

      // if this is a word node, walk back to the match concept and map to index it
      final WordData wordData = child.getData().asWordData();
      if (wordData != null) {
        // map the word text to the word data in the concept form
        for (Tree<Data> parent = node.getParent(); parent != null; parent = parent.getParent()) {
          final ConceptForm conceptForm = parent.getData().asConceptForm();
          if (conceptForm != null) {
            conceptForm.addWordData(wordData);
            break;
          }
        }

        // add the word text to the orthographic variant's variant
        node.getData().asOrthographicVariant().addVariantText(wordData.getWord());
      }
    }

    private final void addChildBottomUp(Tree<Data> node, Tree<Data> child) {

      // NOTE: because of recursion, leaves get added to their parents first,
      //       then those parents to their parents, on up to the root.

      // now that nodes are built and connected with data, add backreferences
      if (node.getData().getNode() == null) node.getData().asAbstractData().setNode(node);
      if (child.getData().getNode() == null) child.getData().asAbstractData().setNode(child);

      // if the child is a form node, gather word nodes (leaves) and index the words
      final ConceptForm conceptForm = child.getData().asConceptForm();
      if (conceptForm != null) {
        final List<Tree<Data>> wordNodes = child.gatherLeaves();
        for (Tree<Data> wordNode : wordNodes) {
          final WordData wordData = wordNode.getData().asWordData();
          if (wordData == null) {
            System.out.println("wordNode=" + wordNode.toString());
          }
          conceptForm.addWordData(wordData);

          // add the word text to the orthographic variant's variant
          wordNode.getParent().getData().asOrthographicVariant().addVariantText(wordData.getWord());
        }
      }
    }
  }
}
