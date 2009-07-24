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
package org.sd.text.sentiment;

import org.sd.util.MathUtil;

import java.util.Arrays;

/**
 * Container class for sentiment types and weight.
 *
 * @author Spence Koehler
 */
public class Sentiment {

  private int positiveVotes;
  private int negativeVotes;
  private int neutralVotes;
  private int mixedVotes;
  private int unknownVotes;

  private int totalVotes;

  private SentimentType _type;
  private Double _weight;

  public Sentiment() {
    this(null);
  }

  public Sentiment(SentimentType type) {
    this(type, 1);
  }

  public Sentiment(SentimentType type, int votes) {
    clearVotes();
    addVotes(type, votes);
  }

  private final void clearVotes() {
    this.positiveVotes = 0;
    this.negativeVotes = 0;
    this.neutralVotes = 0;
    this.mixedVotes = 0;
    this.unknownVotes = 0;
    this.totalVotes = 0;

    this._type = null;
    this._weight = null;
  }

  public boolean hasType() {
    return totalVotes > 0;
  }

  public boolean hasKnownType() {
    return totalVotes > 0 && totalVotes > unknownVotes;
  }

  public void setType(SentimentType type) {
    clearVotes();
    addVotes(type, 1);
  }

  public SentimentType getType() {
    if (_type == null && totalVotes > 0) {
      computeType();
    }
    return _type;
  }

  public void setWeight(double weight) {
    this._weight = weight;
  }

  public Double getWeight() {
    if (_weight == null && totalVotes > 0) {
      computeType();
    }
    return _weight;
  }

  public final void addVote(SentimentType type) {
    addVotes(type, 1);
  }

  public final void addVotes(SentimentType type, int numVotes) {
    if (numVotes > 0 && type != null) {
      switch (type) {
      case POSITIVE :
        ++positiveVotes; break;
      case NEGATIVE :
        ++negativeVotes; break;
      case NEUTRAL :
        ++neutralVotes; break;
      case MIXED :
        ++mixedVotes; break;
      case UNKNOWN :
        ++unknownVotes; break;
      }

      totalVotes += numVotes;
      this._type = null;
      this._weight = null;
    }
  }

  public final void addVotes(Sentiment other) {
    if (other.hasType()) {
      this.positiveVotes += other.positiveVotes;
      this.negativeVotes += other.negativeVotes;
      this.neutralVotes += other.neutralVotes;
      this.mixedVotes += other.mixedVotes;
      this.unknownVotes += other.unknownVotes;
      this.totalVotes += other.totalVotes;

      this._type = null;
      this._weight = null;
    }
  }

  public final void addVotes(Sentiment other, int multiplier) {
    if (other.hasType()) {
      this.positiveVotes += (other.positiveVotes * multiplier);
      this.negativeVotes += (other.negativeVotes * multiplier);
      this.neutralVotes += (other.neutralVotes * multiplier);
      this.mixedVotes += (other.mixedVotes * multiplier);
      this.unknownVotes += (other.unknownVotes * multiplier);
      this.totalVotes += (other.totalVotes * multiplier);

      this._type = null;
      this._weight = null;
    }
  }

  public final SentimentScore[] getSortedScores() {
    if (totalVotes == 0) return null;
    
    final Tally[] tallies = computeSortedTallies();
    final SentimentScore[] scores = new SentimentScore[tallies.length];
    
    for (int i=0; i < tallies.length; i++) {
      scores[i] = new SentimentScore();
      scores[i].type = tallies[i].type;
      scores[i].weight = (double)tallies[i].votes / (double)totalVotes;
    }
    
    return scores;
  }
  
  private final void computeType() {
    if (totalVotes > 0) {
      final Tally[] tallies = computeSortedTallies();

      this._type = tallies[0].type;
      this._weight = (double)tallies[0].votes / (double)totalVotes;
    }
    else {
      this._type = null;
      this._weight = null;
    }
  }

  private Tally[] computeSortedTallies() {
    final Tally[] tallies = new Tally[] {
      new Tally(SentimentType.POSITIVE, positiveVotes),
      new Tally(SentimentType.NEGATIVE, negativeVotes),
      new Tally(SentimentType.NEUTRAL, neutralVotes),
      new Tally(SentimentType.MIXED, mixedVotes),
      new Tally(SentimentType.UNKNOWN, unknownVotes),
    };
    
    Arrays.sort(tallies);
    
    return tallies;
  }

  public String toString() {
    final StringBuilder result = new StringBuilder();

    final SentimentType type = getType();
    final Double weight = getWeight();

    result.append(type == null ? "NONE" : type.name());
    if (weight != null && weight > 0.0 && weight < 1.0) {
      result.append('@').append(MathUtil.doubleString(weight, 3));
    }

    return result.toString();
  }

  private static final class Tally implements Comparable {
    public final SentimentType type;
    public final int votes;

    public Tally(SentimentType type, int votes) {
      this.type = type;
      this.votes = votes;
    }

    public int compareTo(Object o) {
      final Tally other = (Tally)o;
      // sort from most to least votes
      return (other.votes - votes);
    }
  }
}
