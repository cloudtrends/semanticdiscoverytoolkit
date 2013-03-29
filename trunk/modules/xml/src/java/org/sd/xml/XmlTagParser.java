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


import org.sd.util.StringSplitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Thread-safe helper class to parse xml tags.
 * <p>
 * @author Spence Koehler
 */
public class XmlTagParser {
  
  public static final TagResult END_OF_STREAM_RESULT = new TagResult(true);

  private Set<String> optionalEndTags;
  private Map<String, String[]> specialRuleEndTags;
  private boolean specialScriptLogic;
  private boolean ignoreComments;

  /**
   * Construct an xml tag parser with default html options.
   *
   * @param specialScriptLogic  flag for applying special script logic; should
   *                            be true when parsing html; otherwise, false.
   * @param ignoreComments      flag to indicate aggressive skipping over
   *                            comment data.
   */
  public XmlTagParser(boolean specialScriptLogic,
                      boolean ignoreComments) {
    this.optionalEndTags = XmlLite.OPTIONAL_END_TAGS;
    this.specialRuleEndTags = XmlLite.SPECIAL_RULE_END_TAG_MAP;
    this.specialScriptLogic = specialScriptLogic;
    this.ignoreComments = ignoreComments;
  }

  /**
   * Construct an xml tag parser with the given options.
   *
   * @param optionalEndTags     a set of (lowercased) tags to automatically
   *                            close when encountered.
   * @param specialRuleEndTags  keys are (lowercased) tags that when seen
   *                            (opened) causes the first existing value
   *                            (after the first in the array which is the
   *                            key) to close.
   * @param specialScriptLogic  flag for applying special script logic; should
   *                            be true when parsing html; otherwise, false.
   * @param ignoreComments      flag to indicate aggressive skipping over
   *                            comment data.
   */
  public XmlTagParser(Set<String> optionalEndTags,
                      Map<String, String[]> specialRuleEndTags,
                      boolean specialScriptLogic,
                      boolean ignoreComments) {
    this.optionalEndTags = (optionalEndTags != null) ? optionalEndTags : new HashSet<String>();
    this.specialRuleEndTags = (specialRuleEndTags != null) ? specialRuleEndTags : new HashMap<String, String[]>();
    this.specialScriptLogic = specialScriptLogic;
    this.ignoreComments = ignoreComments;
  }

  /**
   * @param inputStream  The stream in the state of just having read an open angle bracket char.
   * @param data         The string builder instance to use for tag text buffering;
   *                     if null, read data will be discarded.
   * @param forceIgnoreComments  Force this read to ignore comments regardless of
   *                             the value of this instance's ignoreComments flag.
   *
   * @return a TagResult containing the result of the read, or null if nothing
   *         of significance read but there is still something more to be read.
   */
  public final TagResult readTag(XmlInputStream inputStream, StringBuilder data, boolean forceIgnoreComments, boolean commonCase) throws IOException {
    TagResult result = null;
    final boolean doIgnoreComments = forceIgnoreComments || ignoreComments;
    int codePoint = inputStream.read();

    // reached end of input
    if (codePoint == -1) {
      result = END_OF_STREAM_RESULT;
    }

    // ignore empty tag
    else if (codePoint == '>') {
      // do nothing. result will be null.
    }

    // ignore (skip over) comment
    else if (codePoint == '!' || codePoint == '?') {
      if (data != null) data.appendCodePoint(codePoint);
      readToEndOfComment(inputStream, data);
      if (codePoint == '!' && !doIgnoreComments) {
        // keep comment data
        final String text = getBuiltText(data, false);  // leave comment text as-is
        if (text.length() > 0) {
          final XmlLite.Comment comment = new XmlLite.Comment(text);
          result = new TagResult(comment);
        }
      }
      else {
        //ignore comment data. result will be null.
        if (data != null) data.setLength(0);
      }
    }

    // deal with end tag
    else if (codePoint == '/') {
      inputStream.readToChar('>', data, -1);
      final String text = getBuiltText(data, true);
      if (text.length() > 0) {
        result = new TagResult(text);
      }
    }

    // deal with script/style or start tag
    else {
      if (data != null) data.appendCodePoint(codePoint);  // don't forget the first char!

      readToTagEnd(inputStream, data, commonCase);

      final String text = getBuiltText(data, false);
      if (text.length() > 0) {
        final String ltext = text.toLowerCase();
        if (specialScriptLogic && ltext.startsWith("script") && (ltext.length() == 6 || ltext.charAt(6) == ' ')) {
          // added this section do deal with all the little nasties in html script nodes
          // things like multiple comment starts w/out ends; c-style commenting of tags; etc.
          // currently we read all data between <script> and </script> and stick it in a
          // script data instance.
          final StringBuilder scriptText = new StringBuilder();
          final int pos = readToEndOfScript(inputStream, scriptText, true);
          final XmlLite.Script script = new XmlLite.Script(getBuiltText(scriptText, false));
          result = new TagResult(script, pos < 0);
        }
        else if (specialScriptLogic && ltext.startsWith("style") && (ltext.length() == 5 || ltext.charAt(5) == ' ')) {
          final StringBuilder styleText = new StringBuilder();
          final int pos = readToEndOfStyle(inputStream, styleText, true);
          final XmlLite.Style style = new XmlLite.Style(getBuiltText(styleText, false));
          result = new TagResult(style, pos < 0);
        }
        else {
          final XmlLite.Tag tag = new XmlLite.Tag(text, commonCase);
          if (isOptionalEndTag(tag.name)) tag.setSelfTerminating();
          result = new TagResult(tag);
        }
      }
    }

    return result;
  }

  public final boolean isOptionalEndTag(String tagName) {
    return optionalEndTags.contains(tagName.toLowerCase());
  }

  public final boolean isSpecialRuleEndTag(String tagName) {
    return specialRuleEndTags.containsKey(tagName);
  }

  public final String[] getSpecialToCloseTags(String tagName) {
    return specialRuleEndTags.get(tagName);
  }

  public final boolean specialScriptLogic() {
    return specialScriptLogic;
  }

  public final boolean ignoreComments() {
    return ignoreComments;
  }

  protected final String getBuiltText(StringBuilder data, boolean fix) {
    if (data == null) return "";
    final String text = fix ? XmlLite.fixText(data.toString()) : StringSplitter.hypertrim(data.toString());
    data.setLength(0);
    return text;
  }

  private final void readToEndOfComment(XmlInputStream inputStream, StringBuilder result) throws IOException {
    final StringBuilder temp = new StringBuilder();

    // if no -- after ! means don't look for closing '-->', just '>'
    final int cp1 = inputStream.read();
    if (cp1 == -1 || cp1 == '>') return;
    final int cp2 = inputStream.read();
    if (cp2 == -1) return;
    if (cp1 != '-' || cp2 != '-') {
      if (cp2 == '>') {
        if (result != null) result.appendCodePoint(cp1);
        return;
      }
      temp.appendCodePoint(cp1).appendCodePoint(cp2);
      spinToEndOfComment(inputStream, temp);

      if (result != null) result.append(temp);
      return;
    }

    temp.appendCodePoint(cp1).appendCodePoint(cp2);
    boolean keepGoing = (spinToEndOfComment(inputStream, temp) >= 0);
    
    while (keepGoing) {
      final int len = temp.length();
      result.append(temp);
      if ((len >= 2 && temp.charAt(len - 1) == '-' && temp.charAt(len - 2) == '-')) {
        // found it!
        break;
      }
      temp.setLength(0);
      result.append('>');  // need to keep this after all.
      keepGoing = (spinToEndOfComment(inputStream, temp) >= 0);
    }
  }

  private final int spinToEndOfComment(XmlInputStream inputStream, StringBuilder result) throws IOException {
    int retval = -1;

    while ((retval = inputStream.readToChar('>', result, '!')) == '!') {
      result.append('!');
      if (result.length() >= 2 && result.charAt(result.length() - 2) == '<') {
        final int nextcp = inputStream.read();
        result.appendCodePoint(nextcp);
        if (nextcp != '[') {
          // found a nested comment! read to its end, then to the end of this comment.
          readToEndOfComment(inputStream, result);
          result.append('>');
          retval = '>';
        }
      }
    }

    return retval;
  }

  /**
   * Having read an open angle bracket (and the first character after it), read
   * to the corresponding close angle bracket.
   * <p>
   * The trick here is to skip over angle brackets within xml attribute values
   * within the tag being read.
   */
  private final void readToTagEnd(XmlInputStream inputStream, StringBuilder data, boolean htmlFlag) throws IOException {

    // read up to '>' or '=', whichever comes first
    for (int stoppedAt = inputStream.readToChar('>', data, '=');
         !(stoppedAt == -1 || stoppedAt == '>');
         stoppedAt = inputStream.readToChar('>', data, '=')) {

      //NOTE: we're only here if (stoppedAt == '=')
      if (data != null) data.appendCodePoint(stoppedAt);

      // if the next char is \' or \", then read to corresponding end quote
      // and then loop to next of '>' or '='
      final int quoteChar = inputStream.read();

      if (quoteChar == -1 || quoteChar == '>') {
        break;
      }
      else {
        // squirrel away the read character
        if (data != null) data.appendCodePoint(quoteChar);

        if (quoteChar == '\'' || quoteChar == '"') {
          int nextStop = -1;

          if (htmlFlag) {
            // read up to end quote, skipping backslash-quoted characters
            for (nextStop = inputStream.readToChar((char)quoteChar, data, '\\');
                 nextStop == '\\';
                 nextStop = inputStream.readToChar((char)quoteChar, data, '\\')) {

              // found backslash. store it and the next char and keep going
              if (data != null) data.appendCodePoint(nextStop);
              final int quotedChar = inputStream.read();
              if (quotedChar == -1) break;
              if (data != null) data.appendCodePoint(quotedChar);
            }
          }
          else {
            // don't use backslash quoting in normal xml
            nextStop = inputStream.readToChar((char)quoteChar, data, -1);
          }

          if (nextStop == quoteChar && data != null) data.appendCodePoint(nextStop);
        }

        // loop to next of '>' or '='
      }
    }
  }

  // protected and static access for junit testing
  protected static final int readToEndOfScript(XmlInputStream inputStream, StringBuilder result, boolean firstOne) throws IOException {
    int pos = -1;
    while ((pos = inputStream.readToChar('>', result, -1)) >= 0) {
      if (endsWithEndScript(result)) {
        // if firstOne, remove </script> from end of result
        if (firstOne) result.setLength(result.length() - 8);
        else result.append('>');
        break;
      }
/*
      else if (endsWithStartScript(result)) {
        // recurse to readToEndOf embedded script.
        result.append('>');
        pos = readToEndOfScript(inputStream, result, false);
        if (pos < 0) break;
      }
*/
      else {
        // else, keep reading. haven't found end of script yet.
        result.append('>');
      }
    }
    return pos;
  }

  // return true if builder.toString().endsWith("</script")
  private static final boolean endsWithEndScript(StringBuilder builder) {
    final int len = builder.length();
    if (len >= 8) {
      final String end = builder.substring(len - 8).toLowerCase();
      return "</script".equals(end);
    }
    return false;
  }

  // return true if builder.toString().endsWith("<script" or "<script ...")
  private static final boolean endsWithStartScript(StringBuilder builder) {
    final int len = builder.length();
    final int closePos = builder.lastIndexOf(">");
    final int openPos = builder.lastIndexOf("<");

    if (openPos >= 0 && openPos > closePos && openPos + 6 <= len) {
      final String end = builder.substring(openPos + 1).toLowerCase();
      if (end.startsWith("script")) {
        if (end.length() == 6) return true;
        else if (end.charAt(6) == ' ') {
          // make sure it's not self-terminating
          if (builder.charAt(len - 1) != '/') return true;
        }
      }
    }

    return false;
  }

  // protected and static access for junit testing
  protected static final int readToEndOfStyle(XmlInputStream inputStream, StringBuilder result, boolean firstOne) throws IOException {
    int pos = -1;
    while ((pos = inputStream.readToChar('>', result, -1)) >= 0) {
      if (endsWithEndStyle(result)) {
        // if firstOne, remove </style> from end of result
        if (firstOne) result.setLength(result.length() - 7);
        else result.append('>');
        break;
      }
/*
      else if (endsWithStartStyle(result)) {
        // recurse to readToEndOf embedded style.
        result.append('>');
        pos = readToEndOfStyle(inputStream, result, false);
        if (pos < 0) break;
      }
*/
      else {
        // else, keep reading. haven't found end of style yet.
        result.append('>');
      }
    }
    return pos;
  }

  // return true if builder.toString().endsWith("</style")
  private static final boolean endsWithEndStyle(StringBuilder builder) {
    final int len = builder.length();
    if (len >= 7) {
      final String end = builder.substring(len - 7).toLowerCase();
      return "</style".equals(end);
    }
    return false;
  }

  // return true if builder.toString().endsWith("<style" or "<style ...")
  private static final boolean endsWithStartStyle(StringBuilder builder) {
    final int len = builder.length();
    final int closePos = builder.lastIndexOf(">");
    final int openPos = builder.lastIndexOf("<");

    if (openPos >= 0 && openPos > closePos && openPos + 5 <= len) {
      final String end = builder.substring(openPos + 1).toLowerCase();
      if (end.startsWith("style")) {
        if (end.length() == 5) return true;
        else if (end.charAt(5) == ' ') {
          // make sure it's not self-terminating
          if (builder.charAt(len - 1) != '/') return true;
        }
      }
    }

    return false;
  }

  public static class TagResult {

    private boolean endOfStream;
    private XmlLite.Tag tag;
    private String endTag;
    private XmlLite.Comment comment;
    private XmlLite.Script script;
    private XmlLite.Style style;

    private TagResult() {
      this.endOfStream = false;
      this.tag = null;
      this.endTag = null;
      this.comment = null;
      this.script = null;
      this.style = null;
    }

    private TagResult(boolean endOfStream) {
      this();
      this.endOfStream = endOfStream;
    }

    public TagResult(XmlLite.Tag tag) {
      this();
      this.tag = tag;

      if (tag.isSelfTerminating()) {
        this.endTag = tag.name;
      }
    }

    public TagResult(String endTag) {
      this();
      this.endTag = endTag;
    }

    public TagResult(XmlLite.Comment comment) {
      this();
      this.comment = comment;
    }

    public TagResult(XmlLite.Script script, boolean endOfStream) {
      this();
      this.tag = new XmlLite.Tag("script", true);
      this.script = script;
      this.endOfStream = endOfStream;
    }

    public TagResult(XmlLite.Style style, boolean endOfStream) {
      this();
      this.tag = new XmlLite.Tag("style", true);
      this.style = style;
      this.endOfStream = endOfStream;
    }

    /**
     * Get whether the end of stream was hit.
     */
    public boolean hitEndOfStream() {
      return endOfStream;
    }

    public boolean hasTag() {
      return tag != null;
    }

    /**
     * Get this result's tag if present, or null.
     */
    public XmlLite.Tag getTag() {
      return tag;
    }

    public boolean hasEndTag() {
      return endTag != null;
    }

    /**
     * Get this result's end tag name if present, or null.
     */
    public String getEndTag() {
      return endTag;
    }

    public boolean hasComment() {
      return comment != null;
    }

    /**
     * Get this result's comment if present, or null.
     */
    public XmlLite.Comment getComment() {
      return comment;
    }

    public boolean hasScript() {
      return script != null;
    }

    /**
     * Get this result's script if present, or null.
     */
    public XmlLite.Script getScript() {
      return script;
    }

    public boolean hasStyle() {
      return style != null;
    }

    /**
     * Get this result's style if present, or null.
     */
    public XmlLite.Style getStyle() {
      return style;
    }

    public String toString() {
      final StringBuilder result = new StringBuilder();

      if (tag != null) {
        result.append("tag:").append(tag.name);
      }
      if (endTag != null) {
        if (result.length() > 0) result.append(',');
        result.append("endTag:").append(endTag);
      }
      if (comment != null) {
        result.append("comment");
      }
      if (script != null) {
        result.append("script");
      }
      if (endOfStream) {
        if (result.length() > 0) result.append(',');
        result.append("endOfStream");
      }

      return result.toString();
    }
  }
}
