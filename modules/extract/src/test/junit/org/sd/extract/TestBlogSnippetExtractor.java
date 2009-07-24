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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.sd.io.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * JUnit Tests for the BlogSnippetExtractor class.
 *
 * @author Spence Koehler
 */
public class TestBlogSnippetExtractor extends TestCase {

  private final void verifySnippets(BlogSnippetExtractor snippetizer, File htmlFile,
                                    int[][] headingPathIndexes, int[][] bodyPathRanges,
                                    String[] extractionDataStrings) throws IOException {

    // to see the snippets do:
    //java -Xmx640m org.sd.doc.BlogSnippetExtractor /home/sbk/co/core/src/test/unit/org.sd.doc/resources/snippet-sample08.html


    final List<ExtractionSnippet> snippets = snippetizer.getSnippets(htmlFile);

    if (headingPathIndexes == null) {
      System.out.println(htmlFile);
      
      if (snippets == null) {
        System.out.println("  no snippets!\n");
      }
      else {
        System.out.println(" headingPathIndexes:");
        for (ExtractionSnippet snippet : snippets) {
          final List<Extraction> headings = snippet.getHeadings();

          System.out.print("\t{");
          for (Extraction heading : headings) {
            System.out.print(heading.getPathIndex() + ", ");
          }
          System.out.println("},");
        }

        System.out.println(" bodyPathRanges:");
        for (ExtractionSnippet snippet : snippets) {
          final List<DocText> body = snippet.getBody();

          final int firstBodyPath = body.get(0).getPathIndex();
          final int lastBodyPath = body.get(body.size() - 1).getPathIndex();

          System.out.println("\t{" + firstBodyPath + ", " + lastBodyPath + "},");
        }

        System.out.println(" extractionDataStrings:");
        for (ExtractionSnippet snippet : snippets) {
          final Extraction primaryExtraction = snippet.getPrimaryExtraction();
          if (primaryExtraction != null) {
            System.out.println("\t\"" + primaryExtraction.getData().toString() + "\",");
          }
          else {
            System.out.println("\t\"\",");
          }
        }

        System.out.println();
      }
    }
    else {
      if (headingPathIndexes.length == 0) {
        assertNull(htmlFile.toString(), snippets);
      }
      else {
        assertEquals(htmlFile.toString(), headingPathIndexes.length, snippets.size());

        int index1 = 0;
        for (ExtractionSnippet snippet : snippets) {
          final List<Extraction> headings = snippet.getHeadings();
          final List<DocText> body = snippet.getBody();

          assertEquals(htmlFile.toString(), headingPathIndexes[index1].length, headings.size());

          int index2 = 0;
          for (Extraction heading : headings) {
            assertEquals(htmlFile.toString() + " heading #" + index1 + "," + index2,
                         headingPathIndexes[index1][index2], heading.getPathIndex());
            ++index2;
          }

          final int firstBodyPath = body.get(0).getPathIndex();
          final int lastBodyPath = body.get(body.size() - 1).getPathIndex();

          assertEquals(htmlFile.toString() + " bodyPath #" + index1 + " (start)", bodyPathRanges[index1][0], firstBodyPath);
          assertEquals(htmlFile.toString() + " bodyPath #" + index2 + " (end)", bodyPathRanges[index1][1], lastBodyPath);

          final Extraction primaryExtraction = snippet.getPrimaryExtraction();
          if ("".equals(extractionDataStrings[index1])) {
            assertNull(htmlFile.toString(), primaryExtraction);
          }
          else {
            assertEquals(htmlFile.toString() + " extractionString #" + index1,
                         extractionDataStrings[index1], primaryExtraction.getData().toString());
          }

          ++index1;
        }
      }
    }
  }

  public void testSamples() throws IOException {
    final BlogSnippetExtractor snippetizer = new BlogSnippetExtractor(2000);

    verifySnippets(snippetizer, FileUtil.getFile(this.getClass(), "resources/cache/cache-9999/bl/blog.maudoune.com.html.gz"),
        new int[][] {
          {0, },
          {0, },
          {0, },
          {0, },
          {0, }},
        new int [][] {
          {9, 17},
          {18, 27},
          {28, 41},
          {42, 53},
          {54, 66}},
        new String[] {
          "[(DATE_TIME (DATE (WEEKDAY 'lundi') (DAY '4') (MONTH 'février') (YEAR '2008')) (TIME (HOUR '12') (MINUTE '38&space;-')))]",
          "[(DATE_TIME (DATE (WEEKDAY 'jeudi') (DAY '31') (MONTH 'janvier') (YEAR '2008')) (TIME (HOUR '13') (MINUTE '55&space;-')))]",
          "[(DATE_TIME (DATE (WEEKDAY 'samedi') (DAY '1') (MONTH 'décembre') (YEAR '2007')) (TIME (HOUR '13') (MINUTE '06&space;-')))]",
          "[(DATE_TIME (DATE (WEEKDAY 'samedi') (DAY '17') (MONTH 'novembre') (YEAR '2007')) (TIME (HOUR '08') (MINUTE '04&space;-')))]",
          "[(DATE_TIME (DATE (WEEKDAY 'mercredi') (DAY '7') (MONTH 'novembre') (YEAR '2007')) (TIME (HOUR '17') (MINUTE '36&space;-')))]"});

    verifySnippets(snippetizer, FileUtil.getFile(this.getClass(), "resources/cache/cache-9999/ad/adwords-de.blogspot.html.gz"),
        new int[][] {
          {0, },
          {0, },
          {0, 35, }},
        new int [][] {
          {14, 34},
          {35, 43},
          {44, 70}},
        new String[] {
          "",
          "[(DATE_TIME (DATE (WEEKDAY 'Donnerstag') (DAY '13.') (MONTH 'März') (YEAR '2008')) (TIME (PREP 'at') (TIME (HOUR '16') (MINUTE '52') (SECOND '00'))))]",
          "[(DATE_TIME (DATE (WEEKDAY 'Montag') (DAY '10.') (MONTH 'März') (YEAR '2008')) (TIME (PREP 'at') (TIME (HOUR '17') (MINUTE '00') (SECOND '00'))))]"});

    verifySnippets(snippetizer, FileUtil.getFile(this.getClass(), "resources/cache/cache-9999/bl/blogdeviajes.com.ar.html.gz"),
        new int[][] {
           {0, 8, },
           {0, },
           {0, },
           {0, },
           {0, },
           {0, },
           {0, }},
        new int [][] {
           {10, 17},
           {18, 80},
           {81, 103},
           {104, 150},
           {151, 198},
           {199, 230},
           {231, 272}},
        new String[] {
           "[(DATE_TIME (DATE (MONTH 'Marzo') (DAY '18th') (YEAR '2008')) (TIME (PREP 'at') (TIME (HOUR '1') (MINUTE '56') (AMPM 'pm'))))]",
           "[(DATE (MONTH 'Marzo') (DAY '18th') (YEAR '2008'))]",
           "[(DATE (MONTH 'Marzo') (DAY '18th') (YEAR '2008'))]",
           "[(DATE (MONTH 'Marzo') (DAY '17th') (YEAR '2008'))]",
           "[(DATE (MONTH 'Marzo') (DAY '17th') (YEAR '2008'))]",
           "[(DATE (MONTH 'Marzo') (DAY '14th') (YEAR '2008'))]",
           "[(DATE (MONTH 'Marzo') (DAY '13th') (YEAR '2008'))]"});

    verifySnippets(snippetizer, FileUtil.getFile(this.getClass(), "resources/snippet-sample01.html.gz"),
        new int[][] {
          {0, 2, },
          {0, 2, 7, },
          {0, 2, 22, },
          {0, 2, 36, },
          {0, 2, 50, },
          {0, 2, 64, },
        },
        new int[][] {
          {5, 19},
          {20, 33},
          {34, 47},
          {48, 61},
          {62, 75},
          {76, 90},
        },
        new String[] {
          "[(DATE (MONTH 'December') (DAY '28') (YEAR '2005'))]",
          "[(DATE (MONTH 'December') (DAY '26') (YEAR '2005'))]",
          "[(DATE (MONTH 'December') (DAY '23') (YEAR '2005'))]",
          "[(DATE (MONTH 'December') (DAY '20') (YEAR '2005'))]",
          "[(DATE (MONTH 'December') (DAY '18') (YEAR '2005'))]",
          "[(DATE (MONTH 'December') (DAY '13') (YEAR '2005'))]",
        });

    verifySnippets(snippetizer, FileUtil.getFile(this.getClass(), "resources/snippet-sample02.html.gz"),
                   new int[][] {
                     {0, 1, },
                     {0, 1, 6, },
                     {0, 1, 21, },
                     {0, 1, 36, },
                     {0, 1, 51, },
                   },
                   new int[][] {
                     {4, 18},
                     {19, 33},
                     {34, 48},
                     {49, 63},
                     {64, 78},
                   },
                   new String[] {
                     "[(DATE (MONTH 'January') (DAY '1') (YEAR '2006'))]",
                     "[(DATE (MONTH 'December') (DAY '28') (YEAR '2005'))]",
                     "[(DATE (MONTH 'December') (DAY '26') (YEAR '2005'))]",
                     "[(DATE (MONTH 'December') (DAY '18') (YEAR '2005'))]",
                     "[(DATE (MONTH 'December') (DAY '17') (YEAR '2005'))]",
                   });

    verifySnippets(snippetizer, FileUtil.getFile(this.getClass(), "resources/snippet-sample03.html.gz"),
                   new int[][] {
                     {0, 14, },
                     {0, 14, },
                     {0, 14, },
                     {0, 14, },
                     {0, 14, },
                     {0, 14, },
                     {0, 14, },
                     {0, 14, },
                     {0, 14, },
                     {0, 14, },
                     {0, 14, },
                     {0, 14, },
                     {0, 14, },
                     {0, 14, },
                     {0, 14, },
                     {0, 14, },
                     {0, 14, },
                     {0, 14, },
                     {0, 14, },
                     {0, 14, },
                     {0, 14, },
                     {0, 14, },
                     {0, 14, },
                     {0, 14, },
                     {0, 14, },
                     {0, 14, },
                     {0, 14, },
                     {0, 14, },
                     {0, 14, },
                     {0, 14, },
                     {0, 14, },
                     {0, 14, },
                     {0, 14, },
                     {0, 14, },
                     {0, 14, },
                     {0, 14, },
                     {0, 14, },
                     {0, 14, },
                     {0, 14, },
                     {0, 14, },
                     {0, 14, },
                     {0, 14, },
                     {0, 14, },
                     {0, 14, },
                     {0, 14, },
                     {0, 14, },
                     {0, 14, },
                     {0, 14, },
                     {0, 14, },
                     {0, 14, },
                     {0, 14, },
                     {0, 14, },
                     {0, 14, },
                   },
                   new int[][] {
                     {15, 34},
                     {35, 56},
                     {57, 76},
                     {77, 92},
                     {93, 118},
                     {119, 135},
                     {136, 176},
                     {177, 212},
                     {213, 239},
                     {240, 294},
                     {295, 310},
                     {311, 333},
                     {334, 356},
                     {357, 436},
                     {437, 454},
                     {455, 472},
                     {473, 509},
                     {510, 550},
                     {551, 572},
                     {573, 588},
                     {589, 606},
                     {607, 621},
                     {622, 637},
                     {638, 651},
                     {652, 668},
                     {669, 686},
                     {687, 707},
                     {708, 721},
                     {722, 745},
                     {746, 769},
                     {770, 786},
                     {787, 800},
                     {801, 825},
                     {826, 855},
                     {856, 871},
                     {872, 889},
                     {890, 905},
                     {906, 966},
                     {967, 979},
                     {980, 993},
                     {994, 1007},
                     {1008, 1026},
                     {1027, 1040},
                     {1041, 1060},
                     {1061, 1076},
                     {1077, 1094},
                     {1095, 1109},
                     {1110, 1141},
                     {1142, 1166},
                     {1167, 1180},
                     {1181, 1200},
                     {1201, 1244},
                     {1245, 1267},
                   },
                   new String[] {
                     "[(DATE (WEEKDAY 'Thursday') (MONTH 'December') (DAY '29') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Tuesday') (MONTH 'December') (DAY '27') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Monday') (MONTH 'December') (DAY '12') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Thursday') (MONTH 'November') (DAY '24') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Monday') (MONTH 'November') (DAY '21') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Saturday') (MONTH 'November') (DAY '19') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Friday') (MONTH 'November') (DAY '18') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Wednesday') (MONTH 'November') (DAY '16') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Monday') (MONTH 'November') (DAY '14') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Sunday') (MONTH 'November') (DAY '06') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Wednesday') (MONTH 'November') (DAY '02') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Saturday') (MONTH 'October') (DAY '29') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Tuesday') (MONTH 'October') (DAY '25') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Monday') (MONTH 'October') (DAY '24') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Sunday') (MONTH 'October') (DAY '23') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Saturday') (MONTH 'October') (DAY '22') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Friday') (MONTH 'October') (DAY '21') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Thursday') (MONTH 'October') (DAY '20') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Wednesday') (MONTH 'October') (DAY '19') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Tuesday') (MONTH 'October') (DAY '18') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Monday') (MONTH 'October') (DAY '17') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Sunday') (MONTH 'October') (DAY '16') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Saturday') (MONTH 'October') (DAY '15') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Friday') (MONTH 'October') (DAY '14') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Thursday') (MONTH 'October') (DAY '13') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Wednesday') (MONTH 'October') (DAY '12') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Tuesday') (MONTH 'October') (DAY '11') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Monday') (MONTH 'October') (DAY '10') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Sunday') (MONTH 'October') (DAY '09') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Saturday') (MONTH 'October') (DAY '08') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Friday') (MONTH 'October') (DAY '07') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Thursday') (MONTH 'October') (DAY '06') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Wednesday') (MONTH 'October') (DAY '05') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Tuesday') (MONTH 'October') (DAY '04') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Monday') (MONTH 'October') (DAY '03') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Sunday') (MONTH 'October') (DAY '02') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Saturday') (MONTH 'October') (DAY '01') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Friday') (MONTH 'September') (DAY '30') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Thursday') (MONTH 'September') (DAY '29') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Wednesday') (MONTH 'September') (DAY '28') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Tuesday') (MONTH 'September') (DAY '27') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Monday') (MONTH 'September') (DAY '26') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Sunday') (MONTH 'September') (DAY '25') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Saturday') (MONTH 'September') (DAY '24') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Friday') (MONTH 'September') (DAY '23') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Thursday') (MONTH 'September') (DAY '22') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Wednesday') (MONTH 'September') (DAY '21') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Tuesday') (MONTH 'September') (DAY '20') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Monday') (MONTH 'September') (DAY '19') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Sunday') (MONTH 'September') (DAY '18') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Saturday') (MONTH 'September') (DAY '17') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Friday') (MONTH 'September') (DAY '16') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Thursday') (MONTH 'September') (DAY '15') (YEAR '2005'))]",
                   });

    verifySnippets(snippetizer, FileUtil.getFile(this.getClass(), "resources/snippet-sample04.html.gz"),
                   new int[][] {
                     {0, },
                     {0, 2, },
                     {0, 2, },
                     {0, 2, },
                     {0, 2, },
                     {0, 2, },
                     {0, 2, },
                     {0, 2, },
                     {0, 2, },
                     {0, 2, },
                     {0, 2, },
                     {0, 2, },
                   },
                   new int[][] {
                     {2, 13},
                     {14, 88},
                     {89, 112},
                     {113, 140},
                     {141, 177},
                     {178, 211},
                     {212, 235},
                     {236, 252},
                     {253, 269},
                     {270, 284},
                     {285, 307},
                     {308, 322},
                   },
                   new String[] {
                     "[(DATE (WEEKDAY 'Saturday') (MONTH 'December') (DAY '31st') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Thursday') (MONTH 'December') (DAY '29th') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Tuesday') (MONTH 'December') (DAY '27th') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Thursday') (MONTH 'December') (DAY '22nd') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Wednesday') (MONTH 'December') (DAY '21st') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Tuesday') (MONTH 'December') (DAY '20th') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Monday') (MONTH 'December') (DAY '19th') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Sunday') (MONTH 'December') (DAY '18th') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Saturday') (MONTH 'December') (DAY '17th') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Friday') (MONTH 'December') (DAY '16th') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Wednesday') (MONTH 'December') (DAY '14th') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Tuesday') (MONTH 'December') (DAY '13th') (YEAR '2005'))]",
                   });

    verifySnippets(snippetizer, FileUtil.getFile(this.getClass(), "resources/snippet-sample05.html.gz"),
                   new int[][] {
                     {1, },
                     {1, 21, },
                     {1, 42, },
                     {1, 67, },
                     {1, 90, },
                     {1, 117, },
                     {1, 133, },
                     {1, 167, },
                     {1, 196, },
                     {1, 217, },
                     {1, 245, },
                     {1, 285, },
                     {1, 307, },
                     {1, 331, },
                     {1, 353, },
                     {1, 377, },
                     {1, 406, },
                     {1, 435, },
                     {1, 467, },
                     {1, 497, },
                   },
                   new int[][] {
                     {2, 22},
                     {23, 43},
                     {44, 68},
                     {69, 91},
                     {92, 118},
                     {119, 134},
                     {135, 168},
                     {169, 197},
                     {198, 218},
                     {219, 246},
                     {247, 286},
                     {287, 308},
                     {309, 332},
                     {333, 354},
                     {355, 378},
                     {379, 407},
                     {408, 436},
                     {437, 468},
                     {469, 498},
                     {499, 525},
                   },
                   new String[] {
                     "[(DATE_TIME (TIME (HOUR '12') (MINUTE '08am')) (DATE (DAY '28') (MONTH '12') (YEAR '2006')))]",
                     "[(DATE_TIME (TIME (HOUR '11') (MINUTE '32pm')) (DATE (DAY '27') (MONTH '12') (YEAR '2006')))]",
                     "[(DATE_TIME (TIME (HOUR '10') (MINUTE '24am')) (DATE (DAY '26') (MONTH '12') (YEAR '2006')))]",
                     "[(DATE_TIME (TIME (HOUR '10') (MINUTE '25pm')) (DATE (DAY '19') (MONTH '12') (YEAR '2006')))]",
                     "[(DATE_TIME (TIME (HOUR '12') (MINUTE '00am')) (DATE (DAY '16') (MONTH '12') (YEAR '2006')))]",
                     "[(DATE_TIME (TIME (HOUR '08') (MINUTE '37pm')) (DATE (DAY '13') (MONTH '12') (YEAR '2006')))]",
                     "[(DATE_TIME (TIME (HOUR '11') (MINUTE '07pm')) (DATE (MONTH '02') (DAY '12') (YEAR '2006'))), (DATE_TIME (TIME (HOUR '11') (MINUTE '07pm')) (DATE (DAY '02') (MONTH '12') (YEAR '2006')))]",
                     "[(DATE_TIME (TIME (HOUR '12') (MINUTE '50pm')) (DATE (DAY '28') (MONTH '11') (YEAR '2006')))]",
                     "[(DATE_TIME (TIME (HOUR '08') (MINUTE '42pm')) (DATE (DAY '21') (MONTH '11') (YEAR '2006')))]",
                     "[(DATE_TIME (TIME (HOUR '01') (MINUTE '17am')) (DATE (DAY '21') (MONTH '11') (YEAR '2006')))]",
                     "[(DATE_TIME (TIME (HOUR '07') (MINUTE '45pm')) (DATE (DAY '20') (MONTH '11') (YEAR '2006')))]",
                     "[(DATE_TIME (TIME (HOUR '01') (MINUTE '05pm')) (DATE (DAY '13') (MONTH '11') (YEAR '2006')))]",
                     "[(DATE_TIME (TIME (HOUR '10') (MINUTE '36pm')) (DATE (MONTH '07') (DAY '11') (YEAR '2006'))), (DATE_TIME (TIME (HOUR '10') (MINUTE '36pm')) (DATE (DAY '07') (MONTH '11') (YEAR '2006')))]",
                     "[(DATE_TIME (TIME (HOUR '11') (MINUTE '48pm')) (DATE (DAY '29') (MONTH '10') (YEAR '2006')))]",
                     "[(DATE_TIME (TIME (HOUR '11') (MINUTE '35pm')) (DATE (DAY '20') (MONTH '10') (YEAR '2006')))]",
                     "[(DATE_TIME (TIME (HOUR '02') (MINUTE '50pm')) (DATE (DAY '15') (MONTH '10') (YEAR '2006')))]",
                     "[(DATE_TIME (TIME (HOUR '10') (MINUTE '22pm')) (DATE (DAY '13') (MONTH '10') (YEAR '2006')))]",
                     "[(DATE_TIME (TIME (HOUR '10') (MINUTE '12pm')) (DATE (MONTH '10') (DAY '10') (YEAR '2006'))), (DATE_TIME (TIME (HOUR '10') (MINUTE '12pm')) (DATE (DAY '10') (MONTH '10') (YEAR '2006')))]",
                     "[(DATE_TIME (TIME (HOUR '06') (MINUTE '50pm')) (DATE (DAY '25') (MONTH '09') (YEAR '2006')))]",
                     "[(DATE_TIME (TIME (HOUR '10') (MINUTE '48pm')) (DATE (DAY '18') (MONTH '09') (YEAR '2006')))]",
                   });

    // todo: fix this when we can interpret the dates!
    verifySnippets(snippetizer, FileUtil.getFile(this.getClass(), "resources/snippet-sample06.html.gz"),
                   new int[][] {
                     {0, 6, },
                     {0, 6, },
                     {0, 6, },
                     {0, 6, },
                   },
                   new int[][] {
                     {78, 166},
                     {167, 281},
                     {282, 348},
                     {349, 367},
                   },
                   new String[] {
                     "[(DATE (YEAR '2005') (MONTH '12') (DAY '29'))]",
                     "[(DATE (YEAR '2005') (MONTH '12') (DAY '27'))]",
                     "[(DATE (YEAR '2005') (MONTH '12') (DAY '26'))]",
                     "[(DATE (YEAR '2005') (MONTH '12') (DAY '25'))]",
                   });

    verifySnippets(snippetizer, FileUtil.getFile(this.getClass(), "resources/snippet-sample07.html.gz"),
                   new int[][] {
                     {0, 40, },
                     {0, 57, },
                     {0, 57, },
                     {0, 57, },
                   },
                   new int[][] {
                     {44, 79},
                     {80, 103},
                     {104, 131},
                     {132, 165},
                   },
                   new String[] {
                     "[(DATE_TIME (DATE (MONTH 'December') (DAY '29') (YEAR '2005')) (TIME (HOUR '9') (MINUTE '45') (SECOND '58') (AMPM 'PM') (TZ 'EST')))]",
                     "[(DATE_TIME (DATE (MONTH 'December') (DAY '28') (YEAR '2005')) (TIME (HOUR '8') (MINUTE '18') (SECOND '46') (AMPM 'AM') (TZ 'EST')))]",
                     "[(DATE_TIME (DATE (MONTH 'December') (DAY '27') (YEAR '2005')) (TIME (HOUR '2') (MINUTE '26') (SECOND '33') (AMPM 'PM') (TZ 'EST')))]",
                     "[(DATE_TIME (DATE (MONTH 'December') (DAY '27') (YEAR '2005')) (TIME (HOUR '11') (MINUTE '25') (SECOND '34') (AMPM 'AM') (TZ 'EST')))]",
                   });

    verifySnippets(snippetizer, FileUtil.getFile(this.getClass(), "resources/snippet-sample08.html.gz"),
                   new int[][] {
                     {0, },
                   },
                   new int[][] {
                     {5, 93},
                   },
                   new String[] {
                     "[(DATE (MONTH 'December') (DAY '31') (YEAR '2005'))]",
                   });

    verifySnippets(snippetizer, FileUtil.getFile(this.getClass(), "resources/snippet-sample09.html.gz"),
                   new int[][] {
                     {0, },
                     {0, 2, },
                     {0, 2, },
                     {0, 2, },
                     {0, 2, },
                     {0, 2, },
                     {0, 2, },
                     {0, 2, },
                     {0, 2, },
                     {0, 2, },
                     {0, 2, },
                     {0, 2, },
                     {0, 2, },
                     {0, 2, },
                     {0, 2, },
                     {0, 2, },
                     {0, 2, },
                     {0, 2, },
                   },
                   new int[][] {
                     {2, 11},
                     {12, 38},
                     {39, 49},
                     {50, 91},
                     {92, 131},
                     {132, 150},
                     {151, 170},
                     {171, 218},
                     {219, 247},
                     {248, 258},
                     {259, 338},
                     {339, 369},
                     {370, 429},
                     {430, 465},
                     {466, 520},
                     {521, 587},
                     {588, 626},
                     {627, 676},
                   },
                   new String[] {
                     "[(DATE (WEEKDAY 'Sunday') (MONTH 'January') (DAY '1st') (YEAR '2006'))]",
                     "[(DATE (WEEKDAY 'Tuesday') (MONTH 'December') (DAY '27th') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Sunday') (MONTH 'December') (DAY '25th') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Friday') (MONTH 'December') (DAY '23rd') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Thursday') (MONTH 'December') (DAY '22nd') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Wednesday') (MONTH 'December') (DAY '21st') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Sunday') (MONTH 'December') (DAY '18th') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Friday') (MONTH 'December') (DAY '16th') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Tuesday') (MONTH 'December') (DAY '13th') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Saturday') (MONTH 'December') (DAY '10th') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Friday') (MONTH 'December') (DAY '9th') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Thursday') (MONTH 'December') (DAY '8th') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Tuesday') (MONTH 'December') (DAY '6th') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Monday') (MONTH 'December') (DAY '5th') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Friday') (MONTH 'December') (DAY '2nd') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Sunday') (MONTH 'November') (DAY '27th') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Sunday') (MONTH 'November') (DAY '20th') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Saturday') (MONTH 'November') (DAY '19th') (YEAR '2005'))]",
                   });

    verifySnippets(snippetizer, FileUtil.getFile(this.getClass(), "resources/snippet-sample10.html.gz"),
                   new int[][] {
                     {0, },
                     {0, },
                   },
                   new int[][] {
                     {2, 3},
                     {4, 58},
                   },
                   new String[] {
                     "",
                     "[(DATE_TIME (DATE (WEEKDAY 'Friday') (MONTH 'Dec.') (DAY '30') (YEAR '2005')) (TIME (PREP 'at') (TIME (HOUR '9') (MINUTE '11') (AMPM 'pm'))))]",
                   });

    verifySnippets(snippetizer, FileUtil.getFile(this.getClass(), "resources/snippet-sample11.html.gz"),
                   new int[][] {
                     {0, 3, },
                     {0, 3, },
                     {0, 3, },
                     {0, 3, },
                     {0, 3, },
                     {0, 3, },
                     {0, 3, },
                     {0, 3, },
                     {0, 3, },
                     {0, 3, },
                   },
                   new int[][] {
                     {4, 28},
                     {29, 54},
                     {55, 68},
                     {69, 108},
                     {109, 121},
                     {122, 166},
                     {167, 198},
                     {199, 224},
                     {225, 254},
                     {255, 267},
                   },
                   new String[] {
                     "[(DATE (YEAR '01') (MONTH 'Jan')), (DATE_TIME (DATE (DAY '01') (MONTH 'Jan') (YEAR '2006')) (TIME (HOUR '01') (MINUTE '12') (AMPM 'am')))]",
                     "[(DATE_TIME (DATE (DAY '31') (MONTH 'Dec') (YEAR '2005')) (TIME (HOUR '07') (MINUTE '17') (AMPM 'am')))]",
                     "[(DATE_TIME (DATE (DAY '30') (MONTH 'Dec') (YEAR '2005')) (TIME (HOUR '01') (MINUTE '15') (AMPM 'pm')))]",
                     "[(DATE_TIME (DATE (DAY '29') (MONTH 'Dec') (YEAR '2005')) (TIME (HOUR '07') (MINUTE '20') (AMPM 'pm')))]",
                     "[(DATE_TIME (DATE (DAY '29') (MONTH 'Dec') (YEAR '2005')) (TIME (HOUR '07') (MINUTE '17') (AMPM 'am')))]",
                     "[(DATE_TIME (DATE (DAY '28') (MONTH 'Dec') (YEAR '2005')) (TIME (HOUR '01') (MINUTE '16') (AMPM 'pm')))]",
                     "[(DATE_TIME (DATE (DAY '27') (MONTH 'Dec') (YEAR '2005')) (TIME (HOUR '07') (MINUTE '20') (AMPM 'pm')))]",
                     "[(DATE_TIME (DATE (DAY '27') (MONTH 'Dec') (YEAR '2005')) (TIME (HOUR '07') (MINUTE '20') (AMPM 'am')))]",
                     "[(DATE_TIME (DATE (DAY '26') (MONTH 'Dec') (YEAR '2005')) (TIME (HOUR '07') (MINUTE '13') (AMPM 'pm')))]",
                     "[(DATE_TIME (DATE (DAY '22') (MONTH 'Dec') (YEAR '2005')) (TIME (HOUR '07') (MINUTE '09') (AMPM 'pm')))]",
                   });

    verifySnippets(snippetizer, FileUtil.getFile(this.getClass(), "resources/snippet-sample12.html.gz"),
                   new int[][] {
                     {0, },
                   },
                   new int[][] {
                     {3, 124},
                   },
                   new String[] {
                     "[(DATE (MONTH 'Dec.') (DAY '27') (YEAR '2005'))]",
                   });

    verifySnippets(snippetizer, FileUtil.getFile(this.getClass(), "resources/snippet-sample13.html.gz"),
                   new int[][] {
                     {0, 14, },
                     {0, 14, },
                     {0, 14, },
                     {0, 14, },
                     {0, 14, },
                     {0, 14, },
                     {0, 14, },
                   },
                   new int[][] {
                     {61, 661},
                     {662, 1472},
                     {1473, 2273},
                     {2274, 3124},
                     {3125, 3955},
                     {3956, 4756},
                     {4757, 5587},
                   },
                   new String[] {
                     "[(DATE (WEEKDAY 'Tuesday') (MONTH 'April') (DAY '05') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Monday') (MONTH 'April') (DAY '04') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Sunday') (MONTH 'April') (DAY '03') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Saturday') (MONTH 'April') (DAY '02') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Friday') (MONTH 'April') (DAY '01') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Thursday') (MONTH 'March') (DAY '31') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Wednesday') (MONTH 'March') (DAY '30') (YEAR '2005'))]",
                   });

    verifySnippets(snippetizer, FileUtil.getFile(this.getClass(), "resources/snippet-sample14.html.gz"),
                   new int[][] {
                     {0, 41, },
                     {0, 75, },
                     {0, 75, },
                   },
                   new int[][] {
                     {59, 97},
                     {98, 161},
                     {162, 184},
                   },
                   new String[] {
                     "[(DATE (WEEKDAY 'Wednesday') (MONTH 'December') (DAY '28') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Tuesday') (MONTH 'December') (DAY '27') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Friday') (MONTH 'December') (DAY '23') (YEAR '2005'))]",
                   });

    verifySnippets(snippetizer, FileUtil.getFile(this.getClass(), "resources/snippet-sample15.html.gz"),
                   new int[][] {
                     {0, 14, },
                     {0, 14, 18, },
                     {0, 14, 115, },
                     {0, 14, 146, },
                     {0, 14, 169, },
                     {0, 14, 195, },
                     {0, 14, 214, },
                   },
                   new int[][] {
                     {16, 112},
                     {113, 143},
                     {144, 166},
                     {167, 192},
                     {193, 211},
                     {212, 243},
                     {244, 271},
                   },
                   new String[] {
                     "[(DATE (WEEKDAY 'Saturday') (MONTH 'December') (DAY '31') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Friday') (MONTH 'December') (DAY '30') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Wednesday') (MONTH 'December') (DAY '28') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Monday') (MONTH 'December') (DAY '26') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Saturday') (MONTH 'December') (DAY '24') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Thursday') (MONTH 'December') (DAY '15') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Monday') (MONTH 'December') (DAY '12') (YEAR '2005'))]",
                   });

    verifySnippets(snippetizer, FileUtil.getFile(this.getClass(), "resources/snippet-sample16.html.gz"),
                   new int[][] {
                     {1, 12, },
                   },
                   new int[][] {
                     {88, 194},
                   },
                   new String[] {
                     "[(DATE (YEAR '02') (MONTH 'December')), (DATE_TIME (DATE (DAY '02') (MONTH 'December') (YEAR '2007')) (TIME (HOUR '11') (MINUTE '05') (AMPM 'pm')))]",
                   });

    verifySnippets(snippetizer, FileUtil.getFile(this.getClass(), "resources/snippet-sample17.html.gz"),
                   new int[][] {
                     {0, },
                     {0, },
                     {0, },
                     {0, },
                     {0, },
                   },
                   new int[][] {
                     {11, 44},
                     {45, 87},
                     {88, 125},
                     {126, 167},
                     {168, 220},
                   },
                   new String[] {
                     "[(DATE (WEEKDAY 'Thursday') (MONTH 'March') (DAY '6') (YEAR '2008'))]",
                     "[(DATE (WEEKDAY 'Wednesday') (MONTH 'March') (DAY '5') (YEAR '2008'))]",
                     "[(DATE (WEEKDAY 'Wednesday') (MONTH 'February') (DAY '27') (YEAR '2008'))]",
                     "[(DATE (WEEKDAY 'Monday') (MONTH 'February') (DAY '18') (YEAR '2008'))]",
                     "[(DATE (WEEKDAY 'Tuesday') (MONTH 'February') (DAY '12') (YEAR '2008'))]",
                   });

    verifySnippets(snippetizer, FileUtil.getFile(this.getClass(), "resources/snippet-sample18.html.gz"),
                   new int[][] {
                     {1, 4, },
                   },
                   new int[][] {
                     {6, 118},
                   },
                   new String[] {
                     "[(DATE (MONTH 'February') (DAY '24') (YEAR '2008'))]",
                   });

/*
//just keeping this around for editing ease when we add more files...
                   new int[][] {
                   },
                   new int[][] {
                   },
                   new String[] {
                   });
*/
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestBlogSnippetExtractor.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
