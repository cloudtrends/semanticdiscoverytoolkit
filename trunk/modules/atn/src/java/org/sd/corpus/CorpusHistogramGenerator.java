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
package org.sd.corpus;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sd.io.FileUtil;
import org.sd.token.StandardTokenizer;
import org.sd.token.StandardTokenizerFactory;
import org.sd.token.StandardTokenizerOptions;
import org.sd.token.Token;
import org.sd.token.Tokenizer;
import org.sd.util.Histogram;
import org.sd.util.SentenceIterator;
import org.sd.xml.DomElement;
import org.sd.xml.DomNode;
import org.sd.xml.DataProperties;
import org.w3c.dom.NodeList;

/**
 * Base class for generating histograms from a corpus.
 * <p>
 * @author Spence Koehler
 */
public abstract class CorpusHistogramGenerator {
  
  /** Generate histogram strings (ok if null) from the token. */
  protected abstract String[] generateHistogramStrings(Token token);

  public static interface InstanceBuilder {
    public CorpusHistogramGenerator buildInstance(DataProperties options, String[] args, File histogramGeneratorConfig) throws IOException;
  }


  private DataProperties options;
  private FileSelector fileSelector;
  private StandardTokenizerOptions tokenizerOptions;
  private boolean oneLine;

  //
  // Histogram Generator Config:
  //
  // <config oneLine='true|false'>
  //   <fileSelector>
  //     <dirPattern>...</dirPattern>    <!-- regex match pattern; accept all if omitted -->
  //     <filePattern>...</filePattern>  <!-- file match pattern; accept all if omitted -->
  //   </fileSelector>
  //   <tokenizerOptions>
  //     ...
  //   </tokenizerOptions>
  // </config>

  protected CorpusHistogramGenerator(File histogramGeneratorConfig) throws IOException {
    this.options = new DataProperties(histogramGeneratorConfig);
    init(options.getDomElement());
  }

  protected CorpusHistogramGenerator(DomElement histogramGeneratorConfig) {
    this.options = new DataProperties(histogramGeneratorConfig);
    init(histogramGeneratorConfig);
  }

  public boolean oneLine() {
    return oneLine;
  }

  public void setOneLine(boolean oneLine) {
    this.oneLine = oneLine;
  }

  private final void init(DomElement configElt) {
    this.oneLine = configElt.getAttributeBoolean("oneLine", false);

    final NodeList childNodes = configElt.getChildNodes();
    final int numChildNodes = childNodes.getLength();
    for (int childNum = 0; childNum < numChildNodes; ++childNum) {
      final DomNode childNode = (DomNode)childNodes.item(childNum);
      if (childNode.getNodeType() != DomNode.ELEMENT_NODE) continue;

      final String childName = childNode.getNodeName().toLowerCase();
      if ("fileselector".equals(childName)) {
        this.fileSelector = new FileSelector((DomElement)childNode);
      }
      else if ("tokenizeroptions".equals(childName)) {
        this.tokenizerOptions = new StandardTokenizerOptions((DomElement)childNode);
      }
    }
  }

  public DataProperties getOptions() {
    return options;
  }

  public FileSelector getFileSelector() {
    return fileSelector;
  }

  public StandardTokenizerOptions getTokenizerOptions() {
    return tokenizerOptions;
  }

  public Histogram<String> generateHistogram(File corpusDir) throws IOException {
    final Histogram<String> result = new Histogram<String>();
    generateHistogram(corpusDir, result);
    return result;
  }

  public void generateHistogram(File corpusFile, Histogram<String> result) throws IOException {
    if (corpusFile == null || !corpusFile.exists() || !process(corpusFile)) return;

    if (corpusFile.isDirectory()) {
      final File[] files = corpusFile.listFiles();
      for (File file : files) {
        generateHistogram(file, result);
      }
    }
    else {
      System.out.print(".");
      final BufferedReader reader = FileUtil.getReader(corpusFile, "UTF-8");
      String line = null;

      while ((line = reader.readLine()) != null) {
        line = line.trim();
        if ("".equals(line) || line.charAt(0) == '#') continue;
        generateHistogram(line, result);
      }

      reader.close();
    }
  }

  public void generateHistogram(String line, Histogram<String> result) {
    if (!oneLine) {
      for (SentenceIterator iter = new SentenceIterator(line, true); iter.hasNext(); ) {
        final String sentence = iter.next();
        doGenerateHistogram(sentence, result);
      }
    }
    else {
      doGenerateHistogram(line, result);
    }
  }

  private final void doGenerateHistogram(String line, Histogram<String> result) {
    final Tokenizer tokenizer = buildTokenizer(line);

    if (tokenizer != null) {
      for (Token token = tokenizer.getToken(0); token != null; token = token.getNextToken()) {
        generateHistogram(token, result);
      }
    }
  }

  public void generateHistogram(Token token, Histogram<String> result) {
    final String[] items = generateHistogramStrings(token);
    if (items != null) {
      for (String item : items) {
        result.add(item);
      }
    }
  }

  /** Determine whether to process the corpus file or directory. */
  protected boolean process(File corpusFile) {
    boolean result = true;

    if (fileSelector != null) {
      result = fileSelector.accept(corpusFile);
    }

    return result;
  }

  protected Tokenizer buildTokenizer(String line) {
    return
      new StandardTokenizer(line,
                            tokenizerOptions == null ?
                            StandardTokenizerFactory.DEFAULT_OPTIONS :
                            tokenizerOptions);
  }


  public static class FileSelector {

    private Pattern dirPattern;
    private Pattern filePattern;

    public FileSelector(DomElement fileSelectorElt) {
      //   <fileSelector>
      //     <dirPattern>...</dirPattern>    <!-- regex match pattern; accept all if omitted -->
      //     <filePattern>...</filePattern>  <!-- file match pattern; accept all if omitted -->
      //   </fileSelector>
      final DomNode dirPatternNode = fileSelectorElt.selectSingleNode("dirPattern");
      final String dirPatternString = dirPatternNode == null ? null : dirPatternNode.getTextContent();

      final DomNode filePatternNode = fileSelectorElt.selectSingleNode("filePattern");
      final String filePatternString = filePatternNode == null ? null : filePatternNode.getTextContent();

      this.dirPattern = dirPatternString == null ? null : Pattern.compile(dirPatternString);
      this.filePattern = filePatternString == null ? null : Pattern.compile(filePatternString);
    }

    public boolean accept(File file) {
      boolean result = false;

      if (file != null && file.exists()) {
        final Pattern pattern = file.isDirectory() ? dirPattern : filePattern;
        result = true;
        if (pattern != null) {
          final Matcher m = pattern.matcher(file.getName());
          result = m.matches();
        }
      }

      return result;
    }
  }


  public static CorpusHistogramGenerator doMain(String[] args, InstanceBuilder instanceBuilder) throws IOException {
    //
    // Properties:
    //
    //  config -- (required) path to CorpusHistogramGenerator config (xml) file.
    //  out -- (required) output file for generated histogram
    //
    //  args -- paths to files/dirs to process
    //

    final DataProperties dataProperties = new DataProperties(args);
    args = dataProperties.getRemainingArgs();

    final String config = dataProperties.getString("config");
    final String out = dataProperties.getString("out");
    

    final CorpusHistogramGenerator generator = instanceBuilder.buildInstance(dataProperties, args, new File(config));


    final Histogram<String> h = new Histogram<String>();
    for (String arg : args) {
      generator.generateHistogram(new File(arg), h);
    }
    

    final BufferedWriter writer = FileUtil.getWriter(new File(out));
    writer.write(h.toString(0));
    writer.close();


    return generator;
  }
}
