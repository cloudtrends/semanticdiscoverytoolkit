/*
    Copyright 2013 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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
package org.sd.atn;


import java.util.List;
import org.sd.token.TokenInfo;
import org.sd.util.InputContext;

/**
 * An InputContext used specifically for parsing.
 * <p>
 * Generally, a wrapper around an InputContext, that optionally supplies
 * tokenizer data for input lines.
 *
 * @author Spence Koehler
 */
public interface ParseInputContext extends InputContext {
  
  /**
   * Get the tokenInfo instances for building an AtnParseBasedTokenizer for
   * this input, if available.
   *
   * @return the tokenInfo instances or null.
   */
  public List<TokenInfo> getTokenInfos();

  /**
   * Get the spans of tokens that are designated as hard breaks.
   */
  public List<TokenInfo> getHardBreaks();
}
