/*
    Copyright 2011 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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
package org.sd.wn;


import java.io.File;
import java.io.IOException;
import java.util.List;
import org.sd.lang.AnagramGenerator;

/**
 * A WordValidator for anagrams backed by WordNet.
 * <p>
 * @author Spence Koehler
 */
public class WordNetWordValidator implements AnagramGenerator.WordValidator {

  private SenseIndex senseIndex;

  public WordNetWordValidator(File dictDir) throws IOException {
    this.senseIndex = SenseIndex.getInstance(dictDir);
  }
  
  public boolean isValid(String word) {
    final List<WordSense> wordSenses = senseIndex.getWordSenses(word);
    return wordSenses != null;
  }


  // java -Xmx640m -classpath `cpgen /home/skoehler/co/googlecode/semanticdiscoverytoolkit/modules/lang` org.sd.wn.WordNetWordValidator ~/co/googlecode/semanticdiscoverytoolkit/modules/lang/resources/data/WordNet latino
  public static void main(String[] args) throws IOException {
    //arg0: dictFile
    //args1+: strings for anagrams
    final WordNetWordValidator wordValidator = new WordNetWordValidator(new File(args[0]));
    final AnagramGenerator agen = new AnagramGenerator();
    agen.setWordValidator(wordValidator);

    agen.doMain(args, 1);
  }
}
