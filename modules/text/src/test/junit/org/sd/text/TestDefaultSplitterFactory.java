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


import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.sd.nlp.GeneralNormalizer;

/**
 * JUnit Tests for the DefaultSplitterFactory class.
 * <p>
 * @author Spence Koehler
 */
public class TestDefaultSplitterFactory extends TestCase {

  public TestDefaultSplitterFactory(String name) {
    super(name);
  }
  

  public void testBadNormalizerString() {
    // set bad normalizer; expect IllegalArgumentException
    final Properties properties = new Properties();
    properties.setProperty("normalizer", "nonexistent-normalizer");

    try {
      final DefaultSplitterFactory splitterFactory = new DefaultSplitterFactory(properties);
      fail("Expected an error while attempting to build nonexistent normalizer!");
    }
    catch (IllegalArgumentException e) {
      // success!!!
    }
  }

  public void testBadWordAcceptorString() {
    // set bad acceptor; expect IllegalArgumentException
    // set bad normalizer; expect IllegalArgumentException
    final Properties properties = new Properties();
    properties.setProperty("acceptor", "nonexistent-acceptor");

    try {
      final DefaultSplitterFactory splitterFactory = new DefaultSplitterFactory(properties);
      fail("Expected an error while attempting to build nonexistent acceptor!");
    }
    catch (IllegalArgumentException e) {
      // success!!!
    }
  }

  public void testOnlyNormalizer() {
    // don't set locale; do set normalizer not acceptor; expect normalizer and (null) acceptor.
    final Properties properties = new Properties();
    properties.setProperty("normalizer", "org.sd.nlp.GeneralNormalizer:getCaseInsensitiveInstance");

    final DefaultSplitterFactory splitterFactory = new DefaultSplitterFactory(properties);
    assertEquals(GeneralNormalizer.getCaseInsensitiveInstance(), splitterFactory.getNormalizer());
  }

  public void testGoodNormalizerAndAcceptor() {
    // don't set locale; do set normalizer and acceptor; expect normalizer and acceptor.
    // don't set locale; do set normalizer not acceptor; expect normalizer and (null) acceptor.
    final Properties properties = new Properties();
    properties.setProperty("normalizer", "org.sd.nlp.GeneralNormalizer:getCaseInsensitiveInstance");
    properties.setProperty("acceptor", "org.sd.text.GeneralWordAcceptor");

    final DefaultSplitterFactory splitterFactory = new DefaultSplitterFactory(properties);
    assertEquals(GeneralNormalizer.getCaseInsensitiveInstance(), splitterFactory.getNormalizer());
    assertEquals("org.sd.text.GeneralWordAcceptor", splitterFactory.getWordAcceptor().getClass().getName());
  }

  public void testNoNormalizer() {
    // don't set locale or normalizer; expect non-null normalizer
    final Properties properties = new Properties();

    final DefaultSplitterFactory splitterFactory = new DefaultSplitterFactory(properties);
    assertNotNull(splitterFactory.getNormalizer());
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestDefaultSplitterFactory.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
