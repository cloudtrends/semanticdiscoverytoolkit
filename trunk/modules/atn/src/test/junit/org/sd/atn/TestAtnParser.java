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
    runParseTest("ParserTest.8h", skipParser1, tokenizer8h, "<parseOptions><skipTokenLimit>2</skipTokenLimit><consumeAllText>false</consumeAllText></parseOptions>", new String[] { "(X A B C)" }, new String[] { "Z Z Z" }, false, false);

    // Disable consume all text, don't skip, and verify that initial "Z Z Z" is skipped while trailing "Z Z Z" remains after parse
    final StandardTokenizer tokenizer8i = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "Z Z Z A B C Z Z Z");
    runParseTest("ParserTest.8i", skipParser1, tokenizer8i, "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>false</consumeAllText></parseOptions>", new String[] { "(X A B C)" }, new String[] { "Z Z Z" }, true, false);
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
    // A <- B? C? B?(rC) D
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
    // A <- B? C? B?(uC) D
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
                   "(A B D)",  // matches first optional B
                   "(A B D)",  // matches second B after skipping first optional B
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

  public void testDontReviseAfterPop() throws IOException {
    // E <- X W
    // W <- T (="in July")
    // W <- P(="in") M(="July")

    final AtnParser test13_Parser = AtnParseTest.buildParser("<grammar><classifiers><T><jclass>org.sd.atn.RoteListClassifier</jclass><terms><term>in July</term></terms></T><P><jclass>org.sd.atn.RoteListClassifier</jclass><terms><term>in</term></terms></P><M><jclass>org.sd.atn.RoteListClassifier</jclass><terms><term>July</term></terms></M></classifiers><rules><E start='true'><X/><W/></E><W><T/></W><W><P/><M/></W></rules></grammar>", false);

    final StandardTokenizer tokenizer13a = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>LSL</revisionStrategy></tokenizer>", "X in July");
    runParseTest("ParserTest.13a",
                 test13_Parser,
                 tokenizer13a,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(E X (W (T in July)))",
                   "(E X (W (P in) (M July)))",
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

    runParseTest("parserTest_19b",
                 test19_Parser,
                 tokenizer19b,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(Z (Y A) (Y a A))",
                 });


    final StandardTokenizer tokenizer19c = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "A (A a)");

    runParseTest("parserTest_19c",
                 test19_Parser,
                 tokenizer19c,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(Z (Y A) (Y A a))",
                 });
  }


  public void testConstituentPreDelim() throws IOException {
    //
    // E <- (^-)D (-)D
    // D <- a (-) b (-) c
    //
    // "a-b-c - a-b-c" -> (E (D a b c) (D a b c))
    //
    final AtnParser test20_Parser = AtnParseTest.buildParser("<grammar><rules><E start='true'><D><predelim><allowall/><disallow type='substr'>-</disallow></predelim></D><D><predelim><require type='substr'>-</require></predelim></D></E><D><a/><b><predelim><disallowall/><allow type='exact'>-</allow></predelim></b><c><predelim><disallowall/><allow type='exact'>-</allow></predelim></c></D></rules></grammar>", false);

    final StandardTokenizer tokenizer20a = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "a-b-c - a-b-c");

    runParseTest("parserTest_20a",
                 test20_Parser,
                 tokenizer20a,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(E (D a b c) (D a b c))",
                 });
  }

  //public void testConstituentPostDelim() throws IOException {

  public void testSkipOptionalAfterPush() throws IOException {
    //
    // P <- G S?
    // G <- c? c
    // S <- c? c
    //
    // c c -> (P (G c) (S c))
    // c c -> (P (G c c))
    //
    final AtnParser test21_Parser = AtnParseTest.buildParser("<grammar><rules><P start='true'><G/><S optional='true'/></P><G><c optional='true'/><c/></G><S><c optional='true'/><c/></S></rules></grammar>", false);

    final StandardTokenizer tokenizer21 = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "c c");

    runParseTest("parserTest_21",
                 test21_Parser,
                 tokenizer21,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(P (G c c))",
                   "(P (G c) (S c))",
                 });
  }

  public void testPermutedRules1() throws IOException {
    //
    // Test when low-level (token matching) rules are permuted
    //
    // A <- B+
    // B! <- C+ D? E
    //
    // C E
    // C D E
    // E C
    // E C C
    // E D C
    // C E D
    //
    // D C (fail -- no E)
    // C D (fail -- no E)
    // C E C (fail -- split C or 2nd B's C has no E)

    final AtnParser test22_Parser = AtnParseTest.buildParser("<grammar><rules><A start='true'><B repeats='true'/></A><B permuted='true'><C repeats='true' cluster='true'/><D optional='true'/><E/></rules></grammar>", false);

    final StandardTokenizer tokenizer22a = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "C E");

    runParseTest("parserTest_22a",
                 test22_Parser,
                 tokenizer22a,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(A (B C E))",
                   });

    final StandardTokenizer tokenizer22b = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "C D E");
    runParseTest("parserTest_22b",
                 test22_Parser,
                 tokenizer22b,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(A (B C D E))",
                   });

    final StandardTokenizer tokenizer22c = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "E C");
    runParseTest("parserTest_22c",
                 test22_Parser,
                 tokenizer22c,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(A (B E C))",
                   });

    final StandardTokenizer tokenizer22d = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "E C C");
    runParseTest("parserTest_22d",
                 test22_Parser,
                 tokenizer22d,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(A (B E C C))",
                   });

    final StandardTokenizer tokenizer22e = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "E D C");
    runParseTest("parserTest_22e",
                 test22_Parser,
                 tokenizer22e,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(A (B E D C))",
                   });

    final StandardTokenizer tokenizer22f = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "C E D");
    runParseTest("parserTest_22f",
                 test22_Parser,
                 tokenizer22f,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(A (B C E D))",
                   });

    final StandardTokenizer tokenizer22g = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "D C");
    runParseTest("parserTest_22g",
                 test22_Parser,
                 tokenizer22g,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 null);

    final StandardTokenizer tokenizer22h = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "C D");
    runParseTest("parserTest_22h",
                 test22_Parser,
                 tokenizer22h,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 null);

    final StandardTokenizer tokenizer22i = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "C E C");
    runParseTest("parserTest_22i",
                 test22_Parser,
                 tokenizer22i,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 null);

    final StandardTokenizer tokenizer22j = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "C E C E");
    runParseTest("parserTest_22j",
                 test22_Parser,
                 tokenizer22j,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(A (B C E) (B C E))",
                 });
  }

  public void testPermutedRules2() throws IOException {
    //
    // Test when the start rule/higher-order rules are permuted
    //
    // A! <- e? B+ C* D?
    // B <- b+
    // C <- c+
    // D <- d+
    //
    // b
    // b c d
    // d b
    // d c b
    // c b d
    // 
    // e c b d
    // b c e d
    // b d c e

    final AtnParser test23_Parser = AtnParseTest.buildParser("<grammar><rules><A start='true' permuted='true'><e optional='true'/><B repeats='true'/><C optional='true' repeats='true'/><D optional='true'/></A><B><b repeats='true'/></B><C><c repeats='true'/></C><D><d repeats='true'/></D></rules></grammar>", false);

    final StandardTokenizer tokenizer23a = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "b");
    runParseTest("parserTest_23a",
                 test23_Parser,
                 tokenizer23a,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(A (B b))",
                   });

    final StandardTokenizer tokenizer23b = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "b c d");
    runParseTest("parserTest_23b",
                 test23_Parser,
                 tokenizer23b,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(A (B b) (C c) (D d))",
                   });

    final StandardTokenizer tokenizer23c = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "d b");
    runParseTest("parserTest_23c",
                 test23_Parser,
                 tokenizer23c,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(A (D d) (B b))",
                   });

    final StandardTokenizer tokenizer23d = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "d c b");
    runParseTest("parserTest_23d",
                 test23_Parser,
                 tokenizer23d,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(A (D d) (C c) (B b))",
                   });

    final StandardTokenizer tokenizer23e = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "c b d");
    runParseTest("parserTest_23e",
                 test23_Parser,
                 tokenizer23e,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(A (C c) (B b) (D d))",
                   });

    final StandardTokenizer tokenizer23f = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "e c b d");
    runParseTest("parserTest_23f",
                 test23_Parser,
                 tokenizer23f,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(A e (C c) (B b) (D d))",
                   });

    final StandardTokenizer tokenizer23g = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "b c e d");
    runParseTest("parserTest_23g",
                 test23_Parser,
                 tokenizer23g,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(A (B b) (C c) e (D d))",
                   });

    final StandardTokenizer tokenizer23h = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "b d c e");
    runParseTest("parserTest_23h",
                 test23_Parser,
                 tokenizer23h,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(A (B b) (D d) (C c) e)",
                   });
  }

  public void testTokenLimit1() throws IOException {
    //
    // Z <- X+        (tokenLimit=4, repeatLimit=4)
    // X <- a* Y+ b*  (tokenLimit=4, repeatLimit(Y)=2)
    // Y <- c* d+     (tokenLimit=4, repeatLimit(d)=3)
    //
    // a c d c d b      PASS  1:a 2:Y 3:Y 4:b
    // c d c d c d b    PASS, but multiple Z's
    // a a c d b        PASS  1:a 2:a 3:Y 4:b
    // d d d            PASS
    // d d d d          FAIL (too many d's)
    // a a a c d c d b  FAIL (a's repeatLimit)
    // a c d c d c d b  FAIL (X's tokenLimit)
    // 

    final AtnParser test24_Parser = AtnParseTest.buildParser("<grammar><rules><Z start='true' tokenLimit='4'><X repeats='true' repeatLimit='4' /></Z><X tokenLimit='4'><a optional='true' repeats='true'/><Y repeats='true' cluster='true' repeatLimit='2'/><b optional='true' repeats='true'/></X><Y tokenLimit='4'><c optional='true' repeats='true'/><d repeats='true' repeatLimit='3' cluster='true'/></Y></rules></grammar>", false);

    final StandardTokenizer tokenizer24a = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "a c d c d b");
    runParseTest("parserTest_24a",
                 test24_Parser,
                 tokenizer24a,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(Z (X a (Y c d) (Y c d) b))",
                 });

    final StandardTokenizer tokenizer24b = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "c d c d c d b");
    runParseTest("parserTest_24b",
                 test24_Parser,
                 tokenizer24b,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   //NOTE: fails due to Y-repeatLimit=2
                   //"(Z (X (Y c d) (Y c d) (Y c d) b))",
                 });

    final StandardTokenizer tokenizer24c = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "a a c d b");
    runParseTest("parserTest_24c",
                 test24_Parser,
                 tokenizer24c,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(Z (X a a (Y c d) b))",
                 });

    final StandardTokenizer tokenizer24d = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "d d d");
    runParseTest("parserTest_24d",
                 test24_Parser,
                 tokenizer24d,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(Z (X (Y d d d)))",
                 });

    final StandardTokenizer tokenizer24e = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "d d d d");
    runParseTest("parserTest_24e",
                 test24_Parser,
                 tokenizer24e,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                 });

    final StandardTokenizer tokenizer24f = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "a a a c d c d b");
    runParseTest("parserTest_24f",
                 test24_Parser,
                 tokenizer24f,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                 });

    final StandardTokenizer tokenizer24g = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "a c d c d c d b");
    runParseTest("parserTest_24g",
                 test24_Parser,
                 tokenizer24g,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                 });
  }

  public void testAddAllStarts() throws IOException {
    //
    // X <- ab* b+
    //
    // b
    // 

    final AtnParser test25_Parser = AtnParseTest.buildParser("<grammar><classifiers><ab><jclass>org.sd.atn.RoteListClassifier</jclass><terms caseSensitive='true'><term>a</term><term>b</term></terms></ab></classifiers><rules><X start='true'><ab optional='true' repeats='true'/><b/></X></rules></grammar>", false);

    final StandardTokenizer tokenizer25a = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "b");
    runParseTest("parserTest_25a",
                 test25_Parser,
                 tokenizer25a,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(X b)",
                 });
  }

  public void testTokenTestStepStartForClassifierStep() throws IOException {
    //
    // Z <- X+
    // X <- a* Y+ b*
    // Y <- c* d+
    //
    // fail step "c" if "c" is preceded by "a"
    //
    // a d b   PASS
    // c d b   PASS
    // c d     PASS
    // a c d   FAIL
    // a c d b FAIL
    //

    final AtnParser test26_Parser = AtnParseTest.buildParser("<grammar><rules><Z start='true'><X repeats='true' /></Z><X><a optional='true' repeats='true'/><Y repeats='true' cluster='true'/><b optional='true' repeats='true'/></X><Y><c optional='true' repeats='true'><test reverse='true' prev='true' refToken='stepStart'><jclass>org.sd.atn.TokenTest</jclass><classifier cat='a'/></test></c><d repeats='true' cluster='true'/></Y></rules></grammar>", false);

    final StandardTokenizer tokenizer26a = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "a d b");
    runParseTest("parserTest_26a",
                 test26_Parser,
                 tokenizer26a,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(Z (X a (Y d) b))",
                 }, true);

    final StandardTokenizer tokenizer26b = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "c d b");
    runParseTest("parserTest_26b",
                 test26_Parser,
                 tokenizer26b,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(Z (X (Y c d) b))",
                 }, true);

    final StandardTokenizer tokenizer26c = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "c d");
    runParseTest("parserTest_26c",
                 test26_Parser,
                 tokenizer26c,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(Z (X (Y c d)))",
                 }, true);

    final StandardTokenizer tokenizer26d = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "a c d");
    runParseTest("parserTest_26d",
                 test26_Parser,
                 tokenizer26d,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                 }, true);

    final StandardTokenizer tokenizer26e = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "a c d b");
    runParseTest("parserTest_26e",
                 test26_Parser,
                 tokenizer26e,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                 }, true);
  }

  public void testTokenTestStepStartForConstituentStep() throws IOException {
    //
    // Z <- X+
    // X <- a* Y+ b*
    // Y <- c* d+
    //
    // fail step "Y" if "Y" is preceded by "a"
    //
    // d d b   PASS
    // c d b   PASS
    // c d     PASS
    // a c d   FAIL
    // a c d b FAIL
    // a d d b FAIL
    //

    final AtnParser test27_Parser = AtnParseTest.buildParser("<grammar><rules><Z start='true'><X repeats='true' /></Z><X><a optional='true' repeats='true'/><Y repeats='true' cluster='true'><test reverse='true' prev='true' refToken='stepStart'><jclass>org.sd.atn.TokenTest</jclass><classifier cat='a'/></test></Y><b optional='true' repeats='true'/></X><Y><c optional='true' repeats='true'/><d repeats='true' cluster='true'/></Y></rules></grammar>", false);

    final StandardTokenizer tokenizer27a = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "d d b");
    runParseTest("parserTest_27a",
                 test27_Parser,
                 tokenizer27a,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(Z (X (Y d d) b))",
                 }, true);

    final StandardTokenizer tokenizer27b = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "c d b");
    runParseTest("parserTest_27b",
                 test27_Parser,
                 tokenizer27b,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(Z (X (Y c d) b))",
                 }, true);

    final StandardTokenizer tokenizer27c = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "c d");
    runParseTest("parserTest_27c",
                 test27_Parser,
                 tokenizer27c,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(Z (X (Y c d)))",
                 }, true);

    final StandardTokenizer tokenizer27d = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "a c d");
    runParseTest("parserTest_27d",
                 test27_Parser,
                 tokenizer27d,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                 }, true);

    final StandardTokenizer tokenizer27e = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "a c d b");
    runParseTest("parserTest_27e",
                 test27_Parser,
                 tokenizer27e,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                 }, true);

    final StandardTokenizer tokenizer27f = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "a d d b");
    runParseTest("parserTest_27f",
                 test27_Parser,
                 tokenizer27f,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                 }, true);

  }

  public void testTokenStepStartForPopTestStep() throws IOException {
    //
    // Z <- X+
    // X <- a* Y+ b*
    // Y <- c* d+ T
    //
    // fail step T if "d" is preceded by "c"
    //
    // a d b   PASS
    // d b     PASS
    // d d b   PASS
    // c d b   FAIL
    // c d     FAIL
    // a c d   FAIL
    // a c d b FAIL
    //

    final AtnParser test28_Parser = AtnParseTest.buildParser("<grammar><rules><Z start='true'><X repeats='true' /></Z><X><a optional='true' repeats='true'/><Y repeats='true' cluster='true'/><b optional='true' repeats='true'/></X><Y><c optional='true' repeats='true'/><d repeats='true' cluster='true'/><no-op popTest='true'><test reverse='true' prev='true' refToken='stepStart'><jclass>org.sd.atn.TokenTest</jclass><classifier cat='c'/></test></no-op></Y></rules></grammar>", false);

    final StandardTokenizer tokenizer28a = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "a d b");
    runParseTest("parserTest_28a",
                 test28_Parser,
                 tokenizer28a,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(Z (X a (Y d) b))",
                 }, true);

    final StandardTokenizer tokenizer28b = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "d b");
    runParseTest("parserTest_28b",
                 test28_Parser,
                 tokenizer28b,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(Z (X (Y d) b))",
                 }, true);

    final StandardTokenizer tokenizer28c = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "d d b");
    runParseTest("parserTest_28c",
                 test28_Parser,
                 tokenizer28c,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(Z (X (Y d d) b))",
                 }, true);

    final StandardTokenizer tokenizer28d = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "c d b");
    runParseTest("parserTest_28d",
                 test28_Parser,
                 tokenizer28d,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                 }, true);

    final StandardTokenizer tokenizer28e = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "c d");
    runParseTest("parserTest_28e",
                 test28_Parser,
                 tokenizer28e,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                 }, true);

    final StandardTokenizer tokenizer28f = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "a c d");
    runParseTest("parserTest_28f",
                 test28_Parser,
                 tokenizer28f,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                 }, true);

    final StandardTokenizer tokenizer28g = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "a c d b");
    runParseTest("parserTest_28g",
                 test28_Parser,
                 tokenizer28g,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                 }, true);
  }

  public void testTokenRuleStartForClassifierStep() throws IOException {
    //
    // Z <- X+
    // X <- a* Y+ b*
    // Y <- c* d+
    //
    // fail step "d" if rule "Y" is preceded by "a"
    //
    // d b     PASS
    // d d b   PASS
    // c d b   PASS
    // c d     PASS
    // a d b   FAIL
    // a c d   FAIL
    // a c d b FAIL
    //

    final AtnParser test29_Parser = AtnParseTest.buildParser("<grammar><rules><Z start='true'><X repeats='true' /></Z><X><a optional='true' repeats='true'/><Y repeats='true' cluster='true'/><b optional='true' repeats='true'/></X><Y><c optional='true' repeats='true'/><d repeats='true' cluster='true'><test reverse='true' prev='true' refToken='ruleStart'><jclass>org.sd.atn.TokenTest</jclass><classifier cat='a'/></test></d></Y></rules></grammar>", false);

    final StandardTokenizer tokenizer29a = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "d b");
    runParseTest("parserTest_29a",
                 test29_Parser,
                 tokenizer29a,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(Z (X (Y d) b))",
                 }, true);

    final StandardTokenizer tokenizer29b = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "d d b");
    runParseTest("parserTest_29b",
                 test29_Parser,
                 tokenizer29b,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(Z (X (Y d d) b))",
                 }, true);

    final StandardTokenizer tokenizer29c = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "c d b");
    runParseTest("parserTest_29c",
                 test29_Parser,
                 tokenizer29c,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(Z (X (Y c d) b))",
                 }, true);

    final StandardTokenizer tokenizer29d = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "c d");
    runParseTest("parserTest_29d",
                 test29_Parser,
                 tokenizer29d,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(Z (X (Y c d)))",
                 }, true);

    final StandardTokenizer tokenizer29e = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "a d b");
    runParseTest("parserTest_29e",
                 test29_Parser,
                 tokenizer29e,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                 }, true);

    final StandardTokenizer tokenizer29f = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "a c d");
    runParseTest("parserTest_29f",
                 test29_Parser,
                 tokenizer29f,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                 }, true);

    final StandardTokenizer tokenizer29g = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "a c d b");
    runParseTest("parserTest_29g",
                 test29_Parser,
                 tokenizer29g,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                 }, true);

  }

  public void testTokenTestRuleStartForConstituentStep() throws IOException {
    //
    // Z <- x? X+
    // X <- a* Y+ b*
    // Y <- c* d+
    //
    // fail step "Y" if rule "X" is preceded by "x"
    //
    // d b     PASS
    // d d b   PASS
    // c d b   PASS
    // c d     PASS
    // a d b   PASS
    // a c d   PASS
    // a c d b PASS
    // x d b   FAIL
    // x c d   FAIL
    // x a d b FAIL
    // x a c d FAIL
    //

    final AtnParser test30_Parser = AtnParseTest.buildParser("<grammar><rules><Z start='true'><x optional='true'/><X repeats='true' /></Z><X><a optional='true' repeats='true'/><Y repeats='true' cluster='true'><test reverse='true' prev='true' refToken='ruleStart'><jclass>org.sd.atn.TokenTest</jclass><classifier cat='x'/></test></Y><b optional='true' repeats='true'/></X><Y><c optional='true' repeats='true'/><d repeats='true' cluster='true'/></Y></rules></grammar>", false);

    final StandardTokenizer tokenizer30a = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "d b");
    runParseTest("parserTest_30a",
                 test30_Parser,
                 tokenizer30a,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(Z (X (Y d) b))",
                 }, true);

    final StandardTokenizer tokenizer30b = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "d d b");
    runParseTest("parserTest_30b",
                 test30_Parser,
                 tokenizer30b,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(Z (X (Y d d) b))",
                 }, true);

    final StandardTokenizer tokenizer30c = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "c d b");
    runParseTest("parserTest_30c",
                 test30_Parser,
                 tokenizer30c,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(Z (X (Y c d) b))",
                 }, true);

    final StandardTokenizer tokenizer30d = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "c d");
    runParseTest("parserTest_30d",
                 test30_Parser,
                 tokenizer30d,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(Z (X (Y c d)))",
                 }, true);

    final StandardTokenizer tokenizer30e = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "a d b");
    runParseTest("parserTest_30e",
                 test30_Parser,
                 tokenizer30e,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(Z (X a (Y d) b))",
                 }, true);

    final StandardTokenizer tokenizer30f = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "a c d");
    runParseTest("parserTest_30f",
                 test30_Parser,
                 tokenizer30f,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(Z (X a (Y c d)))",
                 }, true);

    final StandardTokenizer tokenizer30g = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "a c d b");
    runParseTest("parserTest_30g",
                 test30_Parser,
                 tokenizer30g,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(Z (X a (Y c d) b))",
                 }, true);

    final StandardTokenizer tokenizer30h = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "x d b");
    runParseTest("parserTest_30h",
                 test30_Parser,
                 tokenizer30h,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                 }, true);

    final StandardTokenizer tokenizer30i = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "x c d");
    runParseTest("parserTest_30i",
                 test30_Parser,
                 tokenizer30i,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                 }, true);

    final StandardTokenizer tokenizer30j = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "x a d b");
    runParseTest("parserTest_30j",
                 test30_Parser,
                 tokenizer30j,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                 }, true);

    final StandardTokenizer tokenizer30k = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "x a c d");
    runParseTest("parserTest_30k",
                 test30_Parser,
                 tokenizer30k,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                 }, true);
  }

  public void testAmbiguousTokenSucceeds() throws IOException {
    //
    // Z <- x? y? z
    //
    // where "yz" is either "y" or "z"
    //
    // yz
    // x yz
    //

    final AtnParser test31_Parser = AtnParseTest.buildParser("<grammar><classifiers><y><jclass>org.sd.atn.RoteListClassifier</jclass><terms><term>yz</term></terms></y><z><jclass>org.sd.atn.RoteListClassifier</jclass><terms><term>yz</term></terms></z></classifiers><rules><Z start='true'><x optional='true'/><y optional='true'/><z /></Z></rules></grammar>", false);

    final StandardTokenizer tokenizer31a = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "yz");
    runParseTest("parserTest_31a",
                 test31_Parser,
                 tokenizer31a,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(Z (z yz))",
                 }, true);

    final StandardTokenizer tokenizer31b = AtnParseTest.buildTokenizer("<tokenizer><revisionStrategy>SO</revisionStrategy></tokenizer>", "x yz");
    runParseTest("parserTest_31b",
                 test31_Parser,
                 tokenizer31b,
                 "<parseOptions><skipTokenLimit>0</skipTokenLimit><consumeAllText>true</consumeAllText></parseOptions>",
                 new String[] {
                   "(Z x (z yz))",
                 }, true);
  }

/* Not necessary -- essentially same as testTokenTestRuleStartForConstituentStep()
  public void testTokenTestRuleStartForPopTestStep() throws IOException {
    //
    // Z <- x? X+
    // X <- a* Y+ b* T
    // Y <- c* d+
    //
    // fail step "T" if rule "X" is preceded by "x"
    //
    // d b     PASS
    // d d b   PASS
    // c d b   PASS
    // c d     PASS
    // a d b   PASS
    // a c d   PASS
    // a c d b PASS
    // x d b   FAIL
    // x c d   FAIL
    // x a d b FAIL
    // x a c d FAIL
    //
  }
*/


  private final void runParseTest(String name, AtnParser parser, StandardTokenizer tokenizer, String parseOptionsXml, String[] expectedTreeStrings) throws IOException {
    runParseTest(name, parser, tokenizer, parseOptionsXml, expectedTreeStrings, null, false, false);
  }

  private final void runParseTest(String name, AtnParser parser, StandardTokenizer tokenizer, String parseOptionsXml, String[] expectedTreeStrings, boolean failIfExtras) throws IOException {
    runParseTest(name, parser, tokenizer, parseOptionsXml, expectedTreeStrings, null, false, failIfExtras);
  }

  private final void runParseTest(String name, AtnParser parser, StandardTokenizer tokenizer, String parseOptionsXml, String[] expectedTreeStrings, String[] expectedRemainder, boolean seek, boolean failIfExtras) throws IOException {

    final AtnParseOptions parseOptions = AtnParseTest.buildParseOptions(parseOptionsXml);

    final MyParseTest atnParseTest = new MyParseTest(name, parser, tokenizer, parseOptions, expectedTreeStrings, expectedRemainder, seek, failIfExtras);
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
    private boolean failIfExtras;
    private Set<Integer> stopList;

    public MyParseTest(String name, AtnParser parser, Tokenizer tokenizer, AtnParseOptions options, String[] expectedTreeStrings, String[] expectedRemainder, boolean seek, boolean failIfExtras) {
      init(name, parser, tokenizer, options, expectedTreeStrings, expectedRemainder, seek, failIfExtras, null);
    }

    public MyParseTest(String name, AtnParser parser, Tokenizer tokenizer, AtnParseOptions options, String[] expectedTreeStrings, String[] expectedRemainder, boolean seek, boolean failIfExtras, Set<Integer> stopList) {
      init(name, parser, tokenizer, options, expectedTreeStrings, expectedRemainder, seek, failIfExtras, stopList);
    }

    private final void init(String name, AtnParser parser, Tokenizer tokenizer, AtnParseOptions options, String[] expectedTreeStrings, String[] expectedRemainder, boolean seek, boolean failIfExtras, Set<Integer> stopList) {
      this.name = name;
      this.parser = parser;
      this.tokenizer = tokenizer;
      this.options = options;
      this.expectedTreeStrings = expectedTreeStrings;
      this.expectedRemainder = expectedRemainder;
      this.seek = seek;
      this.failIfExtras = failIfExtras;
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

        assertNull(name + ": Got parse '" + (parse == null ? "<NULL>" : parseTree.toString()) + "' (and " + (parseResult.getNumParses() - 1) + " others) where none was expected.",
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
        if (failIfExtras) {
          fail(name + " yielded " + parseResult.getNumParses() +
               " parses when expected " + expectedCount + ".");
        }
        else {
          System.out.println("*** WARNING: " + name + " yielded " + parseResult.getNumParses() +
                             " parses when expected " + expectedCount + ".");

          for (int parseNum = 0; parseNum < parseResult.getNumParses(); ++parseNum) {
            System.out.println("\t" + parseResult.getParse(parseNum).getParseTree().toString());
          }
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
