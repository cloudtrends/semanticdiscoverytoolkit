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
package org.sd.fsm.impl;


import org.sd.fsm.State;
import org.sd.fsm.Token;
import org.sd.fsm.Validator;

import java.util.Iterator;
import java.util.List;

/**
 * A pattern validator.
 * <p>
 * @author Spence Koehler
 */
public class PatternValidator extends Validator {

  public PatternValidator(GrammarImpl grammar) {
    super(new FSMImpl(grammar));
  }
  
  public List<State> getValidStates(final String[] tokens) {
    return
      validate(
        new Iterator<Token>() {
          int i = 0;
          public boolean hasNext() {
            return i < tokens.length;
          }
          public Token next() {
            return new StringToken(tokens[i++]);
          }
          public void remove() {
            throw new UnsupportedOperationException("not implemented.");
          }
        },
        true);
  }
}
