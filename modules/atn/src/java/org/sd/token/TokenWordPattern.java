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


import java.util.HashMap;
import java.util.Map;

/**
 * Generator and container for the token word pattern from a tokenizer.
 * <p>
 * @author Spence Koehler
 */
public class TokenWordPattern {

  // Unless the revision strategy is LO, the pattern is taken from the shortest
  // tokens sequence; otherwise the pattern is taken from the longest tokens
  // sequence.

  // Pattern Key:
  //   l = (all) lower (2 or more)
  //   c = capitalized
  //   C = all caps (2 or more)
  //   i = single lower letter
  //   I = single upper letter
  //   m = mixed case starting with lower
  //   M = mixed case starting with upper
  //   n = number (digits only)
  //   N = number (digits with letters or symbols)
  //   s = special (e.g., composed of delimiters)

  // Squash flags
  //   W = squash whitespace
  //   Q = squash quoted blocks
  //   P = squash parenthesized blocks


  private Tokenizer tokenizer;
  private StringBuilder pattern;
  private PatternKey patternKey;

  public TokenWordPattern(Tokenizer tokenizer) {
    this(tokenizer, null);
  }

  public TokenWordPattern(Tokenizer tokenizer, PatternKey patternKey) {
    this.tokenizer = tokenizer;
    this.patternKey = patternKey;
    this.pattern = new StringBuilder();
    buildPattern(tokenizer);
  }
  
  public TokenWordPattern(Tokenizer tokenizer, String keyLabels, String labelChars) {
    this(tokenizer, keyLabels != null && labelChars != null ? new PatternKey(keyLabels, labelChars) : null);
  }

  public Tokenizer getTokenizer() {
    return tokenizer;
  }

  public String getPattern() {
    return getPattern(null);
  }

  public String getPattern(String squashFlags) {
    return squash(pattern.toString(), squashFlags);
  }

  public static String squash(String input, String squashFlags) {
    String result = input;

    boolean lastWasSpace = false;
    if (squashFlags != null) {
      final SquashFlags flags = new SquashFlags(squashFlags);

      final StringBuilder builder = new StringBuilder();
      final int inputLen = input.length();

      BlockRecognizer blockRecognizer = null;

      if (flags.squashBlocks()) {
        blockRecognizer = new BlockRecognizer(new char[][]{});
        if (flags.squashQuotes()) {
          blockRecognizer.addBlockChars('"', '"');
          blockRecognizer.addBlockChars('\'', '\'');
        }
        if (flags.squashParens()) {
          blockRecognizer.addBlockChars('(', ')');
        }
      }

      //TODO: we need to ignore "block start/end" chars that aren't at the
      //      ends of words. For example, internal apostrophe's are not
      //      begin/end quotes, but they will be recognized as such in the
      //      current implementation. Use Word-Based SegmentPointerIterator here?
      //      If so, move SegmentPointer classes to token package.

      for (int cPos = 0; cPos < inputLen; ++cPos) {
        final char c = input.charAt(cPos);
        boolean addC = true;

        if (addC && flags.squashBlocks()) {
          final boolean updated = blockRecognizer.updateStack(c);
          if (!blockRecognizer.stackIsEmpty() || updated) {
            addC = false;
          }
        }

        if (addC && flags.squashWhite() && Character.isWhitespace(c)) {
          addC = false;
        }

        if (addC) {
          if (!(c == ' ' && lastWasSpace)) {
            builder.append(c);
            lastWasSpace = (c == ' ');
          }
        }
      }

      result = builder.toString();
    }

    return result;
  }


  private final void buildPattern(Tokenizer tokenizer) {
    final Token firstToken = tokenizer.getSmallestToken(0);
    if (firstToken == null) return;

    pattern.append(firstToken.getPreDelim());

    for (Token token = firstToken; token != null; token = token.getNextSmallestToken()) {
      final TokenPattern tokenPattern = new TokenPattern(token, patternKey);
      pattern.append(tokenPattern.getKey()).append(token.getPostDelim());
    }
  }


  private static final class TokenPattern {
    private String key;

    public TokenPattern(Token token, PatternKey patternKey) {
      this.key = buildKey(token.getSoftWords(), patternKey);
    }

    public String getKey() {
      return key;
    }

    private final String buildKey(String[] pieces, PatternKey patternKey) {
      final StringBuilder result = new StringBuilder();

      for (String piece : pieces) {
        if (result.length() > 0) result.append(' ');
        result.append(buildKey(piece, patternKey));
      }

      return result.toString();
    }

    private final char buildKey(String text, PatternKey patternKey) {
      final WordCharacteristics wc = new WordCharacteristics(text);
      final KeyLabel keyLabel = wc.getKeyLabel();
      final char result = getChar(patternKey, keyLabel);
      return result;
    }
  }

  private static final class SquashFlags {
    private boolean squashWhite;
    private boolean squashQuotes;
    private boolean squashParens;
    private boolean squashBlocks;

    public SquashFlags(String squashFlags) {
      this.squashWhite = false;
      this.squashQuotes = false;
      this.squashParens = false;
      this.squashBlocks = false;

      final int numSquashFlags = squashFlags.length();

      for (int cPos = 0; cPos < numSquashFlags; ++cPos) {
        final char flag = squashFlags.charAt(cPos);
        switch (flag) {
          case 'W' :
          case 'w' :
            squashWhite = true;
            break;
          case 'Q' :
          case 'q' :
            squashQuotes = true;
            squashBlocks = true;
            break;
          case 'P' :
          case 'p' :
            squashParens = true;
            squashBlocks = true;
            break;
        }
      }
    }

    public boolean squashWhite() {
      return squashWhite;
    }

    public boolean squashQuotes() {
      return squashQuotes;
    }

    public boolean squashParens() {
      return squashParens;
    }

    public boolean squashBlocks() {
      return squashBlocks;
    }
  }


  private static final char getChar(PatternKey patternKey, KeyLabel keyLabel) {
    return patternKey == null ? keyLabel.getDefaultChar() : patternKey.getLabel(keyLabel);
  }

  public static final Map<Character, KeyLabel> defaultKeyLabel = new HashMap<Character, KeyLabel>();
  static {
    defaultKeyLabel.put(KeyLabel.AllLower.getDefaultChar(), KeyLabel.AllLower);
    defaultKeyLabel.put(KeyLabel.Capitalized.getDefaultChar(), KeyLabel.Capitalized);
    defaultKeyLabel.put(KeyLabel.AllCaps.getDefaultChar(), KeyLabel.AllCaps);
    defaultKeyLabel.put(KeyLabel.SingleLower.getDefaultChar(), KeyLabel.SingleLower);
    defaultKeyLabel.put(KeyLabel.SingleUpper.getDefaultChar(), KeyLabel.SingleUpper);
    defaultKeyLabel.put(KeyLabel.LowerMixed.getDefaultChar(), KeyLabel.LowerMixed);
    defaultKeyLabel.put(KeyLabel.UpperMixed.getDefaultChar(), KeyLabel.UpperMixed);
    defaultKeyLabel.put(KeyLabel.Number.getDefaultChar(), KeyLabel.Number);
    defaultKeyLabel.put(KeyLabel.MixedNumber.getDefaultChar(), KeyLabel.MixedNumber);
    defaultKeyLabel.put(KeyLabel.Special.getDefaultChar(), KeyLabel.Special);
  }

  public static final class PatternKey {

    private Map<KeyLabel, Character> overrides;

    public PatternKey() {
      this.overrides = null;
    }

    public PatternKey(KeyLabel[] keyLabels, char[] chars) {
      this.overrides = new HashMap<KeyLabel, Character>();
      for (int i = 0; i < keyLabels.length; ++i) {
        overrides.put(keyLabels[i], chars[i]);
      }
    }

    public PatternKey(String keyLabels, String chars) {
      this.overrides = new HashMap<KeyLabel, Character>();
      final int len = keyLabels.length();
      for (int i = 0; i < len; ++i) {
        final KeyLabel keyLabel = defaultKeyLabel.get(keyLabels.charAt(i));
        if (keyLabel == null) throw new IllegalArgumentException("Bad default keyLabel '" + keyLabels.charAt(i) + "'");
        overrides.put(keyLabel, chars.charAt(i));
      }
    }

    public char getLabel(KeyLabel keyLabel) {
      char result = keyLabel.getDefaultChar();

      if (overrides != null && overrides.containsKey(keyLabel)) {
        Character override = overrides.get(keyLabel);
        if (override != null) {
          result = override;
        }
      }

      return result;
    }

    public void setLabel(KeyLabel keyLabel, char label) {
      if (overrides == null) overrides = new HashMap<KeyLabel, Character>();
      overrides.put(keyLabel, label);
    }
  }


  public static void main(String[] args) {
    // arg0: input
    // arg1: (optional) keyLabels
    // arg2: (optional) labelChars
    // arg3: (optional) squashFlags

    final String input = args[0];
    final String keyLabels = (args.length > 1) ? args[1] : null;
    final String labelChars = (args.length > 2) ? args[2] : null;
    final String squashFlags = (args.length > 3) ? args[3] : null;

    final StandardTokenizerOptions options = new StandardTokenizerOptions();
    final Tokenizer t = StandardTokenizerFactory.getTokenizer(input);
    final TokenWordPattern wordPattern = new TokenWordPattern(t, keyLabels, labelChars);
    System.out.println(input + ":\t" + wordPattern.getPattern(squashFlags));
  }
}
