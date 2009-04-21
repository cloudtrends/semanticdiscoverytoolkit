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
package org.sd.extract;

import org.sd.extract.datetime.DateTimeExtractor;
import org.sd.extract.datetime.DateTimeInterpretation;
import org.sd.text.PatternFinder;
import org.sd.text.TermFinder;
import org.sd.util.HashingFunction;
import org.sd.util.WhirlpoolHashingFunction;
import org.sd.util.tree.Tree;
import org.sd.xml.TagStack;
import org.sd.xml.XmlLite;
import org.sd.xml.XmlTreeHelper;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Container for holding snippet posts.
 *
 * @author Spence Koehler
 */
public class Post {

  private static final Set<String> NON_CORE_TAGS = new HashSet<String>();
  private static final Set<String> NON_BODY_TEXT = new HashSet<String>();
  static {
    NON_CORE_TAGS.add("a");
  }
  static {
    NON_BODY_TEXT.add("blog");
    NON_BODY_TEXT.add("blogs");
    NON_BODY_TEXT.add("article");
    NON_BODY_TEXT.add("articles");
    NON_BODY_TEXT.add("story");
    NON_BODY_TEXT.add("stories");
    NON_BODY_TEXT.add("comment");
    NON_BODY_TEXT.add("comments");
    NON_BODY_TEXT.add("commented");
    NON_BODY_TEXT.add("trackback");
    NON_BODY_TEXT.add("track back");
    NON_BODY_TEXT.add("track-back");
    NON_BODY_TEXT.add("trackbacks");
    NON_BODY_TEXT.add("track backs");
    NON_BODY_TEXT.add("track-backs");
    NON_BODY_TEXT.add("pingback");
    NON_BODY_TEXT.add("ping back");
    NON_BODY_TEXT.add("ping-back");
    NON_BODY_TEXT.add("pingbacks");
    NON_BODY_TEXT.add("ping backs");
    NON_BODY_TEXT.add("ping-backs");
    NON_BODY_TEXT.add("link");
    NON_BODY_TEXT.add("links");
    NON_BODY_TEXT.add("linked");
    NON_BODY_TEXT.add("linking");
    NON_BODY_TEXT.add("permalink");
    NON_BODY_TEXT.add("permalinks");
    NON_BODY_TEXT.add("post");
    NON_BODY_TEXT.add("posts");
    NON_BODY_TEXT.add("posted");
    NON_BODY_TEXT.add("related");
    NON_BODY_TEXT.add("mood");
    NON_BODY_TEXT.add("feeling");
    NON_BODY_TEXT.add("music");
    NON_BODY_TEXT.add("listening");
    NON_BODY_TEXT.add("next");
    NON_BODY_TEXT.add("forward");
    NON_BODY_TEXT.add("previous");
    NON_BODY_TEXT.add("back");
    NON_BODY_TEXT.add("category");
    NON_BODY_TEXT.add("categories");
    NON_BODY_TEXT.add("email");
    NON_BODY_TEXT.add("e-mail");
    NON_BODY_TEXT.add("address");
    NON_BODY_TEXT.add("url");
    NON_BODY_TEXT.add("tag");
    NON_BODY_TEXT.add("tags");
    NON_BODY_TEXT.add("date");
    NON_BODY_TEXT.add("view");
  }


  private ExtractionSnippet snippet;
  private List<DocText> texts;
  private LinkedList<Extraction> moreHeadings;
  private DateTimeInterpretation keyDateTime;

  private Integer year;
  private Integer month;
  private Integer day;
  private Integer hour;
  private Integer minute;
  private Integer second;
  private Integer ampm;
  private Integer timezone;

  // lazily loaded caches
  private String _fullContent;
  private String _allContent;
  private String _coreContent;
  private String _bodyContent;
  private String _headings;
  private String _hashString;
  private Tree<XmlLite.Data> _contentTree;
  private List<Bite> _bites;

  private HashingFunction _hashingFunction;


  public Post(ExtractionSnippet snippet, DocText firstText) {
    this(snippet, firstText, null);
  }
  
  private Post(ExtractionSnippet snippet, DocText firstText, LinkedList<Extraction> moreHeadings) {
    clearCaches();

    this.snippet = snippet;
    this.texts = new LinkedList<DocText>();
    this.moreHeadings = moreHeadings;
    this.keyDateTime = snippet.getDateTimeInterpretation();
    setDate(keyDateTime);

    addDocText(firstText);
  }

  private final void clearCaches() {
    _fullContent = null;
    _allContent = null;
    _coreContent = null;
    _headings = null;
    _hashString = null;
    _contentTree = null;
  }

  private final void setDate(DateTimeInterpretation datetime) {
    if (datetime == null) return;
    if (datetime.hasYear()) this.year = datetime.getYear();
    if (datetime.hasMonth()) this.month = datetime.getMonth();
    if (datetime.hasDay()) this.day = datetime.getDay();
    if (datetime.hasHour()) this.hour = datetime.getHour();
    if (datetime.hasMinute()) this.minute = datetime.getMinute();
    if (datetime.hasSecond()) this.second = datetime.getSecond();
    if (datetime.hasAmPm()) this.ampm = datetime.getAmPm();
    if (datetime.hasTimeZone()) this.timezone = datetime.getTimeZone();
  }

  public boolean isValid() {
//todo: instead of texts.size() > 0 use coreContent.length() > 0
    return year != null && month != null && day != null && texts.size() > 0;
  }

  public final Post addDocText(DocText docText) {

    clearCaches();

    //  check if the docText is a heading
    //    if belongs "under" moreHeadings, add as a heading;
    //    otherwise, time to end this post.
    //  add docText to texts if it wasn't a heading;
    //  pull out any date extractions and add non-conflicting info to calendar
    //    (ignoring other dates as they may be spurious dates found in the post.)
    
    boolean isHeading = false;

    final Integer headingStrength = HeadingExtractor.getHeadingProperty(docText);
    if (headingStrength != null && headingStrength > 0) {
      if (headingBelongsInPost(headingStrength)) {
        final Extraction heading = docText.getExtraction(HeadingExtractor.EXTRACTION_TYPE);
        if (heading != null) {
          if (moreHeadings == null) this.moreHeadings = new LinkedList<Extraction>();
          this.moreHeadings.add(heading);
          isHeading = true;
        }
      }
      else {
        // time to end this post.
        LinkedList<Extraction> nextHeadings = null;
        if (moreHeadings != null) {
          for (Extraction heading : moreHeadings) {
            final Integer nextHeadingStrength = HeadingExtractor.getHeadingProperty(heading.getDocText());
            if (HeadingExtractor.isUnder(headingStrength, nextHeadingStrength)) {
              if (nextHeadings == null) nextHeadings = new LinkedList<Extraction>();
              nextHeadings.add(heading);
            }
            else break;
          }
        }

        return new Post(snippet, docText, nextHeadings);
      }
    }

    if (!isHeading) {
      texts.add(docText);

      //
      //todo: add extractors for "non body" elements like comments, etc.
      //      for non-DateTimeExtractor.EXTRACTION_TYPE extractions that
      //      are non-body extractions, do:
      //        extraction.setProperty(ExtractionProperties.NON_BODY_EXTRACTION, true);
      //
    }

    final Extraction extraction = getExtraction(docText);
    if (extraction != null) addNonConflictingInfo(extraction);

    return this;
  }

  private final boolean headingBelongsInPost(Integer headingStrength) {
    boolean result = true;  // or maybe this should be (texts.size() == 0) ?

    if (moreHeadings != null && moreHeadings.size() > 0) {
      final Extraction lastHeading = moreHeadings.getLast();
      final Integer lastHeadingStrength = HeadingExtractor.getHeadingProperty(lastHeading.getDocText());
      result = HeadingExtractor.isUnder(headingStrength, lastHeadingStrength);
    }

    return result;
  }

  private final Extraction getExtraction(DocText docText) {
    return docText.getExtraction(DateTimeExtractor.EXTRACTION_TYPE);
  }

  private final void addNonConflictingInfo(Extraction extraction) {
    final DateTimeInterpretation datetime = (DateTimeInterpretation)extraction.getInterpretation();

    if (datetime != keyDateTime) {  // unless already added this info.
      if (!conflicts(datetime)) {
        setDate(datetime);
        ExtractionProperties.setDateTimeProperties(extraction);
      }
    }
  }

  private final boolean conflicts(DateTimeInterpretation datetime) {
    return
      datetime != null &&
      ((datetime.hasYear() && this.year != null) ||
       (datetime.hasMonth() && this.month != null) ||
       (datetime.hasDay() && this.day != null) ||
       (datetime.hasHour() && this.hour != null) ||
       (datetime.hasMinute() && this.minute != null) ||
       (datetime.hasSecond() && this.second != null));
  }

  public Date getDate() {
    final GregorianCalendar calendar = new GregorianCalendar();
    calendar.clear();
    calendar.setTimeZone(TimeZone.getTimeZone("GMT"));

    if (this.year != null) calendar.set(Calendar.YEAR, year);
    if (this.month != null) calendar.set(Calendar.MONTH, month);
    if (this.day != null) calendar.set(Calendar.DAY_OF_MONTH, day);
    if (this.hour != null) calendar.set(Calendar.HOUR, hour);
    if (this.minute != null) calendar.set(Calendar.MINUTE, minute);
    if (this.second != null) calendar.set(Calendar.SECOND, second);
    if (this.ampm != null) calendar.set(Calendar.AM_PM, ampm);
    if (this.timezone != null) calendar.set(Calendar.ZONE_OFFSET, timezone);

    return calendar.getTime();
  }

  public String getFullContent() {
    if (_fullContent == null) {
      final StringBuilder result = new StringBuilder();

      result.append(getHeadings());
      if (result.length() > 0) result.append("\n");
      result.append(getAllContent());

      _fullContent = result.toString();
    }
    return _fullContent;
  }

  public String getAllContent() {
    if (_allContent == null) {
      final StringBuilder result = new StringBuilder();

      for (DocText text : texts) {
        final String string = text.getString();
        if (string != null && !"".equals(string)) {
          if (result.length() > 0) result.append('\n');
          result.append(string);
        }
      }

      _allContent = result.toString();
    }
    return _allContent;
  }

  public Tree<XmlLite.Data> getContentTree() {
    if (_contentTree == null) {
      _contentTree = DocText.buildXmlTree(texts);
    }
    return _contentTree;
  }

  /**
   * Get this post's content docTexts, excluding heading docTexts.
   */
  public List<DocText> getTexts() {
    return texts;
  }

  /**
   * Get the minimum path index from the document for this post's texts.
   *
   * @return the minimum path index or -1 if there are no texts.
   */
  public int getMinPathIndex() {
    int result = -1;

    if (texts != null && texts.size() > 0) {
      final DocText docText = texts.get(0);
      result = docText.getPathIndex();
    }

    return result;
  }

  /**
   * Get the maximum path index from the document for this post's texts.
   *
   * @return the maximum path index or -1 if there are no texts.
   */
  public int getMaxPathIndex() {
    int result = -1;

    if (texts != null && texts.size() > 0) {
      final DocText docText = texts.get(texts.size() - 1);
      result = docText.getPathIndex();
    }

    return result;
  }

  public String getCoreContent() {
    if (_coreContent == null) {
      final StringBuilder result = new StringBuilder();

      for (DocText text : texts) {
        final String coreContent = getCoreContent(text);
        if (coreContent != null && !"".equals(coreContent)) {
          if (result.length() > 0) result.append('\n');
          result.append(coreContent);
        }
      }

      _coreContent = result.toString();
    }
    return _coreContent;
  }

  private final String getCoreContent(DocText text) {
    String result = null;

    final TagStack tagStack = text.getTagStack();
    if (tagStack.hasTag(NON_CORE_TAGS) < 0) {
      result = XmlTreeHelper.getAllText(XmlTreeHelper.excludeTags(text.getXmlNode(), NON_CORE_TAGS));
    }

    return result;
  }

  public String getBodyContent() {
    if (_bodyContent == null) {
      final StringBuilder result = new StringBuilder();

      for (DocText text : texts) {
        System.out.println("getBodyContent() docText: " + text);
        final String bodyContent = getBodyContent(text);
        if (bodyContent != null && !"".equals(bodyContent)) {
          if (result.length() > 0) result.append('\n');
          result.append(bodyContent);
        }
      }

      _bodyContent = result.toString();
    }
    return _bodyContent;
  }

  private final String getBodyContent(DocText text) { 
    String result = null;

    // get contentTree then use the algorithm to the sentences.

    // the short term strategy is to find to identify boundaries for the post body, which in theory ought to be a relatively meaty
    // chunk of text.  we know which type of words will appear in the footer and header(things like "blog", "comments", "links", "articles")
    // and should be able to identify a strategy to exclude them.  until a bulky text node is reached, we can be more lenient in our
    // strategy, and once a bulky text node is found, we can be more exclusive
    final TagStack tagStack = text.getTagStack();
    
    String allText = XmlTreeHelper.getAllText(text.getXmlNode());
    if("".equals(allText)) return null;

    int score = 0;

    String[] sections = allText.split("\\W+");
    if (sections == null || sections.length == 0) return null;

    // Strings which match one of the phrases ought to 
    TermFinder finder = new TermFinder("Non-Body Text", false, NON_BODY_TEXT.toArray(new String[0]));
    if (finder.hasPattern(allText, PatternFinder.FULL_WORD)){
      score -= 5 - sections.length;
    }

    if (sections.length < 5){
      int wordCharCount = 0;
      for (String section : sections){
        wordCharCount += section.length();
      }
      // Average word length ought to exceed 3 chars
      if (wordCharCount/sections.length < 3) score -= 1;
    }

    // Only include text in body if this text node has a positive score
    if(score < 0) return null;
    
    result = allText;
    System.out.println("getBodyContent() allText for docText: " + result);
    return result;
  }

  public final String getIndexableContent() {
    return Bite.getIndexableContent(getBites());
  }

  /**
   * Get the post's headings, in document order, as a string delimited by '\n' chars.
   */
  public String getHeadings() {
    if (_headings == null) {
      final StringBuilder result = new StringBuilder();

      if (snippet.getHeadings() != null) {
        for (Extraction heading : snippet.getHeadings()) {
          final String headingString = heading.asString();
          if (headingString != null && !"".equals(headingString)) {
            if (result.length() > 0) result.append('\n');
            result.append(headingString);
          }
        }
      }

      if (moreHeadings != null) {
        for (Extraction heading : moreHeadings) {
          if (heading != null && !"".equals(heading)) {
            if (result.length() > 0) result.append('\n');          
            result.append(heading);
          }
        }
      }

      _headings = result.toString();
    }

    return _headings;
  }

  public String getHashString() {
    if (_hashString == null) {
      _hashString = hash(getCoreContent());
    }
    return _hashString;
  }

  private final String hash(String string) {
    if (_hashingFunction == null) _hashingFunction = new WhirlpoolHashingFunction();
    return _hashingFunction.getHashString(_hashingFunction.getHashBytes(string));
  }

  public final List<Bite> getBites() {
    if (_bites == null) {
      this._bites = Bite.buildBites(getContentTree());
    }
    return _bites;
  }


  public String toString() {
    final StringBuilder result = new StringBuilder();
    SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM dd yyyy hh:mm a");
    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

    result.append(dateFormat.format(getDate()));

    if (snippet.getHeadings() != null) {
      for (Extraction heading : snippet.getHeadings()) {
        result.append("\n\t(H)\t").append(heading);
      }
    }
    if (moreHeadings != null) {
      for (Extraction heading : moreHeadings) {
        result.append("\n\t(h)\t").append(heading);
      }
    }

//     for (DocText text : texts) {
//       result.append("\n   \t").append(text);
//     }

//     try {
//       result.append("\n").append(XmlLite.asXml(getContentTree(), false));
//     }
//     catch (IOException e) {
//       result.append("\n  ***ERROR: " + e);
//     }

    final List<Bite> bites = getBites();
    for (Bite bite : bites) {
      result.append('\n').append(bite.toString());
    }

    return result.toString();
  }


  public static final void main(String[] args) throws IOException {
    //arg0: html file to snippetize and extract posts

    final File htmlFile = new File(args[0]);

    final BlogSnippetExtractor snippetizer = new BlogSnippetExtractor(2000);
    final List<ExtractionSnippet> snippets = snippetizer.getSnippets(htmlFile);

    if (snippets == null) {
      System.out.println("No snippets!");
    }
    else {
      int snippetNum = 0;
      int numPosts = 0;

      for (ExtractionSnippet snippet : snippets) {
//    	System.out.println("SNIPPET: " + snippet);
        final List<Post> posts = snippet.getPosts();
        int postNum = 0;
        for (Post post : posts) {
          if (post.isValid()) {
            System.out.println("(Snippet " + snippetNum + ", Post " + postNum + ")");
            System.out.println(post);
//             System.out.println(" Bites:");
//             final List<Bite> bites = post.getBites();
//             for (Bite bite : bites) {
//               System.out.println(bite);
//             }
//             System.out.println();


//            final Tree<XmlLite.Data> xmlTree = post.getContentTree();
//            PathHelper.dumpPaths(xmlTree);
//            System.out.println("FULL: " + post.getFullContent());
//            System.out.println("BODY: " + post.getBodyContent());
            ++postNum;
            ++numPosts;
          }
          else {
        	  System.out.println("Invalid post: " + post);
          }
        }
        ++snippetNum;
      }

      System.out.println("Done. " + snippetNum + " Snippets, " + numPosts + " Posts.");
    }
  }
}
