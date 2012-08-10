/*
    Copyright 2011 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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
package org.sd.token;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import org.sd.io.FileUtil;
import org.sd.util.SentenceIterator;
import org.sd.util.StatsAccumulator;

/**
 * Simple utility to analyze tokenization speed.
 * <p>
 * This is used to ensure that implementations or changes to tokenization
 * don't adversely affect performance.
 *
 * @author Spence Koehler
 */
public class TokenizerSpeedTest {
  
  private StandardTokenizerOptions tokenizerOptions;
  private boolean splitSentences;
  private FileFilter fileFilter;
  private StatsAccumulator initTimes;
  private StatsAccumulator nextTimes;
  private StatsAccumulator reviseTimes;

  public TokenizerSpeedTest(StandardTokenizerOptions tokenizerOptions, boolean splitSentences, FileFilter fileFilter) {
    this.tokenizerOptions = tokenizerOptions == null ? StandardTokenizerFactory.DEFAULT_OPTIONS : tokenizerOptions;
    this.splitSentences = splitSentences;
    this.fileFilter = fileFilter;

    this.initTimes = new StatsAccumulator("initTimes");
    this.nextTimes = new StatsAccumulator("nextTimes");
    this.reviseTimes = new StatsAccumulator("reviseTimes");
  }

  public StatsAccumulator getInitTimes() {
    return initTimes;
  }

  public StatsAccumulator getNextTimes() {
    return nextTimes;
  }

  public StatsAccumulator getReviseTimes() {
    return reviseTimes;
  }

  public Tokenizer getTokenizer(String text) {
    return new StandardTokenizer(text, tokenizerOptions);
  }

  public void analyze(File file) throws IOException {
    if (file.isDirectory()) {
      final File[] files = (fileFilter == null) ? file.listFiles() : file.listFiles(fileFilter);
      for (File f : files) {
        analyze(f);
      }
    }
    else {
      final BufferedReader reader = FileUtil.getReader(file);
      String line = null;
      while ((line = reader.readLine()) != null) {
        if ("".equals(line) || line.charAt(0) == '#') continue;
        analyze(line);
      }
      reader.close();
    }
  }

  public void analyze(String inputString) {
    if (splitSentences) {
      for (SentenceIterator iter = new SentenceIterator(inputString); iter.hasNext(); ) {
        final String curString = iter.next();
        doAnalyze(curString);
      }
    }
    else {
      doAnalyze(inputString);
    }
  }

  private final void doAnalyze(String inputString) {
    final long initStart = System.currentTimeMillis();
    final Tokenizer tokenizer = getTokenizer(inputString);
    Token token = tokenizer.getToken(0);

    initTimes.add(System.currentTimeMillis() - initStart);

    for (; token != null; token = getNextToken(tokenizer, token)) {
      Token revisedToken = getRevisedToken(tokenizer, token);
      for (; revisedToken != null; revisedToken = getRevisedToken(tokenizer, revisedToken)) {
        // nothing to do
      }
    }
  }

  private final Token getNextToken(Tokenizer tokenizer, Token token) {
    final long nextStart = System.currentTimeMillis();
    final Token result = tokenizer.getNextToken(token);
    nextTimes.add(System.currentTimeMillis() - nextStart);
    return result;
  }

  private final Token getRevisedToken(Tokenizer tokenizer, Token token) {
    final long reviseStart = System.currentTimeMillis();
    final Token result = tokenizer.revise(token);
    reviseTimes.add(System.currentTimeMillis() - reviseStart);
    return result;
  }


  public static void main(String[] args) throws IOException {
    final StandardTokenizerOptions options = new StandardTokenizerOptions();
    //options.setTokenBreakLimit(5);


    // currently, just test standard tokenizer w/default options
    final TokenizerSpeedTest tester = new TokenizerSpeedTest(options, true, null);
    for (String arg : args) {
      final File file = new File(arg);
      if (file.exists()) {
        tester.analyze(file);
      }
      else {
        tester.analyze(arg);
      }
    }

    System.out.println(tester.getInitTimes());
    System.out.println(tester.getNextTimes());
    System.out.println(tester.getReviseTimes());
  }
}
