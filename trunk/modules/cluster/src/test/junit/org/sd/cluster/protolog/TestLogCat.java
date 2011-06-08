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
package org.sd.cluster.protolog;


import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Pattern;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit Tests for the LogCat class.
 * <p>
 * @author Spence Koehler
 */
public class TestLogCat extends TestCase {

	public TestLogCat(String name) {
		super(name);
	}
	

	public void testDecodeFileTimestamp() {
		final LogCat logCat = new LogCat(null, null);

		final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd.kk:mm:ss.SSS");
		final long timestamp = logCat.decodeFileTimestamp(
			new File("protolog.2009-10-09.16:11:17.802.log"),
			Pattern.compile(".*?\\.(.*)\\.log"),
			dateFormat);

		assertEquals("2009-10-09.16:11:17.802", dateFormat.format(new Date(timestamp)));
	}


	public static Test suite() {
		TestSuite suite = new TestSuite(TestLogCat.class);
		return suite;
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
}
