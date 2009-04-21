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
package org.sd.util;


import java.util.Collection;
import java.util.Random;
import java.util.TreeSet;

/**
 * Utility to generate a list of random numbers (i.e. for sampling).
 * <p>
 * @author Spence Koehler
 */
public class RandomNumbers {
  
  private int min;
  private int max;
  private int range;
  private Random random;

  /**
   * Construct for a range from 0 (inclusive) to max (exclusive).
   */
  public RandomNumbers(int max) {
    this(0, max);
  }

  /**
   * Construct for a range from min (inclusive) to max (exclusive).
   */
  public RandomNumbers(int min, int max) {
    this.min = min;
    this.max = max;
    this.range = max - min;
    this.random = new Random();
  }

  public Collection<Integer> getSample(int sampleSize) {
    final TreeSet<Integer> result = new TreeSet<Integer>();

    if (sampleSize > range) {
      throw new IllegalArgumentException("Can't generate a sample of size '" + sampleSize +
                                         "' from only '" + range + "' values! (range=[" +
                                         min + "," + max + ")).");
    }

    while (result.size() < sampleSize) {
      result.add(random.nextInt(range) + min);
    }

    return result;
  }

//zcat deduped_caches.112107.txt.gz | grep sd917.uk.sd_derrived_urls.attempt_2 > sample1/uk-expansion.caches.txt
//./run org.sd.util.RandomNumbers 2000 21224 > /home/spence/tmp/caches/cluster_caches/sample1/sample-2000-of-21224.txt
//pushd /home/spence/tmp/caches/cluster_caches/sample1
//sort sample-2000-of-21224.txt > sample-2000-of-21224.s.txt
//awk '{ printf("%d|%s\n", FNR, $0) }' uk-expansion.caches.txt | sort -t\| -k1,1 > uk-expansion.caches.numbered.s.txt
//join -j 1 -t\| sample-2000-of-21224.s.txt uk-expansion.caches.numbered.s.txt > uk-expansion.caches.sampled.txt

  /**
   * arg0: sampleSize
   * arg1: max (inclusive)
   * arg2: min (inclusive) [optional, default=1]
   *
   * stdout: sampleSize numbers from min to max.
   *
   * application:
   * - to choose a random sample of lines from a file,
   *   - add a column of line numbers to the file,
   *   - generate a list of random line numbers
   *   - join the line-numbered file with the random numbers
   *   - remove the extra column
   */
  public static final void main(String[] args) {
    //arg0: sampleSize
    //arg1: max
    //arg2: min (optional, default=1)

    final int sampleSize = Integer.parseInt(args[0]);
    final int max = Integer.parseInt(args[1]);
    final int min = args.length > 2 ? Integer.parseInt(args[2]) : 1;

    final RandomNumbers randomNumbers = new RandomNumbers(min, max + 1);
    final Collection<Integer> numbers = randomNumbers.getSample(sampleSize);

    for (Integer number : numbers) {
      System.out.println(number);
    }
  }
}
