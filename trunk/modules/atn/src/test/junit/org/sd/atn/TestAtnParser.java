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
import org.sd.token.Tokenizer;
import org.sd.util.tree.Tree;

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
    final AtnParser xwParser1 = AtnParseTest.buildParser("<grammar><rules><X start='true'><A/><W/><B/></X><W><C/><D/><E/></W></rules></grammar>", false);
    final StandardTokenizer tokenizer1 = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A C D E B");
    runParseTest("ParserTest.1", xwParser1, tokenizer1, null, new String[] { "(X A (W C D E) B)" });
  }

  public void testRepeatAtEnd() throws IOException {
    // Test repeat (at end)
    //
    // X <- Y+
    // Y <- A B
    final AtnParser xyParser1 = AtnParseTest.buildParser("<grammar><rules><X start='true'><Y repeats='true'/></X><Y><A/><B/></Y></rules></grammar>", false);
    final StandardTokenizer tokenizer2a = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A B");
    runParseTest("ParserTest.2a", xyParser1, tokenizer2a, null, new String[] { "(X (Y A B))" });

    final StandardTokenizer tokenizer2b = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A B A B");
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
    final AtnParser abaParser1 = AtnParseTest.buildParser("<grammar><rules><X start='true'><A/><B optional='true' repeats='true'/><A/></X></rules></grammar>", false);
    final StandardTokenizer tokenizer3a = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A A");
    runParseTest("ParserTest.3a", abaParser1, tokenizer3a, null, new String[] { "(X A A)" });

    final StandardTokenizer tokenizer3b = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A B A");
    runParseTest("ParserTest.3b", abaParser1, tokenizer3b, null, new String[] { "(X A B A)" });

    final StandardTokenizer tokenizer3c = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A B B A");
    runParseTest("ParserTest.3c", abaParser1, tokenizer3c, null, new String[] { "(X A B B A)" });

    final StandardTokenizer tokenizer3d = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A B B B A");
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
    final AtnParser abaParser2 = AtnParseTest.buildParser("<grammar><rules><X start='true'><A/><B optional='true'/><A/></X></rules></grammar>", false);
    final StandardTokenizer tokenizer4a = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A A");
    runParseTest("ParserTest.4a", abaParser2, tokenizer4a, null, new String[] { "(X A A)" });

    final StandardTokenizer tokenizer4b = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A B A");
    runParseTest("ParserTest.4b", abaParser2, tokenizer4b, null, new String[] { "(X A B A)" });

    final StandardTokenizer tokenizer4c = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A B B A");
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
    final AtnParser xwParser2 = AtnParseTest.buildParser("<grammar><rules><X start='true'><A/><W optional='true' repeats='true'/><B/></X><W><C/><D/><E optional='true'/></W></rules></grammar>", false);
    final StandardTokenizer tokenizer5a = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A C D E B");
    runParseTest("ParserTest.5a", xwParser2, tokenizer5a, null, new String[] { "(X A (W C D E) B)" });

    final StandardTokenizer tokenizer5b = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A C D E C D E B");
    runParseTest("ParserTest.5b", xwParser2, tokenizer5b, null, new String[] { "(X A (W C D E) (W C D E) B)" });

    final StandardTokenizer tokenizer5c = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A C D C D E B");
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
    final AtnParser xwvParser1 = AtnParseTest.buildParser("<grammar><rules><X start='true'><A/><W optional='true' repeats='true'/></X><W><B/><V optional='true' repeats='true'/></W><V><C/><D/></V></rules></grammar>", false);
    final StandardTokenizer tokenizer6a = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A B C D");
    runParseTest("ParserTest.6a", xwvParser1, tokenizer6a, null, new String[] { "(X A (W B (V C D)))" });

    final StandardTokenizer tokenizer6b = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A B B");
    runParseTest("ParserTest.6b", xwvParser1, tokenizer6b, null, new String[] { "(X A (W B) (W B))" });

    final StandardTokenizer tokenizer6c = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A B B C D B");
    runParseTest("ParserTest.6c", xwvParser1, tokenizer6c, null, new String[] { "(X A (W B) (W B (V C D)) (W B))" });

    final StandardTokenizer tokenizer6d = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A B B C");
    runParseTest("ParserTest.6d", xwvParser1, tokenizer6d, null, null);

    final StandardTokenizer tokenizer6e = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A");
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
    final AtnParser xyzParser1 = AtnParseTest.buildParser("<grammar><rules><X start='true'><Y repeats='true'/><Z optional='true'/></X><Y><A optional='true'/><B/><C/></Y><Z><B/><C/></Z></rules></grammar>", false);
    final StandardTokenizer tokenizer7a = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A B C B C");
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
    final AtnParser skipParser1 = AtnParseTest.buildParser("<grammar><rules><X start='true'><A/><B/><C/></X></rules></grammar>", false);
    final StandardTokenizer tokenizer8a = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A Z B C");
    runParseTest("ParserTest.8a", skipParser1, tokenizer8a, "<parseOptions><skipTokenLimit>2</skipTokenLimit></parseOptions>", new String[] { "(X A (? Z) B C)" });

    final StandardTokenizer tokenizer8b = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A Z Z B C");
    runParseTest("ParserTest.8b", skipParser1, tokenizer8b, "<parseOptions><skipTokenLimit>2</skipTokenLimit></parseOptions>", new String[] { "(X A (? Z) (? Z) B C)" });

    final StandardTokenizer tokenizer8c = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "Z Z A B C");
    runParseTest("ParserTest.8c", skipParser1, tokenizer8c, "<parseOptions><skipTokenLimit>2</skipTokenLimit></parseOptions>", new String[] { "(X (? Z) (? Z) A B C)" });

    final StandardTokenizer tokenizer8d = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A B C Z Z");
    runParseTest("ParserTest.8d", skipParser1, tokenizer8d, "<parseOptions><skipTokenLimit>2</skipTokenLimit></parseOptions>", null);

    final StandardTokenizer tokenizer8e = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A Z Z Z B C");
    runParseTest("ParserTest.8e", skipParser1, tokenizer8e, "<parseOptions><skipTokenLimit>2</skipTokenLimit></parseOptions>", null);

    final StandardTokenizer tokenizer8f = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "Z Z Z A B C");
    runParseTest("ParserTest.8f", skipParser1, tokenizer8f, "<parseOptions><skipTokenLimit>2</skipTokenLimit></parseOptions>", null);

    final StandardTokenizer tokenizer8g = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A B C Z Z Z");
    runParseTest("ParserTest.8g", skipParser1, tokenizer8g, "<parseOptions><skipTokenLimit>2</skipTokenLimit></parseOptions>", null);

    // Disable consume all text and verify that "Z Z Z" remains after parse
    final StandardTokenizer tokenizer8h = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A B C Z Z Z");
    runParseTest("ParserTest.8h", skipParser1, tokenizer8h, "<parseOptions><skipTokenLimit>2</skipTokenLimit><consumeAllText>false</consumeAllText></parseOptions>", new String[] { "(X A B C)" }, new String[] { "Z Z Z" }, false);

    // Disable consume all text, don't skip, and verify that initial "Z Z Z" is skipped while trailing "Z Z Z" remains after parse
    final StandardTokenizer tokenizer8i = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "Z Z Z A B C Z Z Z");
    runParseTest("ParserTest.8i", skipParser1, tokenizer8i, "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>false</consumeAllText></parseOptions>", new String[] { "(X A B C)" }, new String[] { "Z Z Z" }, true);
  }

  public void testEndConditionsWithOptionalOptionalRquiredGrammar() throws IOException {
    // test end conditions with a optional-optional-required grammar
    final AtnParser oorParser = AtnParseTest.buildParser("<grammar><rules><A start='true'><X optional='true'/><Y optional='true'/><Z/></A><B start='true'><X optional='true'/><Z optional='true'/><Y/></B></rules></grammar>", false);
    final StandardTokenizer tokenizer8j = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "Y");
    runParseTest("ParserTest.8j", oorParser, tokenizer8j, "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>", new String[] { "(B Y)" });
  }

  public void testNonOptionalRepeatingEnd() throws IOException {
    // test a non-optional repeating end isn't considered to be optional.
    final AtnParser noreParser = AtnParseTest.buildParser("<grammar><rules><X start='true'><A optional='true'/><B repeats='true'/></X></rules></grammar>", false);
    final StandardTokenizer tokenizer9a = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A");
    runParseTest("ParserTest.9a", noreParser, tokenizer9a, "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>", null);

    final StandardTokenizer tokenizer9b = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A B");
    runParseTest("ParserTest.9b", noreParser, tokenizer9b, "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>", new String[] { "(X A B)" });

    final StandardTokenizer tokenizer9c = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A B B B");
    runParseTest("ParserTest.9c", noreParser, tokenizer9c, "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>", new String[] { "(X A B B B)" });

    final StandardTokenizer tokenizer9d = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "B B B");
    runParseTest("ParserTest.9d", noreParser, tokenizer9d, "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>", new String[] { "(X B B B)" });

    final StandardTokenizer tokenizer9e = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "B");
    runParseTest("ParserTest.9e", noreParser, tokenizer9e, "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>", new String[] { "(X B)" });
  }

  public void testNonOptionalRepeatEndAfterPush() throws IOException {
    // test a non-optional repeat end after a "push" isn't considered to be optional.
    final AtnParser norepParser = AtnParseTest.buildParser("<grammar><rules><X start='true'><Y optional='true'/><B repeats='true'/></X><Y><A/></Y></rules></grammar>", false);
    final StandardTokenizer tokenizer9f = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A");
    runParseTest("ParserTest.9f", norepParser, tokenizer9f, "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>", null);

    final StandardTokenizer tokenizer9g = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A B");
    runParseTest("ParserTest.9g", norepParser, tokenizer9g, "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>", new String[] { "(X (Y A) B)" });

    final StandardTokenizer tokenizer9h = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A B B B");
    runParseTest("ParserTest.9h", norepParser, tokenizer9h, "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>", new String[] { "(X (Y A) B B B)" });

    final StandardTokenizer tokenizer9i = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "B B B");
    runParseTest("ParserTest.9i", norepParser, tokenizer9i, "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>", new String[] { "(X B B B)" });

    final StandardTokenizer tokenizer9j = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "B");
    runParseTest("ParserTest.9j", norepParser, tokenizer9j, "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>", new String[] { "(X B)" });
  }

  public void testMultipleParses1() throws IOException {
    final AtnParser test10_Parser = AtnParseTest.buildParser("<grammar><rules><X start='true'><A/><Y optional='true' repeats='true'/></X><Y><Z optional='true'/><A repeats='true'/></Y><Z><B/><D optional='true'/></Z></rules></grammar>", false);
    final StandardTokenizer tokenizer10a = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A B B A");
    runParseTest("ParserTest.10a", test10_Parser, tokenizer10a, "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>", null);
    final StandardTokenizer tokenizer10b = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A B A B A");
    runParseTest("ParserTest.10b", test10_Parser, tokenizer10b, "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>", new String[] { "(X A (Y (Z B) A) (Y (Z B) A))" });
    final StandardTokenizer tokenizer10c = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A A B A A A B A A");
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
    final AtnParser test11_Parser = AtnParseTest.buildParser("<grammar><rules><A start='true'><B repeats='true'/></A><B><C repeats='true'/></B></rules></grammar>", false);
    final StandardTokenizer tokenizer11 = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "C C C");
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
    final AtnParser test11_Parser = AtnParseTest.buildParser("<grammar><rules><A start='true'><B repeats='true'/></A><B><C repeats='true' cluster='true'/></B></rules></grammar>", false);
    final StandardTokenizer tokenizer11 = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "C C C");
    runParseTest("ParserTest.11",
                 test11_Parser,
                 tokenizer11,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                  new String[] {
                   "(A (B C C C))",
                   });
  }

  public void testStepRequireFlat() throws IOException {
    // A <- B C? B?(rC) D
    final AtnParser test12_Parser = AtnParseTest.buildParser("<grammar><rules><A start='true'><B optional='true'/><C optional='true'/><B require='C' optional='true'/><D/></A></rules></grammar>", false);

    // no parses
    final StandardTokenizer tokenizer12b = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "B B D");
    runParseTest("ParserTest.12b",
                 test12_Parser,
                 tokenizer12b,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 null);

    // parse
    final StandardTokenizer tokenizer12a = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "B C B D");
    runParseTest("ParserTest.12a",
                 test12_Parser,
                 tokenizer12a,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(A B C B D)",
                 });

    // parse
    final StandardTokenizer tokenizer12c = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "B C D");
    runParseTest("ParserTest.12c",
                 test12_Parser,
                 tokenizer12c,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(A B C D)",
                 });

    // parse
    final StandardTokenizer tokenizer12d = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "C B D");
    runParseTest("ParserTest.12d",
                 test12_Parser,
                 tokenizer12d,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(A C B D)",
                 });

    // parse
    final StandardTokenizer tokenizer12e = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "B D");
    runParseTest("ParserTest.12e",
                 test12_Parser,
                 tokenizer12e,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(A B D)",
                 });

    // parse
    final StandardTokenizer tokenizer12f = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "C D");
    runParseTest("ParserTest.12f",
                 test12_Parser,
                 tokenizer12f,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(A C D)",
                 });

    // parse
    final StandardTokenizer tokenizer12g = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "D");
    runParseTest("ParserTest.12g",
                 test12_Parser,
                 tokenizer12g,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(A D)",
                 });
  }

  public void testStepRequireDeep() throws IOException {
    // X <- Y B? C?(rB) D
    // Y <- A C?
    final AtnParser test12_Parser = AtnParseTest.buildParser("<grammar><rules><X start='true'><Y/><B optional='true'/><C require='B' optional='true'/><D/></X><Y><A/><C optional='true'/></Y></rules></grammar>", false);

    // no parses
    final StandardTokenizer tokenizer12b = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A C C D");
    runParseTest("ParserTest.12b",
                 test12_Parser,
                 tokenizer12b,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 null);

    // parse
    final StandardTokenizer tokenizer12a = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A C D");
    runParseTest("ParserTest.12a",
                 test12_Parser,
                 tokenizer12a,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(X (Y A C) D)",
                 });

    // parse
    final StandardTokenizer tokenizer12c = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A B C D");
    runParseTest("ParserTest.12c",
                 test12_Parser,
                 tokenizer12c,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(X (Y A) B C D)",
                 });

    // parse
    final StandardTokenizer tokenizer12d = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A D");
    runParseTest("ParserTest.12d",
                 test12_Parser,
                 tokenizer12d,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(X (Y A) D)",
                 });

    // parse
    final StandardTokenizer tokenizer12e = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A C B C D");
    runParseTest("ParserTest.12e",
                 test12_Parser,
                 tokenizer12e,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(X (Y A C) B C D)",
                 });
  }

  public void testStepRequireConstituent() throws IOException {
    // X <- Y B? C?(rB) D
    // Y <- A C?
    // B <- Q P
    final AtnParser test12_Parser = AtnParseTest.buildParser("<grammar><rules><X start='true'><Y/><B optional='true'/><C require='B' optional='true'/><D/></X><Y><A/><C optional='true'/></Y><B><Q/><P/></B></rules></grammar>", false);

    // no parses
    final StandardTokenizer tokenizer12b = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A C C D");
    runParseTest("ParserTest.12b",
                 test12_Parser,
                 tokenizer12b,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 null);

    // parse
    final StandardTokenizer tokenizer12a = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A C D");
    runParseTest("ParserTest.12a",
                 test12_Parser,
                 tokenizer12a,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(X (Y A C) D)",
                 });

    // parse
    final StandardTokenizer tokenizer12c = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A Q P C D");
    runParseTest("ParserTest.12c",
                 test12_Parser,
                 tokenizer12c,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(X (Y A) (B Q P) C D)",
                 });

    // parse
    final StandardTokenizer tokenizer12d = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A D");
    runParseTest("ParserTest.12d",
                 test12_Parser,
                 tokenizer12d,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(X (Y A) D)",
                 });

    // parse
    final StandardTokenizer tokenizer12e = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A C Q P C D");
    runParseTest("ParserTest.12e",
                 test12_Parser,
                 tokenizer12e,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(X (Y A C) (B Q P) C D)",
                 });
  }


//I'm here...
  public void testStepUnlessFlat() throws IOException {
    // A <- B C? B?(uC) D
    final AtnParser test12_Parser = AtnParseTest.buildParser("<grammar><rules><A start='true'><B optional='true'/><C optional='true'/><B unless='C' optional='true'/><D/></A></rules></grammar>", false);

    // no parses
    final StandardTokenizer tokenizer12b = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "B B D");
    runParseTest("ParserTest.12b",
                 test12_Parser,
                 tokenizer12b,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(A B B D)",
                 });

    // parse
    final StandardTokenizer tokenizer12a = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "B C B D");
    runParseTest("ParserTest.12a",
                 test12_Parser,
                 tokenizer12a,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 null);

    // parse
    final StandardTokenizer tokenizer12c = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "B C D");
    runParseTest("ParserTest.12c",
                 test12_Parser,
                 tokenizer12c,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(A B C D)"
                 });

    // parse
    final StandardTokenizer tokenizer12d = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "C B D");
    runParseTest("ParserTest.12d",
                 test12_Parser,
                 tokenizer12d,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 null);

    // parse
    final StandardTokenizer tokenizer12e = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "B D");
    runParseTest("ParserTest.12e",
                 test12_Parser,
                 tokenizer12e,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(A B D)",
                 });

    // parse
    final StandardTokenizer tokenizer12f = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "C D");
    runParseTest("ParserTest.12f",
                 test12_Parser,
                 tokenizer12f,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(A C D)",
                 });

    // parse
    final StandardTokenizer tokenizer12g = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "D");
    runParseTest("ParserTest.12g",
                 test12_Parser,
                 tokenizer12g,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(A D)",
                 });
  }

  public void testStepUnlessDeep() throws IOException {
    // X <- Y B? C?(uB) D
    // Y <- A C?
    final AtnParser test12_Parser = AtnParseTest.buildParser("<grammar><rules><X start='true'><Y/><B optional='true'/><C unless='B' optional='true'/><D/></X><Y><A/><C optional='true'/></Y></rules></grammar>", false);

    // no parses
    final StandardTokenizer tokenizer12b = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A C C D");
    runParseTest("ParserTest.12b",
                 test12_Parser,
                 tokenizer12b,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(X (Y A C) C D)",
                 });

    // parse
    final StandardTokenizer tokenizer12a = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A C D");
    runParseTest("ParserTest.12a",
                 test12_Parser,
                 tokenizer12a,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(X (Y A) C D)",
                   "(X (Y A C) D)",
                 });

    // parse
    final StandardTokenizer tokenizer12c = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A B C D");
    runParseTest("ParserTest.12c",
                 test12_Parser,
                 tokenizer12c,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 null);

    // parse
    final StandardTokenizer tokenizer12d = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A D");
    runParseTest("ParserTest.12d",
                 test12_Parser,
                 tokenizer12d,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(X (Y A) D)",
                 });

    // parse
    final StandardTokenizer tokenizer12e = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A C B C D");
    runParseTest("ParserTest.12e",
                 test12_Parser,
                 tokenizer12e,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 null);
  }

  public void testStepUnlessConstituent() throws IOException {
    // X <- Y B? C?(uB) D
    // Y <- A C?
    // B <- Q P
    final AtnParser test12_Parser = AtnParseTest.buildParser("<grammar><rules><X start='true'><Y/><B optional='true'/><C unless='B' optional='true'/><D/></X><Y><A/><C optional='true'/></Y><B><Q/><P/></B></rules></grammar>", false);

    // no parses
    final StandardTokenizer tokenizer12b = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A C C D");
    runParseTest("ParserTest.12b",
                 test12_Parser,
                 tokenizer12b,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(X (Y A C) C D)",
                 });

    // parse
    final StandardTokenizer tokenizer12a = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A C D");
    runParseTest("ParserTest.12a",
                 test12_Parser,
                 tokenizer12a,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(X (Y A) C D)",
                   "(X (Y A C) D)",
                 });

    // parse
    final StandardTokenizer tokenizer12c = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A Q P C D");
    runParseTest("ParserTest.12c",
                 test12_Parser,
                 tokenizer12c,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 null);

    // parse
    final StandardTokenizer tokenizer12d = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A D");
    runParseTest("ParserTest.12d",
                 test12_Parser,
                 tokenizer12d,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(X (Y A) D)",
                 });

    // parse
    final StandardTokenizer tokenizer12e = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A C Q P C D");
    runParseTest("ParserTest.12e",
                 test12_Parser,
                 tokenizer12e,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 null);
  }

  public void testConstituentStepUnlessConstituent() throws IOException {
    // X <- A? Y?(uA) Z?(uY)
    // Y <- B
    // Z <- C
    final AtnParser test12B_Parser = AtnParseTest.buildParser("<grammar><rules><X start='true'><A optional='true'/><Y optional='true' unless='A'/><Z optional='true' unless='Y'/></X><Y><B/></Y><Z><C/></Z></rules></grammar>", false);

    // Succeed:
    // - B
    // - C
    // - A
    final StandardTokenizer tokenizer12Ba = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "B"); 
    runParseTest("ParserTest.12Ba",
                 test12B_Parser,
                 tokenizer12Ba,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(X (Y B))",
                 });
    final StandardTokenizer tokenizer12Bb = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "C"); 
    runParseTest("ParserTest.12Bb",
                 test12B_Parser,
                 tokenizer12Bb,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(X (Z C))",
                 });

    final StandardTokenizer tokenizer12Bc = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A"); 
    runParseTest("ParserTest.12Bc",
                 test12B_Parser,
                 tokenizer12Bc,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(X A)",
                 });

    // Fail:
    // - A B
    // - A B C
    // - B C
    final StandardTokenizer tokenizer12Bd = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A B"); 
    runParseTest("ParserTest.12Bd",
                 test12B_Parser,
                 tokenizer12Bd,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 null);

    final StandardTokenizer tokenizer12Be = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A B C"); 
    runParseTest("ParserTest.12Be",
                 test12B_Parser,
                 tokenizer12Be,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 null);

    final StandardTokenizer tokenizer12Bf = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "B C"); 
    runParseTest("ParserTest.12Bf",
                 test12B_Parser,
                 tokenizer12Bf,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 null);
  }

  public void testUnlessForOptionalPlacement() throws IOException {
    // A <- C? B? Z_rB? D C_uC?  (optionally places C at beginning or end)
    // B <- X Y
    // C <- W
    // D <- Y X
    final AtnParser test12C_Parser = AtnParseTest.buildParser("<grammar><rules><A start='true'><C optional='true'/><B optional='true'/><Z optional='true' require='B'/><D/><C optional='true' unless='C'/></A><B><X/><Y/></B><C><W/></C><D><Y/><X/></D></rules></grammar>", false);

    // W Y X (good)
    final StandardTokenizer tokenizer12Ca = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "W Y X");
    runParseTest("ParserTest.12Ca",
                 test12C_Parser,
                 tokenizer12Ca,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(A (C W) (D Y X))"
                 });
    // Y X W (good)
    final StandardTokenizer tokenizer12Cb = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "Y X W");
    runParseTest("ParserTest.12Cb",
                 test12C_Parser,
                 tokenizer12Cb,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(A (D Y X) (C W))"
                 });
    // Y X (good)
    final StandardTokenizer tokenizer12Cc = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "Y X");
    runParseTest("ParserTest.12Cc",
                 test12C_Parser,
                 tokenizer12Cc,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(A (D Y X))"
                 });
    // W Y X W (bad)
    final StandardTokenizer tokenizer12Cd = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "W Y X W");
    runParseTest("ParserTest.12Cd",
                 test12C_Parser,
                 tokenizer12Cd,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 null);


    // W X Y Y X (good)
    final StandardTokenizer tokenizer12Ce = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "W X Y Y X");
    runParseTest("ParserTest.12Ce",
                 test12C_Parser,
                 tokenizer12Ce,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(A (C W) (B X Y) (D Y X))"
                 });
    // X Y Y X W (good)
    final StandardTokenizer tokenizer12Cf = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "X Y Y X W");
    runParseTest("ParserTest.12Cf",
                 test12C_Parser,
                 tokenizer12Cf,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(A (B X Y) (D Y X) (C W))"
                 });
    // X Y Y X (good)
    final StandardTokenizer tokenizer12Cg = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "X Y Y X");
    runParseTest("ParserTest.12Cg",
                 test12C_Parser,
                 tokenizer12Cg,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(A (B X Y) (D Y X))"
                 });
    // W X Y Y X W (bad)
    final StandardTokenizer tokenizer12Ch = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "W X Y Y X W");
    runParseTest("ParserTest.12Ch",
                 test12C_Parser,
                 tokenizer12Ch,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 null);


    // W X Y Z Y X (good)
    final StandardTokenizer tokenizer12Ci = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "W X Y Z Y X");
    runParseTest("ParserTest.12Ci",
                 test12C_Parser,
                 tokenizer12Ci,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(A (C W) (B X Y) Z (D Y X))"
                 });
    // X Y Z Y X W (good)
    final StandardTokenizer tokenizer12Cj = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "X Y Z Y X W");
    runParseTest("ParserTest.12Cj",
                 test12C_Parser,
                 tokenizer12Cj,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(A (B X Y) Z (D Y X) (C W))"
                 });
    // X Y Z Y X (good)
    final StandardTokenizer tokenizer12Ck = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "X Y Z Y X");
    runParseTest("ParserTest.12Ck",
                 test12C_Parser,
                 tokenizer12Ck,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(A (B X Y) Z (D Y X))"
                 });
    // W X Y Z Y X W (bad)
    final StandardTokenizer tokenizer12Cl = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "W X Y Z Y X W");
    runParseTest("ParserTest.12Cl",
                 test12C_Parser,
                 tokenizer12Cl,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 null);

    // W Z Y X (bad -- requires (B X Y) before Z)
    final StandardTokenizer tokenizer12Cm = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "W Z Y X");
    runParseTest("ParserTest.12Cm",
                 test12C_Parser,
                 tokenizer12Cm,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 null);
  }

  public void testLenghtenToken() throws IOException {
    // D <- H Y
    final AtnParser test13_Parser = AtnParseTest.buildParser("<grammar><classifiers><H><jclass>org.sd.atn.RoteListClassifier</jclass><terms><term>Easter</term><term>Easter Sunday</term></terms></H></classifiers><rules><D start='true'><H/><Y/></rules></grammar>", false);

    final StandardTokenizer tokenizer13a = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SL</revisionStrategy></tokenizer>", "Easter Y");
    runParseTest("ParserTest.13a",
                 test13_Parser,
                 tokenizer13a,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(D (H Easter) Y)"
                 });

    final StandardTokenizer tokenizer13b = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SL</revisionStrategy></tokenizer>", "Easter Sunday Y");
    runParseTest("ParserTest.13b",
                 test13_Parser,
                 tokenizer13b,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(D (H Easter Sunday) Y)"
                 });
  }

  public void testRuleStepSkip() throws IOException {
    //
    // X <- A Y? E(s2)
    // Y <- B C(s1) D
    //
    // A B C D U E  (succeed)
    // A B C D U U E  (succeed)
    // A B U C D U U E  (succeed)
    //
    // A B U U C D E  (fail)
    // A B C D U U U E  (fail)
    //
    // A B D E  (succeed with B,D unknown)
    // A B D U E  (B,D unknown; U fails due to exceeding limit)
    //
    final AtnParser test14_Parser = AtnParseTest.buildParser("<grammar><rules><X start='true'><A/><Y optional='true'/><E skip='2'/></X><Y><B/><C skip='1'/><D/></Y></rules></grammar>", false);

    final StandardTokenizer tokenizer14a = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A B C D U E");

    runParseTest("ParserTest.14a",
                 test14_Parser,
                 tokenizer14a,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(X A (Y B C D) (? U) E)",
                 });

    final StandardTokenizer tokenizer14b = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A B C D U U E");

    runParseTest("ParserTest.14b",
                 test14_Parser,
                 tokenizer14b,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(X A (Y B C D) (? U) (? U) E)",
                 });

    final StandardTokenizer tokenizer14c = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A B U C D U U E");

    runParseTest("ParserTest.14c",
                 test14_Parser,
                 tokenizer14c,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(X A (Y B (? U) C D) (? U) (? U) E)",
                 });

    final StandardTokenizer tokenizer14d = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A B U U C D E");

    runParseTest("ParserTest.14d",
                 test14_Parser,
                 tokenizer14d,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 null);

    final StandardTokenizer tokenizer14e = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A B C D U U U E");

    runParseTest("ParserTest.14e",
                 test14_Parser,
                 tokenizer14e,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 null);

    final StandardTokenizer tokenizer14f = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A B D E");

    runParseTest("ParserTest.14f",
                 test14_Parser,
                 tokenizer14f,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(X A (? B) (? D) E)",
                 });

    final StandardTokenizer tokenizer14g = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A B D U E");

    runParseTest("ParserTest.14g",
                 test14_Parser,
                 tokenizer14g,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 null);
  }

  public void testRuleStepConstituentSkip() throws IOException {
    //
    // X <- Y? Z(s2)
    // Y <- A? B
    // Z <- C D
    //
    // A B C D
    // A B U C D
    // A B U U C D
    // A B U U U C D (fail)
    // B C D
    // B U C D
    // B U U C D
    // B U U U C D (fail)
    // C D
    // U C D
    // U U C D
    // U U U C D (fail)
    //
    final AtnParser test15_Parser = AtnParseTest.buildParser("<grammar><rules><X start='true'><Y optional='true'/><Z skip='2'/></X><Y><A optional='true'/><B/></Y><Z><C/><D/></Z></rules></grammar>", false);

    final StandardTokenizer tokenizer15a = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A B C D");

    runParseTest("ParserTest.15a",
                 test15_Parser,
                 tokenizer15a,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(X (Y A B) (Z C D))",
                 });

    final StandardTokenizer tokenizer15b = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A B U C D");

    runParseTest("ParserTest.15b",
                 test15_Parser,
                 tokenizer15b,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(X (Y A B) (? U) (Z C D))",
                 });

    final StandardTokenizer tokenizer15c = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A B U U C D");

    runParseTest("ParserTest.15c",
                 test15_Parser,
                 tokenizer15c,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(X (Y A B) (? U) (? U) (Z C D))",
                 });

    final StandardTokenizer tokenizer15d = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A B U U U C D");

    runParseTest("ParserTest.15d",
                 test15_Parser,
                 tokenizer15d,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 null);

    final StandardTokenizer tokenizer15e = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "B C D");

    runParseTest("ParserTest.15e",
                 test15_Parser,
                 tokenizer15e,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(X (Y B) (Z C D))",
                 });

    final StandardTokenizer tokenizer15f = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "B U C D");

    runParseTest("ParserTest.15f",
                 test15_Parser,
                 tokenizer15f,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(X (Y B) (? U) (Z C D))",
                 });

    final StandardTokenizer tokenizer15g = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "B U U C D");

    runParseTest("ParserTest.15g",
                 test15_Parser,
                 tokenizer15g,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(X (Y B) (? U) (? U) (Z C D))",
                 });

    final StandardTokenizer tokenizer15h = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "B U U U C D");

    runParseTest("ParserTest.15h",
                 test15_Parser,
                 tokenizer15h,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 null);

    final StandardTokenizer tokenizer15i = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "C D");

    runParseTest("ParserTest.15i",
                 test15_Parser,
                 tokenizer15i,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(X (Z C D))",
                 });

    final StandardTokenizer tokenizer15j = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "U C D");

    runParseTest("ParserTest.15j",
                 test15_Parser,
                 tokenizer15j,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(X (? U) (Z C D))",
                 });
    runParseTest("ParserTest.15j2",
                 test15_Parser,
                 tokenizer15j,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>false</consumeAllText></parseOptions>",
                 null);

    final StandardTokenizer tokenizer15k = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "U U C D");

    runParseTest("ParserTest.15k",
                 test15_Parser,
                 tokenizer15k,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(X (? U) (? U) (Z C D))",
                 });
    runParseTest("ParserTest.15k2",
                 test15_Parser,
                 tokenizer15k,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>false</consumeAllText></parseOptions>",
                 null);

    final StandardTokenizer tokenizer15l = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "U U U C D");

    runParseTest("ParserTest.15l",
                 test15_Parser,
                 tokenizer15l,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 null);
    runParseTest("ParserTest.15l2",
                 test15_Parser,
                 tokenizer15l,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>false</consumeAllText></parseOptions>",
                 null);
  }

  public void testRuleStepDeepConstituentSkip() throws IOException {
    //
    // X <- Y? Z(s2)
    // Y <- A? B(s2)
    // Z <- C D(s2)
    //
    // C D
    // C U D
    // C U U D
    // U C D  (succeed when consumeAllText, fail when !)
    // 
    // B U U C D
    // B U U C U U D
    // U B U U C D  (succeed when consumeAllText, fail when !)
    // U B U U C U U D  (succeed when consumeAllText, fail when !)
    // 

    final AtnParser test16_Parser = AtnParseTest.buildParser("<grammar><rules><X start='true'><Y optional='true'/><Z skip='2'/></X><Y><A optional='true'/><B skip='2'/></Y><Z><C/><D skip='2'/></Z></rules></grammar>", false);

    final StandardTokenizer tokenizer16a = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "C D");

    runParseTest("ParserTest.16a",
                 test16_Parser,
                 tokenizer16a,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(X (Z C D))",
                 });

    final StandardTokenizer tokenizer16b = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "C U D");

    runParseTest("ParserTest.16b",
                 test16_Parser,
                 tokenizer16b,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(X (Z C (? U) D))",
                 });

    final StandardTokenizer tokenizer16c = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "C U U D");

    runParseTest("ParserTest.16c",
                 test16_Parser,
                 tokenizer16c,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(X (Z C (? U) (? U) D))",
                 });

    final StandardTokenizer tokenizer16d = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "U C D");

    runParseTest("ParserTest.16d",
                 test16_Parser,
                 tokenizer16d,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(X (? U) (Z C D))",
                 });
    runParseTest("ParserTest.16d2",
                 test16_Parser,
                 tokenizer16d,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>false</consumeAllText></parseOptions>",
                 null);

    final StandardTokenizer tokenizer16e = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "B U U C D");

    runParseTest("ParserTest.16e",
                 test16_Parser,
                 tokenizer16e,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(X (Y B) (? U) (? U) (Z C D))",
                 });

    final StandardTokenizer tokenizer16f = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "B U U C U U D");

    runParseTest("ParserTest.16f",
                 test16_Parser,
                 tokenizer16f,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(X (Y B) (? U) (? U) (Z C (? U) (? U) D))",
                 });

    final StandardTokenizer tokenizer16g = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "U B U U C D");

    runParseTest("ParserTest.16g",
                 test16_Parser,
                 tokenizer16g,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(X (Y (? U) B) (? U) (? U) (Z C D))",
                 });
    runParseTest("ParserTest.16g2",
                 test16_Parser,
                 tokenizer16g,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>false</consumeAllText></parseOptions>",
                 null);

    final StandardTokenizer tokenizer16h = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "U B U U C U U D");

    runParseTest("ParserTest.16h",
                 test16_Parser,
                 tokenizer16h,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(X (Y (? U) B) (? U) (? U) (Z C (? U) (? U) D))",
                 });
    runParseTest("ParserTest.16h2",
                 test16_Parser,
                 tokenizer16h,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>false</consumeAllText></parseOptions>",
                 null);
    
  }

  public void testDeepPopAndContinueWithSkip() throws IOException {
    //
    // Z <- T Y(s5)
    // Y <- W X
    // W <- V
    // X <- V
    // V <- B? U
    // U <- C
    // T <- A
    //
    // A C C
    // A D D C C
    //

    final AtnParser test17_Parser = AtnParseTest.buildParser("<grammar><rules><Z start='true'><T/><Y skip='5'/></Z><Y><W/><X/></Y><W><V/></W><X><V/></X><V><B optional='true'/><U/></V><U><C/></U><T><A repeats='true' cluster='true'/></T></rules></grammar>", false);

    final StandardTokenizer tokenizer17a = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A C C");

    runParseTest("ParserTest.17a",
                 test17_Parser,
                 tokenizer17a,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(Z (T A) (Y (W (V (U C))) (X (V (U C)))))",
                 });

    final StandardTokenizer tokenizer17b = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A D D C C");

    runParseTest("ParserTest.17b",
                 test17_Parser,
                 tokenizer17b,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(Z (T A) (? D) (? D) (Y (W (V (U C))) (X (V (U C)))))",
                 });
  }

  public void testConstituentClustering() throws IOException {
    //
    // Z <- W+
    // W <- X* Y X*(c)
    // X <- A B
    //
    // A B Y A B A B A B Y A B
    //        z1        ^  z2

    final AtnParser test18_Parser = AtnParseTest.buildParser("<grammar><rules><Z start='true'><W repeats='true'/></Z><W><X optional='true' repeats='true' /><Y /><X optional='true' repeats='true' cluster='true' /></W><X><A /><B /></X></rules></grammar>", false);

    final StandardTokenizer tokenizer18a = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A B Y A B A B A B Y A B");

    runParseTest("parserTest.18a",
                 test18_Parser,
                 tokenizer18a,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                     "(Z (W (X A B) Y (X A B) (X A B) (X A B)) (W Y (X A B)))",
                 });
  }

  public void testBracketsPopTest() throws IOException {
    //
    // Z <- Y+
    // Y <- a? A+ a?(ua)  [BracketPopTest]
    //
    // A (A A)
    // A (a A)
    // A (A a)

    final AtnParser test19_Parser = AtnParseTest.buildParser("<grammar><rules><Z start='true'><Y repeats='true' /></Z><Y><a optional='true' /><A repeats='true' /><a optional='true' unless='a'/><no-op popTest='true'><test allowBalanced='true' includeEnds='false'><jclass>org.sd.atn.BracketPopTest</jclass><brackets><delim><start type='substr'>(</start><end type='substr'>)</end></delim></brackets></test></no-op></Y></rules></grammar>", false);

    final StandardTokenizer tokenizer19a = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A (A A)");

    runParseTest("parserTest_19a",
                 test19_Parser,
                 tokenizer19a,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(Z (Y A) (Y A) (Y A))",
                   "(Z (Y A) (Y A A))",
                 });


    final StandardTokenizer tokenizer19b = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A (a A)");

    runParseTest("parserTest_19a",
                 test19_Parser,
                 tokenizer19b,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(Z (Y A) (Y a A))",
                 });


    final StandardTokenizer tokenizer19c = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A (A a)");

    runParseTest("parserTest_19a",
                 test19_Parser,
                 tokenizer19c,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(Z (Y A) (Y A a))",
                 });
  }



  private final void runParseTest(String name, AtnParser parser, StandardTokenizer tokenizer, String parseOptionsXml, String[] expectedTreeStrings) throws IOException {
    runParseTest(name, parser, tokenizer, parseOptionsXml, expectedTreeStrings, null, false);
  }

  private final void runParseTest(String name, AtnParser parser, StandardTokenizer tokenizer, String parseOptionsXml, String[] expectedTreeStrings, String[] expectedRemainder, boolean seek) throws IOException {

    final AtnParseOptions parseOptions = AtnParseTest.buildParseOptions(parseOptionsXml);

    final MyParseTest atnParseTest = new MyParseTest(name, parser, tokenizer, parseOptions, expectedTreeStrings, expectedRemainder, seek);
    atnParseTest.runTest();
  }


  private final class MyParseTest {

    private String name;
    private AtnParser parser;
    private Tokenizer tokenizer;
    private AtnParseOptions options;
    private String[] expectedTreeStrings;
    private String[] expectedRemainder;
    private boolean seek;
    private Set<Integer> stopList;

    public MyParseTest(String name, AtnParser parser, Tokenizer tokenizer, AtnParseOptions options, String[] expectedTreeStrings) {
      init(name, parser, tokenizer, options, expectedTreeStrings, null, false, null);
    }

    public MyParseTest(String name, AtnParser parser, Tokenizer tokenizer, AtnParseOptions options, String[] expectedTreeStrings, String[] expectedRemainder, boolean seek) {
      init(name, parser, tokenizer, options, expectedTreeStrings, expectedRemainder, seek, null);
    }

    public MyParseTest(String name, AtnParser parser, Tokenizer tokenizer, AtnParseOptions options, String[] expectedTreeStrings, String[] expectedRemainder, boolean seek, Set<Integer> stopList) {
      init(name, parser, tokenizer, options, expectedTreeStrings, expectedRemainder, seek, stopList);
    }

    private final void init(String name, AtnParser parser, Tokenizer tokenizer, AtnParseOptions options, String[] expectedTreeStrings, String[] expectedRemainder, boolean seek, Set<Integer> stopList) {
      this.name = name;
      this.parser = parser;
      this.tokenizer = tokenizer;
      this.options = options;
      this.expectedTreeStrings = expectedTreeStrings;
      this.expectedRemainder = expectedRemainder;
      this.seek = seek;
      this.stopList = stopList;
    }

    public void runTest() {
      final AtnParseResult parseResult = seek ? parser.seekParse(tokenizer, options, stopList, null, null) : parser.parse(tokenizer, options, stopList, null, null);


      if (expectedTreeStrings == null) {
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
        parseResult.generateParses(expectedTreeStrings.length);

        for (int parseNum = 0; parseNum < expectedTreeStrings.length; ++parseNum) {
          final AtnParse parse = parseResult.getParse(parseNum);
          final Tree<String> parseTree = parse == null ? null : parse.getParseTree();
          final String expectedParse = expectedTreeStrings[parseNum];

          assertEquals(name + ": Bad parse #" + parseNum + ".",
                       expectedParse, (parseTree == null) ? null : parseTree.toString());

          if (expectedRemainder != null && parseNum < expectedRemainder.length) {

            assertEquals(name + ": Mismatched remainder for parse #" + parseNum + "'.",
                         expectedRemainder[parseNum], parse.getRemainingText());
          }
        }
      }

      parseResult.generateParses(0);
      final int expectedCount = expectedTreeStrings == null ? 1 : expectedTreeStrings.length;
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
