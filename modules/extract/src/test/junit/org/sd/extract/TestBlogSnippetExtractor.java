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
          {2, },
          {2, },
          {2, },
          {2, },
          {2, }},
        new int [][] {
          {29, 37},
          {38, 47},
          {48, 61},
          {62, 73},
          {74, 86}},
        new String[] {
          "[(DATE_TIME (DATE (WEEKDAY 'lundi') (DAY '4') (MONTH 'février') (YEAR '2008')) (TIME (HOUR '12') (MINUTE '38 -')))]",
          "[(DATE_TIME (DATE (WEEKDAY 'jeudi') (DAY '31') (MONTH 'janvier') (YEAR '2008')) (TIME (HOUR '13') (MINUTE '55 -')))]",
          "[(DATE_TIME (DATE (WEEKDAY 'samedi') (DAY '1') (MONTH 'décembre') (YEAR '2007')) (TIME (HOUR '13') (MINUTE '06 -')))]",
          "[(DATE_TIME (DATE (WEEKDAY 'samedi') (DAY '17') (MONTH 'novembre') (YEAR '2007')) (TIME (HOUR '08') (MINUTE '04 -')))]",
          "[(DATE_TIME (DATE (WEEKDAY 'mercredi') (DAY '7') (MONTH 'novembre') (YEAR '2007')) (TIME (HOUR '17') (MINUTE '36 -')))]"});

    verifySnippets(snippetizer, FileUtil.getFile(this.getClass(), "resources/cache/cache-9999/ad/adwords-de.blogspot.html.gz"),
        new int[][] {
          {8, },
          {8, },
          {8, 58, }},
        new int [][] {
          {27, 57},
          {58, 66},
          {67, 94}},
        new String[] {
          "",
          "[(DATE_TIME (DATE (WEEKDAY 'Donnerstag') (DAY '13.') (MONTH 'März') (YEAR '2008')) (TIME (PREP 'at') (TIME (HOUR '16') (MINUTE '52') (SECOND '00'))))]",
          "[(DATE_TIME (DATE (WEEKDAY 'Montag') (DAY '10.') (MONTH 'März') (YEAR '2008')) (TIME (PREP 'at') (TIME (HOUR '17') (MINUTE '00') (SECOND '00'))))]"});

    verifySnippets(snippetizer, FileUtil.getFile(this.getClass(), "resources/cache/cache-9999/bl/blogdeviajes.com.ar.html.gz"),
        new int[][] {
           {1, 17, },
           {1, },
           {1, },
           {1, },
           {1, },
           {1, },
           {1, }},
        new int [][] {
           {19, 26},
           {27, 92},
           {93, 117},
           {118, 167},
           {168, 218},
           {219, 253},
           {254, 298}},
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
          {5, 7, },
          {5, 7, 12, },
          {5, 7, 26, },
          {5, 7, 40, },
          {5, 7, 54, },
          {5, 7, 68, },
          },
        new int[][] {
          {10, 23},
          {24, 37},
          {38, 51},
          {52, 65},
          {66, 79},
          {80, 93},
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
                     {0, 26, },
                     {0, 26, },
                     {0, 26, },
                     {0, 26, },
                     {0, 26, },
                     {0, 26, },
                     {0, 26, },
                     {0, 26, },
                     {0, 26, },
                     {0, 26, },
                     {0, 26, },
                     {0, 26, },
                     {0, 26, },
                     {0, 26, },
                     {0, 26, },
                     {0, 26, },
                     {0, 26, },
                     {0, 26, },
                     {0, 26, },
                     {0, 26, },
                     {0, 26, },
                     {0, 26, },
                     {0, 26, },
                     {0, 26, },
                     {0, 26, },
                     {0, 26, },
                     {0, 26, },
                     {0, 26, },
                     {0, 26, },
                     {0, 26, },
                     {0, 26, },
                     {0, 26, },
                     {0, 26, },
                     {0, 26, },
                     {0, 26, },
                     {0, 26, },
                     {0, 26, },
                     {0, 26, },
                     {0, 26, },
                     {0, 26, },
                     {0, 26, },
                     {0, 26, },
                     {0, 26, },
                     {0, 26, },
                     {0, 26, },
                     {0, 26, },
                     {0, 26, },
                     {0, 26, },
                     {0, 26, },
                     {0, 26, },
                     {0, 26, },
                     {0, 26, },
                     {0, 26, },
                   },
                   new int[][] {
                     {27, 55},
                     {56, 88},
                     {89, 113},
                     {114, 134},
                     {135, 169},
                     {170, 188},
                     {189, 249},
                     {250, 310},
                     {311, 351},
                     {352, 421},
                     {422, 442},
                     {443, 481},
                     {482, 516},
                     {517, 624},
                     {625, 649},
                     {650, 670},
                     {671, 732},
                     {733, 802},
                     {803, 829},
                     {830, 850},
                     {851, 876},
                     {877, 896},
                     {897, 917},
                     {918, 934},
                     {935, 956},
                     {957, 976},
                     {977, 1007},
                     {1008, 1024},
                     {1025, 1057},
                     {1058, 1088},
                     {1089, 1111},
                     {1112, 1128},
                     {1129, 1164},
                     {1165, 1200},
                     {1201, 1218},
                     {1219, 1241},
                     {1242, 1260},
                     {1261, 1336},
                     {1337, 1351},
                     {1352, 1368},
                     {1369, 1385},
                     {1386, 1410},
                     {1411, 1427},
                     {1428, 1456},
                     {1457, 1477},
                     {1478, 1500},
                     {1501, 1519},
                     {1520, 1568},
                     {1569, 1601},
                     {1602, 1621},
                     {1622, 1650},
                     {1651, 1712},
                     {1713, 1746},
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
                     {0, 11, },
                     {0, 11, },
                     {0, 11, },
                     {0, 11, },
                     {0, 11, },
                     {0, 11, },
                     {0, 11, },
                     {0, 11, },
                     {0, 11, },
                     {0, 11, },
                     {0, 11, },
                   },
                   new int[][] {
                     {11, 21},
                     {22, 93},
                     {94, 116},
                     {117, 143},
                     {144, 179},
                     {180, 211},
                     {212, 234},
                     {235, 252},
                     {253, 270},
                     {271, 284},
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
                     {9, },
                     {9, 30, },
                     {9, 55, },
                     {9, 86, },
                     {9, 113, },
                     {9, 148, },
                     {9, 164, },
                     {9, 206, },
                     {9, 246, },
                     {9, 276, },
                     {9, 315, },
                     {9, 361, },
                     {9, 388, },
                     {9, 413, },
                     {9, 439, },
                     {9, 471, },
                     {9, 503, },
                     {9, 538, },
                     {9, 575, },
                     {9, 611, },
                   },
                   new int[][] {
                     {10, 31},
                     {32, 56},
                     {57, 87},
                     {88, 114},
                     {115, 149},
                     {150, 165},
                     {166, 207},
                     {208, 247},
                     {248, 277},
                     {278, 316},
                     {317, 362},
                     {363, 389},
                     {390, 414},
                     {415, 440},
                     {441, 472},
                     {473, 504},
                     {505, 539},
                     {540, 576},
                     {577, 612},
                     {613, 645},
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
                     {2, 11, },
                     {2, 11, },
                     {2, 11, },
                     {2, 11, },
                   },
                   new int[][] {
                     {83, 171},
                     {172, 286},
                     {287, 353},
                     {354, 372},
                   },
                   new String[] {
                     "[(DATE (YEAR '2005') (MONTH '12') (DAY '29'))]",
                     "[(DATE (YEAR '2005') (MONTH '12') (DAY '27'))]",
                     "[(DATE (YEAR '2005') (MONTH '12') (DAY '26'))]",
                     "[(DATE (YEAR '2005') (MONTH '12') (DAY '25'))]",
                   });

    verifySnippets(snippetizer, FileUtil.getFile(this.getClass(), "resources/snippet-sample07.html.gz"),
                   new int[][] {
                     {2, 56, },
                     {2, 56, },
                     {2, 56, },
                     {2, 56, },
                   },
                   new int[][] {
                     {61, 96},
                     {97, 121},
                     {122, 151},
                     {152, 186},
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
                     {10, 110},
                   },
                   new String[] {
                     "[(DATE (MONTH 'December') (DAY '31') (YEAR '2005'))]",
                   });

    verifySnippets(snippetizer, FileUtil.getFile(this.getClass(), "resources/snippet-sample09.html.gz"),
                   new int[][] {
                     {0, },
                     {0, 10, },
                     {0, 10, },
                     {0, 10, },
                     {0, 10, },
                     {0, 10, },
                     {0, 10, },
                     {0, 10, },
                     {0, 10, },
                     {0, 10, },
                     {0, 10, },
                     {0, 10, },
                     {0, 10, },
                     {0, 10, },
                     {0, 10, },
                     {0, 10, },
                     {0, 10, },
                     {0, 10, },
                   },
                   new int[][] {
                     {10, 19},
                     {20, 50},
                     {51, 62},
                     {63, 112},
                     {113, 164},
                     {165, 186},
                     {187, 207},
                     {208, 259},
                     {260, 294},
                     {295, 305},
                     {306, 398},
                     {399, 431},
                     {432, 496},
                     {497, 544},
                     {545, 603},
                     {604, 677},
                     {678, 719},
                     {720, 777},
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
                     {1, },
                     {1, },
                   },
                   new int[][] {
                     {3, 5},
                     {6, 75},
                   },
                   new String[] {
                     "",
                     "[(DATE_TIME (DATE (WEEKDAY 'Friday') (MONTH 'Dec.') (DAY '30') (YEAR '2005')) (TIME (PREP 'at') (TIME (HOUR '9') (MINUTE '11') (AMPM 'pm'))))]",
                   });

    verifySnippets(snippetizer, FileUtil.getFile(this.getClass(), "resources/snippet-sample11.html.gz"),
                   new int[][] {
                     {0, 10, },
                     {0, 10, },
                     {0, 10, },
                     {0, 10, },
                     {0, 10, },
                     {0, 10, },
                     {0, 10, },
                     {0, 10, },
                     {0, 10, },
                     {0, 10, },
                   },
                   new int[][] {
                     {11, 39},
                     {40, 72},
                     {73, 89},
                     {90, 136},
                     {137, 153},
                     {154, 206},
                     {207, 247},
                     {248, 280},
                     {281, 315},
                     {316, 330},
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
                     {4, 164},
                   },
                   new String[] {
                     "[(DATE (MONTH 'Dec.') (DAY '27') (YEAR '2005'))]",
                   });

    verifySnippets(snippetizer, FileUtil.getFile(this.getClass(), "resources/snippet-sample13.html.gz"),
                   new int[][] {
                     {0, 26, },
                     {0, 26, },
                     {0, 26, },
                     {0, 26, },
                     {0, 26, },
                     {0, 26, },
                     {0, 26, },
                   },
                   new int[][] {
                     {73, 733},
                     {734, 1625},
                     {1626, 2506},
                     {2507, 3442},
                     {3443, 4356},
                     {4357, 5237},
                     {5238, 6151},
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
                     {0, 56, },
                     {0, 93, },
                     {0, 93, },
                   },
                   new int[][] {
                     {77, 115},
                     {116, 183},
                     {184, 206},
                   },
                   new String[] {
                     "[(DATE (WEEKDAY 'Wednesday') (MONTH 'December') (DAY '28') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Tuesday') (MONTH 'December') (DAY '27') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Friday') (MONTH 'December') (DAY '23') (YEAR '2005'))]",
                   });

    verifySnippets(snippetizer, FileUtil.getFile(this.getClass(), "resources/snippet-sample15.html.gz"),
                   new int[][] {
                     {0, 26, },
                     {0, 26, 30, },
                     {0, 26, 160, },
                     {0, 26, 207, },
                     {0, 26, 238, },
                     {0, 26, 276, },
                     {0, 26, 304, },
                   },
                   new int[][] {
                     {28, 157},
                     {158, 204},
                     {205, 235},
                     {236, 273},
                     {274, 301},
                     {302, 345},
                     {346, 373},
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
                     {7, 23, },
                   },
                   new int[][] {
                     {102, 288},
                   },
                   new String[] {
                     "[(DATE (YEAR '02') (MONTH 'December')), (DATE_TIME (DATE (DAY '02') (MONTH 'December') (YEAR '2007')) (TIME (HOUR '11') (MINUTE '05') (AMPM 'pm')))]",
                   });

    verifySnippets(snippetizer, FileUtil.getFile(this.getClass(), "resources/snippet-sample17.html.gz"),
                   new int[][] {
                     {9, },
                     {9, },
                     {9, },
                     {9, },
                     {9, },
                   },
                   new int[][] {
                     {21, 56},
                     {57, 123},
                     {124, 162},
                     {163, 225},
                     {226, 288},
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
                     {6, 9, },
                   },
                   new int[][] {
                     {11, 168},
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
