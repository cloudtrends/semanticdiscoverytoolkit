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

import org.sd.extract.datetime.DateTimeInterpreter;
import org.sd.io.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.GregorianCalendar;

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
    DateTimeInterpreter.setDefaultCalendar(new GregorianCalendar(2010, 6, 1));
    final BlogSnippetExtractor snippetizer = new BlogSnippetExtractor(2000);

    verifySnippets(snippetizer, FileUtil.getFile(this.getClass(), "resources/cache/cache-9999/bl/blog.maudoune.com.html.gz"),
        new int[][] {
          {2, },
          {2, },
          {2, },
          {2, },
          {2, }},
        new int [][] {
          {27, 35},
          {36, 45},
          {46, 59},
          {60, 71},
          {72, 84}},
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
          {8, 59, }},
        new int [][] {
          {24, 58},
          {59, 67},
          {68, 100}},
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
           {27, 93},
           {94, 120},
           {121, 171},
           {172, 223},
           {224, 259},
           {260, 305}},
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
          {5, 6, },
          {5, 6, 11, },
          {5, 6, 28, },
          {5, 6, 44, },
          {5, 6, 60, },
          {5, 6, 76, },
          },
        new int[][] {
          {9, 25},
          {26, 41},
          {42, 57},
          {58, 73},
          {74, 89},
          {90, 106},
        },
        new String[] {
          "[(DATE (MONTH 'December') (DAY '28') (YEAR '2005'))]",
          "[(DATE (MONTH 'December') (DAY '26') (YEAR '2005'))]",
          "[(DATE (MONTH 'December') (DAY '23') (YEAR '2005'))]",
          "[(DATE (MONTH 'December') (DAY '20') (YEAR '2005'))]",
          "[(DATE (MONTH 'December') (DAY '18') (YEAR '2005'))]",
          "[(DATE (MONTH 'December') (YEAR '13')), (DATE (MONTH 'December') (DAY '13') (YEAR '2005'))]",
          });

    verifySnippets(snippetizer, FileUtil.getFile(this.getClass(), "resources/snippet-sample02.html.gz"),
                   new int[][] {
                     {0, 1, },
                     {0, 1, 6, },
                     {0, 1, 24, },
                     {0, 1, 42, },
                     {0, 1, 60, },
                     },
                   new int[][] {
                     {4, 21},
                     {22, 39},
                     {40, 57},
                     {58, 75},
                     {76, 93},
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
                     {0, 24, },
                     {0, 24, },
                     {0, 24, },
                     {0, 24, },
                     {0, 24, },
                     {0, 24, },
                     {0, 24, },
                     {0, 24, },
                     {0, 24, },
                     {0, 24, },
                     {0, 24, },
                     {0, 24, },
                     {0, 24, },
                     {0, 24, },
                     {0, 24, },
                     {0, 24, },
                     {0, 24, },
                     {0, 24, },
                     {0, 24, },
                     {0, 24, },
                     {0, 24, },
                     {0, 24, },
                     {0, 24, },
                     {0, 24, },
                     {0, 24, },
                     {0, 24, },
                     {0, 24, },
                     {0, 24, },
                     {0, 24, },
                     {0, 24, },
                     {0, 24, },
                     {0, 24, },
                     {0, 24, },
                     {0, 24, },
                     {0, 24, },
                     {0, 24, },
                     {0, 24, },
                     {0, 24, },
                     {0, 24, },
                     {0, 24, },
                     {0, 24, },
                     {0, 24, },
                     {0, 24, },
                     {0, 24, },
                     {0, 24, },
                     {0, 24, },
                     {0, 24, },
                     {0, 24, },
                     {0, 24, },
                     {0, 24, },
                     {0, 24, },
                     {0, 24, },
                     {0, 24, },
                   },
                   new int[][] {
                     {25, 62},
                     {63, 106},
                     {107, 136},
                     {137, 162},
                     {163, 204},
                     {205, 225},
                     {226, 300},
                     {301, 374},
                     {375, 425},
                     {426, 511},
                     {512, 537},
                     {538, 586},
                     {587, 633},
                     {634, 768},
                     {769, 800},
                     {801, 824},
                     {825, 911},
                     {912, 1011},
                     {1012, 1043},
                     {1044, 1069},
                     {1070, 1101},
                     {1102, 1125},
                     {1126, 1151},
                     {1152, 1171},
                     {1172, 1199},
                     {1200, 1224},
                     {1225, 1265},
                     {1266, 1285},
                     {1286, 1327},
                     {1328, 1365},
                     {1366, 1394},
                     {1395, 1414},
                     {1415, 1457},
                     {1458, 1497},
                     {1498, 1518},
                     {1519, 1546},
                     {1547, 1568},
                     {1569, 1676},
                     {1677, 1693},
                     {1694, 1713},
                     {1714, 1733},
                     {1734, 1764},
                     {1765, 1784},
                     {1785, 1822},
                     {1823, 1848},
                     {1849, 1876},
                     {1877, 1899},
                     {1900, 1957},
                     {1958, 1995},
                     {1996, 2018},
                     {2019, 2056},
                     {2057, 2130},
                     {2131, 2175},
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
                     {11, 22},
                     {23, 97},
                     {98, 121},
                     {122, 149},
                     {150, 186},
                     {187, 220},
                     {221, 244},
                     {245, 265},
                     {266, 286},
                     {287, 301},
                     {302, 328},
                     {329, 344},
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
                     {8, },
                     {8, 29, },
                     {8, 57, },
                     {8, 93, },
                     {8, 123, },
                     {8, 165, },
                     {8, 181, },
                     {8, 239, },
                     {8, 290, },
                     {8, 323, },
                     {8, 372, },
                     {8, 438, },
                     {8, 469, },
                     {8, 494, },
                     {8, 522, },
                     {8, 562, },
                     {8, 605, },
                     {8, 645, },
                     {8, 696, },
                     {8, 738, },
                   },
                   new int[][] {
                     {9, 30},
                     {31, 58},
                     {59, 94},
                     {95, 124},
                     {125, 166},
                     {167, 182},
                     {183, 240},
                     {241, 291},
                     {292, 324},
                     {325, 373},
                     {374, 439},
                     {440, 470},
                     {471, 495},
                     {496, 523},
                     {524, 563},
                     {564, 606},
                     {607, 646},
                     {647, 697},
                     {698, 739},
                     {740, 781},
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
                     {2, 59, },
                     {2, 59, },
                     {2, 59, },
                     {2, 59, },
                   },
                   new int[][] {
                     {64, 106},
                     {107, 135},
                     {136, 174},
                     {175, 223},
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
                     {9, 112},
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
                     {10, 20},
                     {21, 54},
                     {55, 67},
                     {68, 130},
                     {131, 186},
                     {187, 211},
                     {212, 233},
                     {234, 289},
                     {290, 327},
                     {328, 339},
                     {340, 446},
                     {447, 490},
                     {491, 566},
                     {567, 624},
                     {625, 693},
                     {694, 773},
                     {774, 819},
                     {820, 886},
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
                     {2, 4},
                     {5, 79},
                   },
                   new String[] {
                     "",
                     "[(DATE_TIME (DATE (WEEKDAY 'Friday') (MONTH 'Dec.') (DAY '30') (YEAR '2005')) (TIME (PREP 'at') (TIME (HOUR '9') (MINUTE '11') (AMPM 'pm'))))]",
                     });

    verifySnippets(snippetizer, FileUtil.getFile(this.getClass(), "resources/snippet-sample11.html.gz"),
                   new int[][] {
                     {0, 9, },
                     {0, 9, },
                     {0, 9, },
                     {0, 9, },
                     {0, 9, },
                     {0, 9, },
                     {0, 9, },
                     {0, 9, },
                     {0, 9, },
                     {0, 9, },
                   },
                   new int[][] {
                     {10, 38},
                     {39, 78},
                     {79, 98},
                     {99, 145},
                     {146, 162},
                     {163, 215},
                     {216, 265},
                     {266, 305},
                     {306, 340},
                     {341, 357},
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
                     {3, 165},
                   },
                   new String[] {
                     "[(DATE (MONTH 'Dec.') (DAY '27') (YEAR '2005'))]",
                     });

    verifySnippets(snippetizer, FileUtil.getFile(this.getClass(), "resources/snippet-sample13.html.gz"),
                   new int[][] {
                     {0, 24, },
                     {0, 24, },
                     {0, 24, },
                     {0, 24, },
                     {0, 24, },
                     {0, 24, },
                     {0, 24, },
                   },
                   new int[][] {
                     {71, 731},
                     {732, 1623},
                     {1624, 2504},
                     {2505, 3440},
                     {3441, 4354},
                     {4355, 5235},
                     {5236, 6149},
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
                     {0, 57, },
                     {0, 94, },
                     {0, 94, },
                   },
                   new int[][] {
                     {78, 117},
                     {118, 192},
                     {193, 216},
                   },
                   new String[] {
                     "[(DATE (WEEKDAY 'Wednesday') (MONTH 'December') (DAY '28') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Tuesday') (MONTH 'December') (DAY '27') (YEAR '2005'))]",
                     "[(DATE (WEEKDAY 'Friday') (MONTH 'December') (DAY '23') (YEAR '2005'))]",
                     });

    verifySnippets(snippetizer, FileUtil.getFile(this.getClass(), "resources/snippet-sample15.html.gz"),
                   new int[][] {
                     {0, 24, },
                     {0, 24, 28, },
                     {0, 24, 162, },
                     {0, 24, 213, },
                     {0, 24, 247, },
                     {0, 24, 288, },
                     {0, 24, 319, },
                   },
                   new int[][] {
                     {26, 159},
                     {160, 210},
                     {211, 244},
                     {245, 285},
                     {286, 316},
                     {317, 363},
                     {364, 393},
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
                     {6, 22, },
                   },
                   new int[][] {
                     {101, 363},
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
                     {19, 54},
                     {55, 122},
                     {123, 162},
                     {163, 230},
                     {231, 295},
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
                     {5, 7, },
                   },
                   new int[][] {
                     {9, 249},
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
