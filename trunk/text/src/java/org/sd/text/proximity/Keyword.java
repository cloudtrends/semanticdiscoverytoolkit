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
package org.sd.text.proximity;


/**
 * Container for a keyword found in text.
 * <p>
 * @author Spence Koehler
 */
public class Keyword {
  
  private String word;
  private Keywords source;    // source of keyword.
  private int rank;           // rank of keyword in its source list
  private double power;       // percentage of source terms having word's count or less

  public Keyword(String word, Keywords source, int rank, double power) {
    this.word = word;
    this.source = source;
    this.rank = rank;
    this.power = power;
  }

  public String getWord() {
    return word;
  }

  public Keywords getSource() {
    return source;
  }

  public int getRank() {
    return rank;
  }

  public double getPower() {
    return power;
  }
}
