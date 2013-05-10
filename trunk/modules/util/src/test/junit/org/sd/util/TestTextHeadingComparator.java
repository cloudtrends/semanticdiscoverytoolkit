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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit Tests for the TextHeadingComparator class.
 * <p>
 * @author Abe Sanderson
 */
public class TestTextHeadingComparator extends TestCase {

  public TestTextHeadingComparator(String name) {
    super(name);
  }

  public void testCompare()
  {
    final String[][] inputs = new String[][] {
      new String[] {
        "ANGELA BENGFORT",
        "MERRIAM, Kan. ? Angela Bengfort, 92, of Mer-riam, Kan., and formerly of Festina, Iowa, died Sunday (Nov. 9, 2003) at the Trinity Lutheran Manor in Merriam, Kan., following a long illness.",      },
      new String[] {
        "MERRIAM, Kan. ? Angela Bengfort, 92, of Mer-riam, Kan., and formerly of Festina, Iowa, died Sunday (Nov. 9, 2003) at the Trinity Lutheran Manor in Merriam, Kan., following a long illness.",
        "A Mass of Christian Burial was held at 11 a.m. Wednesday, Nov. 12, at Our Lady of Seven Dolors Catholic Church in Festina. Angela was laid to rest in Our Lady of Seven Dolors Cemtery in Festina. ",  
      },
      new String[] {
        "A Mass of Christian Burial was held at 11 a.m. Wednesday, Nov. 12, at Our Lady of Seven Dolors Catholic Church in Festina. Angela was laid to rest in Our Lady of Seven Dolors Cemtery in Festina. ",
        "Angela Elizabeth Bengfort was born Sept. 27, 1911, in Festina, the daughter of Bern-ard and Elizabeth (Lechtenberg) Schmitz. Angela attended school in Festina.",
      },
      new String[] {
        "Schluter Balik Funeral Home, French Chapel, Calmar, (563) 562-3393.",
        "MABLE F. DALY",
      },
      new String[] {
        "Schluter Balik Funeral Home, French Chapel, Calmar, (563) 562-3393.",
        "",
      },
      new String[] {
        "",
        "MABLE F. DALY",
      },
      new String[] {
        "MABLE F. DALY",
        "",
      },
      new String[] {
        "Published: October 25, 2007 06:43 pm Thursday, October 25, 2007 Obituaries Glasgow Daily Times Fred Nation",
        "GLASGOW - Fred Nation, 87, died Tuesday, Oct. 16, 2007, at his home.",
      },
      new String[] {
        "GLASGOW - Fred Nation, 87, died Tuesday, Oct. 16, 2007, at his home. The Barren County native was retired from General Motors Company and a World War II veteran and survivor of Pearl Harbor, U.S. Arizona. He was born Sept. 7, 1920, in Starks, La., and was the son of Pete and Annie Nation (both deceased). He was preceded in death by his beloved wife, Ruby Nation; brothers: Pat, Ned and Kenneth Nation; one sister: Jane Lawson; and one stepson: Richard Vore. He is survived by two sons: Fred Nation Jr. (Melinda) and Roger Nation (Annette) both of Texas; one stepson; Ron Vore (Virginia) of Florida; sisters: Sadie Pennick of Texas and Belle Lawson of Louisiana; granddaughter, Kathy Hoig, and daughter-in-law: Joan Vore, both of Michigan, as well as many other grandchildren, great-grandchildren, nieces and nephews; also good friends, Jim and Wanda Clement, Dick and Carol Hammontree, Lisa Parke and Jonathan Dubree.",
        "Per Fred's request there will be no services. A.F. Crow & Son is handling the cremation arrangements.",
      },

    };
    final int[] expected = new int[] {
      1,
      0,
      0,
      -1,
      0,
      -1,
      1,
      -1,
      0,
    };

    final TextHeadingComparator comparator = new TextHeadingComparator();
    for(int i = 0; i < inputs.length; i++)
    {
      String[] ins = inputs[i];
      int ex = expected[i];

      assertEquals("comparison for inputs at index "+i+" does not match",
                   ex, comparator.compare(ins[0], ins[1]));
    }
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestTextHeadingComparator.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
