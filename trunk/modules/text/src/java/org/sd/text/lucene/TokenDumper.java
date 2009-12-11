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
package org.sd.text.lucene;


import java.io.IOException;
import java.util.List;
import java.util.Properties;
import org.sd.util.PropertiesParser;
import org.sd.util.ReflectUtil;
import org.sd.text.lucene.LuceneUtils;

/**
 * Utility to dump tokens, e.g. for viewing effects of or debugging analyzers.
 * <p>
 * @author Spence Koehler
 */
public class TokenDumper {
  
//java -Xmx640m -classpath `cpgen /home/sbk/co/googlecode/semanticdiscoverytoolkit/modules/text` org.sd.text.lucene.TokenDumper fieldsClass=org.sd.text.lucene.XFields:getInstance field=email phraseFlag=true fooBar@bar.baz.com

  /**
   * Properties:
   * <ul>
   * <li>fieldsClass -- class/constructor of LuceneFields class to use (i.e. XFields:getInstance)</li>
   * <li>field -- field(s) to tokenize. Comma-delimited if multiple fields</li>
   * <li>phraseFlag -- (optional, default=false) true to delineate phrases</li>
   * </ul>
   * Arguments (non-property):
   * <ol>
   * <li>strings to tokenize</li>
   * </ol>
   */
  public static void main(String[] args) throws IOException {
    final PropertiesParser pp = new PropertiesParser(args);
    final Properties properties = pp.getProperties();
    args = pp.getArgs();

    final String fieldsClassString = properties.getProperty("fieldsClass");
    final String fieldString = properties.getProperty("field");
    final boolean phraseFlag = "true".equalsIgnoreCase(properties.getProperty("phraseFlag", "false"));

    System.out.println("fieldsClass=" + fieldsClassString);
    System.out.println("fieldString=" + fieldString);
    System.out.println("phraseFlag=" + phraseFlag);

    final LuceneFields luceneFields = (LuceneFields)ReflectUtil.buildInstance(fieldsClassString, properties);

    for (String string : args) {

      for (String field : fieldString.split("\\s*,\\s*")) {
        if (!phraseFlag) {
          final String[] tokens = luceneFields.tokenize(field, string);

          System.out.println("\n" + field + " '" + string + "' has " + tokens.length + " tokens:");

          int num = 0;
          for (String token : tokens) {
            System.out.println("\t" + num + ": '" + token + "'");
            ++num;
          }
        }
        else {
          final List<List<String>> phraseTexts = LuceneUtils.getPhraseTexts(luceneFields.getAnalyzer(), field, string);

          System.out.println("\n" + field + " '" + string + "' has " + phraseTexts.size() + " phraseTexts:");

          int phraseNum = 0;
          for (List<String> phraseText : phraseTexts) {
            System.out.println("Phrase #" + phraseNum + ":");

            int textNum = 0;
            for (String text : phraseText) {
              System.out.println("\t" + textNum + ": " + text);
            }
          }
        }
      }
    }
  }
}
