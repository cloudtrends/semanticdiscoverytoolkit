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
package org.sd.cio;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.sd.io.FileUtil;

/**
 * JUnit Tests for the FileJoiner class.
 * <p>
 * @author Spence Koehler
 */
public class TestFileJoiner extends TestCase {

  private static final Set<String> vowels = new HashSet<String>(Arrays.asList(new String[]{"a", "e", "i", "o", "u", "y"}));


  public TestFileJoiner(String name) {
    super(name);
  }
  

  public void testSubtracting() throws IOException {
    final Properties properties = new Properties();

    final File outputFile = new File("/tmp/TestFileJoiner/fileA-B.txt");
    if (outputFile.exists()) outputFile.delete();

    properties.setProperty("fileA", FileUtil.getFilename(this.getClass(), "resources/fileA.txt"));
    properties.setProperty("fileB", FileUtil.getFilename(this.getClass(), "resources/fileB.txt"));
    properties.setProperty("outputCols", "A.0/B.0");
    properties.setProperty("joinColA", "0");
    properties.setProperty("joinColB", "0");
    properties.setProperty("keepAllA", "true");
    properties.setProperty("keepAllB", "false");
    properties.setProperty("subtract", "true");
    properties.setProperty("outputFile", outputFile.getAbsolutePath());
    properties.setProperty("verbose", "false");

    final FileJoiner fileJoiner = new FileJoiner(properties);
    fileJoiner.doJoin();

    // verify the output: all letters of the alphabet without vowels.
    verifyConsonants(outputFile);

    if (outputFile.exists()) outputFile.delete();
  }

  public void testFiltering() throws IOException {
    final Properties properties = new Properties();

    final File outputFile = new File("/tmp/TestFileJoiner/fileA.filtered.txt");
    if (outputFile.exists()) outputFile.delete();

    properties.setProperty("fileA", FileUtil.getFilename(this.getClass(), "resources/fileA.txt"));
    properties.setProperty("fileB", FileUtil.getFilename(this.getClass(), "resources/fileA.txt"));
    properties.setProperty("outputCols", "A.0/B.0");
    properties.setProperty("joinColA", "0");
    properties.setProperty("joinColB", "0");
    properties.setProperty("keepAllA", "true");
    properties.setProperty("keepAllB", "false");
    properties.setProperty("subtract", "true");
    properties.setProperty("keyGeneratorB", "org.sd.cio.TestFileJoiner$VowelFilterGenerator");
    properties.setProperty("outputFile", outputFile.getAbsolutePath());
    properties.setProperty("verbose", "false");

    final FileJoiner fileJoiner = new FileJoiner(properties);
    fileJoiner.doJoin();

    // verify the output: all letters of the alphabet without vowels.
    verifyConsonants(outputFile);

    if (outputFile.exists()) outputFile.delete();
  }

  private final void verifyConsonants(File file) throws IOException {
    final Set<String> lines = new HashSet<String>();
    FileUtil.readStrings(lines, file, null, null, null, true, false);

    assertEquals(20, lines.size());

    for (char c = 'a'; c <= 'z'; ++c) {
      final String cstring = Character.toString(c);
      if (vowels.contains(cstring)) {
        assertFalse("vowel=" + c, lines.contains(cstring));
      }
      else {
        assertTrue("consonant=" + c, lines.contains(cstring));
      }
    }
  }


  public static final class VowelFilterGenerator implements FileJoiner.KeyGenerator {

    public VowelFilterGenerator() {
    }

    public String generateKey(String fieldData) {
      String result = null;

      if (vowels.contains(fieldData)) result = fieldData;

      return result;
    }
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestFileJoiner.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
