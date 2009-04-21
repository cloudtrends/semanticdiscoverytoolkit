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
package org.sd.nlp;


/**
 * Pointer to a token and accessor for related tokens.
 * <p>
 * @author Spence Koehler
 */
public interface TokenPointer {
  
  /**
   * Get the referenced token's string.
   */
  public String getString();

  /**
   * Get the referenced token's categories.
   */
  public Categories getCategories();

  /**
   * Get the (start word) position of this token within its string.
   */
  public int getPosition();

  /**
   * Get another token pointer based on shortening or lengthening the
   * referenced token's size. This revision strategy must be correlated
   * with the "next" strategy.
   */
  public TokenPointer revise();

  /**
   * Get the next token pointer that immediately follows this one.
   * The strategy for building the next token (longest, shortest, etc.)
   * must be correlated with the "revise" strategy.
   *
   * @param allowSkip  if true, allow skipping text within limits established
   *                   on construction; otherwise, don't skip.
   */
  public TokenPointer next(boolean allowSkip);

  /**
   * Get the full unnormalized input up through the referenced token, using the
   * unnormalized input string as reference if necessary.
   */
  public String getInputThrough(String unnormalizedInput);

  /**
   * Get the full unnormalized input after the referenced token, using the
   * unnormalized input string as reference if necessary.
   */
  public String getInputBeyond(String unnormalizedInput);

  /**
   * Determine whether this token pointer is guessable.
   * <p>
   * Note that calling this method can have side-effects on the instance.
   */
  public boolean isGuessable();
}
