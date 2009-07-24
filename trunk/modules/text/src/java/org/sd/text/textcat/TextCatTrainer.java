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
package org.sd.text.textcat;


import org.sd.io.FileUtil;
import org.sd.util.tree.Tree;
import org.sd.xml.HtmlHelper;
import org.sd.xml.XmlLite;
import org.sd.xml.XmlNodeRipper;
import org.sd.xml.XmlTreeHelper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility to train up a language module.
 * <p>
 * @author Spence Koehler
 */
public class TextCatTrainer {

  /**
   * Train up a language module.
   *
   * @param inputDirName      name of directory holding a language's html files.
   * @param inputFilePattern  regular expression pattern for selecting training files, applied to each file name (excluding path).
   * @param language          name of language (including encoding if desired, excluding ".lm" extension).
   * @param outputDir         name of directory to write trained language module to.
   */
  public static void train(String inputDirName, String inputFilePattern, String language, String outputDir) throws IOException {
    final File inputDir = new File(inputDirName);
    final Pattern pattern = Pattern.compile(inputFilePattern);

    final File[] languageFiles = inputDir.listFiles(new FileFilter() {
        public boolean accept(File file) {
          final String name = file.getName();
          final Matcher matcher = pattern.matcher(name);
          return matcher.matches();
        }
      });

    final Map<String, TextCat.NGram> chars2ngram = new HashMap<String, TextCat.NGram>();
    for (File languageFile : languageFiles) {
      addFileNGrams(chars2ngram, languageFile);
    }

    final List<TextCat.NGram> ngrams = TextCat.processNGrams(chars2ngram, 400);

    // create language module
    final String languageModule = FileUtil.getFilename(outputDir, language + ".lm");
    final BufferedWriter writer = FileUtil.getWriter(languageModule);

    for (TextCat.NGram ngram : ngrams) {
      writer.write(ngram.toString());
      writer.newLine();
    }

    writer.close();
  }

  private static final void addFileNGrams(Map<String, TextCat.NGram> chars2ngram, File languageFile) throws IOException {
    final XmlNodeRipper ripper = new XmlNodeRipper(languageFile, true/*isHtml*/, HtmlHelper.DEFAULT_IGNORE_TAGS);

    while (ripper.hasNext()) {
      final Tree<XmlLite.Data> node = ripper.next();
      final String text = XmlTreeHelper.getAllText(node);
      TextCat.addNGrams(chars2ngram, text);
    }
  }
}
