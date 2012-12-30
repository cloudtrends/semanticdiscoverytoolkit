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
package org.sd.atn;


import org.sd.xml.DomElement;

/**
 * Utility for selecting a "parent" state that meets certain criterea.
 * <p>
 * @author Spence Koehler
 */
public class AtnStateSelector {
  
  //
  // <test>
  //   <jclass>org.sd.atn.StateSelectionTest</jclass>
  //   <currentState type="push|pop|match">
  //     <test>...</test>
  //   </currentState>
  //   <priorRepeat distance='N' type="push|pop|match|first" />
  //   <descend path="x.y.z" type="push|pop|match|first" />
  //   <parentConstituent  path="x.y.z" OR distance="N" type="push|pop|match|first" />
  //   <priorMatch distance="N" category="x.y.z" type="push|pop|match|first" />
  //   <nextTokenState distance="N" smallest="true|false" revise="true|false" type="push|pop|match|first">
  //     <...token test params... />
  //   </nextTokenState>
  //   <prevTokenState distance="N" type="push|pop|match|first" path="x.y.z" />
  //  </selectState>


  public AtnStateSelector(DomElement defElement, ResourceManager resourceManager) {
    
  }


  public AtnState findParentState(AtnState startState) {
    AtnState result = null;
//...
    return result;
  }
}
