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
package org.sd.atn;


import org.sd.util.InputContextIterator;

/**
 * Factory class for parse and multi-parse settings.
 * <p>
 * @author Spence Koehler
 */
public class ParseSettingsFactory {
  
  public enum ReconfigureStrategy { INITIAL, RESET, BROADEN, ROOT };

  public static ParseSettings getParseSettings(ReconfigureStrategy strategy, String compoundParserId, String[] atnParserFlow) {
    ParseSettings result = null;

    switch (strategy) {
      case INITIAL :
        result = getInitialParseSettings(compoundParserId, atnParserFlow);
        break;
      case RESET :
        result = getResettingParseSettings(compoundParserId, atnParserFlow);
        break;
      case BROADEN :
        result = getBroadeningParseSettings(compoundParserId, atnParserFlow);
        break;
      case ROOT :
        result = getRootParseSettings(compoundParserId, atnParserFlow);
        break;
    }

    return result;
  }

  /**
   * Build an initial parse settings instance that simply returns its given
   * input as reconfigured.
   */
  public static ParseSettings getInitialParseSettings(String compoundParserId, String[] atnParserFlow) {
    return new ParseSettings(compoundParserId, atnParserFlow) {
      protected InputContextIterator reconfigureInput(InputContextIterator inputContextIterator, ParseOutputCollector output) {
        return inputContextIterator;
      }
    };
  }

  /**
   * Build a parse settings that resets its given input to reconfigure.
   */
  public static ParseSettings getResettingParseSettings(String compoundParserId, String[] atnParserFlow) {
    return new ParseSettings(compoundParserId, atnParserFlow) {
      protected InputContextIterator reconfigureInput(InputContextIterator inputContextIterator, ParseOutputCollector output) {
        if (inputContextIterator != null) inputContextIterator.reset();
        return inputContextIterator;
      }
    };
  }

  /**
   * Build a parse settings that broadens its given input to reconfigure
   * if possible. If unable to broaden, the original input will be reset
   * and returned.
   */
  public static ParseSettings getBroadeningParseSettings(String compoundParserId, String[] atnParserFlow) {
    return new ParseSettings(compoundParserId, atnParserFlow) {
      protected InputContextIterator reconfigureInput(InputContextIterator inputContextIterator, ParseOutputCollector output) {
        InputContextIterator result = inputContextIterator.broaden();

        if (result == null) {
          inputContextIterator.reset();
          result = inputContextIterator;
        }

        return result;
      }
    };
  }

  /**
   * Build a parse settings that broadens its given input to the context root
   * when reconfiguring. If the input is already at the root, then it will be
   * reset and returned.
   */
  public static ParseSettings getRootParseSettings(String compoundParserId, String[] atnParserFlow) {
    return new ParseSettings(compoundParserId, atnParserFlow) {
      protected InputContextIterator reconfigureInput(InputContextIterator inputContextIterator, ParseOutputCollector output) {
        InputContextIterator prev = inputContextIterator;
        InputContextIterator result = inputContextIterator.broaden();

        while (result != null) {
          prev = result;
          result = result.broaden();
        }

        if (prev == inputContextIterator) {
          inputContextIterator.reset();
          result = inputContextIterator;
        }
        else {
          result = prev;
        }

        return result;
      }
    };
  }

  /**
   * Consumer can get an instance containing initial parse settings and add
   * parse settings for additional passes as desired.
   */
  public static MultiParseSettings getInitialSettings(String compoundParserId, String[] atnParserFlow) {
    final MultiParseSettings result = new MultiParseSettings();
    result.add(getInitialParseSettings(compoundParserId, atnParserFlow));
    return result;
  }
}
