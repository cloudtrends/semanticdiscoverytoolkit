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
package org.sd.nlp;


import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to tokenize and normalize a string.
 * <p>
 * @author Spence Koehler
 */
public class NormalizingTokenizer {
  
  private AbstractNormalizer normalizer;
  private BreakStrategy breakStrategy;
  private StringWrapper stringWrapper;
  private TokenPointerFactory tokenPointerFactory;
  private LexicalTokenizer lexicalTokenizer;
  private List<StringWrapper.SubString> tokens;

  public NormalizingTokenizer(AbstractNormalizer normalizer, StringWrapper.SubString subString) {
    this(null, subString.getNormalizedString(normalizer));
  }

  public NormalizingTokenizer(AbstractNormalizer normalizer, String inputString) {
    this(normalizer, GeneralBreakStrategy.getInstance(), inputString);
  }

  public NormalizingTokenizer(AbstractNormalizer normalizer, BreakStrategy breakStrategy, String inputString) {
    this(normalizer, new StringWrapper(inputString, breakStrategy));
  }

  public NormalizingTokenizer(AbstractNormalizer normalizer, StringWrapper stringWrapper) {
    this.normalizer = normalizer;
    this.breakStrategy = stringWrapper.getBreakStrategy();
    this.stringWrapper = stringWrapper;
    this.tokenPointerFactory = new SoTokenPointerFactory(new StubLexicon(normalizer));
    this.lexicalTokenizer = new StringWrapperLexicalTokenizer(stringWrapper, tokenPointerFactory, 0);

    initializeTokens();
  }

  private final void initializeTokens() {
    this.tokens = new ArrayList<StringWrapper.SubString>();

    LexicalEntry lexicalEntry = lexicalTokenizer.getFirstEntry();
    while (lexicalEntry != null) {
      final StringWrapperTokenPointer tokenPointer = (StringWrapperTokenPointer)lexicalEntry.getPointer();
      this.tokens.add(tokenPointer.getSubString());
      lexicalEntry = lexicalEntry.next(false);      
    }
  }

  public String getInputString() {
    return stringWrapper.string;
  }

  public List<StringWrapper.SubString> getTokens() {
    return tokens;
  }

  public int getNumTokens() {
    return tokens.size();
  }

  public StringWrapper.SubString getToken(int index) {
    return tokens.get(index);
  }

  public String getNormalizedToken(int index) {
    return (normalizer != null) ? tokens.get(index).getNormalizedString(normalizer) : getOriginalToken(index);
  }

  public String getOriginalToken(int index) {
    return tokens.get(index).originalSubString;
  }

  /**
   * Get the delimiters following the given token.
   */
  public String getTokenPostDelim(int index) {
    return tokens.get(index).getPostDelims();
  }


  public static Lexicon buildStubLexicon(AbstractNormalizer normalizer) {
    return new StubLexicon(normalizer);
  }

  // empty categories is sufficient.
  private static final Categories CATEGORIES = new Categories();

  /**
   * Stub lexicon to classify every token.
   */
  private static final class StubLexicon extends AbstractLexicon {

    public StubLexicon(AbstractNormalizer normalizer) {
      super(normalizer);
    }

    protected void define(StringWrapper.SubString subString, AbstractNormalizer normalizer) {
      subString.addCategories(CATEGORIES);
    }

    protected boolean alreadyHasTypes(Categories categories) {
      return false;
    }
  }

  public static void main(String[] args) {
    final AbstractNormalizer normalizer = new GeneralNormalizer(true);
    final BreakStrategy breakStrategy = GeneralBreakStrategy.getInstance();

    for (int i = 0; i < args.length; ++i) {
      final String inputString = args[i];
      final NormalizingTokenizer tokenizer = new NormalizingTokenizer(normalizer, breakStrategy, inputString);

      final int num = tokenizer.getNumTokens();

      System.out.print("\n" + inputString + "\n\toriginal: ");
      for (int j = 0; j < num; ++j) {
        System.out.print("'" + tokenizer.getOriginalToken(j) + "' ");
      }
      System.out.print("\n\t  normal: ");
      for (int j = 0; j < num; ++j) {
        System.out.print("'" + tokenizer.getNormalizedToken(j) + "' ");
      }
      System.out.println();
    }
  }
}
