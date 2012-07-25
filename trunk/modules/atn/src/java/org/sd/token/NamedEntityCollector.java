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
package org.sd.token;


import java.util.ArrayList;
import java.util.List;

/**
 * Collector for inner segmentation of named entity words.
 * <p>
 * @author Spence Koehler
 */
public class NamedEntityCollector {
  
  private List<NamedEntityGroup> groups;
  private NamedEntityGroup curGroup;

  public NamedEntityCollector(SegmentPointer firstWord) {
    this(firstWord, false);
  }

  public NamedEntityCollector(SegmentPointer firstWord, boolean isBlock) {
    this.groups = new ArrayList<NamedEntityGroup>();
    this.curGroup = new NamedEntityGroup(firstWord, isBlock);
    groups.add(curGroup);
  }

  public void add(SegmentPointer nextWord) {
    add(nextWord, false);
  }

  public void add(SegmentPointer nextWord, boolean isBlock) {
    if (!curGroup.add(nextWord, isBlock)) {
      curGroup = new NamedEntityGroup(nextWord, isBlock);
      groups.add(curGroup);
    }
  }

  public void setInnerSegments(SegmentPointer source) {
    for (NamedEntityGroup group : groups) {
      source.addInnerSegment(group.getStartPtr(), group.getEndPtr());
    }
  }


  private static final class NamedEntityGroup {
    private List<SegmentPointer> words;
    private SegmentPointer lastWord;
    private SegmentPointer lastNonBlockWord;
    private int numNonBlockWords;
    private boolean lastWasBlock;
    private boolean isClosed;
    private boolean waitingForEndSingleQuote;

    public NamedEntityGroup(SegmentPointer firstWord, boolean isBlock) {
      this.words = new ArrayList<SegmentPointer>();
      this.words.add(firstWord);
      this.lastWord = firstWord;
      this.lastNonBlockWord = isBlock ? null : firstWord;
      this.numNonBlockWords = isBlock ? 0 : 1;
      this.lastWasBlock = isBlock;

      // if group consists of a single block, then don't add any more to this group
      // if group's last word ends with ':' or ';', then don't add any more to this group
      this.isClosed = isBlock || hasDefinitiveEnd(firstWord);

      this.waitingForEndSingleQuote = isBlock ? false : hasStartSingleQuote(firstWord) && !hasEndSingleQuote(firstWord);
    }

    public int getStartPtr() {
      return words.get(0).getTextStart();
    }

    public int getEndPtr() {
      return lastWord.getTextEnd();
    }

    public boolean add(SegmentPointer nextWord, boolean isBlock) {
      if (isClosed) return false;

      boolean result = true;
      boolean nextHasEndSingleQuote = false;

      if (!isBlock) {
        nextHasEndSingleQuote = hasEndSingleQuote(nextWord);

        // use any symbols at the beginning of a word as a break
        if (result && nextWord.getWordCharacteristics().hasStartDelims()) {
          result = false;
        }

        // if group's last word has possessive, then don't add next to this group
        if (result && hasApparentPossessive(nextWord)) {
          if (!(nextHasEndSingleQuote && waitingForEndSingleQuote)) {
            // this does look like a possessive
            result = false;
          }
        }

        if (result && words.size() > 1) {
          // if group's last word ends with ',' and group has more than one word,
          // then don't add to this group
          if (hasPotentialEnd(lastWord)) {
            result = false;
          }
        }

        if (result && numNonBlockWords > 1) {
          // if changing from Caps/Capitalized to Capitalized/Caps and group has more than one word,
          // then don't add to this group
          if (hasCapsChange(lastNonBlockWord, nextWord)) {
            result = false;
          }
        }
      }

      if (result) {
        words.add(nextWord);
        this.lastWord = nextWord;
        this.lastWasBlock = isBlock;
        this.isClosed = hasDefinitiveEnd(nextWord);
        if (!isBlock) {
          this.lastNonBlockWord = nextWord;
          this.numNonBlockWords++;
          if (this.waitingForEndSingleQuote) {
            if (nextHasEndSingleQuote) {
              this.waitingForEndSingleQuote = false;
            }
          }
          else {
            final boolean nextHasStartSingleQuote = hasStartSingleQuote(nextWord);
            if (nextHasStartSingleQuote && !nextHasEndSingleQuote) {
              this.waitingForEndSingleQuote = true;
            }
          }
        }
      }

      return result;
    }

    private final boolean hasStartSingleQuote(SegmentPointer wordPtr) {
      boolean result = false;
      final WordCharacteristics wc = wordPtr.getWordCharacteristics();
      if (wc.hasStartDelims()) {
        result = wc.getStartDelims().indexOf('\'') >= 0;
      }
      return result;
    }

    private final boolean hasEndSingleQuote(SegmentPointer wordPtr) {
      boolean result = false;
      final WordCharacteristics wc = wordPtr.getWordCharacteristics();
      if (wc.hasEndDelims()) {
        result = wc.getEndDelims().indexOf('\'') >= 0;
      }
      return result;
    }

    private final boolean hasApparentPossessive(SegmentPointer wordPtr) {
      boolean result = false;

      final WordCharacteristics wc = wordPtr.getWordCharacteristics();
      if (wc.hasOther() && wc.len() > 2) {
        final String text = wc.getWord();
        final char lastC = text.charAt(text.length() - 1);
        final char penC = text.charAt(text.length() - 2);

        result = (penC == '\'' && lastC == 's') || (lastC == '\'' && penC == 's');
      }

      return result;
    }

    private final boolean hasDefinitiveEnd(SegmentPointer wordPtr) {
      // if group's last word ends with ':' or ';', then don't add any more to this group
      boolean result = false;
      final WordCharacteristics wc = wordPtr.getWordCharacteristics();
      if (wc.hasEndDelims()) {
        final String endDelims = wc.getEndDelims();
        final int len = endDelims.length();
        for (int i = 0; i < len; ++i) {
          final char c = endDelims.charAt(i);
          if (c == ':' || c == ';') {
            result = true;
            break;
          }
        }
      }
      return result;
    }

    private final boolean hasPotentialEnd(SegmentPointer wordPtr) {
      // true if group's last word ends with ',' and group has more than one word,
      boolean result = false;
      final WordCharacteristics wc = wordPtr.getWordCharacteristics();
      if (wc.hasEndDelims()) {
        result = wc.getEndDelims().indexOf(',') >= 0;
      }
      return result;
    }

    private final boolean hasCapsChange(SegmentPointer priorWord, SegmentPointer nextWord) {
      final WordCharacteristics priorWC = priorWord.getWordCharacteristics();
      final WordCharacteristics nextWC = nextWord.getWordCharacteristics();

      if (priorWC.getNumLetters() == 1 || nextWC.getNumLetters() == 1) return false;

      final boolean priorIsAllCaps = priorWC.isAllCaps(true);
      final boolean nextIsAllCaps = nextWC.isAllCaps(true);

      return (priorIsAllCaps && !nextIsAllCaps) || (!priorIsAllCaps && nextIsAllCaps);
    }
  }
}
