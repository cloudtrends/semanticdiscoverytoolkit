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
import org.sd.text.DetailedUrl;
import org.sd.xml.XmlLite;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

/**
 * Utility to extract blog snippets.
 *
 * @author Spence Koehler
 */
public class BlogSnippetExtractor extends SnippetExtractor {

  /**
   * Wrap the given extractors to include a DateTimeExtractor.
   */
  private static final Extractor[] wrapExtractors(int minYear, boolean ignoreExtraInput, int skipUpTo, Extractor[] extractors) {
    final Extractor[] result = new Extractor[extractors == null ? 1 : extractors.length + 1];

    result[0] = new DateTimeExtractor(minYear, ignoreExtraInput, skipUpTo);

    if (extractors != null) {
      for (int i = 0; i < extractors.length; ++i) {
        result[i + 1] = extractors[i];
      }
    }

    return result;
  }


  public BlogSnippetExtractor(int minYear) {
    this(minYear, true, 5);
  }

  public BlogSnippetExtractor(int minYear, boolean ignoreExtraInput, int skipUpTo) {
    this(minYear, ignoreExtraInput, skipUpTo, null);
  }

  public BlogSnippetExtractor(int minYear, boolean ignoreExtraInput, int skipUpTo, Extractor[] extractors) {
    super(wrapExtractors(minYear, ignoreExtraInput, skipUpTo, extractors));
  }

  /**
   * Get the extraction group from the results from which snippets are to be
   * extracted.
   */
  protected ExtractionGroup getExtractionGroup(ExtractionResults results) {
    ExtractionGroup result = null;

    if (results != null) {
      final Collection<ExtractionGroup> groups = results.getExtractionGroups(DateTimeExtractor.EXTRACTION_TYPE);

      if (groups != null) {
        final List<BlogSnippetGroup> snippetGroups = mergeGroups(groups);

        BlogSnippetGroup bestSnippetGroup = null;
        for (BlogSnippetGroup snippetGroup : snippetGroups) {
          bestSnippetGroup = chooseBest(bestSnippetGroup, snippetGroup);
        }
        if (bestSnippetGroup != null) {
          result = bestSnippetGroup.getExtractionGroup();
        }
      }
    }

    return result;
  }

  private final List<BlogSnippetGroup> mergeGroups(Collection<ExtractionGroup> groups) {
    List<BlogSnippetGroup> result = new ArrayList<BlogSnippetGroup>();

    // NOTE: extraction groups are differentiated by path key, but may have common parse signatures or date flags.

    for (ExtractionGroup extractionGroup : groups) {
      if (isKeeper(extractionGroup)) {
        boolean addedIt = false;
        for (BlogSnippetGroup snippetGroup : result) {
          if (snippetGroup.accept(extractionGroup)) {
            addedIt = true;
            break;
          }
        }
        if (!addedIt) {
          final BlogSnippetGroup snippetGroup = new BlogSnippetGroup(extractionGroup);
          result.add(snippetGroup);
        }
      }
    }

    return result;
  }

  private final boolean isKeeper(ExtractionGroup group) {
    boolean result = true;

    // throw out lists of dates (i.e. for archives)
    if (group.size() > 1 && group.getSpread() <= 1) {
      result = false;
    }

    return result;
  }

//  private final ExtractionGroup chooseBest(ExtractionGroup group1, ExtractionGroup group2) {
  private final BlogSnippetGroup chooseBest(BlogSnippetGroup group1, BlogSnippetGroup group2) {
    BlogSnippetGroup result = group2;

    if (group1 != null) {
      // choose the (first) most complete group with a day
      final boolean hasDay1 = group1.hasDay();
      final boolean hasDay2 = group2.hasDay();

      if (hasDay1 && !hasDay2) {
        result = group1;
      }
      else if (hasDay2 && !hasDay1) {
        result = group2;
      }
      else if (hasDay1 && hasDay2) {
        final int infoCount1 = group1.countInfo();
        final int infoCount2 = group2.countInfo();
        if (infoCount2 > infoCount1) {
          result = group2;
        }
        else if (infoCount1 > infoCount2) {
          result = group1;
        }
        else {
          // if groups have the same info (flags), choose the first;
          // otherwise, choose that with the largest average "spread".
          if (group1.getDateTimeFlags().equals(group2.getDateTimeFlags())) {
            // choose the first.
            result = group1;
          }
          else {
            final int spread1 = group1.getExtractionGroup().getTotalSpread();
            final int spread2 = group2.getExtractionGroup().getTotalSpread();

            if (spread2 > spread1) {
              result = group2;
            }
            else {
              result = group1;
            }
          }
        }
      }
    }

    return result;
  }

  private final boolean hasDay(DateTimeInterpretation datetime) {
    boolean result = false;

    if (datetime != null) {
      result = datetime.hasDay();
    }

    return result;
  }

  private final int countInfo(DateTimeInterpretation datetime) {
    int result = 0;

    if (datetime != null) {
      if (datetime.hasDate()) {
        if (datetime.hasDay()) ++result;
        if (datetime.hasMonth()) ++result;
        if (datetime.hasYear() && !datetime.guessedYear()) ++result;
      }
//note: just count day/month/year pieces, not time pieces.
/*
      if (datetime.hasTime()) {
        if (datetime.hasHour()) ++result;
        if (datetime.hasMinute()) ++result;
        if (datetime.hasSecond()) ++result;
      }
*/
    }

    return result;
  }

  private final DateTimeInterpretation getDateTime(ExtractionGroup group) {
    DateTimeInterpretation result = null;

    final List<Extraction> extractions = group.getExtractions();
    if (extractions != null && extractions.size() > 0) {
      final Extraction extraction = extractions.get(0);
      result = (DateTimeInterpretation)extraction.getInterpretation();
    }
    return result;
  }


  public static final void main(String[] args) throws IOException {
    //arg0: html file to snippetize
    SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM dd yyyy hh:mm a");
    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

    final File htmlFile = new File(args[0]);

    final BlogSnippetExtractor snippetizer = new BlogSnippetExtractor(2000);

    final List<ExtractionSnippet> snippets = snippetizer.getSnippets(htmlFile);

    int totalPosts = 0;

    int snippetNum = 0;
    for (ExtractionSnippet snippet : snippets) {
      //System.out.println(snippet);
      final List<Post> posts = snippet.getPosts();
      int postNum = 0;
      for (Post post : posts) {
        System.out.println("\n(" + snippetNum + "," + postNum + ")  date=" + dateFormat.format(post.getDate()) + "\n" + post.getFullContent());

        final String xmlContent = XmlLite.asXml(post.getContentTree(), false);
        System.out.println(xmlContent);
        final Set<DetailedUrl> urls = new DetailedUrl("").extractUrls(xmlContent, false);
        if (urls != null) {
          for (DetailedUrl dUrl : urls) {
            System.out.println(dUrl);
          }
        }

        ++postNum;
        ++totalPosts;
      }
      ++snippetNum;
    }

    System.out.println(totalPosts + " total posts.");
  }
}
