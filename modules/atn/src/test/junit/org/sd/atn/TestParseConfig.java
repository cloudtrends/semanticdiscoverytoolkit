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


import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.sd.util.tree.Tree;
import org.sd.util.tree.TreeBuilderFactory;
import org.sd.xml.DataProperties;
import org.sd.xml.DomContextIterator;
import org.sd.xml.DomContextIteratorFactory;
import org.sd.xml.DomDocument;
import org.sd.xml.DomElement;
import org.sd.xml.DomTextIterationStrategy;
import org.sd.xml.XmlFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * JUnit Tests for the ParseConfig class.
 * <p>
 * @author Spence Koehler
 */
public class TestParseConfig extends TestCase {

  public TestParseConfig(String name) {
    super(name);
  }
  
  // Tests for multipass CompoundParser functionality through ParseConfig.


  private final boolean debug = false;

  public void test1() throws IOException {

    if (debug) AtnState.setTrace(true);
    if (debug) System.out.println("test1a");

    runTest("parseConfigTest.1.xml", "ParseConfigTest.1a", "test1", "input1", "expectedText-1a", "expectedTrees-1a", "1", null, false, null);

    if (debug) System.out.println("test1b");

    runTest("parseConfigTest.1.xml", "ParseConfigTest.1b", "test1", "input1", "expectedText-1b", "expectedTrees-1b", "1", null, true, null);

    if (debug) System.out.println("test1c");

    // run all compound parsers and their parsers in order with 'reset' input reconfiguration
    runTest("parseConfigTest.1.xml", "ParseConfigTest.1c", "test1", "input1", "expectedText-1c", "expectedTrees-1c", null, null, true, ParseSettingsFactory.ReconfigureStrategy.RESET);

    if (debug) System.out.println("test1d");

    // run all compound parsers and their parsers in order with a root input reconfiguration
    runTest("parseConfigTest.1.xml", "ParseConfigTest.1d", "test1", "input1", "expectedText-1d", "expectedTrees-1d", null, null, true, ParseSettingsFactory.ReconfigureStrategy.ROOT);

  }


  public void test2() throws IOException {

    if (debug) System.out.println("test2a");

    runTest("parseConfigTest.2.xml", "ParseConfigTest.2a", "test2", "input2", "expectedText-2a", "expectedTrees-2a", null, null, true, ParseSettingsFactory.ReconfigureStrategy.ROOT);
  }


  public void test3() throws IOException {

    if (debug) System.out.println("test3a");

    runTest("parseConfigTest.3.xml", "ParseConfigTest.3a", "test3", "input3", "expectedText-3a", "expectedTrees-3a", "parser1", null, true, null);
  }


  private final void runTest(String resourceName, String name, String testId, String inputId, String expectedTextId, String expectedTreesId, String compoundParserId, String flowString, boolean onlySelected, ParseSettingsFactory.ReconfigureStrategy strategy) throws IOException {

    final InputStream inputStream = getInputStream(resourceName);
    final TestParamsContainer paramsContainer = new TestParamsContainer(inputStream);
    inputStream.close();

    runTest(name, paramsContainer, testId, inputId, expectedTextId, expectedTreesId, compoundParserId, flowString, onlySelected, strategy);
  }

  private final void runTest(String name, TestParamsContainer paramsContainer, String testId, String inputId, String expectedTextId, String expectedTreesId, String compoundParserId, String flowString, boolean onlySelected, ParseSettingsFactory.ReconfigureStrategy strategy) throws IOException {

    final TestParams testParams = paramsContainer.testId2Params.get(testId);
    final ParseConfig parseConfig = buildParseConfig(testParams.getInput("parseConfig"));
    if (debug) parseConfig.setVerbose(true);
    final DomElement testInput = testParams.getInput(inputId);

    assertNotNull("No input '" + inputId + "' specified for test!",
                  testInput != null);

    // if (configMutator != null) {
    //   configMutator.mutate(parseConfig);
    // }

    final List<List<String>> expectedText = testParams.loadNestedLists(expectedTextId);
    final List<List<Tree<String>>> expectedTrees = loadExpectedTrees(testParams, expectedTreesId);

    runTest(name, parseConfig, testInput, compoundParserId, flowString, expectedText, expectedTrees, onlySelected, strategy);
  }


  private final void runTest(String name, ParseConfig parseConfig, DomElement inputXml, String compoundParserId, String flowStrings, List<List<String>> expectedText, List<List<Tree<String>>> expectedTrees, boolean onlySelected, ParseSettingsFactory.ReconfigureStrategy strategy) throws IOException {

    // prune the input from the config tree
    if (inputXml != null) inputXml.prune();

    // build input
    final DomContextIterator input = buildDomContextIterator(inputXml);


    // build flow
    final String[] flow = buildFlow(flowStrings);


    // build ParseConfigTest
    runTest(name, parseConfig, input, compoundParserId,flow, expectedText, expectedTrees, onlySelected, strategy);
  }

  private final void runTest(String name, ParseConfig parseConfig, DomContextIterator input, String compoundParserId, String[] flow, List<List<String>> expectedText, List<List<Tree<String>>> expectedTrees, boolean onlySelected, ParseSettingsFactory.ReconfigureStrategy strategy) {

    MultiParseSettings settings = null;

    if (compoundParserId == null) {
      settings = parseConfig.buildSettings(strategy);
    }
    else {
      settings = parseConfig.buildSettings(compoundParserId, flow);
    }

    // build ParseConfigTest
    runTest(name, parseConfig, input, settings, expectedText, expectedTrees, onlySelected);
  }

  private final void runTest(String name, ParseConfig parseConfig, DomContextIterator input, MultiParseSettings settings, List<List<String>> expectedText, List<List<Tree<String>>> expectedTrees, boolean onlySelected) {

    // build ParseConfigTest
    final ParseConfigTest unitTest = new ParseConfigTest(name, parseConfig, input, settings, expectedText, expectedTrees, onlySelected);

    unitTest.runTest();
  }


  private final InputStream getInputStream(String resourceName) {
    final String resource = "resources/" + resourceName;

    //System.out.println("loading config '" + this.getClass().getResource(resource) + "'...");

    return this.getClass().getResourceAsStream(resource);
  }

  private final List<List<Tree<String>>> loadExpectedTrees(TestParams testParams, String id) {

    if (id == null) return null;
    
    final List<List<String>> treeStrings = testParams.loadNestedLists(id);
    return buildExpectedTrees(treeStrings);
  }
  
  private final ParseConfig buildParseConfig(String parseConfigXml) throws IOException {

    final DomDocument domDocument = XmlFactory.loadDocument(parseConfigXml, false);
    final DomElement parseConfigElement = domDocument.getDocumentDomElement();
    return buildParseConfig(parseConfigElement);
  }

  private final ParseConfig buildParseConfig(DomElement parseConfigElement) {
    parseConfigElement.setDataProperties(new DataProperties());
    parseConfigElement.getDataProperties().set("TEST_RESOURCES", this.getClass().getResource("resources").getFile());
    final ParseConfig parseConfig = new ParseConfig(parseConfigElement);
    return parseConfig;
  }


  private final DomContextIterator buildDomContextIterator(String inputXml) throws IOException {
    final DomDocument domDocument = XmlFactory.loadDocument(inputXml, false);
    final DomElement inputXmlElement = domDocument.getDocumentDomElement();
    return buildDomContextIterator(inputXmlElement);
  }

  private final DomContextIterator buildDomContextIterator(DomElement inputXmlElement) throws IOException {
    if (inputXmlElement == null) return null;

    final DomContextIterator input = DomContextIteratorFactory.getDomContextIterator(inputXmlElement, null, DomTextIterationStrategy.INSTANCE);
    return input;
  }


  private final String[] buildFlow(String flowStrings) {

    String[] flow = null;

    if (flowStrings != null) {
      flow = flowStrings.split("\\s*,\\s*");
    }

    return flow;
  }


  private final List<List<String>> buildExpectedText(String[][] expectedTextStrings) {

    final List<List<String>> expectedText = expectedTextStrings == null ? null : new ArrayList<List<String>>();

    if (expectedText != null) {
      for (String[] expectedTexts : expectedTextStrings) {
        final List<String> curTexts = new ArrayList<String>();
        expectedText.add(curTexts);

        for (String expectedTextString : expectedTexts) {
          curTexts.add(expectedTextString);
				}
			}
    }

    return expectedText;
  }


  private final List<List<Tree<String>>> buildExpectedTrees(String[][] expectedTreeStrings) {

    final List<List<Tree<String>>> expectedTrees = expectedTreeStrings == null ? null : new ArrayList<List<Tree<String>>>();

    if (expectedTrees != null) {

      for (String[] expectedTreess : expectedTreeStrings) {

        final List<Tree<String>> curTrees = new ArrayList<Tree<String>>();
        expectedTrees.add(curTrees);

        for (String expectedTreeString : expectedTreess) {
          final Tree<String> expectedTree = TreeBuilderFactory.getStringTreeBuilder().buildTree(expectedTreeString);
          curTrees.add(expectedTree);
        }
      }
    }
    
    return expectedTrees;
  }


  private final List<List<Tree<String>>> buildExpectedTrees(List<List<String>> expectedTreeStrings) {
    List<List<Tree<String>>> expectedTrees = expectedTreeStrings == null ? null : new ArrayList<List<Tree<String>>>();

    if (expectedTrees != null) {
      for (List<String> expectedTreess : expectedTreeStrings) {
        final List<Tree<String>> curTrees = new ArrayList<Tree<String>>();
        expectedTrees.add(curTrees);

        for (String expectedTreeString : expectedTreess) {
          final Tree<String> expectedTree = TreeBuilderFactory.getStringTreeBuilder().buildTree(expectedTreeString);
          curTrees.add(expectedTree);
        }
      }
    }

    return expectedTrees;
  }



  private class ParseConfigTest {

    private String name;
    private ParseConfig parseConfig;
    private DomContextIterator input;
    private MultiParseSettings parseSettings;
    private List<List<String>> expectedText;
    private List<List<Tree<String>>> expectedTrees;
    private boolean onlySelected;

    boolean showText = true;
    boolean showParse = true;

    public ParseConfigTest(String name, ParseConfig parseConfig, DomContextIterator input, MultiParseSettings parseSettings, List<List<String>> expectedText, List<List<Tree<String>>> expectedTrees, boolean onlySelected) {

      this.name = name;
      this.parseSettings = parseSettings;
      this.parseConfig = parseConfig;
      this.input = input;
      this.expectedText = expectedText;
      this.expectedTrees = expectedTrees;
      this.onlySelected = onlySelected;
    }


    public void runTest() {

      //parseConfig.setVerbose("ParseConfigTest.3a".equals(name));
      final ParseOutputCollector output = parseConfig.parse(input, parseSettings, null, null);

      if (expectedText != null) {

        final int numParseResults = output == null ? 0 : output.getParseResults() == null ? 0 : output.getParseResults().size();
        int parseResultNum = 0;

        if (numParseResults > 0) {
          for (AtnParseResult parseResult : output.getParseResults()) {

            int expectedParseNum = 0;

            final int numParses = parseResult.getNumParses();
            for (int parseNum = 0; parseNum < numParses; ++parseNum) {

              final AtnParse parse = parseResult.getParse(parseNum);

              if (onlySelected && parse.getSelected() || !onlySelected) {

                assertTrue(name + ": more parseResults (" + (parseResultNum + 1) + ") than expected (" + expectedText.size() + ")!" +
                           " parse=" + parse.getParsedText(),
                           parseResultNum < expectedText.size());
                assertTrue(name + ": more parses(" + (expectedParseNum + 1) + ") than expected (" +
                           expectedText.get(parseResultNum).size() + ") parseResultNum=" + parseResultNum +
                           " parse=" + parse.getParsedText(),
                           expectedParseNum < expectedText.get(parseResultNum).size());

                // ParsedText
                assertEquals(name + ": Bad parsed text (#" + parseResultNum + ", " + expectedParseNum + ").",
                             expectedText.get(parseResultNum).get(expectedParseNum), parse.getParsedText());

                // ParseTree
                assertEquals(name + ": Bad parse (#" + parseResultNum + ", " + expectedParseNum + ").",
                             expectedTrees.get(parseResultNum).get(expectedParseNum), parse.getParseTree());

                ++expectedParseNum;
              }
            }
            ++parseResultNum;
          }
        }

//        final int numParseResults = output == null ? 0 : output.getParseResults() == null ? 0 : output.getParseResults().size();
        assertEquals(name + ": Bad number of parses.",
                     expectedText.size(), parseResultNum);
      }
      else {
        if (showText) {

          if (output.getParseResults() != null) {
            System.out.println("\n<list>");

            for (AtnParseResult parseResult : output.getParseResults()) {

              System.out.println("\t<list>");

              final int numParses = parseResult.getNumParses();
              for (int parseNum = 0; parseNum < numParses; ++parseNum) {

                final AtnParse parse = parseResult.getParse(parseNum);

                if (onlySelected && parse.getSelected() || !onlySelected) {
                  System.out.println("\t\t<item>" + parse.getParsedText() + "</item>");
								}
							}
                
              System.out.println("\t</list>");
						}

            System.out.println("</list>");
					}


					if (showParse) {
						if (output.getParseResults() != null) {
							System.out.println("\n<list>");

							for (AtnParseResult parseResult : output.getParseResults()) {

								System.out.println("\t<list>");

								final int numParses = parseResult.getNumParses();
								for (int parseNum = 0; parseNum < numParses; ++parseNum) {

									final AtnParse parse = parseResult.getParse(parseNum);

									if (onlySelected && parse.getSelected() || !onlySelected) {
										System.out.println("\t\t<item>" + parse.getParseTree().toString() + "</item>");
									}
								}

								System.out.println("\t</list>");
							}
              
							System.out.println("</list>");
						}
						else {
							System.out.println("\n*No Parse Results*");
						}
					}
				}
			}
		}
  }



  private final class TestParams {

    public final DomElement testElement;

    public TestParams(DomElement testElement) {
      this.testElement = testElement;
    }

    public DomElement getInput(String nodeName) {
      return (DomElement)testElement.selectSingleNode("inputs/" + nodeName);
    }

    public DomElement getExpectation(String nodeName) {
      return (DomElement)testElement.selectSingleNode("expectations/" + nodeName);
    }

    public List<List<String>> loadNestedLists(String expectationId) {
      if (expectationId == null) return null;

      final DomElement element = getExpectation(expectationId);
      final DomElement listElement = (DomElement)element.selectSingleNode("list");
      return loadNestedLists(listElement);
    }

    private final List<List<String>> loadNestedLists(DomElement domElement) {
			final List<List<String>> result = new ArrayList<List<String>>();

      final NodeList nodeList = domElement.selectNodes("list");
      for (int i = 0; i < nodeList.getLength(); ++i) {
        final DomElement listNode = (DomElement)nodeList.item(i);
        result.add(loadList(listNode));
			}

			return result;
    }

    private final List<String> loadList(DomElement listNode) {
			final List<String> result = new ArrayList<String>();

      final NodeList nodeList = listNode.selectNodes("item");
      for (int i = 0; i < nodeList.getLength(); ++i ) {
        final DomElement itemNode = (DomElement)nodeList.item(i);
				result.add(itemNode.getTextContent());
			}

			return result;
    }
  }

  private final class TestParamsContainer {
    public final DomElement testRoot;
    public final Map<String, TestParams> testId2Params;

    public TestParamsContainer(InputStream testInputStream) throws IOException {
      final DomDocument domDocument = XmlFactory.loadDocument(testInputStream, false, null);
      this.testRoot = domDocument.getDocumentDomElement();
      this.testId2Params = new HashMap<String, TestParams>();

      final NodeList testNodes = testRoot.selectNodes("test");
      for (int i = 0; i < testNodes.getLength(); ++i) {
        final DomElement testElement = (DomElement)testNodes.item(i);

        final DomElement testIdElement = (DomElement)testElement.selectSingleNode("id");
        final String id = testIdElement.getTextContent();
        testId2Params.put(id, new TestParams(testElement));
      }
    }
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestParseConfig.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
