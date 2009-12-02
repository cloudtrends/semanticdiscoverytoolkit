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
package org.sd.crawl;


import org.sd.cio.MessageHelper;
import org.sd.nlp.Normalizer;
import org.sd.nlp.SentenceSplitter;
import org.sd.text.GeneralWordAcceptor;
import org.sd.text.IndexingNormalizer;
import org.sd.text.NGramFreq;
import org.sd.text.WordGramStats;
import org.sd.text.WordGramSplitter.WordAcceptor;
import org.sd.text.lucene.LuceneUtils;
import org.sd.util.tree.Tree;
import org.sd.xml.XmlLite;
import org.sd.xml.XmlTreeHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * A crawled page processor that builds NGrams from the content.
 * <p>
 * @author Spence Koehler
 */
public class NGramProcessor extends CrawledPageRipperProcessor {
  
  private Normalizer normalizer;
  private WordGramStats wordGramStats;
  private String limitType;
  private int nGramLimit;

  private List<ProcessorResult> results;

  private static final Normalizer defaultNormalizer = IndexingNormalizer.getInstance(IndexingNormalizer.DEFAULT_NORMALIZATION_OPTIONS);
  private static final SentenceSplitter sentenceSplitter = new SentenceSplitter();

  /**
   * Properties:
   * <ul>
   * <li>lowN -- (optional, default=1) the low 'N' for N-grams to collect.</li>
   * <li>highN -- (optional, default=4) the high 'N' for N-grams to collect.</li>
   * <li>limitType -- (optional, default="count") either "freq" or "count".</li>
   * <li>nGramLimit -- (optional, default=10) number of top N-grams or minFreq of N-grams to keep, depending on limitType.</li>
   * </ul>
   */
  public NGramProcessor(Properties properties) {
    this.results = null;

    this.limitType = properties.getProperty("limitType", "count");
    this.nGramLimit = Integer.parseInt(properties.getProperty("nGramLimit", "10"));

//todo: get params for word acceptor from properties
    final String[] stopwords = LuceneUtils.DEFAULT_STOPWORDS_ARRAY;
    final WordAcceptor wordAcceptor = new GeneralWordAcceptor(stopwords);

    final int lowN = Integer.parseInt(properties.getProperty("lowN", "1"));
    final int highN = Integer.parseInt(properties.getProperty("highN", "4"));

//todo: get param for normalizer, defaulting to defaultNormalizer
    this.normalizer = defaultNormalizer;

//todo: parameterize lowN, highN
    this.wordGramStats = new WordGramStats(lowN, highN, normalizer, wordAcceptor);

//todo: parameterize (bitmask) for collecting over:
//      all content
//        meta keywords
//        meta description
//        title
//        headings
//        link text
  }

  /**
   * Get the stats instance.
   */
  public WordGramStats getWordGramStats() {
    return wordGramStats;
  }

  /**
   * Get the top NGrams for each N from lowN to highN.
   */
  public Map<Integer, NGramFreq[]> getTopNGrams() {
    final Map<Integer, NGramFreq[]> result = new HashMap<Integer, NGramFreq[]>();

    for (int i = wordGramStats.lowN; i <= wordGramStats.highN; ++i) {
      final NGramFreq[] topNGrams =
        "freq".equals(limitType) ?
        wordGramStats.getTopNGramsLimitedByFreq(i, nGramLimit, null) :
        wordGramStats.getTopNGramsLimitedByCount(i, nGramLimit, null);
      result.put(i, topNGrams);
    }
    
    return result;
  }

  /**
   * Process the ripped node.
   *
   * @return true if successful; otherwise, false.
   */
  protected boolean processRippedNode(CrawledPage crawledPage, Tree<XmlLite.Data> rippedNode) {
    this.results = null;

    boolean result = true;

//todo: get meta keywords and descriptions, if warranted.

    final String text = XmlTreeHelper.getAllText(rippedNode);
    final String[] sentences = sentenceSplitter.split(text);

    for (String sentence : sentences) {
      wordGramStats.add(sentence);
    }

    return result;
  }

  /**
   * Run the post ripping hook.
   */
  protected void postRippingHook(CrawledPage crawledPage) {
    wordGramStats.flush(crawledPage.getUrl());
  }

  /**
   * Access method to dump results after running.
   */
  protected void dumpResults() {
    final Map<Integer, NGramFreq[]> topNGramsMap = getTopNGrams();

    for (Map.Entry<Integer, NGramFreq[]> entry : topNGramsMap.entrySet()) {
      System.out.println("\n" + entry.getKey() + "-Grams");
      final NGramFreq[] topNGrams = entry.getValue();

      for (NGramFreq nGram : topNGrams) {
        System.out.println("\t" + nGram.toString());
      }
    }
  }

  /**
   * Get this processor's results.
   */
  public List<ProcessorResult> getResults() {
    if (results == null) {
      results = new ArrayList<ProcessorResult>();

      final Map<Integer, NGramFreq[]> topNGramsMap = getTopNGrams();

      for (Map.Entry<Integer, NGramFreq[]> entry : topNGramsMap.entrySet()) {
        final Integer n = entry.getKey();
        final String key = n + "-Grams";
        final NGramFreq[] topNGrams = entry.getValue();

        results.add(new NGramResult(key, n, !"freq".equals(limitType),
                                    nGramLimit, topNGrams));
      }
    }
    return results;
  }


  public static final class NGramResult extends ProcessorResult {
    private int n;
    private boolean limitByCount;  // otherwise, limitByFreq
    private int nGramLimit;
    private NGramFreq[] nGramFreqs;

    /**
     * Default constructor for publishable reconstruction only.
     */
    public NGramResult() {
      super();
    }

    public NGramResult(String name, int n, boolean limitByCount, int nGramLimit, NGramFreq[] nGramFreqs) {
      super(name);

      this.n = n;
      this.limitByCount = limitByCount;
      this.nGramLimit = nGramLimit;
      this.nGramFreqs = nGramFreqs;
    }

    /**
     * Get this result's information with html markup.
     */
    public String toHtml(int maxRows) {
      final StringBuilder result = new StringBuilder();

      result.
        append("<table border=\"1\"><tr><th>").append(getSimpleName()).
        append("</th></tr>\n<tr><td><table>");

      if (nGramFreqs != null) {
        if (maxRows > 0) {
          // fit the data into enough columns to meet the max rows
          final int numCols = (int)Math.ceil(((double)nGramFreqs.length) / ((double)maxRows));
          final String[][] cells = new String[numCols][];
          cells[0] = new String[maxRows];

          int colNum = 0;
          int rowNum = 0;

          for (NGramFreq nGramFreq : nGramFreqs) {
            cells[colNum][rowNum++] = getElementAsHtml(nGramFreq);

            if (rowNum >= maxRows) {
              ++colNum;
              rowNum = 0;
              if (colNum < cells.length) cells[colNum] = new String[maxRows];
              else break;
            }
          }

          for (int rowNumI = 0; rowNumI < maxRows; ++rowNumI) {
            result.append("<tr>");
            for (int colNumI = 0; colNumI < numCols; ++colNumI) {
              result.append("<td \"nowrap\">");
              if (cells[colNumI][rowNumI] != null) {
                result.append(cells[colNumI][rowNumI]);
              }
              result.append("</td>");
            }
            result.append("</tr>");
          }
        }
        else {
          // just one column
          for (NGramFreq nGramFreq : nGramFreqs) {
            result.append("<tr><td>").append(getElementAsHtml(nGramFreq)).append("</td></tr>\n");
          }
        }
      }

      result.append("</table></tr></table>\n");

      return result.toString();
    }

    protected String getElementAsHtml(NGramFreq nGramFreq) {

      // ngram (freq)   ...todo: add sources with hyperlinks?
      return nGramFreq.toString();

    }

    /**
     * Get this result's value as a string.
     * <p>
     * This concatenates the "ngram (freq)" toString forms with comma delimiters.
     */
    protected String createValueAsString() {
      final StringBuilder result = new StringBuilder();

      for (NGramFreq nGramFreq : nGramFreqs) {
        if (result.length() > 0) result.append(',');
        result.append(nGramFreq.toString());
      }

      return result.toString();
    }

    /**
     * Do the work of combining other instance's value into this instance.
     */
    protected void doCombineWithOthers(ProcessorResult[] others) {
      final WordGramStats stats = new WordGramStats(n);

      if (nGramFreqs != null) {
        for (NGramFreq nGramFreq : nGramFreqs) stats.add(nGramFreq);
      }

      for (ProcessorResult other : others) {
        if (other instanceof NGramResult) {
          NGramResult o = (NGramResult)other;
          for (NGramFreq nGramFreq : o.nGramFreqs) stats.add(nGramFreq);
        }
      }

      this.nGramFreqs = (limitByCount) ?
        stats.getTopNGramsLimitedByCount(n, nGramLimit, null) :
        stats.getTopNGramsLimitedByFreq(n, nGramLimit, null);
    }

    /**
     * Do the work of combining this instance's value with the given string
     * forms of its value.
     */
    protected void doCombineWithOthers(String[] values) {
      final WordGramStats stats = new WordGramStats(n);

      if (nGramFreqs != null) {
        for (NGramFreq nGramFreq : nGramFreqs) stats.add(nGramFreq);
      }

      // split on commas, add all, re-sort re-trim
      for (String stringValue : values) {
        final String[] pieces = stringValue.split(",");
        for (String piece : pieces) {
          final NGramFreq nGramFreq = new NGramFreq(piece);
          stats.add(nGramFreq);
        }
      }
          
      this.nGramFreqs = (limitByCount) ?
        stats.getTopNGramsLimitedByCount(n, nGramLimit, null) :
        stats.getTopNGramsLimitedByFreq(n, nGramLimit, null);
    }
    
    /**
     * Write this message to the dataOutput stream such that this message
     * can be completely reconstructed through this.read(dataInput).
     *
     * @param dataOutput  the data output to write to.
     */
    public void write(DataOutput dataOutput) throws IOException {
      super.write(dataOutput);

      dataOutput.writeInt(n);
      dataOutput.writeBoolean(limitByCount);
      dataOutput.writeInt(nGramLimit);

      if (nGramFreqs == null) {
        dataOutput.writeInt(-1);
      }
      else {
        dataOutput.writeInt(nGramFreqs.length);
        for (NGramFreq nGramFreq : nGramFreqs) {
          MessageHelper.writePublishable(dataOutput, nGramFreq);
        }
      }
    }

    /**
     * Read this message's contents from the dataInput stream that was written by
     * this.write(dataOutput).
     * <p>
     * NOTE: this requires all implementing classes to have a default constructor
     *       with no args.
     *
     * @param dataInput  the data output to write to.
     */
    public void read(DataInput dataInput) throws IOException {
      super.read(dataInput);

      this.n = dataInput.readInt();
      this.limitByCount = dataInput.readBoolean();
      this.nGramLimit = dataInput.readInt();

      final int numFreqs = dataInput.readInt();
      if (numFreqs >= 0) {
        this.nGramFreqs = new NGramFreq[numFreqs];
        for (int i = 0; i < numFreqs; ++i) {
          this.nGramFreqs[i] = (NGramFreq)MessageHelper.readPublishable(dataInput);
        }
      }
    }
  }
}
