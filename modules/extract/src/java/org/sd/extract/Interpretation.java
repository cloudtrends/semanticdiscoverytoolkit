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


//import org.sd.util.tree.Tree;

/**
 * Container for an interpretation of an extraction.
 * <p>
 * @author Spence Koehler
 */
public interface Interpretation {
  
//   public Tree<String> getInterpretationTree();
//   public double getScore();
//   public String getText();
//   public String getPreText();
//   public String getPostText();

  /**
   * Get the extraction that contains this interpretation.
   */
  public Extraction getExtraction();  //backpointer.

  /**
   * Set or update the extraction backpointer.
   */
  public void setExtraction(Extraction extraction);

  /**
   * Get this interpretation's data as a pipe-delimited string of fields
   * specific to the type and implementation.
   */
  public String getFieldsString();

  /**
   * Get a key identifying the structure (not content) of this interpretation.
   */
  public String getStructureKey();

  /**
   * Get an unambiguous string representing this interpretation's content.
   */
  public String asString();
}
