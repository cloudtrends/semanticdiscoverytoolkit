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
package org.sd.xml;


import org.sd.util.MappedString;
import org.sd.util.StringUtil;

import java.util.HashMap;
//import java.util.HashSet;
import java.util.Map;
//import java.util.Set;

/**
 * Utility class to convert entity names to characters.
 * <p>
 * @author Spence Koehler
 */
public class EntityConverter {

  private static final Map<String, Integer> entity2char = new HashMap<String, Integer>();
  private static final Map<Integer, String> char2entity = new HashMap<Integer, String>();
  private static final Map<Integer, Integer> ascii2utf8 = new HashMap<Integer, Integer>();
//  private static final Set<Integer> possibleAscii = new HashSet<Integer>();

//todo: add unicode equivalents where possible and where differ from code point.
  private static Entity[] entities = {
//    new Entity("vb",     124,  124),   //(int)'|'  html: "&#124;"  url: "%7C"
    new Entity("nbsp",   160,   32),   //(int)' '
    new Entity("iexcl",  161,  161),   //(int)'¡'
    new Entity("cent",   162,  162),   //(int)'¢'
    new Entity("pound",  163,  163),   //(int)'£'
    new Entity("curren", 164,  164),   //(int)'¤'
    new Entity("yen",    165,  165),   //(int)'¥'
    new Entity("brvbar", 166,  166),   //(int)'¦'
    new Entity("sect",   167,  167),   //(int)'§'
    new Entity("uml",    168,  168),   //(int)'¨'
    new Entity("copy",   169,  169),   //(int)'©'
    new Entity("ordf",   170,  170),   //(int)'ª'
    new Entity("laquo",  171,   34),   //(int)'"'
    new Entity("not",    172,  172),   //(int)'¬'
    new Entity("shy",    173,  173),   //(int)'­'
    new Entity("reg",    174,  174),   //(int)'®'
    new Entity("macr",   175,  175),   //(int)'¯'
    new Entity("deg",    176,  176),   //(int)'°'
    new Entity("plusmn", 177,  177),   //(int)'±'
    new Entity("sup2",   178,  178),   //(int)'²'
    new Entity("sup3",   179,  179),   //(int)'³'
    new Entity("acute",  180,  180),   //(int)'´'
    new Entity("micro",  181,  181),   //(int)'µ'
    new Entity("para",   182,  182),   //(int)'¶'
    new Entity("middot", 183,  183),   //(int)'·'
    new Entity("cedil",  184,  184),   //(int)'¸'
    new Entity("sup1",   185,  185),   //(int)'¹'
    new Entity("ordm",   186,  186),   //(int)'º'
    new Entity("raquo",  187,   34),   //(int)'"'
    new Entity("frac14", 188,  188),   //(int)'¼'
    new Entity("frac12", 189,  189),   //(int)'½'
    new Entity("frac34", 190,  190),   //(int)'¾',
    new Entity("iquest", 191,  191),   //(int)'¿'
    new Entity("Agrave", 192,  192),   //(int)'À'
    new Entity("Aacute", 193,  193),   //(int)'Á'
    new Entity("Acirc",  194,  194),   //(int)'Â'
    new Entity("Atilde", 195,  195),   //(int)'Ã'
    new Entity("Auml",   196,  196),   //(int)'Ä'
    new Entity("Aring",  197,  197),   //(int)'Å'
    new Entity("AElig",  198,  198),   //(int)'Æ'
    new Entity("Ccedil", 199,  199),   //(int)'Ç'
    new Entity("Egrave", 200,  200),   //(int)'È'
    new Entity("Eacute", 201,  201),   //(int)'É'
    new Entity("Ecirc",  202,  202),   //(int)'Ê'
    new Entity("Euml",   203,  203),   //(int)'Ë'
    new Entity("Igrave", 204,  204),   //(int)'Ì'
    new Entity("Iacute", 205,  205),   //(int)'Í'
    new Entity("Icirc",  206,  206),   //(int)'Î'
    new Entity("Iuml",   207,  207),   //(int)'Ï'
    new Entity("ETH",    208,  208),   //(int)'Ð'
    new Entity("Ntilde", 209,  209),   //(int)'Ñ'
    new Entity("Ograve", 210,  210),   //(int)'Ò'
    new Entity("Oacute", 211,  211),   //(int)'Ó'
    new Entity("Ocirc",  212,  212),   //(int)'Ô'
    new Entity("Otilde", 213,  213),   //(int)'Õ'
    new Entity("Ouml",   214,  214),   //(int)'Ö'
    new Entity("times",  215,  215),   //(int)'×'
    new Entity("Oslash", 216,  216),   //(int)'Ø'
    new Entity("Ugrave", 217,  217),   //(int)'Ù'
    new Entity("Uacute", 218,  218),   //(int)'Ú'
    new Entity("Ucirc",  219,  219),   //(int)'Û'
    new Entity("Uuml",   220,  220),   //(int)'Ü'
    new Entity("Yacute", 221,  221),   //(int)'Ý'
    new Entity("THORN",  222,  222),   //(int)'Þ'
    new Entity("szlig",  223,  223),   //(int)'ß'
    new Entity("agrave", 224,  224),   //(int)'à'
    new Entity("aacute", 225,  225),   //(int)'á'
    new Entity("acirc",  226,  226),   //(int)'â'
    new Entity("atilde", 227,  227),   //(int)'ã'
    new Entity("auml",   228,  228),   //(int)'ä'
    new Entity("aring",  229,  229),   //(int)'å'
    new Entity("aelig",  230,  230),   //(int)'æ'
    new Entity("ccedil", 231,  231),   //(int)'ç'
    new Entity("egrave", 232,  232),   //(int)'è'
    new Entity("eacute", 233,  233),   //(int)'é'
    new Entity("ecirc",  234,  234),   //(int)'ê'
    new Entity("euml",   235,  235),   //(int)'ë'
    new Entity("igrave", 236,  236),   //(int)'ì'
    new Entity("iacute", 237,  237),   //(int)'í'
    new Entity("icirc",  238,  238),   //(int)'î'
    new Entity("iuml",   239,  239),   //(int)'ï'
    new Entity("eth",    240,  240),   //(int)'ð'
    new Entity("ntilde", 241,  241),   //(int)'ñ'
    new Entity("ograve", 242,  242),   //(int)'ò'
    new Entity("oacute", 243,  243),   //(int)'ó'
    new Entity("ocirc",  244,  244),   //(int)'ô'
    new Entity("otilde", 245,  245),   //(int)'õ'
    new Entity("ouml",   246,  246),   //(int)'ö'
    new Entity("divide", 247,  247),   //(int)'÷'
    new Entity("oslash", 248,  248),   //(int)'ø'
    new Entity("ugrave", 249,  249),   //(int)'ù'
    new Entity("uacute", 250,  250),   //(int)'ú'
    new Entity("ucirc",  251,  251),   //(int)'û'
    new Entity("uuml",   252,  252),   //(int)'ü'
    new Entity("yacute", 253,  253),   //(int)'ý'
    new Entity("thorn",  254,  254),   //(int)'þ'
    new Entity("yuml",   255,  255),   //(int)'ÿ'
    new Entity("fnof",     402,  402),   //(int)'ƒ'  // is this right?
    new Entity("Alpha",    913, null),
    new Entity("Beta",     914, null),
    new Entity("Gamma",    915, null),
    new Entity("Delta",    916, null),
    new Entity("Epsilon",  917, null),
    new Entity("Zeta",     918, null),
    new Entity("Eta",      919, null),
    new Entity("Theta",    920, null),
    new Entity("Iota",     921, null),
    new Entity("Kappa",    922, null),
    new Entity("Lambda",   923, null),
    new Entity("Mu",       924, null),
    new Entity("Nu",       925, null),
    new Entity("Xi",       926, null),
    new Entity("Omicron",  927, null),
    new Entity("Pi",       928, null),
    new Entity("Rho",      929, null),
    new Entity("Sigma",    931, null),
    new Entity("Tau",      932, null),
    new Entity("Upsilon",  933, null),
    new Entity("Phi",      934, null),
    new Entity("Chi",      935, null),
    new Entity("Psi",      936, null),
    new Entity("Omega",    937, null),
    new Entity("alpha",    945, null),
    new Entity("beta",     946, null),
    new Entity("gamma",    947, null),
    new Entity("delta",    948, null),
    new Entity("epsilon",  949, null),
    new Entity("zeta",     950, null),
    new Entity("eta",      951, null),
    new Entity("theta",    952, null),
    new Entity("iota",     953, null),
    new Entity("kappa",    954, null),
    new Entity("lambda",   955, null),
    new Entity("mu",       956, null),
    new Entity("nu",       957, null),
    new Entity("xi",       958, null),
    new Entity("omicron",  959, null),
    new Entity("pi",       960, null),
    new Entity("rho",      961, null),
    new Entity("sigmaf",   962, null),
    new Entity("sigma",    963, null),
    new Entity("tau",      964, null),
    new Entity("upsilon",  965, null),
    new Entity("phi",      966, null),
    new Entity("chi",      967, null),
    new Entity("psi",      968, null),
    new Entity("omega",    969, null),
    new Entity("thetasym", 977, null),
    new Entity("upsih",    978, null),
    new Entity("piv",      982, null),
    new Entity("bull",     8226,  8226),  //(int)'•'
    new Entity("hellip",   8230, null),
    new Entity("prime",    8242, null),
    new Entity("Prime",    8243, null),
    new Entity("oline",    8254, null),
    new Entity("frasl",    8260, null),
    new Entity("weierp",   8472, null),
    new Entity("image",    8465, null),
    new Entity("real",     8476, null),
    new Entity("trade",    8482,  174),    //(int)'®'),
    new Entity("alefsym",  8501, null),
    new Entity("larr",     8592, null),
    new Entity("uarr",     8593, null),
    new Entity("rarr",     8594, null),
    new Entity("darr",     8595, null),
    new Entity("harr",     8596, null),
    new Entity("crarr",    8629, null),
    new Entity("lArr",     8656, null),
    new Entity("uArr",     8657, null),
    new Entity("rArr",     8658, null),
    new Entity("dArr",     8659, null),
    new Entity("hArr",     8660, null),
    new Entity("forall",   8704, null),
    new Entity("part",     8706, null),
    new Entity("exist",    8707, null),
    new Entity("empty",    8709, null),
    new Entity("nabla",    8711, null),
    new Entity("isin",     8712, null),
    new Entity("notin",    8713, null),
    new Entity("ni",       8715, null),
    new Entity("prod",     8719, null),
    new Entity("sum",      8721, null),
    new Entity("minus",    8722, null),
    new Entity("lowast",   8727, null),
    new Entity("radic",    8730, null),
    new Entity("prop",     8733, null),
    new Entity("infin",    8734, null),
    new Entity("ang",      8736, null),
    new Entity("and",      8743, null),
    new Entity("or",       8744, null),
    new Entity("cap",      8745, null),
    new Entity("cup",      8746, null),
    new Entity("int",      8747, null),
    new Entity("there4",   8756, null),
    new Entity("sim",      8764, null),
    new Entity("cong",     8773, null),
    new Entity("asymp",    8776, null),
    new Entity("ne",       8800, null),
    new Entity("equiv",    8801, null),
    new Entity("le",       8804, null),
    new Entity("ge",       8805, null),
    new Entity("sub",      8834, null),
    new Entity("sup",      8835, null),
    new Entity("nsub",     8836, null),
    new Entity("sube",     8838, null),
    new Entity("supe",     8839, null),
    new Entity("oplus",    8853, null),
    new Entity("otimes",   8855, null),
    new Entity("perp",     8869, null),
    new Entity("sdot",     8901, null),
    new Entity("lceil",    8968, null),
    new Entity("rceil",    8969, null),
    new Entity("lfloor",   8970, null),
    new Entity("rfloor",   8971, null),
    new Entity("lang",     9001, null),
    new Entity("rang",     9002, null),
    new Entity("loz",      9674, null),
    new Entity("spades",   9824, null),
    new Entity("clubs",    9827, null),
    new Entity("hearts",   9829, null),
    new Entity("diams",    9830, null),
    new Entity("quot",    34, null),
    new Entity("amp",     38, null),
    new Entity("lt",      60, null),
    new Entity("gt",      62, null),
    new Entity(null,      149, 8226),    //(int)'•'  // "ascii" bullet
    new Entity("OElig",   338,  338),    //(int)'Œ'
    new Entity("oelig",   339,  339),    //(int)'œ'
    new Entity("Scaron",  352, null),
    new Entity("scaron",  353, null),
    new Entity("Yuml",    376, null),
    new Entity("circ",    710, null),
    new Entity("tilde",   732,  126),    //(int)'~'
    new Entity("ensp",    8194, null),
    new Entity("emsp",    8195, null),
    new Entity("thinsp",  8201, null),
    new Entity("zwnj",    8204, null),
    new Entity("zwj",     8205, null),
    new Entity("lrm",     8206, null),
    new Entity("rlm",     8207, null),
    new Entity("ndash",   8211,   45),   //(int)'-'
    new Entity("mdash",   8212,   45),   //(int)'-'
    new Entity("apos",      39,   39),   //(int)'\''
    new Entity("lsquo",   8216,   39),   //(int)'\''
    new Entity("rsquo",   8217,   39),   //(int)'\''
    new Entity("sbquo",   8218,   39),   //(int)'\''
    new Entity("ldquo",   8220,   34),   //(int)'"'
    new Entity("rdquo",   8221,   34),   //(int)'"'
    new Entity("bdquo",   8222,   34),   //(int)'"'
    new Entity("dagger",  8224, null),
    new Entity("Dagger",  8225, null),
    new Entity("permil",  8240, null),
    new Entity("lsaquo",  8249, null),
    new Entity("rsaquo",  8250, null),
    new Entity("euro",    8364, null),
  };


  /**
   * Convert entities to characters and standardize ambiguous characters.
   */
  public static final String unescape(String string) {
    final String e2c = convertEntitiesToCharacters(string);
    return convertAsciiToUtf8(e2c);
  }

  /**
   * Convert special characters to entities of the form "&entity;".
   */
  public static final String escape(String string) {
    return convertCharactersToEntities(string);
  }

  /**
   * Convert "&amp;&lt;entity&gt;;" to the entity's corresponsing char.
   */
  public static final String convertEntitiesToCharacters(String string) {
    return mapEntitiesToCharacters(string).getMappedString();
  }

  /**
   * Convert "&amp;&lt;entity&gt;;" to the entity's corresponsing char.
   */
  public static final MappedString mapEntitiesToCharacters(String string) {
    final MappedString result = new MappedString();

    final SearchResult searchResult = new SearchResult();
    final int len = string.length();
    int fromPos = 0;
    
    while (fromPos < len) {
      if (fromPos <= len - 3 && findEntity(string, fromPos, searchResult)) {
        result.
          append(string.substring(fromPos, searchResult.getStartPosition())).
          append(searchResult.getCodePoint(),
                 string.substring(searchResult.getStartPosition(),
                                  searchResult.getEndPosition() + 1));
        fromPos = searchResult.getEndPosition() + 1;
      }
      else {
        result.append(string.substring(fromPos));
        break;
      }
    }

    return result;
  }

  public static final boolean isGoodAscii(int codePoint) {
    boolean result = true;
    
    if (codePoint < 32) {
      if (codePoint != 10 && codePoint != 13 && codePoint != 9) {
        result = false;
      }
    }
    else if (codePoint > 126 && codePoint < 160) {
      result = false;
    }
    else if (codePoint > 255) {
      result = false;
    }

    return result;
  }

  public static final int convertAsciiToUtf8(int codePoint) {
    final Integer utf8 = ascii2utf8.get(codePoint);
    return (utf8 == null) ? codePoint : utf8;
  }

  public static final String convertAsciiToUtf8(String string) {
    final StringBuilder result = new StringBuilder();

    for (StringUtil.StringIterator iter = new StringUtil.StringIterator(string); iter.hasNext(); ) {
      StringUtil.StringPointer pointer = iter.next();
      final int utf8 = convertAsciiToUtf8(pointer.codePoint);
      result.appendCodePoint(utf8);
    }
    
    return result.toString();
  }

  /**
   * Convert mappable characters to "&amp;&lt;entity&gt;;".
   */
  public static final String convertCharactersToEntities(String string) {
    final StringBuilder result = new StringBuilder();

    for (StringUtil.StringIterator iter = new StringUtil.StringIterator(string); iter.hasNext(); ) {
      StringUtil.StringPointer pointer = iter.next();
      final String entity = char2entity.get(pointer.codePoint);
      if (entity == null) {
        // use numeric entities for non printing, unmapped ASCII values
        if ((pointer.codePoint > 122 && pointer.codePoint < 256) ||
//            (pointer.codePoint < ' ' && pointer.codePoint != '\t')) {
            (pointer.codePoint < '0' && pointer.codePoint != '\t' && pointer.codePoint != ' ')) {
          // non printing char
          result.append("&#").append(pointer.codePoint).append(';');
        }
        else {
          // printing char
          result.appendCodePoint(pointer.codePoint);
        }
      }
      else {
        result.append('&').append(entity).append(';');
      }
    }
    
    return result.toString();
  }

  private static final boolean findEntity(String string, int fromPos, SearchResult result) {
    boolean retval = false;
    final int ampPos = string.indexOf('&', fromPos);
    if (ampPos >= 0) {
      final int semiPos = findEntityEnd(string, ampPos + 1);
      if (semiPos >= 0) {
        final String entity = string.substring(ampPos + 1, semiPos);
        final Integer codePoint = entity2char.get(entity);
        if (codePoint != null) {
          result.setValues(ampPos, semiPos, codePoint);
          retval = true;
        }
        else {
          // auto decode if &#<decimalValue>; or &#x<hexValue>;
          if (string.charAt(ampPos + 1) == '#' && semiPos > ampPos + 2) {
            final char c = string.charAt(ampPos + 2);
            int radix = 10;
            int startPos = ampPos + 2;
            if (c == 'X' || c == 'x') {
              radix = 16;
              ++startPos;
            }
            try {
              final int value = Integer.parseInt(string.substring(startPos, semiPos), radix);
              if (Character.isValidCodePoint(value)) {
                result.setValues(ampPos, semiPos, value);
                retval = true;
              }
            }
            catch (NumberFormatException e) {
              // didn't find correct entity end... 
              retval = false;
            }
          }
        }
      }
      // found & but not ; or valid entity
      if (!retval) {
        result.setValues(ampPos, ampPos, '&');
        retval = true;
      }
    }
    return retval;
  }

  private static final int findEntityEnd(String string, int fromPos) {
    // look for ';' (or, actually,for non letter or digit)
    final int len = string.length();
    int curPos = fromPos;
    while (curPos < len) {
      final char c = string.charAt(curPos);
      if (c == ';' || c == ' ' || c == '>' || c == ',') break;
      ++curPos;
    }
    return curPos == len ? -1 : curPos;
  }

  private static final class SearchResult {
    
    private int startPosition;
    private int endPosition;
    private int codePoint;

    public SearchResult() {
      this.startPosition = -1;
      this.endPosition = -1;
      this.codePoint = -1;
    }

    public final void setValues(int startPosition, int endPosition, int codePoint) {
      this.startPosition = startPosition;
      this.endPosition = endPosition;
      this.codePoint = codePoint;
    }

    public final int getStartPosition() {
      return startPosition;
    }

    public final int getEndPosition() {
      return endPosition;
    }

    public final int getCodePoint() {
      return codePoint;
    }
  }

  private static final class Entity {
    public String string;
    public int codePoint;
    public Integer utf8;

    /**
     * Enable mapping to/from an entity string to a code point or utf8 value where possible.
     */
    public Entity(String string, int codePoint, Integer utf8) {
      this.string = string;
      this.codePoint = codePoint;
      this.utf8 = (utf8 != null) ? utf8 : codePoint;
      
      if (string != null) entity2char.put(string, this.utf8);
      char2entity.put(codePoint, string);  // only map non-utf8 codes back to entities.
      if (utf8 != null) {
        if (codePoint != utf8) ascii2utf8.put(codePoint, utf8);
//        possibleAscii.add(codePoint);
      }
    }
  }


  public static void main(String[] args) {
    for (String arg : args) {
      System.out.println(arg + "\t" + escape(arg) + "\t" + unescape(arg));
    }
  }
}
