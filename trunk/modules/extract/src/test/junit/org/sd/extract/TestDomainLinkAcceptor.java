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
package org.sd.extract;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit Tests for the DomainLinkAcceptor class.
 * <p>
 * @author Spence Koehler
 */
public class TestDomainLinkAcceptor extends TestCase {

  public TestDomainLinkAcceptor(String name) {
    super(name);
  }
  
  public void testIsDomainRoot() {
    final DomainLinkAcceptor acceptor = new DomainLinkAcceptor();

    assertTrue(acceptor.isDomainRoot("https://foo.bar.com"));
    assertTrue(acceptor.isDomainRoot("https://foo.bar.com/"));
    assertTrue(acceptor.isDomainRoot("https://foo.bar.com/index.html"));
    assertTrue(acceptor.isDomainRoot("https://foo.bar.com/indexb.html"));
    assertTrue(acceptor.isDomainRoot("https://foo.bar.com/indexb.php?baz"));
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestDomainLinkAcceptor.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
