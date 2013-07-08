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
package org.sd.text;


import org.sd.io.FileUtil;
import org.sd.nlp.AbstractNormalizer;
import org.sd.nlp.BreakStrategy;
import org.sd.nlp.GeneralBreakStrategy;
import org.sd.nlp.GeneralNormalizer;
import org.sd.nlp.NormalizingTokenizer;
import org.sd.xml.HtmlHelper;
import org.sd.xml.HtmlTagStack;
import org.sd.xml.MutableTagStack;
import org.sd.xml.XmlFactory;
import org.sd.xml.XmlTagParser;
import org.sd.xml.XmlTextRipper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;


/**
 * Utility to extract words from xml.
 * <p>
 * @author Spence Koehler
 */
public class XmlWordRipper {
  
  private List<String> words;

  private int count;
  private AbstractNormalizer normalizer;
  private BreakStrategy breakStrategy;
  private MutableTagStack tagStack;
  private XmlTagParser xmlTagParser;
  private Set<String> ignoreTags;

  /**
   * Construct an instance with the given parameters.
   *
   * @param count          The minimum number of words to read. Words from a
   *                       text node will not be broken up, but no more words
   *                       will be added after this limit is reached.
   * @param normalizer     The normalizer to apply to ripped words. If null,
   *                       then no normalization will be applied.
   * @param breakStrategy  The break strategy to identify word boundaries and
   *                       must be non-null.
   *                       (Shortest Only words will be collected, meaning that
   *                       all breaks, hard or soft, are used to identify word
   *                       boundaries.)
   * @param tagStack       The tag stack to use for text ripping. If null,
   *                       then no tags will be kept; but if xmlTagParser is
   *                       non-null then tags will be parsed (skipped) according
   *                       to the tag parser's rules.
   * @param xmlTagParser   The xml tag parser to use to parse tags. If null,
   *                       no tags will be kept.
   * @param ignoreTags     Tags to ignore the text under. Only applied when
   *                       tagStack and xmlTagParser are non-null.
   */
  public XmlWordRipper(int count, AbstractNormalizer normalizer, BreakStrategy breakStrategy,
                       MutableTagStack tagStack, XmlTagParser xmlTagParser, Set<String> ignoreTags) {
    this.words = new ArrayList<String>();

    this.count = count;
    this.normalizer = normalizer;
    this.breakStrategy = breakStrategy;
    this.tagStack = tagStack;
    this.xmlTagParser = xmlTagParser;
    this.ignoreTags = ignoreTags;
  }

  /**
   * Get a copy of the words.
   */
  public List<String> getWords() {
    return new ArrayList<String>(words);
  }

  /**
   * Get the number of words ripped so far.
   */
  public int getCurCount() {
    return words.size();
  }

  /**
   * Determine whether the 'count' limit has been reached.
   */
  public boolean isFull() {
    return words.size() >= count;
  }

  /**
   * Reset this instance for re-use, dumping all ripped words.
   */
  public void reset() {
    this.words.clear();
  }

  /**
   * Add the words from the given xml file, returning false if we have reached
   * the limit.
   */
  public boolean addWords(File xmlFile) throws IOException {
    boolean result = false;

    if (isFull()) return result;

    final XmlTextRipper ripper = new XmlTextRipper(FileUtil.getInputStream(xmlFile), false, tagStack, xmlTagParser, ignoreTags, null, false);
    try {
      while (ripper.hasNext() && !isFull()) {
        final String text = ripper.next();
        final NormalizingTokenizer tokenizer = new NormalizingTokenizer(normalizer, breakStrategy, text);
        final int numTokens = tokenizer.getNumTokens();
        for (int i = 0; i < numTokens; ++i) {
          final String word = tokenizer.getNormalizedToken(i);
          if (word.length() > 0) words.add(word);
        }
      }
    }
    finally {
      ripper.close();
    }

    return result;
  }

  public static void main(String[] args) throws IOException {
    final XmlWordRipper ripper =
      new XmlWordRipper(100,
                        GeneralNormalizer.getCaseInsensitiveInstance(),
                        GeneralBreakStrategy.getInstance(),
                        new HtmlTagStack(),
                        XmlFactory.HTML_TAG_PARSER_IGNORE_COMMENTS,
                        HtmlHelper.DEFAULT_IGNORE_TAGS);

    for (int i = 0; i < args.length; ++i) {
      System.out.println("Ripping words from '" + args[i] + "'...");
      if (!ripper.addWords(new File(args[i]))) {
        break;
      }
    }

    System.out.println("Ripped " + ripper.getCurCount() + " words:");
    System.out.println(ripper.getWords());
  }
}
