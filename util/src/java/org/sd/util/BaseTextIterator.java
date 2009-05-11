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
package org.sd.util;


import java.text.BreakIterator;
import java.util.Iterator;

/**
 * A naive iterator over portions of a string based on java.text.BreakIterator.
 * <p>
 * @author Spence Koehler
 */
public class BaseTextIterator implements Iterator<String> {
	
	private BreakIterator breakIterator;
	private String text;
	private int start;
	private int end;
	private String next;

	/**
	 * Construct with the string whose text is to be iterated over.
	 */
	public BaseTextIterator(BreakIterator breakIterator) {
		this.breakIterator = breakIterator;
	}

	/**
	 * Set the text to be iterated over, resetting iteration to the
	 * beginning of the text.
	 */
	public final void setText(String text) {
		this.text = text;

		breakIterator.setText(text);
		this.start = breakIterator.first();
		this.end = breakIterator.next();

		computeNext();
	}

	/**
	 * Get the full text being iterated over.
	 */
	public final String getText() {
		return text;
	}

	/**
	 * Get the next text.
	 */
	public String next() {
		final String result = next;
		computeNext();
		return result;
	}

	/**
	 * Determine whether there is a next text.
	 */
	public boolean hasNext() {
		return next != null;
	}

	/**
	 * Remove the last text returned by 'next'.
	 * <p>
	 * Not implemented!
	 *
	 * @throws UnsupportedOperationException
	 */
	public void remove() {
		throw new UnsupportedOperationException("Not implemented!");
	}

	/**
	 * Determine whether the the substring should be accepted as text to return.
	 * <p>
	 * This default implementation accepts text if there is a letter or
	 * digit codepoint within the span.
	 */
	protected boolean accept(String text, int start, int end) {
		boolean result = false;

		for (int i = start; i < end; ++i) {
			if (Character.isLetterOrDigit(text.codePointAt(i))) {
				result = true;
				break;
			}
		}

		return result;
	}

	/**
	 * Get the next text.
	 */
	private void computeNext() {
		String result = null;

		while (result == null && computeHasNext()) {
			if (accept(text, start, end)) {
				result = text.substring(start, end).trim();
			}			

			start = end;
			end = breakIterator.next();
		}

		this.next = result;
	}

	/**
	 * Determine whether there is a next text.
	 */
	private final boolean computeHasNext() {
		return start != BreakIterator.DONE && end != BreakIterator.DONE;
	}
}
