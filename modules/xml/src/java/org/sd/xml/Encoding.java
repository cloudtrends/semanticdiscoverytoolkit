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


import java.util.Map;
import java.util.HashMap;
import java.util.Collection;

/**
 * An enumeration of types to be used to specify character encodings.
 * <p>
 * @author Spence Koehler
 */
public class Encoding {
  
  private static int nextId = 0;
  private static Map<Integer, Encoding> id2type = new HashMap<Integer, Encoding>();
  private static Map<String, Encoding> label2type = new HashMap<String, Encoding>();
  
  /** raw encoding. */
  public static final Encoding RAW = new Encoding("raw");

  /** ascii encoding. */
  public static final Encoding ASCII = new Encoding("ascii");
  static {
    label2type.put("us-ascii", Encoding.ASCII)  ;
  }

  /** latin1 encoding. */
  public static final Encoding LATIN1 = new Encoding("latin1");

  /** utf8 encoding. */
  public static final Encoding UTF8 = new Encoding("utf8");
  static {
    label2type.put("utf-8", Encoding.UTF8)  ;
  }

  /** iso2022 encoding. */
  public static final Encoding ISO2022 = new Encoding("iso2022");

  /** macroman encoding. */
  public static final Encoding MACROMAN = new Encoding("macroman");

  /** windows encoding. */
  public static final Encoding WINDOWS = new Encoding("windows");

  /** iso8859-1 */
  public static final Encoding ISO8859_1 = new Encoding("iso8859-1", true);
  static {
    label2type.put("iso-8859-1", Encoding.ISO8859_1)  ;
  }

  /** iso8859-2 */
  public static final Encoding ISO8859_2= new Encoding("iso8859-2", true);
  static {
    label2type.put("iso-8859-2", Encoding.ISO8859_2)  ;
  }

  /** gb_1988-80 encoding */
  public static final Encoding GB_1988_80 = new Encoding("gb_1988-80", true);

  /** gb18030 encoding */
  public static final Encoding GB18030 = new Encoding("gb18030", true);

  /** gb_2312-80 encoding */
  public static final Encoding GB_2312_80 = new Encoding("gb_2312-80", true);

  /** gbk encoding */
  public static final Encoding GBK = new Encoding("gbk", true);
  static {
    label2type.put("cp936", Encoding.GBK)  ;
  }

  /** euc_cn encoding */
  public static final Encoding EUC_CN = new Encoding("euc_cn", true);
  static {
    label2type.put("gb2312", Encoding.EUC_CN)  ;
  }
  
  /** big5 encoding */
  public static final Encoding BIG_5 = new Encoding("big5", true);

  /** ms950 encoding */
  public static final Encoding MS950 = new Encoding("ms950", true);

  /** cp1252 encoding */
  public static final Encoding CP1252 = new Encoding("cp1252", true);
  static {
    label2type.put("windows-1252", Encoding.CP1252)  ;
  }

  /** cp1257 encoding */
  public static final Encoding CP1257 = new Encoding("cp1257", true);
  static {
    label2type.put("windows-1252", Encoding.CP1257)  ;
  }

  private int id;
  private String label;
  private boolean useReader;

  /** Always constructs a unique instance. */
  private Encoding(String label) {
    this(label, false);
  }

  private Encoding(String label, boolean useReader) {
    this.id = nextId++;
    this.label = label;
    this.useReader = useReader;
    id2type.put(id, this);
    label2type.put(label, this);
  }

  int getId() {
    return id;
  }

  /**
   * Get this encoding's label.
   */
  public String getLabel() {
    return label;
  }

  /**
   * Test whether an input stream reader with the 'label' as encoding should be used.
   */
  public boolean useReader() {
    return useReader;
  }

  /** Compute a hashCode for this compareType. */
  public int hashCode() {
    return getId();
  }

  /** Determine equality. */
  public boolean equals(Object other) {
    return (this == other);
  }

  /**
   * Get a string describing this compareType.
   */
  public String toString() {
    return label;
  }

  /**
   * Get the instance of encoding that has the given id.
   */
  static Encoding getEncoding(int id) {
    return id2type.get(id);
  }

  /**
   * Get the instance of the encoding that has the given label.
   */
  public static Encoding getEncoding(String label) {
    return label2type.get(label);
  }

  /** Get all encoding instances. */
  static Encoding[] getEncodings() {
    Collection<Encoding> values = id2type.values();
    return values.toArray(new Encoding[values.size()]);
  }
}
