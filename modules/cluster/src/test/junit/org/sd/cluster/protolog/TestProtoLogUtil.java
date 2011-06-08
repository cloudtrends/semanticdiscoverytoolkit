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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit Tests for the ProtoLogUtil class.
 * <p>
 * @author Spence Koehler
 */
public class TestProtoLogUtil extends TestCase {

	public TestProtoLogUtil(String name) {
		super(name);
	}
	

	public void testDecodeFileTimestamp() {
		final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd.kk:mm:ss.SSS");
		final Date timestamp = ProtoLogUtil.decodeFileTimestamp(
			new File("segevent.2009-10-09.16:11:17.802.log"),
			Pattern.compile(".*?\\.(.*)\\.log"),
			dateFormat);

		assertEquals("2009-10-09.16:11:17.802", dateFormat.format(timestamp));
	}

	public void testGetField() {
		final Foo foo = new Foo(new Bar(123, new int[]{4, 5, 6}));

		assertEquals("123", ProtoLogUtil.getField(foo, "bar.baz").toString());

		final Object[] values = ProtoLogUtil.getRepeatingField(foo, "bar.blah");
		assertEquals(3, values.length);
		assertEquals(4, values[0]);
		assertEquals(5, values[1]);
		assertEquals(6, values[2]);
	}


	static final class Foo {
		private Bar bar;

		Foo(Bar bar) {
			this.bar = bar;
		}

		public Bar getBar() {
			return bar;
		}
	}

	static final class Bar {
		private int baz;
		private List<Integer> blah;

		Bar(int baz, int[] blah) {
			this.baz = baz;
			this.blah = new ArrayList<Integer>();
			for (int b : blah) this.blah.add(b);
		}

		public int getBaz() {
			return baz;
		}

		public List<Integer> getBlah() {
			return blah;
		}
	}


	public static Test suite() {
		TestSuite suite = new TestSuite(TestProtoLogUtil.class);
		return suite;
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}
}
