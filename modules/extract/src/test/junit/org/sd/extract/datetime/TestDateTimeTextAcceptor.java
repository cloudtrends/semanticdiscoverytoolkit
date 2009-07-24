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
package org.sd.extract.datetime;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.sd.extract.DocText;

import java.io.IOException;

/**
 * JUnit Tests for the DateTimeTextAcceptor class.
 * <p>
 * @author Spence Koehler
 */
public class TestDateTimeTextAcceptor extends TestCase {

  public TestDateTimeTextAcceptor(String name) {
    super(name);
  }
  
  private final void verify(DateTimeTextAcceptor textAcceptor, String sentence, boolean accept) throws IOException {
    assertEquals(accept, textAcceptor.accept(DocText.makeDocText(sentence)));
  }

  public void test1() throws IOException {
    final DateTimeTextAcceptor textAcceptor = DateTimeTextAcceptor.getInstance();

    verify(textAcceptor, "Tuesday December 27, 2005", true);
    verify(textAcceptor, "[24 Dec 2005|", true);
    verify(textAcceptor, "03:19pm", true);
    verify(textAcceptor, "Originally Posted on 7/13/2005 11:17:50 AMContent source: http://lexorsantos.blogspot.com/2005/07/top-ten-reasons-people-file-for.html", true);
    verify(textAcceptor, "Published: Tuesday, December 27, 2005", true);
    verify(textAcceptor, "Wednesday December 21, 2005 1:46 AM", true);
    verify(textAcceptor, "This is too long to assume the following date applies 24 Dec 2005.", false);
    verify(textAcceptor, "2007-06-28", true);
    verify(textAcceptor, "Last modified: Thu Jun 28 13:46:16 MDT 2007 ", true);
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestDateTimeTextAcceptor.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
