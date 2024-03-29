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
package org.sd.crawl;


/**
 * An exception to encapsulate robots exclusions.
 * <p>
 * @author Spence Koehler
 */
public class RobotsDeniedException extends RuntimeException {

  private static final long serialVersionUID = 42L;


  private String url;
  private String disallowedPattern;

  public RobotsDeniedException(String url, String disallowedPattern) {
    super("URL '" + url + "' is robots disallowed by pattern '" + disallowedPattern + "'");

    this.url = url;
    this.disallowedPattern = disallowedPattern;
  }

  /**
   * Get the url string that is disallowed.
   */
  public String getUrl() {
    return url;
  }

  /**
   * Get the pattern string that disallowed the url.
   */
  public String getDisallowedPattern() {
    return disallowedPattern;
  }
}
