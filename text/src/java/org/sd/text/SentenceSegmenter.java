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


import org.sd.nlp.SentenceSplitter;

/**
 * A TextSegmenter that uses an nlp.SentenceSplitter.
 * <p>
 * @author Spence Koehler
 */
public class SentenceSegmenter implements TextSegmenter {
	
	private String[] sentences;
	private int index;

	public SentenceSegmenter(String text) {
		final SentenceSplitter splitter = new SentenceSplitter();
		this.sentences = splitter.split(text);
		this.index = 0;
	}

	/**
	 * Determine whether there is a next sentence.
	 */
	public boolean hasNext() {
		return index < sentences.length;
	}

	/**
	 * Get the next sentence.
	 */
	public String next() {
		String result = null;

		if (hasNext()) {
			result = sentences[index];
			++index;
		}

		return result;
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
	 * Determine whether the last segment returned by 'next' should be flushed.
	 * <p>
	 * When segments are not flushed, repeated word N-Grams only count as
	 * a single instance for purposes of tallying frequencies.
	 * <p>
	 * This implementation always returns true.
	 */
	public boolean shouldFlush() {
		return true;
	}
}
