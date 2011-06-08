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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Pattern;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit Tests for the MultiLogIterator class.
 * <p>
 * @author Spence Koehler
 */
public class TestMultiLogIterator extends TestCase {

	public TestMultiLogIterator(String name) {
		super(name);
	}
	

	private final MultiLogIterator buildMultiLogIterator(Long minTime, Long maxTime) {
		return 
			new MultiLogIterator(
				null, null,
        ProtoLogStreamer.DEFAULT_INSTANCE,
        ProtoLogStreamer.MAX_MESSAGE_BYTES,
				Pattern.compile(".*?\\.(.*)\\.log"),
				new SimpleDateFormat("yyyy-MM-dd.kk:mm:ss.SSS"),
				minTime, maxTime);
	}

	public void testIsInRange_specified() {
		final long curtime = System.currentTimeMillis();

		final MultiLogIterator logIterator = buildMultiLogIterator(curtime - 1000, curtime + 1000);

		assertEquals(0, logIterator.isInRange(curtime));
		assertEquals(0, logIterator.isInRange(curtime - 1000));
		assertEquals(0, logIterator.isInRange(curtime + 1000));
				
		assertEquals(-1, logIterator.isInRange(curtime - 1001));
		assertEquals(1, logIterator.isInRange(curtime + 1001));
	}

	public void testIsInRange_open() {
		final long curtime = System.currentTimeMillis();

		final MultiLogIterator logIterator = buildMultiLogIterator(null, null);

		assertEquals(0, logIterator.isInRange(curtime));
		assertEquals(0, logIterator.isInRange(curtime - 1000));
		assertEquals(0, logIterator.isInRange(curtime + 1000));
				
		assertEquals(0, logIterator.isInRange(curtime - 1001));
		assertEquals(0, logIterator.isInRange(curtime + 1001));
	}

	public void testIsInRange_openLower() {
		final long curtime = System.currentTimeMillis();

		final MultiLogIterator logIterator = buildMultiLogIterator(null, curtime + 1000);

		assertEquals(0, logIterator.isInRange(curtime));
		assertEquals(0, logIterator.isInRange(curtime - 1000));
		assertEquals(0, logIterator.isInRange(curtime + 1000));
				
		assertEquals(0, logIterator.isInRange(curtime - 1001));
		assertEquals(1, logIterator.isInRange(curtime + 1001));
	}

	public void testIsInRange_openUpper() {
		final long curtime = System.currentTimeMillis();

		final MultiLogIterator logIterator = buildMultiLogIterator(curtime - 1000, null);

		assertEquals(0, logIterator.isInRange(curtime));
		assertEquals(0, logIterator.isInRange(curtime - 1000));
		assertEquals(0, logIterator.isInRange(curtime + 1000));
				
		assertEquals(-1, logIterator.isInRange(curtime - 1001));
		assertEquals(0, logIterator.isInRange(curtime + 1001));
	}

	public void testCouldHaveRange() {
		final long curtime = System.currentTimeMillis();
		MultiLogIterator logIterator =	buildMultiLogIterator(curtime - 1000, curtime + 1000);
		final DateFormat dateFormat = logIterator.getDateFormat();

		final File[] logfiles = new File[] {
			new File("foo." + dateFormat.format(new Date(curtime - 2000)) + ".log"),
			new File("foo." + dateFormat.format(new Date(curtime - 1000)) + ".log"),
			new File("foo." + dateFormat.format(new Date(curtime)) + ".log"),
			new File("foo." + dateFormat.format(new Date(curtime + 1000)) + ".log"),
			new File("foo." + dateFormat.format(new Date(curtime + 2000)) + ".log"),
		};


		// [-1000, 1000]
		logIterator = buildMultiLogIterator(curtime - 1000, curtime + 1000);
		assertEquals(false, logIterator.couldHaveRange(logfiles, 0));
		assertEquals(true, logIterator.couldHaveRange(logfiles, 1));
		assertEquals(true, logIterator.couldHaveRange(logfiles, 2));
		assertEquals(true, logIterator.couldHaveRange(logfiles, 3));
		assertEquals(false, logIterator.couldHaveRange(logfiles, 4));

		// (,) open
		logIterator = buildMultiLogIterator(null, null);
		assertEquals(true, logIterator.couldHaveRange(logfiles, 0));
		assertEquals(true, logIterator.couldHaveRange(logfiles, 1));
		assertEquals(true, logIterator.couldHaveRange(logfiles, 2));
		assertEquals(true, logIterator.couldHaveRange(logfiles, 3));
		assertEquals(true, logIterator.couldHaveRange(logfiles, 4));

		// (,1000] unbounded below
		logIterator = buildMultiLogIterator(null, curtime + 1000);
		assertEquals(true, logIterator.couldHaveRange(logfiles, 0));
		assertEquals(true, logIterator.couldHaveRange(logfiles, 1));
		assertEquals(true, logIterator.couldHaveRange(logfiles, 2));
		assertEquals(true, logIterator.couldHaveRange(logfiles, 3));
		assertEquals(false, logIterator.couldHaveRange(logfiles, 4));

		// [1000,) unbounded above
		logIterator = buildMultiLogIterator(curtime - 1000, null);
		assertEquals(false, logIterator.couldHaveRange(logfiles, 0));
		assertEquals(true, logIterator.couldHaveRange(logfiles, 1));
		assertEquals(true, logIterator.couldHaveRange(logfiles, 2));
		assertEquals(true, logIterator.couldHaveRange(logfiles, 3));
		assertEquals(true, logIterator.couldHaveRange(logfiles, 4));


		// Test where lower-than-range file could have entries within
		// range because next file starts higher than lower bound of range
		// [-500, 500]
		logIterator = buildMultiLogIterator(curtime - 500, curtime + 500);
		assertEquals(false, logIterator.couldHaveRange(logfiles, 0));
		assertEquals(true, logIterator.couldHaveRange(logfiles, 1));
		assertEquals(true, logIterator.couldHaveRange(logfiles, 2));
		assertEquals(false, logIterator.couldHaveRange(logfiles, 3));
		assertEquals(false, logIterator.couldHaveRange(logfiles, 4));

		// [-1500, 1500]
		logIterator = buildMultiLogIterator(curtime - 1500, curtime + 1500);
		assertEquals(true, logIterator.couldHaveRange(logfiles, 0));
		assertEquals(true, logIterator.couldHaveRange(logfiles, 1));
		assertEquals(true, logIterator.couldHaveRange(logfiles, 2));
		assertEquals(true, logIterator.couldHaveRange(logfiles, 3));
		assertEquals(false, logIterator.couldHaveRange(logfiles, 4));
	}

	public static Test suite() {
		TestSuite suite = new TestSuite(TestMultiLogIterator.class);
		return suite;
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
}
