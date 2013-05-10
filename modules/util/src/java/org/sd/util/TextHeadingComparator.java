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
    GNU Lesser General Public License for more details.\

    You should have received a copy of the GNU Lesser General Public License
    along with The Semantic Discovery Toolkit.  If not, see <http://www.gnu.org/licenses/>.
*/
package org.sd.util;

import java.util.Comparator;

/**
 * Comparator which examines the content of text to determine headingness
 */
public class TextHeadingComparator 
  implements Comparator<String> 
{
  public static final int DEFAULT_MIN_WORD_LENGTH = 3;
  public static final int DEFAULT_MAX_ABBREV_LENGTH = 4;

  private int minWordLength;
  private int maxAbbrevLength;
  
  public TextHeadingComparator() {
    this(DEFAULT_MIN_WORD_LENGTH, DEFAULT_MAX_ABBREV_LENGTH);
  }
  public TextHeadingComparator(int minWordLength, 
                               int maxAbbrevLength)
  {
    this.minWordLength = minWordLength;
    this.maxAbbrevLength = maxAbbrevLength;
  }

  public int compare(String str1, String str2)
  {
    double p1 = percentCaps(str1);
    double p2 = percentCaps(str2);
    //System.out.println("string("+p1+"): "+str1);
    //System.out.println("string("+p2+"): "+str2);

    // todo: figure out best method for blank lines
    if(p1 > 0.75 || p2 > 0.75)
      return Double.compare(p1,p2);
    else
      return (Double.compare(p1,p2) >= 0 ? 0 : -1);
  }

  public double computeHeadingStrength(String text) {
    return percentCaps(text) * 1.0;
  }

  private boolean isCandidateWord(String word)
  {
    // words <=2 chars  are not candidates
    // words >=5 chars are candidates
    if(word.length() < minWordLength)
      return false;
    else if(word.length() > maxAbbrevLength)
      return true;
    
    // non abbrevs are candidates
    return !StringUtil.isLikelyAbbreviation(word);
  }

  private double percentCaps(String text)
  {
    double result = 0.0;
    
    boolean sawNonCaps = false;
    int wordCount = 0;
    int capsWordCount = 0;
    for(WordIterator it = new WordIterator(text); it.hasNext(); )
    {
      String word = it.next();
      if(!isCandidateWord(word))
        continue;

      wordCount++;
      if(!sawNonCaps)
      {
        if(StringUtil.allCaps(word))
          capsWordCount++;
        else
          sawNonCaps = true;
      }
    }
    
    result = (wordCount == 0 ? 0.0 : (double)capsWordCount/(double)wordCount);
    return result;
  }
}
