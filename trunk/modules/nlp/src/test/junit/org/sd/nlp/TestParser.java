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


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.sd.fsm.Grammar;
import org.sd.fsm.impl.GrammarImpl;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * JUnit Tests for the Parser class.
 * <p>
 * @author Spence Koehler
 */
public class TestParser extends TestCase {

  private Parser _parser;

  public TestParser(String name) throws IOException {
    super(name);
    this._parser = buildParser();
  }
  
  private String[][] examples = {
    {"the boy ate fish",
     "(S (Subject (NP (det 'the') (noun 'boy'))) (Direct_Object (VP (verb 'ate') (NP (noun 'fish')))))"},
    {"the boy",
     "(Subject (NP (det 'the') (noun 'boy')))"},
    {"the boy ate",
     "(S (Subject (NP (det 'the') (noun 'boy'))) (Direct_Object (VP (verb_intrans 'ate'))))"},
    {"the flippery floo ate the blaftety blar",
     "(S (Subject (NP (det 'the') (adj 'flippery') (noun 'floo'))) (Direct_Object (VP (verb 'ate') (NP (det 'the') (adj 'blaftety') (noun 'blar')))))",
     "(S (Subject (NP (det 'the') (adj 'flippery') (noun 'floo'))) (Direct_Object (VP (verb 'ate') (NP (det 'the') (noun 'blaftety') (noun 'blar')))))",
     "(S (Subject (NP (det 'the') (noun 'flippery') (noun 'floo'))) (Direct_Object (VP (verb 'ate') (NP (det 'the') (adj 'blaftety') (noun 'blar')))))",
     "(S (Subject (NP (det 'the') (noun 'flippery') (noun 'floo'))) (Direct_Object (VP (verb 'ate') (NP (det 'the') (noun 'blaftety') (noun 'blar')))))"},
    {"the boy ate fish at the zoo on a log",
     "(S (Subject (NP (det 'the') (noun 'boy'))) (Direct_Object (VP (verb 'ate') (NP (noun 'fish'))) (PP (prep 'at') (NP (det 'the') (noun 'zoo'))) (PP (prep 'on') (NP (det 'a') (noun 'log')))))"},
    {"the boy slept",
     "(S (Subject (NP (det 'the') (noun 'boy'))) (Direct_Object (VP (verb_intrans 'slept'))))"},
    {"the sleepy big boy ate tiny fish at the nifty zoo on a log",
     "(S (Subject (NP (det 'the') (adj 'sleepy') (adj 'big') (noun 'boy'))) (Direct_Object (VP (verb 'ate') (NP (adj 'tiny') (noun 'fish'))) (PP (prep 'at') (NP (det 'the') (adj 'nifty') (noun 'zoo'))) (PP (prep 'on') (NP (det 'a') (noun 'log')))))"},
    {"the sleepy big whale ate tiny mackerels at the airy marina on the sea",
     "(S (Subject (NP (det 'the') (adj 'sleepy') (adj 'big') (noun 'whale'))) (Direct_Object (VP (verb 'ate') (NP (adj 'tiny') (noun 'mackerels'))) (PP (prep 'at') (NP (det 'the') (adj 'airy') (noun 'marina'))) (PP (prep 'on') (NP (det 'the') (noun 'sea')))))",
     "(S (Subject (NP (det 'the') (adj 'sleepy') (adj 'big') (noun 'whale'))) (Direct_Object (VP (verb 'ate') (NP (adj 'tiny') (noun 'mackerels'))) (PP (prep 'at') (NP (det 'the') (noun 'airy') (noun 'marina'))) (PP (prep 'on') (NP (det 'the') (noun 'sea')))))"},
    {"time flies like an arrow",
     "(Subject (NP (noun 'time') (noun 'flies')) (PP (prep 'like') (NP (det 'an') (noun 'arrow'))))",
     "(S (Subject (NP (noun 'time') (noun 'flies'))) (Direct_Object (VP (verb 'like') (NP (det 'an') (noun 'arrow')))))",
     "(S (Subject (NP (noun 'time'))) (Direct_Object (VP (verb_intrans 'flies')) (PP (prep 'like') (NP (det 'an') (noun 'arrow')))))"},
    {"fruit flies like a banana",
     "(Subject (NP (noun 'fruit') (noun 'flies')) (PP (prep 'like') (NP (det 'a') (noun 'banana'))))",
     "(S (Subject (NP (noun 'fruit') (noun 'flies'))) (Direct_Object (VP (verb 'like') (NP (det 'a') (noun 'banana')))))",
     "(S (Subject (NP (noun 'fruit'))) (Direct_Object (VP (verb_intrans 'flies')) (PP (prep 'like') (NP (det 'a') (noun 'banana')))))"},
    {"fruit flies like an apple",
     "(Subject (NP (noun 'fruit') (noun 'flies')) (PP (prep 'like') (NP (det 'an') (noun 'apple'))))",
     "(S (Subject (NP (noun 'fruit') (noun 'flies'))) (Direct_Object (VP (verb 'like') (NP (det 'an') (noun 'apple')))))",
     "(S (Subject (NP (noun 'fruit'))) (Direct_Object (VP (verb_intrans 'flies')) (PP (prep 'like') (NP (det 'an') (noun 'apple')))))"},
    {"the boy ate potato salad",
     "(S (Subject (NP (det 'the') (noun 'boy'))) (Direct_Object (VP (verb 'ate') (NP (noun 'potato salad')))))"},
    {"the circus clown ate potato salad",
     "(S (Subject (NP (det 'the') (noun 'circus clown'))) (Direct_Object (VP (verb 'ate') (NP (noun 'potato salad')))))"}
  };


  private final Parser getParser() {
    return _parser;
  }

  private final Parser buildParser() throws IOException {
    final InputStream grammarInputStream = Parser.class.getResourceAsStream("resources/simpleTestGrammar.txt");
    final InputStream lexiconInputStream = this.getClass().getResourceAsStream("resources/simpleTestLexicon.csv");
    final CategoryFactory cfactory = buildCategoryFactory();
    final Grammar grammar = GrammarImpl.loadGrammar(grammarInputStream, ExtendedGrammarTokenFactory.getInstance(cfactory), ParseStateDecoder.getInstance());
    final Lexicon lexicon = LexiconLoader.loadCsvLexicon(cfactory, lexiconInputStream, null);
    return new Parser(grammar, lexicon);
  }

  private final CategoryFactory buildCategoryFactory() throws IOException {
    final CategoryFactory result = new CategoryFactory();

//todo: make a resource with this data and read it in.

    result.defineCategory("S", false);
    result.defineCategory("SUBJECT", false);
    result.defineCategory("DIRECT_OBJECT", false);
    result.defineCategory("INDIRECT_OBJECT", false);
    result.defineCategory("NP", false);
    result.defineCategory("VP", false);
    result.defineCategory("PP", false);
    result.defineCategory("DET", false);
    result.defineCategory("ADJ", true);
    result.defineCategory("NOUN", true);
    result.defineCategory("ADV", false);
    result.defineCategory("VERB", false);
    result.defineCategory("VERB_INTRANS", false);
    result.defineCategory("PREP", false);
    result.defineCategory("PARTICIPLE", false);

    return result;
  }

  private void doTest(String[] sentenceAndExpectedParses, LexicalTokenizer tokenizer) {
    final Parser parser = getParser();
    List<Parser.Parse> parses = (tokenizer == null) ? parser.parse(sentenceAndExpectedParses[0]) : parser.parse(tokenizer, null, true);

    if (sentenceAndExpectedParses.length == 1) {
      assertNull("expected null, but got: " + parses, parses);
    }
    else {
      assertNotNull(sentenceAndExpectedParses[0], parses);
      if (sentenceAndExpectedParses.length - 1 != parses.size()) {
        System.out.println("got parses: " + parses);
      }
      assertEquals("got parses: " + parses, sentenceAndExpectedParses.length - 1, parses.size());

      int index = 0;
      for (Parser.Parse parse : parses) {
        assertEquals("got=" + parse.getTree().toString() + ", expected=" + sentenceAndExpectedParses[index + 1], sentenceAndExpectedParses[index + 1], parse.toString());
        ++index;
      }
    }
  }

  public void testDefaultParsing() {
    for (String[] block : examples) {
      doTest(block, null);
    }
  }

  public void testDefaultStringWrapperTokenizerParsing() {
    final Parser parser = getParser();
    final Lexicon lexicon = parser.getLexicon();
    final TokenPointerFactory tokenPointerFactory = new DefaultTokenPointerFactory(lexicon);

    for (String[] block : examples) {
      final StringWrapperLexicalTokenizer tokenizer = new StringWrapperLexicalTokenizer(block[0], tokenPointerFactory, 0);
      doTest(block, tokenizer);
    }
  }

  public void testLslStringWrapperTokenizerParsing() {
    final Parser parser = getParser();
    final Lexicon lexicon = parser.getLexicon();
    final TokenPointerFactory tokenPointerFactory = new LslTokenPointerFactory(lexicon);

    for (String[] block : examples) {
      final StringWrapperLexicalTokenizer tokenizer = new StringWrapperLexicalTokenizer(block[0], tokenPointerFactory, 0);
      doTest(block, tokenizer);
    }
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestParser.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
