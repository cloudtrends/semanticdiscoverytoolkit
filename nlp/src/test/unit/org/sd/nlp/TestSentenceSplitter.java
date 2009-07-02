package org.sd.nlp;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit Tests for the SentenceSplitter class.
 * <p>
 * @author Spence Koehler
 */
public class TestSentenceSplitter extends TestCase {

  public TestSentenceSplitter(String name) {
    super(name);
  }
  

  private final void doSplitTest(String text, String[] expected) {
    final SentenceSplitter splitter = new SentenceSplitter();
    final String[] sentences = splitter.split(text);

    if (expected == null) {
      System.out.println(text + " --(" + sentences.length + ")-->");
      int index = 0;
      for (String sentence : sentences) {
        System.out.println("\t" + index + ": " + sentence);
        ++index;
      }
    }
    else {
      for (int i = 0; i < sentences.length; ++i) {
        assertEquals("i=" + i, expected[i], sentences[i]);
      }
      assertEquals(expected.length, sentences.length);
    }
  }

  public void testExclamationInName() {
    doSplitTest("Names like Yahoo! shouldn't split! But other sentences should.",
                new String[] {
                  "Names like Yahoo! shouldn't split!",
                  "But other sentences should.",
                });
  }

  public void testAcronyms() {
    doSplitTest("Shouldn't split on M.D. or Ph.D. or Mr. or Mrs. but can split otherwise. Mostly.",
                new String[] {
                  "Shouldn't split on M.D. or Ph.D. or Mr. or Mrs. but can split otherwise.",
                  "Mostly.",
                });
  }

  public void testQuoted1() {
    doSplitTest("\"Split appropriately with quotes.\" (At least, \"try.\") Hopefully.",
                new String[] {
                  "\"Split appropriately with quotes.\"",
                  "(At least, \"try.\")",
                  "Hopefully.",
                });

  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestSentenceSplitter.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
