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
package org.sd.wn;


import java.util.HashSet;
import java.util.Set;

/**
 * Base implementation of the pointer filter interface.
 * <p>
 * @author Spence Koehler
 */
public class BasePointerFilter implements PointerFilter {
  
  private boolean rejectReverseRelations;
  private Integer maxDepth;
  private Set<PointerSymbol> acceptPointers;
  private Set<PointerSymbol> rejectPointers;

  /**
   * Default constructor rejects reverse relations for expansion.
   */
  public BasePointerFilter() {
    this(true, null, null, null);
  }

  /**
   * Construct with the given parameters.
   * <p>
   * Note that even if the rejectReverseRelations parameter is false, the direct source leading to a word sense
   * will not be in the word sense's expansion set.
   */
  public BasePointerFilter(boolean rejectReverseRelations, Integer maxDepth,
                           PointerSymbol[] acceptPointers, PointerSymbol[] rejectPointers) {
    this.rejectReverseRelations = rejectReverseRelations;
    this.maxDepth = null;
    this.acceptPointers = null;
    this.rejectPointers = null;

    if (maxDepth != null) {
      setMaxDepth(maxDepth);
    }
    if (acceptPointers != null) {
      for (PointerSymbol acceptPointer : acceptPointers) {
        addAcceptPointer(acceptPointer);
      }
    }
    if (rejectPointers != null) {
      for (PointerSymbol rejectPointer : rejectPointers) {
        addRejectPointer(rejectPointer);
      }
    }
  }

  /**
   * Set whether reverse relations should be rejected.
   * <p>
   * Note that even if this is false, the direct source leading to a word sense
   * will not be in the word sense's expansion set.
   */
  protected final void setRejectReverseRelations(boolean rejectReverseRelations) {
    this.rejectReverseRelations = rejectReverseRelations;
  }

  /**
   * Determine whether reverse relations will be rejected.
   */
  protected final boolean rejectReverseRelations() {
    return rejectReverseRelations;
  }

  /**
   * Set the maxDepth for expansion.
   * <p>
   * If null, negative, or zero, then there will be no limit.
   */
  protected final void setMaxDepth(Integer maxDepth) {
    if (maxDepth == null || maxDepth <= 0) {
      this.maxDepth = null;
    }
    else {
      this.maxDepth = maxDepth;
    }
  }

  /**
   * Get the maxDepth for expansion.
   * <p>
   * Null indicates no limit.
   */
  protected final Integer getMaxDepth() {
    return maxDepth;
  }

  /**
   * Add a type of pointer to accept. If there are no pointers added here, all
   * are accepted. Once there are pointers added here, only the pointers that
   * have been added will be accepted.
   */
  protected final void addAcceptPointer(PointerSymbol acceptPointer) {
    if (acceptPointers == null) acceptPointers = new HashSet<PointerSymbol>();
    acceptPointers.add(acceptPointer);
  }

  /**
   * Add a type of pointer to reject. If there are no pointers added here, all
   * are accepted. Once there are pointers added here, only the pointers that
   * have not been added will be accepted.
   */
  protected final void addRejectPointer(PointerSymbol rejectPointer) {
    if (rejectPointers == null) rejectPointers = new HashSet<PointerSymbol>();
    rejectPointers.add(rejectPointer);
  }

  /**
   * Get the accept pointers, possibly null.
   */
  protected final Set<PointerSymbol> getAcceptPointers() {
    return acceptPointers;
  }

  /**
   * Get the reject pointers, possibly null.
   */
  protected final Set<PointerSymbol> getRejectPointers() {
    return rejectPointers;
  }

  /**
   * Accept a relation for expansion from the source unless rejecting reverse
   * relations and the proposed pointer is a reverse relation from what has
   * been expanded in any source wrapper.
   * <p>
   * Extending classes should call this (super) method and check for other
   * relevant constraints.
   */
  public boolean accept(WordNetFile.Pointer pointer, WordSenseWrapper source) {

    if (maxDepth != null && source.depth() >= maxDepth) return false;

    boolean result = true;

    if (rejectReverseRelations) {
      result = !isReverse(pointer, source);
    }
    if (result) {
      if (!acceptPointer(pointer, source)) {
        result = false;
      }
      else if (!acceptPOS(pointer.partOfSpeech, source)) {
        result = false;
      }
    }
    

    return result;
  }

  /**
   * Determine whether to accept the given expansion from the source word sense.
   */
  public boolean accept(WordSenseWrapper expanded, WordSenseWrapper source) {
    boolean result = true;

    final WordNetFile.Entry expandedEntry = expanded.getWordSense().getFileEntry();

    if (!acceptLexName(expandedEntry.getLexName(), source)) {
      result = false;
    }

    return result;
  }

  /**
   * Determine whether the pointer should be accepted for expansion.
   * <p>
   * Note that this is applied after checking for reverse relations if applicable.
   * <p>
   * Extending classes can override this for desired behavior. If this method is
   * overridden then the default accept/reject pointer behavior will not be
   * automatically applied.
   */
  protected boolean acceptPointer(WordNetFile.Pointer pointer, WordSenseWrapper source) {
    boolean result = true;

    if (acceptPointers != null) {
      result = acceptPointers.contains(pointer.pointerSymbol);
    }

    if (result && rejectPointers != null) {
      result = !rejectPointers.contains(pointer.pointerSymbol);
    }

    return result;
  }

  /**
   * Determine whether the part of speech should be accepted for expansion.
   * <p>
   * Extending classes can override this for desired behavior.
   */
  protected boolean acceptPOS(POS partOfSpeech, WordSenseWrapper source) {
    return true;
  }

  /**
   * Determine whether the lex name should be accepted for expansion.
   * <p>
   * Extending classes can override this for desired behavior.
   */
  public boolean acceptLexName(LexName lexName, WordSenseWrapper source) {
    return true;
  }

  /**
   * Determine whether the given pointer is the reverse of any pointer
   * previously traversed.
   */
  private final boolean isReverse(WordNetFile.Pointer pointer, WordSenseWrapper source) {
    final PointerSymbol reverseSymbol = pointer.pointerSymbol.reflect();

    if (reverseSymbol != null) {
      for (WordSenseWrapper prev = source; prev != null; prev = prev.getSource()) {
        if (reverseSymbol == prev.getRelationFromSource()) {
          return true;
        }
      }
    }

    return false;
  }
}
