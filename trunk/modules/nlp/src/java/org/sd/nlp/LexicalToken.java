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


import org.sd.fsm.impl.AbstractToken;

/**
 * A lexical token has a lexical entry and serves as a factory for other lexical tokens.
 * <p>
 * @author Spence Koehler
 */
public class LexicalToken extends AbstractToken<LexicalEntry> {

  public LexicalToken(LexicalEntry lexicalEntry) {
    super(lexicalEntry, null);
  }
  
  /**
   * Get this lexical token's lexical entry.
   */
  public LexicalEntry getLexicalEntry() {
    return getToken();
  }

  /**
   * Get this token's string.
   */
  public String getString() {
    return getToken().getPointer().getString();
  }

  public LexicalToken revise() {
    LexicalToken result = null;
    final LexicalEntry entry = getToken().revise();
    if (entry != null) {
      result = new LexicalToken(entry);
    }
    return result;
  }

  public LexicalToken next() {
    LexicalToken result = null;
    final LexicalEntry entry = getToken().next(false);
    if (entry != null) {
      result = new LexicalToken(entry);
    }
    return result;
  }

  /**
   * Determine whether this token matches the given token (assumed to be an extended grammar token).
   * <p>
   * Note: this is used by the FSM.
   */
  public boolean matches(AbstractToken other) {
    boolean result = false;
    if (other != null && other instanceof ExtendedGrammarToken) {
      final ExtendedGrammarToken grammarToken = (ExtendedGrammarToken)other;
      final String term = grammarToken.getToken();
      final boolean negate = grammarToken.isNegated();

      if (grammarToken.isLiteral()) {
        final String string = getToken().getString();
        result = negate ? !term.equalsIgnoreCase(string) : term.equalsIgnoreCase(string);
      }
      else {
        final Category type = grammarToken.getCategory();
        final Categories categories = this.getToken().getDefinition();
        if (categories != null) {
          result = negate ? !categories.hasType(type) : categories.hasType(type);
        }
        if (!result && this.isGuessable()) {  // see if we can guess!
          if (negate) {
            if (!grammarToken.isGuessable()) {
              getToken().addNotDefinition(type);
              result = true;
            }
          }
          else {
            if (grammarToken.isGuessable()) {
              getToken().addDefinition(type);  // remember that we're guessing this definition for this entry
              //todo: communicate this guess back to the lexicon???
              result = true;
            }
          }
        }
      }
    }

    return result;
  }

  /**
   * Get this token's common (across all token classes) keys.
   */
  public String[] getCommonKeys() {
    return getLexicalEntry().getKeys();
  }

  /**
   * Determine whether this token can be "guessed" for undefined terms.
   */
  public boolean isGuessable() {
    return getToken().isGuessable();
  }

}
