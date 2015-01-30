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


import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.sd.token.TokenInfo;
import org.sd.xml.DomElement;
import org.sd.xml.DomNode;
import org.sd.xml.XmlFactory;
import org.w3c.dom.NodeList;

/**
 * Container for decoding xml-defined semi-structured input.
 * <p>
 * @author Spence Koehler
 */
public class XmlInputDecoder {
  
  //
  // Define paragraphs ("p"), tokens ("t"), and breaks ("b") in the text.
  //
  // <text oneLine="[false|]true">
  //   <p oneLine="[false|]true">
  //     <t [_cat="..."] feature1="value1" ...>...
  //       <t [_cat="..."] feature1="value1" ...>...
  //       </t>
  //     </t>
  //      ...<b [_type="[hard|]soft|none" [delim="..."]/>...
  //   </p>
  //   ...
  // </text>
  //
  // - input root is a single element named "text"
  //   - Notes:
  //     - consecutive text elements are treated as having a single space between
  //     - inner XML spacing is normalized to a single space between words. 
  //   - has zero or more text element children
  //     - consecutive non-empty direct text elements are treated as if under a default paragraph element
  //   - has zero or more "paragraph" element children (named "p")
  //   - has zero or more "break" element children (named "b")
  // - each element named "p" (for "paragraph")
  //   - has attributes:
  //     - oneLine (optional, default="false") true to block automatic sentence segmentation within the paragraph text.
  //     - other optional attributes (like "type") to specify information relevant to the paragraph.
  //   - has zero or more text element children
  //     - paragraph element text boundaries behave as sentence boundaries.
  //   - has zero or more element children named "t" (for "token")
  // - each element named "t" (for "token")
  //   - has attributes:
  //     - "_cat" with value matching a grammar rule's category to behave as if the token were matched by that rule
  //     - any number of feature="value" attributes, where "feature" is any feature name (except "_cat") and "value" is any value to be associated with the feature on the token.
  //   - has one or more text element children
  //   - has no element children named "b" (all are ignored)
  //   - has zero or more element chlidren named "t"
  //     - note that the span of a nested token will follow the break rules defined by that nested token element regardless of the break rules of its parent.
  //   - a token boundary adds a space, even an end boundary preceding a delimiter
  //     - for example:
  //       - "...testing <t>1,2,3</t>, testing..." yields text:
  //       - "...testing 1,2,3 , testing..."
  // - each element named "b" (for "break")
  //   - indicates position for a "hard token break" for the tokenizer
  //   - has attributes:
  //     - "_type" (optional, default="hard") "hard", "soft", or "none" to specify the type of break.
  //       - note that most breaks will be hard
  //       - if "hard" is declared, then a token boundary at the break is guaranteed.
  //       - if "soft" is declared, then a token may span across the identified break.
  //       - if "none" is declared, then the a token is guaranteed to span across the break
  //     - "delim" (optional) with the value of the delimiter chars to be returned by the tokenizer between tokens where the break is.
  //       - if not present, the space character at the break position will be hard
  //       - if present, then it must consist of all delims, including any whitespace
  //         - for example:
  //           - "...testing 1<b/>2<b delim="#$%"/>3<b/>,4<b delim=""/>5 testing..." yields text:
  //           - "...testing 1 2#$%3 ,45 testing..." (with a zero-width hard break at "5")
  //   - has no children (all are ignored.)
  //


  private boolean defaultOneLine;
  private List<Paragraph> paragraphs;
  private Map<String, String> attributes;

  /**
   * Construct with the "text" element.
   */
  public XmlInputDecoder(DomElement textElement) {
    this.paragraphs = new ArrayList<Paragraph>();
    this.attributes = null;
    init(textElement);
  }

  /**
   * Non-XML constructor.
   */
  public XmlInputDecoder(String inputString, boolean oneLine) {
    this.paragraphs = new ArrayList<Paragraph>();
    this.defaultOneLine = oneLine;
    this.attributes = null;

    boolean handled = false;
    if (inputString.indexOf("<text") >= 0) {
      try {
        final DomElement textElement = XmlFactory.buildDomNode(inputString, false).asDomElement();
        init(textElement);
        handled = true;
      }
      catch (IOException e) {
        //handled = false;
      }
    }

    if (!handled) {
      paragraphs.add(new Paragraph(oneLine, inputString, null));
    }
  }

  private final void init(DomElement textElement) {
    if (textElement == null) {
      this.defaultOneLine = false;
    }
    else {
      if (textElement.hasAttributes()) {
        this.attributes = textElement.getDomAttributes().getAttributes();
      }
      this.defaultOneLine = textElement.getAttributeBoolean("oneLine", false);

      final Paragraph p = loadParagraphs(buildParagraph(textElement), textElement);
      if (p != null && !p.isEmpty()) {
        this.paragraphs.add(p);
      }
    }
  }

  public boolean hasAttributes() {
    return attributes != null && attributes.size() > 0;
  }

  public Map<String, String> getAttributes() {
    return attributes;
  }

  public List<Paragraph> getParagraphs() {
    return paragraphs;
  }

  private final Paragraph loadParagraphs(Paragraph partialParagraph, DomElement curElement) {

    final NodeList childNodes = curElement.getChildNodes();
    final int numChildNodes = childNodes.getLength();
    for (int childNum = 0; childNum < numChildNodes; ++childNum) {
      final DomNode childNode = (DomNode)childNodes.item(childNum);
      if (childNode.asDomElement() != null) {
        boolean recurse = false;
        final String nodeName = childNode.getNodeName();
        if (nodeName != null && !"".equals(nodeName)) {
          final char c = Character.toLowerCase(nodeName.charAt(0));
          switch (c) {
            case 'p': 
              if (!partialParagraph.isEmpty()) {
                this.paragraphs.add(partialParagraph);
                partialParagraph = buildParagraph(childNode.asDomElement());
              }
              else {
                partialParagraph.setProperties(childNode.asDomElement().getDomAttributes().getAttributes());
              }
              recurse = true;
              break;
            case 't': 
              loadToken(partialParagraph, childNode.asDomElement());
              break;
            case 'b': 
              final MarkerInfo breakMarker = buildMarkerInfo(Marker.BREAK, childNode.asDomElement());
              partialParagraph.addMarkerInfo(breakMarker);
              break;
          }
        }
        if (recurse) {
          partialParagraph = loadParagraphs(partialParagraph, childNode.asDomElement());
          if (partialParagraph != null && !partialParagraph.isEmpty()) {
            this.paragraphs.add(partialParagraph);
          }
          partialParagraph = buildParagraph(curElement);
        }
      }
      else if (childNode.asDomText() != null) {
        partialParagraph.addText(childNode.asDomText().getHyperTrimmedText());
      }
    }

    return partialParagraph;
  }

  private final Paragraph buildParagraph(DomElement pElt) {
    final boolean oneLine = pElt.getAttributeBoolean("oneLine", defaultOneLine);
    return new Paragraph(oneLine, pElt.getDomAttributes().getAttributes());
  }

  private final void loadToken(Paragraph partialParagraph, DomElement tokenElt) {
    final MarkerInfo tokenStart = buildMarkerInfo(Marker.TOKEN_START, tokenElt);
    partialParagraph.addMarkerInfo(tokenStart);

    final NodeList childNodes = tokenElt.getChildNodes();
    final int numChildNodes = childNodes.getLength();
    for (int childNum = 0; childNum < numChildNodes; ++childNum) {
      final DomNode childNode = (DomNode)childNodes.item(childNum);
      if (childNode.asDomElement() != null) {
        final String nodeName = childNode.getNodeName();
        if (nodeName != null && !"".equals(nodeName)) {
          final char c = Character.toLowerCase(nodeName.charAt(0));
          if (c == 't') {
            // nested token (otherwise, ignore)
            loadToken(partialParagraph, childNode.asDomElement());
          }
        }
      }
      else if (childNode.asDomText() != null) {
        partialParagraph.addText(childNode.asDomText().getHyperTrimmedText());
      }
    }

    if (partialParagraph.length() <= tokenStart.getPos()) {
      // token is empty. ignore.
      partialParagraph.removeMarkerInfo(tokenStart);
    }
    else {
      final MarkerInfo tokenEnd = new MarkerInfo(Marker.TOKEN_END);
      tokenEnd.setOtherInfo(tokenStart);
      tokenStart.setOtherInfo(tokenEnd);
      partialParagraph.addMarkerInfo(tokenEnd);
    }
  }

  private final MarkerInfo buildMarkerInfo(Marker marker, DomElement domElt) {
    final MarkerInfo result = new MarkerInfo(marker);

    if (domElt.hasAttributes()) {
      // load attributes into result
      result.setAttributes(domElt.getDomAttributes().getAttributes());
    }

    return result;
  }


  private enum Marker { TOKEN_START, TOKEN_END, BREAK };

  public static final class MarkerInfo {
    public final Marker marker;
    private Map<String, String> attributes;
    private MarkerInfo otherInfo;  // start/end corresponding to this end/start
    private int pos;
    private String data;  // holds "category" for token or "breakType" for break

    // NOTES:
    // - Attributes notes:
    //   - when marker is TOKEN_START, attributes are features
    //     - "_cat" is pulled out and category data is set accordingly
    //   - when marker is BREAK, look for "delim" in attributes
    //     - and check for "_type", recording breakType data as
    //       - "h" (or null) for "hard",
    //       - "s" for "soft",
    //       - "n" for none
    // - otherInfo notes:
    //   - otherInfo exists for both TOKEN_START and TOKEN_END
    //     - points to opposite token end's MarkerInfo

    private MarkerInfo(Marker marker) {
      this.marker = marker;
      this.attributes = null;
      this.otherInfo = null;
      this.pos = -1;
      this.data = null;
    }

    public boolean hasAttributes() {
      return attributes != null && attributes.size() > 0;
    }

    public void setAttributes(Map<String, String> attributes) {
      if (attributes == null) {
        this.attributes = null;
      }
      else {
        this.attributes = new LinkedHashMap<String, String>();
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
          final String key = entry.getKey();
          final String value = entry.getValue();

          if (marker == Marker.TOKEN_START && "_cat".equals(key)) {
            this.data = value;
          }
          else if (marker == Marker.BREAK && "_type".equals(key) && value != null && !"".equals(value)) {
            this.data = value.substring(0, 1).toLowerCase();
          }
          else {
            this.attributes.put(key, value);
          }
        }
      }
    }

    public void addAttribute(String att, String val) {
      if (attributes == null) attributes = new LinkedHashMap<String, String>();
      attributes.put(att, val);
    }

    public boolean hasAttribute(String att) {
      return attributes != null && attributes.containsKey(att);
    }

    public String getAttribute(String att) {
      return attributes == null ? null : attributes.get(att);
    }

    public Map<String, String> getAttributes() {
      return attributes;
    }

    public boolean hasOtherInfo() {
      return otherInfo != null;
    }

    /** Get start/end corresponding to this end/start. */
    public MarkerInfo getOtherInfo() {
      return otherInfo;
    }

    public void setOtherInfo(MarkerInfo otherInfo) {
      this.otherInfo = otherInfo;
    }

    public void setPos(int pos) {
      this.pos = pos;
    }

    public int getPos() {
      return pos;
    }

    /** Use "delim" attribute to compute */
    public int getEndPos() {
      int result = pos;

      if (attributes != null) {
        final String delims = attributes.get("delim");
        if (delims != null) {
          pos += delims.length();
        }
      }

      return result;
    }

    public String getCategory() {
      return data;
    }

    public String getBreakType() {
      return data;
    }
  }

  public static final class Paragraph {
    private StringBuilder text;
    private TreeMap<Integer, List<MarkerInfo>> pos2markerInfos;
    private boolean oneLine;
    private boolean gotDelim;
    private Map<String, String> properties;

    private List<MarkerInfo> tokenStarts;
    private List<MarkerInfo> breakMarkers;

    private Paragraph(boolean oneLine, Map<String, String> properties) {
      this.text = new StringBuilder();
      this.pos2markerInfos = null;
      this.oneLine = oneLine;
      this.gotDelim = false;
      this.properties = properties;
      this.tokenStarts = null;
      this.breakMarkers = null;
    }

    private Paragraph(boolean oneLine, String trimmedText, Map<String, String> properties) {
      this(oneLine, properties);
      addText(trimmedText);
    }

    public boolean isEmpty() {
      return text.length() == 0;
    }

    public int length() {
      return text.length();
    }

    public boolean oneLine() {
      return oneLine;
    }

    public boolean hasProperties() {
      return properties != null && properties.size() > 0;
    }

    public String getProperty(String att) {
      return (properties != null) ? properties.get(att) : null;
    }

    public Map<String, String> getProperties() {
      return properties;
    }

    public void setProperties(Map<String, String> properties) {
      this.properties = properties;
    }

    public boolean hasTokens() {
      return tokenStarts != null && tokenStarts.size() > 0;
    }

    public boolean hasBreaks() {
      return breakMarkers != null && breakMarkers.size() > 0;
    }

    public final void addText(String trimmedText) {
      if (trimmedText != null && !"".equals(trimmedText)) {
        if (!gotDelim && text.length() > 0) text.append(' ');
        text.append(trimmedText);
        gotDelim = false;
      }
    }

    public String getText() {
      return text.toString();
    }

    public List<MarkerInfo> getTokenStarts() {
      return tokenStarts;
    }

    public List<MarkerInfo> getBreakMarkers() {
      return breakMarkers;
    }

    public void addMarkerInfo(MarkerInfo markerInfo) {
      if (markerInfo != null) {
        if (pos2markerInfos == null) pos2markerInfos = new TreeMap<Integer, List<MarkerInfo>>();
        int pos = text.length();  // point to end of string/=next delim pos
        if (markerInfo.marker == Marker.TOKEN_START && !gotDelim && text.length() > 0) ++pos;  //point to first letter of next trimmedText
        addMarkerInfo(pos, markerInfo);
      }
    }

    public void removeMarkerInfo(MarkerInfo markerInfo) {
      final List<MarkerInfo> markerInfos = pos2markerInfos.get(markerInfo.getPos());
      if (markerInfos != null) {
        markerInfos.remove(markerInfo);

        if (markerInfo.marker == Marker.TOKEN_START) tokenStarts.remove(markerInfo);
        else if (markerInfo.marker == Marker.BREAK) breakMarkers.remove(markerInfo);
      }
    }

    void addMarkerInfo(int pos, MarkerInfo markerInfo) {
      if (markerInfo != null) {
        if (pos2markerInfos == null) pos2markerInfos = new TreeMap<Integer, List<MarkerInfo>>();

        // Add tokenStart
        if (markerInfo.marker == Marker.TOKEN_START) {
          if (tokenStarts == null) tokenStarts = new ArrayList<MarkerInfo>();
          tokenStarts.add(markerInfo);
        }

        // Add break
        else if (markerInfo.marker == Marker.BREAK) {
          if (breakMarkers == null) breakMarkers = new ArrayList<MarkerInfo>();
          breakMarkers.add(markerInfo);

          final String delim = markerInfo.getAttribute("delim");
          if (delim != null) {
            text.append(delim);
            gotDelim = true;
          }
        }

        // Add pos2markerInfo
        List<MarkerInfo> markerInfos = pos2markerInfos.get(pos);
        if (markerInfos == null) {
          markerInfos = new ArrayList<MarkerInfo>();
          pos2markerInfos.put(pos, markerInfos);
        }
        markerInfos.add(markerInfo);
        markerInfo.setPos(pos);  // preserve for easier removal
      }
    }

    public Map<Integer, List<MarkerInfo>> getPos2MarkerInfos() {
      return pos2markerInfos;
    }

    public Integer getFirstPos() {
      return pos2markerInfos == null ? -1 : pos2markerInfos.firstKey();
    }

    public Integer getNextPos(int curPos) {
      return pos2markerInfos ==  null ? null : pos2markerInfos.higherKey(curPos);
    }

    public List<Paragraph> split(Iterator<String> iter) {
      final List<Paragraph> result = new ArrayList<Paragraph>();

      int offset = 0;
      while (iter.hasNext()) {
        final String nextText = iter.next();
        offset = this.text.indexOf(nextText, offset);

        final Paragraph nextParagraph = new Paragraph(true/*already split*/, nextText, properties);
        result.add(nextParagraph);
        final int nextOffset = offset + nextText.length();
        if (pos2markerInfos != null) {
          for (Map.Entry<Integer, List<MarkerInfo>> entry = pos2markerInfos.ceilingEntry(offset) ;
               entry != null && entry.getKey() <= nextOffset ;
               entry = pos2markerInfos.higherEntry(entry.getKey())) {
            final int pos = entry.getKey();
            final List<MarkerInfo> markerInfos = entry.getValue();

            for (MarkerInfo markerInfo : markerInfos) {
              if (pos == nextOffset && markerInfo.marker != Marker.TOKEN_END) {
                // this one doesn't belong. ignore.
              }
              else {
                nextParagraph.addMarkerInfo(pos - offset, markerInfo);
              }
            }
          }
        }

        // update offset
        offset = nextOffset;
      }

      if (result.size() == 0) {
        result.add(this);
      }

      return result;
    }

    public String toString() {
      return text.toString();
    }
  }
}
