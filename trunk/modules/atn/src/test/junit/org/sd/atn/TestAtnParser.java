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
package org.sd.atn;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.sd.token.StandardTokenizer;
import org.sd.token.StandardTokenizerFactory;
import org.sd.token.StandardTokenizerOptions;
import org.sd.token.Tokenizer;
import org.sd.util.tree.Tree;
import org.sd.util.tree.TreeBuilderFactory;
import org.sd.xml.DomDocument;
import org.sd.xml.DomElement;
import org.sd.xml.XmlFactory;

/**
 * JUnit Tests for the AtnParser class.
 * <p>
 * @author Spence Koehler
 */
public class TestAtnParser extends TestCase {

  public TestAtnParser(String name) {
    super(name);
  }
  

  public void testPushingRules() throws IOException {
    // Test pushing rules
    //
    // X <- A W B
    // W <- C D E
    //
    // A C D E B ==>   X
    //               / | \
    //              A  W  B
    //               / | \
    //              C  D  E
    final AtnParser xwParser1 = buildParser("<grammar><rules><X start='true'><A/><W/><B/></X><W><C/><D/><E/></W></rules></grammar>", false);
    final StandardTokenizer tokenizer1 = buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A C D E B");
    runParseTest("ParserTest.1", xwParser1, tokenizer1, null, new String[] { "(X A (W C D E) B)" });
  }

  public void testRepeatAtEnd() throws IOException {
    // Test repeat (at end)
    //
    // X <- Y+
    // Y <- A B
    final AtnParser xyParser1 = buildParser("<grammar><rules><X start='true'><Y repeats='true'/></X><Y><A/><B/></Y></rules></grammar>", false);
    final StandardTokenizer tokenizer2a = buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A B");
    runParseTest("ParserTest.2a", xyParser1, tokenizer2a, null, new String[] { "(X (Y A B))" });

    final StandardTokenizer tokenizer2b = buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A B A B");
    runParseTest("ParserTest.2b", xyParser1, tokenizer2b, null, new String[] { "(X (Y A B) (Y A B))" });
  }

  public void testOptionalRepeatingElements() throws IOException {
    // Test optional repeating elements.
    //
    // X <- A B* A
    //
    // A A ==>  X
    //         / \
    //        A   A
    //
    // A B A ==>  X
    //          / | \
    //         A  B  A
    //
    // A B B A ==>  X
    //           / / \ \
    //          A  B  B  A
    //
    final AtnParser abaParser1 = buildParser("<grammar><rules><X start='true'><A/><B optional='true' repeats='true'/><A/></X></rules></grammar>", false);
    final StandardTokenizer tokenizer3a = buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A A");
    runParseTest("ParserTest.3a", abaParser1, tokenizer3a, null, new String[] { "(X A A)" });

    final StandardTokenizer tokenizer3b = buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A B A");
    runParseTest("ParserTest.3b", abaParser1, tokenizer3b, null, new String[] { "(X A B A)" });

    final StandardTokenizer tokenizer3c = buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A B B A");
    runParseTest("ParserTest.3c", abaParser1, tokenizer3c, null, new String[] { "(X A B B A)" });

    final StandardTokenizer tokenizer3d = buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A B B B A");
    runParseTest("ParserTest.3d", abaParser1, tokenizer3d, null, new String[] { "(X A B B B A)" });
  }

  public void testOptionalElements() throws IOException {
    // Test optional elements
    //
    // X <- A B? A
    //
    // A A ==>  X
    //         / \
    //        A   A
    //
    // A B A ==>  X
    //          / | \
    //         A  B  A
    //
    // A B B A ==>  <FAIL>
    //
    final AtnParser abaParser2 = buildParser("<grammar><rules><X start='true'><A/><B optional='true'/><A/></X></rules></grammar>", false);
    final StandardTokenizer tokenizer4a = buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A A");
    runParseTest("ParserTest.4a", abaParser2, tokenizer4a, null, new String[] { "(X A A)" });

    final StandardTokenizer tokenizer4b = buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A B A");
    runParseTest("ParserTest.4b", abaParser2, tokenizer4b, null, new String[] { "(X A B A)" });

    final StandardTokenizer tokenizer4c = buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A B B A");
    runParseTest("ParserTest.4c", abaParser2, tokenizer4c, null, null);
  }

  public void testOptionallyRepeatedRulesAndOPtionalRuleEndStep() throws IOException {
    // Test pushing optionally repeated rules and optional rule end step
    //
    // X <- A W* B
    // W <- C D E?
    //
    // A C D E B ==>   X
    //               / | \
    //              A  W  B
    //               / | \
    //              C  D  E
    //
    // A C D E C D E B ==>   X
    //                 /   /   \   \
    //                A   W     W   B
    //                   /|\   /|\
    //                  C D E C D E
    //
    // A C D C D E B ==>   X
    //               /   /   \   \
    //              A   W     W   B
    //                 /|    /|\
    //                C D   C D E
    //
    final AtnParser xwParser2 = buildParser("<grammar><rules><X start='true'><A/><W optional='true' repeats='true'/><B/></X><W><C/><D/><E optional='true'/></W></rules></grammar>", false);
    final StandardTokenizer tokenizer5a = buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A C D E B");
    runParseTest("ParserTest.5a", xwParser2, tokenizer5a, null, new String[] { "(X A (W C D E) B)" });

    final StandardTokenizer tokenizer5b = buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A C D E C D E B");
    runParseTest("ParserTest.5b", xwParser2, tokenizer5b, null, new String[] { "(X A (W C D E) (W C D E) B)" });

    final StandardTokenizer tokenizer5c = buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A C D C D E B");
    runParseTest("ParserTest.5c", xwParser2, tokenizer5c, null, new String[] { "(X A (W C D) (W C D E) B)" });
  }

  public void testDoubleLevelOptionalPop() throws IOException {
    // Test double level optional pop
    //
    // X <- A W*
    // W <- B V*
    // V <- C D
    //
    // A B C D ==>  X
    //            /   \
    //           A     W
    //                / \
    //               B   V
    //                  / \
    //                 C   D
    //
    // A B B ==>  X
    //          / | \
    //         A  W  W
    //            |  |
    //            B  B
    //
    // A B B C D B ==>  X
    //               / / \   \
    //              A  W  W   W
    //                 |  | \  \
    //                 B  B  V  B
    //                      / \
    //                     C   D
    //
    final AtnParser xwvParser1 = buildParser("<grammar><rules><X start='true'><A/><W optional='true' repeats='true'/></X><W><B/><V optional='true' repeats='true'/></W><V><C/><D/></V></rules></grammar>", false);
    final StandardTokenizer tokenizer6a = buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A B C D");
    runParseTest("ParserTest.6a", xwvParser1, tokenizer6a, null, new String[] { "(X A (W B (V C D)))" });

    final StandardTokenizer tokenizer6b = buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A B B");
    runParseTest("ParserTest.6b", xwvParser1, tokenizer6b, null, new String[] { "(X A (W B) (W B))" });

    final StandardTokenizer tokenizer6c = buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A B B C D B");
    runParseTest("ParserTest.6c", xwvParser1, tokenizer6c, null, new String[] { "(X A (W B) (W B (V C D)) (W B))" });

    final StandardTokenizer tokenizer6d = buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A B B C");
    runParseTest("ParserTest.6d", xwvParser1, tokenizer6d, null, null);

    final StandardTokenizer tokenizer6e = buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A");
    runParseTest("ParserTest.6e", xwvParser1, tokenizer6e, null, new String[] { "(X A)" });
  }

  public void testAmbiguousParse() throws IOException {
    // Test ambiguous parse
    //
    // X <- Y+ Z?
    // Y <- A? B C
    // Z <- B C
    //
    // A B C B C ==>  X               X
    //              /   \           /   \
    //             Y     Y         Y     Z
    //           / | \   | \     / | \   | \
    //          A  B  C  B  C   A  B  C  B  C
    //
    final AtnParser xyzParser1 = buildParser("<grammar><rules><X start='true'><Y repeats='true'/><Z optional='true'/></X><Y><A optional='true'/><B/><C/></Y><Z><B/><C/></Z></rules></grammar>", false);
    final StandardTokenizer tokenizer7a = buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A B C B C");
    runParseTest("ParserTest.7a", xyzParser1, tokenizer7a, null, new String[] { "(X (Y A B C) (Y B C))", "(X (Y A B C) (Z B C))" });
  }

  public void tetSkippingUnmatchableTokens() throws IOException {
    // Test skipping unmatchable tokens
    //
    // X <- A B C
    //
    // skip = 2
    //
    // A Z B C ==>    X
    //             / / \ \
    //             A ? B C
    //               |
    //               Z
    //
    // A Z Z B C ==>  X
    //           / / / \ \
    //           A ? ? B C
    //             | |
    //             Z Z
    //
    // Z Z A B C ==>  X
    //           / / / \ \
    //           ? ? A B C
    //           | |
    //           Z Z
    //
    // A B C Z Z ==> <FAIL>
    // A Z Z Z B C ==> <FAIL>
    // Z Z Z A B C ==> <FAIL>
    // A B C Z Z Z ==> <FAIL>
    //
    final AtnParser skipParser1 = buildParser("<grammar><rules><X start='true'><A/><B/><C/></X></rules></grammar>", false);
    final StandardTokenizer tokenizer8a = buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A Z B C");
    runParseTest("ParserTest.8a", skipParser1, tokenizer8a, "<parseOptions><skipTokenLimit>2</skipTokenLimit></parseOptions>", new String[] { "(X A (? Z) B C)" });

    final StandardTokenizer tokenizer8b = buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A Z Z B C");
    runParseTest("ParserTest.8b", skipParser1, tokenizer8b, "<parseOptions><skipTokenLimit>2</skipTokenLimit></parseOptions>", new String[] { "(X A (? Z) (? Z) B C)" });

    final StandardTokenizer tokenizer8c = buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "Z Z A B C");
    runParseTest("ParserTest.8c", skipParser1, tokenizer8c, "<parseOptions><skipTokenLimit>2</skipTokenLimit></parseOptions>", new String[] { "(X (? Z) (? Z) A B C)" });

    final StandardTokenizer tokenizer8d = buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A B C Z Z");
    runParseTest("ParserTest.8d", skipParser1, tokenizer8d, "<parseOptions><skipTokenLimit>2</skipTokenLimit></parseOptions>", null);

    final StandardTokenizer tokenizer8e = buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A Z Z Z B C");
    runParseTest("ParserTest.8e", skipParser1, tokenizer8e, "<parseOptions><skipTokenLimit>2</skipTokenLimit></parseOptions>", null);

    final StandardTokenizer tokenizer8f = buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "Z Z Z A B C");
    runParseTest("ParserTest.8f", skipParser1, tokenizer8f, "<parseOptions><skipTokenLimit>2</skipTokenLimit></parseOptions>", null);

    final StandardTokenizer tokenizer8g = buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A B C Z Z Z");
    runParseTest("ParserTest.8g", skipParser1, tokenizer8g, "<parseOptions><skipTokenLimit>2</skipTokenLimit></parseOptions>", null);

    // Disable consume all text and verify that "Z Z Z" remains after parse
    final StandardTokenizer tokenizer8h = buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A B C Z Z Z");
    runParseTest("ParserTest.8h", skipParser1, tokenizer8h, "<parseOptions><skipTokenLimit>2</skipTokenLimit><consumeAllText>false</consumeAllText></parseOptions>", new String[] { "(X A B C)" }, new String[] { "Z Z Z" }, false);

    // Disable consume all text, don't skip, and verify that initial "Z Z Z" is skipped while trailing "Z Z Z" remains after parse
    final StandardTokenizer tokenizer8i = buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "Z Z Z A B C Z Z Z");
    runParseTest("ParserTest.8i", skipParser1, tokenizer8i, "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>false</consumeAllText></parseOptions>", new String[] { "(X A B C)" }, new String[] { "Z Z Z" }, true);
  }

  public void testEndConditionsWithOptionalOptionalRquiredGrammar() throws IOException {
    // test end conditions with a optional-optional-required grammar
    final AtnParser oorParser = buildParser("<grammar><rules><A start='true'><X optional='true'/><Y optional='true'/><Z/></A><B start='true'><X optional='true'/><Z optional='true'/><Y/></B></rules></grammar>", false);
    final StandardTokenizer tokenizer8j = buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "Y");
    runParseTest("ParserTest.8j", oorParser, tokenizer8j, "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>", new String[] { "(B Y)" });
  }

  public void testNonOptionalRepeatingEnd() throws IOException {
    // test a non-optional repeating end isn't considered to be optional.
    final AtnParser noreParser = buildParser("<grammar><rules><X start='true'><A optional='true'/><B repeats='true'/></X></rules></grammar>", false);
    final StandardTokenizer tokenizer9a = buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A");
    runParseTest("ParserTest.9a", noreParser, tokenizer9a, "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>", null);

    final StandardTokenizer tokenizer9b = buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A B");
    runParseTest("ParserTest.9b", noreParser, tokenizer9b, "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>", new String[] { "(X A B)" });

    final StandardTokenizer tokenizer9c = buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A B B B");
    runParseTest("ParserTest.9c", noreParser, tokenizer9c, "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>", new String[] { "(X A B B B)" });

    final StandardTokenizer tokenizer9d = buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "B B B");
    runParseTest("ParserTest.9d", noreParser, tokenizer9d, "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>", new String[] { "(X B B B)" });

    final StandardTokenizer tokenizer9e = buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "B");
    runParseTest("ParserTest.9e", noreParser, tokenizer9e, "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>", new String[] { "(X B)" });
  }

  public void testNonOptionalRepeatEndAfterPush() throws IOException {
    // test a non-optional repeat end after a "push" isn't considered to be optional.
    final AtnParser norepParser = buildParser("<grammar><rules><X start='true'><Y optional='true'/><B repeats='true'/></X><Y><A/></Y></rules></grammar>", false);
    final StandardTokenizer tokenizer9f = buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A");
    runParseTest("ParserTest.9f", norepParser, tokenizer9f, "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>", null);

    final StandardTokenizer tokenizer9g = buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A B");
    runParseTest("ParserTest.9g", norepParser, tokenizer9g, "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>", new String[] { "(X (Y A) B)" });

    final StandardTokenizer tokenizer9h = buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A B B B");
    runParseTest("ParserTest.9h", norepParser, tokenizer9h, "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>", new String[] { "(X (Y A) B B B)" });

    final StandardTokenizer tokenizer9i = buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "B B B");
    runParseTest("ParserTest.9i", norepParser, tokenizer9i, "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>", new String[] { "(X B B B)" });

    final StandardTokenizer tokenizer9j = buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "B");
    runParseTest("ParserTest.9j", norepParser, tokenizer9j, "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>", new String[] { "(X B)" });
  }

  public void testMultipleParses1() throws IOException {
    final AtnParser test10_Parser = buildParser("<grammar><rules><X start='true'><A/><Y optional='true' repeats='true'/></X><Y><Z optional='true'/><A repeats='true'/></Y><Z><B/><D optional='true'/></Z></rules></grammar>", false);
    final StandardTokenizer tokenizer10a = buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A B B A");
    runParseTest("ParserTest.10a", test10_Parser, tokenizer10a, "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>", null);
    final StandardTokenizer tokenizer10b = buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A B A B A");
    runParseTest("ParserTest.10b", test10_Parser, tokenizer10b, "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>", new String[] { "(X A (Y (Z B) A) (Y (Z B) A))" });
    final StandardTokenizer tokenizer10c = buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A A B A A A B A A");
    runParseTest("ParserTest.10c",
                  test10_Parser,
                  tokenizer10c,
                  "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                  new String[] {
                    "(X A (Y A) (Y (Z B) A) (Y A) (Y A) (Y (Z B) A) (Y A))",
                    "(X A (Y A) (Y (Z B) A) (Y A) (Y A) (Y (Z B) A A))",
                    "(X A (Y A) (Y (Z B) A) (Y A A) (Y (Z B) A) (Y A))",
                    "(X A (Y A) (Y (Z B) A) (Y A A) (Y (Z B) A A))",
                    "(X A (Y A) (Y (Z B) A A) (Y A) (Y (Z B) A) (Y A))",
                    "(X A (Y A) (Y (Z B) A A) (Y A) (Y (Z B) A A))",
                    "(X A (Y A) (Y (Z B) A A A) (Y (Z B) A) (Y A))",
                    "(X A (Y A) (Y (Z B) A A A) (Y (Z B) A A))"
                  });
  }

  public void testNoClustering() throws IOException {
    // A <- B+
    // B <- C+
    final AtnParser test11_Parser = buildParser("<grammar><rules><A start='true'><B repeats='true'/></A><B><C repeats='true'/></B></rules></grammar>", false);
    final StandardTokenizer tokenizer11 = buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "C C C");
    runParseTest("ParserTest.11",
                 test11_Parser,
                 tokenizer11,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(A (B C) (B C) (B C))",
                   "(A (B C) (B C C))",
                   "(A (B C C) (B C))",
                   "(A (B C C C))",
                 });
  }

  public void testClustering() throws IOException {
    // A <- B+
    // B <- C+
    final AtnParser test11_Parser = buildParser("<grammar><rules><A start='true'><B repeats='true'/></A><B><C repeats='true' cluster='true'/></B></rules></grammar>", false);
    final StandardTokenizer tokenizer11 = buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "C C C");
    runParseTest("ParserTest.11",
                 test11_Parser,
                 tokenizer11,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                  new String[] {
                   "(A (B C C C))",
                   });
  }


  private final DomElement stringToXml(String xmlString, boolean htmlFlag) throws IOException {
    final DomDocument domDocument = XmlFactory.loadDocument(xmlString, htmlFlag);
    return (DomElement)domDocument.getDocumentElement();
  }

  final private AtnParser buildParser(String grammarXml, boolean htmlFlag) throws IOException {
    final DomElement domElement = stringToXml(grammarXml, htmlFlag);
    final AtnGrammar grammar = new AtnGrammar(domElement, new ResourceManager());
    return new AtnParser(grammar);
  }

  private final StandardTokenizer buildTokenizer(String tokenizerOptionsXml, String inputString) throws IOException {
    final StandardTokenizerOptions tokenizerOptions =
      tokenizerOptionsXml == null ?
      new StandardTokenizerOptions() :
      new StandardTokenizerOptions(stringToXml(tokenizerOptionsXml, false));
    return StandardTokenizerFactory.getTokenizer(inputString, tokenizerOptions);
  }

  private final void runParseTest(String name, AtnParser parser, StandardTokenizer tokenizer, String parseOptionsXml, String[] expectedTreeStrings) throws IOException {
    runParseTest(name, parser, tokenizer, parseOptionsXml, expectedTreeStrings, null, false);
  }

  private final void runParseTest(String name, AtnParser parser, StandardTokenizer tokenizer, String parseOptionsXml, String[] expectedTreeStrings, String[] expectedRemainder, boolean seek) throws IOException {

    final ResourceManager resourceManager = new ResourceManager();

    final AtnParseOptions parseOptions =
      parseOptionsXml == null ?
      new AtnParseOptions(resourceManager) :
      new AtnParseOptions(stringToXml(parseOptionsXml, false), resourceManager);

    List<Tree<String>> expectedOutput = null;

    if (expectedTreeStrings != null) {
      expectedOutput = new ArrayList<Tree<String>>();

      for (String expectedTreeString : expectedTreeStrings) {
        final Tree<String> expectedTree = TreeBuilderFactory.getStringTreeBuilder().buildTree(expectedTreeString);
        expectedOutput.add(expectedTree);
      }
    }

    final AtnParseTest atnParseTest = new AtnParseTest(name, parser, tokenizer, parseOptions, expectedOutput, expectedRemainder, seek);
    atnParseTest.runTest();
  }


  private final class AtnParseTest {

    private String name;
    private AtnParser parser;
    private Tokenizer tokenizer;
    private AtnParseOptions options;
    private List<Tree<String>> expectedOutput;
    private String[] expectedRemainder;
    private boolean seek;
    private Set<Integer> stopList;

    public AtnParseTest(String name, AtnParser parser, Tokenizer tokenizer, AtnParseOptions options, List<Tree<String>> expectedOutput) {
      init(name, parser, tokenizer, options, expectedOutput, null, false, null);
    }

    public AtnParseTest(String name, AtnParser parser, Tokenizer tokenizer, AtnParseOptions options, List<Tree<String>> expectedOutput, String[] expectedRemainder, boolean seek) {
      init(name, parser, tokenizer, options, expectedOutput, expectedRemainder, seek, null);
    }

    public AtnParseTest(String name, AtnParser parser, Tokenizer tokenizer, AtnParseOptions options, List<Tree<String>> expectedOutput, String[] expectedRemainder, boolean seek, Set<Integer> stopList) {
      init(name, parser, tokenizer, options, expectedOutput, expectedRemainder, seek, stopList);
    }

    private final void init(String name, AtnParser parser, Tokenizer tokenizer, AtnParseOptions options, List<Tree<String>> expectedOutput, String[] expectedRemainder, boolean seek, Set<Integer> stopList) {
      this.name = name;
      this.parser = parser;
      this.tokenizer = tokenizer;
      this.options = options;
      this.expectedOutput = expectedOutput;
      this.expectedRemainder = expectedRemainder;
      this.seek = seek;
      this.stopList = stopList;
    }

    public void runTest() {
      final AtnParseResult parseResult = seek ? parser.seekParse(tokenizer, options, stopList) : parser.parse(tokenizer, options, stopList);


      if (expectedOutput == null) {
        final AtnParse parse = parseResult.getParse(0);
        final Tree<String> parseTree = parse == null ? null : parse.getParseTree();

        if (parse != null) {
          System.out.println("\nParses:");
          for (int i = 0; i < parseResult.getNumParses(); ++i) {
            final AtnParse curParse = parseResult.getParse(i);
            final Tree<String> curParseTree = curParse == null ? null : curParse.getParseTree();
            System.out.println("\tparse #" + i + ": " + curParseTree.toString());
          }
        }

        assertNull(name + ": Got parse '" + (parse == null ? "<NULL>" : parseTree.toString()) + "(and " + (parseResult.getNumParses() - 1) + " others) where none was expected.",
                   parse);
      }
      else {
        parseResult.generateParses(expectedOutput.size());

        for (int parseNum = 0; parseNum < expectedOutput.size(); ++parseNum) {
          final AtnParse parse = parseResult.getParse(parseNum);
          final Tree<String> parseTree = parse == null ? null : parse.getParseTree();
          final Tree<String> expectedParse = expectedOutput.get(parseNum);

          assertEquals(name + ": Bad parse #" + parseNum + ".",
                       expectedParse, parseTree);

          if (expectedRemainder != null && parseNum < expectedRemainder.length) {

            assertEquals(name + ": Mismatched remainder for parse #" + parseNum + "'.",
                         expectedRemainder[parseNum], parse.getRemainingText());
          }
        }
      }

      parseResult.generateParses(0);
      final int expectedCount = expectedOutput == null ? 1 : expectedOutput.size();
      if (parseResult.getNumParses() > expectedCount) {
        System.out.println("*** WARNING: " + name + " yielded " + parseResult.getNumParses() +
                           " parses when expected " + expectedCount + ".");

        for (int parseNum = 0; parseNum < parseResult.getNumParses(); ++parseNum) {
          System.out.println("\t" + parseResult.getParse(parseNum).getParseTree().toString());
        }
      }
    }
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestAtnParser.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
