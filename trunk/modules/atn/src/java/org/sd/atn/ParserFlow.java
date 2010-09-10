/*
    Copyright 2010 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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


/**
 * Container for defining the flow of parsing.
 * <p>
 * @author Spence Koehler
 */
public class ParserFlow {

  private boolean active;  // whether the flow is active
  private String flowId;
  private String[] parserIds;
  private String[] activeParserIds;

  /**
   * Construct with the flowId and ALL of its parserIds.
   * <p>
   * On construction, the flow and all of its parser IDs are active.
   */
  ParserFlow(String flowId, String[] parserIds) {
    this.active = true;
    this.flowId = flowId;
    this.parserIds = parserIds;
    this.activeParserIds = parserIds;
  }

  /**
   * Get this flow's id.
   */
  public String getFlowId() {
    return flowId;
  }

  /**
   * Set this instance's entire flow as active (or not).
   */
  public void setActive(boolean active) {
    this.active = active;
  }

  /**
   * Determine whether this instance's flow is active.
   */
  public boolean isActive() {
    return active;
  }

  /**
   * Set the identified parsers as active in the specified order.
   * Note that those parsers that are not identified will be deactivated.
   * Note that this has no effect on this instance's flow's active status.
   * <p>
   * If flowSpec is null, then activate all parsers in their original order.
   * <p>
   * Where flowSpec is of the form: parserId1,parserId2,...
   * <p>
   * @return the prior active parserIds.
   */
  public String[] setActive(String flowSpec) {
    return setActive(flowSpec == null ? (String[])null : flowSpec.split("\\s*,\\s*"));
  }

  /**
   * Set the identified parsers as active in the specified order.
   * Note that those parsers that are not identified will be deactivated.
   * Note that this has no effect on this instance's flow's active status.
   * <p>
   * If activeParserIds is null, then activate all parsers in their original order.
   * <p>
   * @return the prior active parserIds.
   */
  public String[] setActive(String[] activeParserIds) {
    final String[] result = this.activeParserIds;
    this.activeParserIds = (activeParserIds == null) ? this.parserIds : activeParserIds;
    return result;
  }

  /**
   * Get this instance's parser IDs (all or only those that are active).
   */
  public String[] getParserIds(boolean onlyActive) {
    return onlyActive ? activeParserIds : parserIds;
  }

  /**
   * Determine whether the identified parser within this flow is active.
   */
  public boolean isActive(String parserId) {
    boolean result = false;

    for (String activeParserId : activeParserIds) {
      if (activeParserId.equals(parserId)) {
        result = true;
        break;
      }
    }

    return result;
  }

  /**
   * Get this instances flow specification (all or only the active).
   */
  public String getFlowSpec(boolean onlyActive) {
    return makeFlowSpec(onlyActive ? activeParserIds : parserIds);
  }

  private final String makeFlowSpec(String[] parserIds) {
    final StringBuilder result = new StringBuilder();
    for (String parserId : parserIds) {
      if (result.length() > 0) result.append(',');
      result.append(parserId);
    }
    return result.toString();
  }
}
