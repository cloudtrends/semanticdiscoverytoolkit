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
package org.sd.match;


import org.sd.util.Base64;
import org.sd.util.BitUtil;

/**
 * Optimized scoring object.
 * <p>
 * @author Spence Koehler
 */
public class Osob {

  private static final OsobBitDefinition bitDefinition = OsobBitDefinition.getInstance();
  private static final int numMarkerBytes = bitDefinition.numMarkerBytes();

  private static final OsobDeserializer DESERIALIZER = new OsobDeserializer();

  private byte[] bytes;
  private Integer _conceptId = null;

  public Osob(byte[] bytes) {
    this.bytes = bytes;
  }

  public byte[] getBytes() {
    return bytes;
  }

  public String encode() {
    return Base64.encodeBytes(bytes, Base64.DONT_BREAK_LINES);
  }

  public static Osob decode(String encoded) {
    return new Osob(Base64.decode(encoded));
  }

  public int getConceptId() {
    if (_conceptId == null) {
      _conceptId = BitUtil.getInteger(bytes, 0);
    }
    return _conceptId;
  }

  public Pointer firstPointer() {
    return new Pointer(4);
  }

  public Pointer getPointer(int offset) {
//todo: cache/retrieve certain offsets?
    return new Pointer(offset);
  }

  public String getTreeString() {
    final ConceptModel conceptModel = DESERIALIZER.buildConceptModel(this);
    return conceptModel.getTree().toString();
  }

  public ConceptModel getConceptModel() {
    return DESERIALIZER.buildConceptModel(this);
  }

  public final class Pointer {
    private int bytePos;

    private NumberedWord _numberedWord = null;
    private String _word = null;
    private Integer _wordNum = null;
    private Integer _nchars = null;
    private Form.Type _formType = null;
    private Decomp.Type _termType = null;
    private Synonym.Type _synType = null;
    private Variant.Type _varType = null;
    private Word.Type _wordType = null;

    Pointer(int bytePos) {
      this.bytePos = bytePos;
    }

    public boolean hasData() {
      return bytePos < bytes.length;
    }

    public int getOffset() {
      return bytePos;
    }

    public void increment() {
      int nchars = getNumChars();
      bytePos += (numMarkerBytes + nchars + 2);  // skip wordNum byte, numChars byte, and chars

      _numberedWord = null;
      _word = null;
      _wordNum = null;
      _nchars = null;
      _formType = null;
      _termType = null;
      _synType = null;
      _varType = null;
      _wordType = null;
    }

    /**
     * If a new form starts at the pointer's current position, get its type;
     * otherwise, return null.
     */
    public Form.Type getFormType() {
      if (_formType != null) return _formType;

      Form.Type result = null;

      final Integer formValue = bitDefinition.getValue(bitDefinition.FORM, bytes, bytePos);
      if (formValue != null) {
        result = Form.getType(formValue);
      }

      _formType = result;

      return result;
    }

    /**
     * If a new term starts at the pointer's current position, get its type;
     * otherwise, return null.
     */
    public Decomp.Type getTermType() {
      if (_termType != null) return _termType;

      Decomp.Type result = null;

      final Integer termValue = bitDefinition.getValue(bitDefinition.TERM, bytes, bytePos);
      if (termValue != null) {
        result = Decomp.getType(termValue);
      }

      _termType = result;

      return result;
    }

    /**
     * If a new synonym starts at the pointer's current position, get its type;
     * otherwise, return null.
     */
    public Synonym.Type getSynonymType() {
      if (_synType != null) return _synType;

      Synonym.Type result = null;

      final Integer synonymValue = bitDefinition.getValue(bitDefinition.SYNONYM, bytes, bytePos);
      if (synonymValue != null) {
        result = Synonym.getType(synonymValue);
      }

      _synType = result;

      return result;
    }

    /**
     * If a new variant starts at the pointer's current position, get its type;
     * otherwise, return null.
     */
    public Variant.Type getVariantType() {
      if (_varType != null) return _varType;

      Variant.Type result = null;

      final Integer variantValue = bitDefinition.getValue(bitDefinition.VARIANT, bytes, bytePos);
      if (variantValue != null) {
        result = Variant.getType(variantValue);
      }

      _varType = result;

      return result;
    }

    /**
     * If a new word starts at the pointer's current position, get its type;
     * otherwise, return null.
     */
    public Word.Type getWordType() {
      if (_wordType != null) return _wordType;

      Word.Type result = null;

      final Integer wordValue = bitDefinition.getValue(bitDefinition.WORD, bytes, bytePos);
      if (wordValue != null) {
        result = Word.getType(wordValue);
      }

      _wordType = result;

      return result;
    }

    /**
     * Get the current pointer position's word number.
     */
    public int getWordNum() {
      if (_wordNum != null) return _wordNum;

      int result = bytes[bytePos + numMarkerBytes];

      _wordNum = result;

      return result;
    }

    /**
     * Get the current pointer position's word's char count.
     */
    public int getNumChars() {
      if (_nchars != null) return _nchars;

      int result = bytes[bytePos + numMarkerBytes + 1];

      _nchars = result;

      return result;
    }

    /**
     * Get the word at the current pointer position.
     */
    public String getWord() {
      if (_word != null) return _word;

      final int nchars = getNumChars();
      StringBuilder builder = new StringBuilder();

      int curPos = bytePos + numMarkerBytes + 2;
      for (int i = 0; i < nchars; ++i) {
        builder.append((char)bytes[curPos++]);
      }

      final String result = builder.toString();
      _word = result;
      return result;
    }

    /**
     * Get the numbered word at the current pointer position.
     */
    public NumberedWord getNumberedWord() {
      if (_numberedWord != null) return _numberedWord;

      NumberedWord result = null;

      final Word.Type wordType = getWordType();
      final int wordNum = getWordNum();
      final String word = getWord();
      final Form.Type formType = getFormType();
      final Decomp.Type termType = getTermType();
      final Synonym.Type synType = getSynonymType();
      final Variant.Type varType = getVariantType();

      result = new NumberedWord(wordNum, new TypedWord(word, wordType), formType, termType, synType, varType);

      _numberedWord = result;

      return result;
    }

    /**
     * Compare the current word's chars with the given chars.
     */
    public int compareChars(char[] chars) {
      int result = 0;
      final int nchars = getNumChars();

      int curPos = bytePos + numMarkerBytes + 2;
      for (int i = 0; result == 0 && i < nchars && i < chars.length; ++i) {
        result = (bytes[curPos++] - chars[i]);
      }

      if (result == 0) {
        result = (nchars - chars.length);
      }

      return result;
    }

    byte[] getBytes() {
      return bytes;
    }

    /**
     * Compare the current word's chars with the other pointer's current word's chars.
     */
    public int compareChars(Pointer other) {
      int result = 0;
      final int nchars = getNumChars();
      final int onchars = other.getNumChars();
      final byte[] obytes = other.getBytes();

      int curPos = bytePos + numMarkerBytes + 2;
      int ocurPos = other.bytePos + numMarkerBytes + 2;
      for (int i = 0; result == 0 && i < nchars && i < onchars; ++i) {
        result = (bytes[curPos++] - obytes[ocurPos++]);
      }

      if (result == 0) {
        result = (nchars - onchars);
      }

      return result;
    }
  }
}
