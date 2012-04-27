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
package org.sd.atn;

import org.sd.token.Token;
import org.sd.util.Usage;


/**
 * Interface for filtering tokens.
 * <p>
 * @author Spence Koehler
 */
@Usage(notes = "Interface for filtering tokens.")
public interface TokenFilter {
  
  /**
   * Check the token for an appropriate action.
   *
   * If ACCEPT, then continue normally with processing;
   * If IGNORE, then skip the token as if it were not in the input stream;
   * If HALT, then the token will be considered to be beyond the input boundaries.
   */
  public TokenFilterResult checkToken(Token token, boolean isRevision, Token prevToken, AtnState curState);

}
