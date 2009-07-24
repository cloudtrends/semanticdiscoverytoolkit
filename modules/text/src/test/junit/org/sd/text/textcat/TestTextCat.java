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


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.sd.io.FileUtil;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.List;

/**
 * JUnit Tests for the TextCat class.
 * <p>
 * @author Spence Koehler
 */
public class TestTextCat extends TestCase {

  private static final String LANGUAGE_TEST_FILE_DIR1 = "resources/language_texts1";
  private static final String LANGUAGE_TEST_FILE_DIR2 = "resources/language_texts2";

  public TestTextCat(String name) {
    super(name);
  }
  
  private final void doTestTexts(TextCat textCat, File dir, boolean doAll, boolean verbose) throws IOException {

    final File[] languageTestFiles = dir.listFiles(new FileFilter() {
        public final boolean accept(File pathname) {
          final String name = pathname.getName().toLowerCase();
          return name.endsWith(".txt");
        }
      });

    int numPassed = 0;
    int numFailed = 0;

    if (languageTestFiles == null) {
      System.out.println("dir=" + dir + " (exists=" + dir.exists() + ")");
    }

    for (File languageTestFile : languageTestFiles) {
      final String language = textCat.getLanguage(languageTestFile);  // expectation
      if (textCat.hasLanguage(language) || doAll) {
        final String text = FileUtil.readAsString(languageTestFile.getAbsolutePath());
        final List<TextCat.ClassificationResult> languages = textCat.classify(text);

        if (!TextCat.hasLanguage(languages, language)) {
          ++numFailed;

          if (verbose) {
            System.out.println("languageTestFile: " + languageTestFile.getName() + " expected: " + language + " got: " + languages);
          }
        }
        else {
          ++numPassed;
        }
      }
    }

    if (verbose) {
      System.out.println("pass: " + numPassed + " fail: " + numFailed);
    }

    if (doAll) {
      assertTrue(numPassed > 0);
    }
    else {
      assertTrue(numPassed > numFailed);
    }
  }

  public void testTexts() throws IOException {
    final TextCat textCat = new TextCat();  // load all languages
    doTestTexts(textCat, FileUtil.getTestResourceFile(this.getClass(), LANGUAGE_TEST_FILE_DIR1), false, true);
    doTestTexts(textCat, FileUtil.getTestResourceFile(this.getClass(), LANGUAGE_TEST_FILE_DIR2), false, true);
  }

  public void testConstrainedTexts() throws IOException {
    final TextCat textCat = new TextCat(new String[]{"dutch", "english", "german"});
    doTestTexts(textCat, FileUtil.getTestResourceFile(this.getClass(), LANGUAGE_TEST_FILE_DIR1), true, false);
    doTestTexts(textCat, FileUtil.getTestResourceFile(this.getClass(), LANGUAGE_TEST_FILE_DIR2), true, false);
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestTextCat.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
