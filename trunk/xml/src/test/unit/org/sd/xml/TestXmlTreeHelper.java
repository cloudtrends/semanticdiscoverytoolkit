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
package org.sd.xml;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.sd.util.tree.Tree;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * JUnit Tests for the XmlTreeHelper class.
 * <p>
 * @author Spence Koehler
 */
public class TestXmlTreeHelper extends TestCase {

  public TestXmlTreeHelper(String name) {
    super(name);
  }
  
  private final void verify(Tree<XmlLite.Data> node, String[] ignoreTags, String expectedText) {
    final Set<String> tags = new HashSet<String>();
    for (String tag : ignoreTags) tags.add(tag);
    final String text = XmlTreeHelper.getAllText(XmlTreeHelper.excludeTags(node, tags));

    assertEquals(text, expectedText, text);
  }

  public void testExcludeTags() throws IOException {
    final Tree<XmlLite.Data> node = XmlFactory.buildXmlTree("<top><foo>Foo</foo><bar><a>A</a><b>B</b><c>C</c></bar><baz>Baz</baz></top>", false, false);

    verify(node, new String[]{"foo"}, "A B C Baz");
    verify(node, new String[]{"bar"}, "Foo Baz");
    verify(node, new String[]{"a", "c"}, "Foo B Baz");
    verify(node, new String[]{"baz"}, "Foo A B C");
  }

  public void testAlternatePathEquality() throws IOException {
    final String htmlContent = "<div xmlns=\"http://www.w3.org/1999/xhtml\"><p><a href=\"http://pmm.typepad.com/photos/uncategorized/jackbauer.jpg\"><img class=\"image-full\" alt=\"Jackbauer\" title=\"Jackbauer\" src=\"http://pmm.typepad.com/photos/uncategorized/jackbauer.jpg\" border=\"0\" /></a><br /> <i>Dit seizoen: een kotsende Jack Bauer</i></p> <p>Ter gelegenheid van de start van het nieuwe seizoen van thrillerserie 24 heeft Gizmodo <a href=\"http://gizmodo.com/gadgets/cellphones/what-jack-bauer-needs-in-a-cellphone-228794.php\" target=\"_blank\">een lijstje gemaakt</a> met de dingen die er allemaal in zouden moeten zitten. Ik vind het wat beperkt. Goed, een hypersnelle dataverbinding en ingebouwde 10 megapixelcamera zijn hartstikke leuk, maar toch mis ik ondermeer:</p> <p>&#149; Ingebouwde taser (u weet wel, zo'n varkensprikker die Amerikaanse agenten op burgers gebruiken),<br /> &#149; Telepathische interface (die zelfs in de meest stressvolle situaties raadt welke ingewikkelde opdracht u wilt laten uitvoeren, en het bovendien begrijpt dat de knoppen die u ondertussen indrukt, vooral voor de bühne zijn bedoeld),<br /> &#149; Elektromagnetisch schild (dat niet alleen bescherming biedt tegen nucleaire explosies, maar ook tegen minder spectaculaire dreigingen zoals de tientallen kogels die Jack elk uur weer moet zien te ontwijken).</p> <p>Oh, en als u Jack Bauer niets vindt, dan hebben de terroristen al gewonnen. Zoals een bezoeker van het juridische (!) blog The Volokh Conspiracy uitlegt, is Jack trouwens <a href=\"http://volokh.com/posts/1168838134.shtml\" target=\"_blank\">ook financieel gezien de beste keuze</a>:</p> <p><em><blockquote>We have to face the facts. Jack is the only guy we have who can save the country in 24 hours. No shaken-not-stirred martinis. No bimbos. No whizbang sports cars. He just gets the job done.</p> <p>Suppose he costs the government $125k/year with burdens and benefits. So we get the country saved for just $500 based on a five day work week with ten holidays.</p> <p>Nobody can do it better. Nobody can do it faster. Nobody can do it cheaper.</blockquote></em></p></div>";
    final Tree<XmlLite.Data> xmlNode = XmlFactory.buildXmlTree(htmlContent, true, true);

    final String allText = XmlTreeHelper.getAllText(xmlNode);
    final String content = XmlFactory.stripHtmlFormatting(htmlContent);

    assertEquals(allText, content);
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestXmlTreeHelper.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
