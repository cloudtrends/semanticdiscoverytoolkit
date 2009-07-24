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


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

//import org.sd.domain.DomainFieldId;
import org.sd.nlp.NormalizedString;
import org.sd.text.IndexingNormalizer;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;

/**
 * JUnit Tests for the SdTokenStream class.
 * <p>
 * @author Spence Koehler
 */
public class TestSdTokenStream extends TestCase {

  public TestSdTokenStream(String name) {
    super(name);
  }
  

  private final void verifyTokens(TokenStream tokenStream, String[] expected) throws IOException {
    int index = 0;
    Token token = new Token();
    for (token = tokenStream.next(token); token != null; token = tokenStream.next(token)) {
      final char[] termBuffer = token.termBuffer();
      final int len = token.termLength();
      final String term = new String(termBuffer, 0, len);

      if (expected != null) {
        assertTrue("got " + term + " at index=" + index, index < expected.length);
        assertEquals("got " + term + " at index=" + index, expected[index], term);
      }
      else {
        System.out.println(index + ":" + term);
      }

      ++index;
    }
    if (expected != null) {
      assertEquals(expected.length, index);
    }
  }

  public void testNormalizedStringForm() throws IOException {
    final IndexingNormalizer normalizer = IndexingNormalizer.getInstance(IndexingNormalizer.ALL_OPTIONS);
    final NormalizedString nString = normalizer.normalize("a foo b bar c baz");
    final String[] expected = new String[]{"a", "foo", "b", "bar", "c", "baz"};

    final SdTokenStream tokenStream = new SdTokenStream(nString);
    tokenStream.reset();
    verifyTokens(tokenStream, expected);

    tokenStream.reset();
    verifyTokens(tokenStream, expected);
  }

  public void testReaderForm() throws IOException {
    final IndexingNormalizer normalizer = IndexingNormalizer.getInstance(IndexingNormalizer.ALL_OPTIONS);
    final Reader reader = new StringReader("a foo b bar c baz\n\nnow is the time\n");
    final String[] expected = new String[]{"a", "foo", "b", "bar", "c", "baz", "now", "is", "the", "time"};

    final SdTokenStream tokenStream = new SdTokenStream(reader, normalizer);
    tokenStream.reset();
    verifyTokens(tokenStream, expected);

    tokenStream.reset();
    verifyTokens(tokenStream, expected);
  }

//   public void testFoo() throws IOException {
//     final SdAnalyzer sdAnalyzer = new SdAnalyzer(IndexingNormalizer.getInstance(IndexingNormalizer.DEFAULT_INDEXING_OPTIONS), DomainFieldId.DEFAULT_STOPWORDS, true);
//     final TokenStream tokenStream =
//       sdAnalyzer.tokenStream(
//         DomainFieldId.FULL_CONTENT.getLabel(),
//         new StringReader(
//           "stored/compressed,indexed,tokenized<fullContent:Asian Manufacturers, Asian Suppliers - AsianNet / Global B2B - Asia, China, Taiwan, Trade Directory, Marketplace, Chinese Products, Taiwan Products, China Exporters, Taiwan Exporters\n" +
// "\n" +
// "\n" +
// "Sign In\n" +
// "|\n" +
// "My Manager\n" +
// "|\n" +
// "Join Free\n" +
// "Global business starts here:\n" +
// "Companies\n" +
// "Products\n" +
// "Buy Leads\n" +
// "Sell Leads\n" +
// "501 New Members Today, 1,340,035 Trade Opportunities, 630,439 Companies, 943,218 Products\n" +
// "Sponsor\n" +
// "Catering\n" +
// "Ribbon & Label\n" +
// "Mesh Chair\n" +
// "Plate Levers\n" +
// "Testing Machine\n" +
// "Knife Set\n" +
// "Hand Tool\n" +
// "Casting\n" +
// "Bluetooth GPS\n" +
// "Panel Sander\n" +
// "Queue Stand\n" +
// "Tire\n" +
// "Display Baskets\n" +
// "Jelly Cup\n" +
// "\n" +
// "More Leads >>\n" +
// "Buy:\n" +
// "want ss seamless pipes\n" +
// "Jul 22\n" +
// "Buy:\n" +
// "Bell for baby rattles\n" +
// "Jul 22\n" +
// "Buy:\n" +
// "Bulk Buying\n" +
// "Jul 22\n" +
// "Buy:\n" +
// "Want to buy Frozen Fish\n" +
// "Jul 22\n" +
// "Buy:\n" +
// "Demand Goods Advertised\n" +
// "Jul 22\n" +
// "Buy:\n" +
// "Interested in Bathroom ..\n" +
// "Jul 22\n" +
// "Buy:\n" +
// "Inquiry of Ceramic Tile ..\n" +
// "Jul 22\n" +
// "Buy:\n" +
// "Request for Garden and ..\n" +
// "Jul 22\n" +
// "Buy:\n" +
// "In need of Whole Furnit ..\n" +
// "Jul 22\n" +
// "Buy:\n" +
// "Interested in Guitar\n" +
// "Jul 22\n" +
// "Buy:\n" +
// "Searching for Digital T ..\n" +
// "Jul 22\n" +
// "Buy:\n" +
// "Need Glucometer, Rapid ..\n" +
// "Jul 22\n" +
// "Buy:\n" +
// "In need of Autoparts an ..\n" +
// "Jul 22\n" +
// "Buy:\n" +
// "Request for Yarns and T ..\n" +
// "Jul 22\n" +
// "Buy:\n" +
// "Inquiry of Cameras, Pho ..\n" +
// "Jul 22\n" +
// "More News >>\n" +
// "Conductors (AAC and AC ..\n" +
// "Jul 22\n" +
// "PVC Insulated Wires\n" +
// "Jul 22\n" +
// "XLPE Insulated Power C ..\n" +
// "Jul 22\n" +
// "Double Zero Foil\n" +
// "Jul 22\n" +
// "PS Baseplate\n" +
// "Jul 22\n" +
// "SEAMLESS STEEL PIPE/TU ..\n" +
// "Jul 22\n" +
// "Bemitallic Parallel Gr ..\n" +
// "Jul 22\n" +
// "Aluminium Parallel Gro ..\n" +
// "Jul 22\n" +
// "Copper Parallel Groove ..\n" +
// "Jul 22\n" +
// "Browse\n" +
// "--\n" +
// "Companies\n" +
// "|\n" +
// "Products\n" +
// "|\n" +
// "Trade Leads\n" +
// "-\n" +
// "BUY\n" +
// "|\n" +
// "SELL\n" +
// "Find buyers or sellers?\n" +
// "Agricultural, Animal & Aquatic\n" +
// "Apparel & Fashion\n" +
// "Automobiles & Motorcycles\n" +
// "Beauty Supplies & Cosmetic\n" +
// "Bicycles & Tricycles\n" +
// "Buildings & Construction\n" +
// "Chemical Products\n" +
// "Computer & Communication\n" +
// "Electrical Supplies\n" +
// "Electronic Supplies\n" +
// "Foods, Snacks & Beverages\n" +
// "Furniture & Furnishings\n" +
// "Games & Accessories\n" +
// "Gifts & Crafts\n" +
// "Health & Pharmaceutical\n" +
// "Household & Restaurant Supplies\n" +
// "Jewelry, Valuables & Accessories\n" +
// "Leather & Shoes\n" +
// "Machinery & Industrial Supplies\n" +
// "Measuring Instruments\n" +
// "Metals & Hardware Products\n" +
// "Optical Products & Cameras\n" +
// "Packaging Products & Equipment\n" +
// "Plastic & Rubber Products\n" +
// "Security, Alarm & Safety\n" +
// "Sporting & Leisure Goods\n" +
// "Stationery & Office Supplies\n" +
// "Textile & Fibers\n" +
// "Tools\n" +
// "Water Treatment Products\n" +
// "Premier Suppliers:\n" +
// "Adaptor\n" +
// "-\n" +
// "Thermometers\n" +
// "-\n" +
// "Metal Hardware\n" +
// "-\n" +
// "Shoring\n" +
// "-\n" +
// "Door lock\n" +
// "-\n" +
// "Industrial Chemical\n" +
// "-\n" +
// "Pouch Bag\n" +
// "-\n" +
// "Fuel Additive\n" +
// "-\n" +
// "Capacitors\n" +
// "-\n" +
// "Aquaculture Machinery\n" +
// "-\n" +
// "Insect Repellent\n" +
// "-\n" +
// "Embroidery\n" +
// "-\n" +
// "Tire Building Machine\n" +
// "-\n" +
// "Security Detector\n" +
// "-\n" +
// "Furnace\n" +
// "-\n" +
// "Lock\n" +
// "-\n" +
// "Hydraulic Coupling\n" +
// "-\n" +
// "Die Casting\n" +
// "-\n" +
// "Hospital Furniture\n" +
// "-\n" +
// "Plastic Container\n" +
// "-\n" +
// "Automobile Parts\n" +
// "-\n" +
// "Plastic bags\n" +
// "-\n" +
// "Cleaning Brush\n" +
// "-\n" +
// "RF Connector Parts\n" +
// "-\n" +
// "Plastic Injection\n" +
// "-\n" +
// "Casting\n" +
// "-\n" +
// "Formosan Teas\n" +
// "-\n" +
// "Plastic Injection Molding Machine\n" +
// "-\n" +
// "Rearview mirrors\n" +
// "-\n" +
// "Exercise Equipment\n" +
// "-\n" +
// "Hardware\n" +
// "-\n" +
// "Oxy Hydrogen\n" +
// "-\n" +
// "Screws\n" +
// "-\n" +
// "Plastic Processing Machinery\n" +
// "-\n" +
// "Cutting Machine\n" +
// "-\n" +
// "Knife Manufacturer\n" +
// "-\n" +
// "Soft material product\n" +
// "-\n" +
// "Preform\n" +
// "-\n" +
// "White Board\n" +
// "-\n" +
// "Powder Metallurgy\n" +
// "-\n" +
// "Precision Springs\n" +
// "-\n" +
// "Office Chair\n" +
// "-\n" +
// "Labeling Machine\n" +
// "-\n" +
// "Mill\n" +
// "-\n" +
// "Pet Appliances\n" +
// "-\n" +
// "Container\n" +
// "-\n" +
// "Wiper\n" +
// "-\n" +
// "Food Saver\n" +
// "-\n" +
// "Thermal Grease\n" +
// "-\n" +
// "Drinking disposable cup\n" +
// "-\n" +
// "Mining Equipment\n" +
// "-\n" +
// "Packing Materials & Machinery\n" +
// "-\n" +
// "Pre-stressing Material & Equipment\n" +
// "-\n" +
// "Server Rack\n" +
// "-\n" +
// "Fitness Equipment\n" +
// "-\n" +
// "Fitness equipment\n" +
// "-\n" +
// "Vertical Machining Center\n" +
// "-\n" +
// "Packaging Bag\n" +
// "-\n" +
// "Embroidery\n" +
// "-\n" +
// "Vacuum Components\n" +
// "-\n" +
// "Massage Chair\n" +
// "-\n" +
// "Shaker\n" +
// "-\n" +
// "Bike Parts\n" +
// "-\n" +
// "Tester\n" +
// "-\n" +
// "Vibrating Ring\n" +
// "-\n" +
// "Inline Speed Wheels\n" +
// "-\n" +
// "CNC Machining\n" +
// "-\n" +
// "Thermally Conductive Plastic\n" +
// "-\n" +
// "Flow Wrapper\n" +
// "-\n" +
// "Yoga Mat\n" +
// "-\n" +
// "Idler\n" +
// "-\n" +
// "Cable\n" +
// "-\n" +
// "RO water system\n" +
// "-\n" +
// "Environmental Testing Equipment\n" +
// "-\n" +
// "Anaglyph Glazed Ceramic Mosaic\n" +
// "-\n" +
// "Embroidered Badges\n" +
// "-\n" +
// "MIM Products\n" +
// "-\n" +
// "Plastic Mould\n" +
// "-\n" +
// "Holiday Decoration\n" +
// "-\n" +
// "Ball valve\n" +
// "-\n" +
// "Painting Brush\n" +
// "-\n" +
// "Aquaculture Equipment\n" +
// "-\n" +
// "Office Application Furniture\n" +
// "-\n" +
// "Curtain Fabrics\n" +
// "-\n" +
// "LED Display\n" +
// "-\n" +
// "LCD clock\n" +
// "-\n" +
// "Food Processing Mchinery\n" +
// "-\n" +
// "Mattress Ticking\n" +
// "-\n" +
// "Optical Transceivers\n" +
// "-\n" +
// "Thin Film Resistor\n" +
// "-\n" +
// "IR Filter\n" +
// "-\n" +
// "Powder metallurgy\n" +
// "-\n" +
// "Wireless LAN Antenna\n" +
// "-\n" +
// "Helmet\n" +
// "-\n" +
// "Nonwoven Machine\n" +
// "-\n" +
// "Carbon fiber\n" +
// "-\n" +
// "Filling Machine\n" +
// "-\n" +
// "Spring Roll Machine\n" +
// "-\n" +
// "Water Filter\n" +
// "-\n" +
// "Fastener\n" +
// "-\n" +
// "Juicer\n" +
// "-\n" +
// "Balun\n" +
// "-\n" +
// "Chipboard Screws\n" +
// "-\n" +
// "Injection Mold\n" +
// "-\n" +
// "Panel Saw\n" +
// "-\n" +
// "Autoclave\n" +
// "-\n" +
// "Electronic Manufacturing Service\n" +
// "-\n" +
// "Indoor Lamp\n" +
// "-\n" +
// "Manometer\n" +
// "-\n" +
// "Optical Transceiver\n" +
// "-\n" +
// "Optical Transceiver\n" +
// "-\n" +
// "Fitness Equipment\n" +
// "-\n" +
// "Cosmetic Machine\n" +
// "-\n" +
// "Industrial Motherboard\n" +
// "-\n" +
// "Car Brakes\n" +
// "-\n" +
// "Packaging Bag\n" +
// "-\n" +
// "Auto Tools\n" +
// "-\n" +
// "Trash Can\n" +
// "-\n" +
// "Zinc Oxide\n" +
// "-\n" +
// "Jelly\n" +
// "-\n" +
// "Softener\n" +
// "-\n" +
// "Fabric\n" +
// "-\n" +
// "Can Food\n" +
// "-\n" +
// "Industrial Oven\n" +
// "-\n" +
// "Blind Rivets\n" +
// "-\n" +
// "Power Amplifier\n" +
// "-\n" +
// "Print & Cut series\n" +
// "-\n" +
// "Diversity Functions Screws\n" +
// "-\n" +
// "Rubber Tires\n" +
// "-\n" +
// "Plastic injection\n" +
// "-\n" +
// "Knife Maker\n" +
// "-\n" +
// "Recorders\n" +
// "-\n" +
// "Thermoplastics Compounds\n" +
// "-\n" +
// "Cell phone jammer\n" +
// "-\n" +
// "Siangsiu TW\n" +
// "-\n" +
// "Hand Tool\n" +
// "-\n" +
// "Plating Equipment\n" +
// "-\n" +
// "Connector\n" +
// "-\n" +
// "GPS Antenna\n" +
// "-\n" +
// "Wafer Frame\n" +
// "-\n" +
// "Panel PC\n" +
// "-\n" +
// "Car Accessories\n" +
// "-\n" +
// "USB Connectors\n" +
// "-\n" +
// "Wet Suit\n" +
// "-\n" +
// "Resins\n" +
// "-\n" +
// "Generator\n" +
// "-\n" +
// "GPS\n" +
// "-\n" +
// "Exercise Mats\n" +
// "-\n" +
// "Spring\n" +
// "-\n" +
// "Fresnel Lens\n" +
// "-\n" +
// "Sports Supports\n" +
// "-\n" +
// "Aluminum\n" +
// "-\n" +
// "CNC Machining Center\n" +
// "-\n" +
// "Polycarbonate\n" +
// "-\n" +
// "Strapping machine\n" +
// "-\n" +
// "Linear Diaphragm Air Pumps\n" +
// "-\n" +
// "LCoS TV\n" +
// "-\n" +
// "Shading Net\n" +
// "-\n" +
// "Cable Assembly\n" +
// "-\n" +
// "Polycarbonate Sheet\n" +
// "-\n" +
// "Quick Die\n" +
// "-\n" +
// "Medical Equipment\n" +
// "-\n" +
// "Auto Door\n" +
// "-\n" +
// "Air Hose\n" +
// "-\n" +
// "Voltage Regulator\n" +
// "-\n" +
// "Knives\n" +
// "-\n" +
// "Sidac\n" +
// "-\n" +
// "Pneumatic Tools\n" +
// "-\n" +
// "Guardian\n" +
// "-\n" +
// "Industrial Furnace\n" +
// "-\n" +
// "Guardian\n" +
// "-\n" +
// "Valves\n" +
// "-\n" +
// "Balancing Machine\n" +
// "-\n" +
// "Cosmetic\n" +
// "-\n" +
// "Wrench\n" +
// "-\n" +
// "Jacquard Fabric\n" +
// "-\n" +
// "Capacitor\n" +
// "-\n" +
// "Silicone Products\n" +
// "-\n" +
// "Massage Chair\n" +
// "-\n" +
// "Crimped Wire Cup Brush\n" +
// "-\n" +
// "Silicone Tape\n" +
// "-\n" +
// "Wood Veneer\n" +
// "-\n" +
// "Door\n" +
// "-\n" +
// "MP3 Player\n" +
// "-\n" +
// "Compression Machine\n" +
// "-\n" +
// "Consumer Audio\n" +
// "-\n" +
// "Laminating Machine\n" +
// "-\n" +
// "Eva Wheel\n" +
// "-\n" +
// "Valve\n" +
// "-\n" +
// "Splitter\n" +
// "-\n" +
// "Fasteners\n" +
// "-\n" +
// "Plastic Thermoforming\n" +
// "-\n" +
// "El Keyboard\n" +
// "-\n" +
// "Ball Valve\n" +
// "-\n" +
// "Material Testing Machine\n" +
// "-\n" +
// "Keychain\n" +
// "-\n" +
// "Chlorella Tablet\n" +
// "-\n" +
// "Nursing Equipments\n" +
// "-\n" +
// "Antenna\n" +
// "-\n" +
// "Screw\n" +
// "-\n" +
// "Paper Bags\n" +
// "-\n" +
// "Silicone\n" +
// "-\n" +
// "Taiwan Embroidery\n" +
// "-\n" +
// "Machining Manufacturing\n" +
// "-\n" +
// "Martial Arts Uniforms\n" +
// "-\n" +
// "Plastic Packaging Molds\n" +
// "-\n" +
// "Pigment\n" +
// "-\n" +
// "Wireless Booster\n" +
// "-\n" +
// "Piston\n" +
// "-\n" +
// "Cover Tape\n" +
// "-\n" +
// "Thermocouple Head\n" +
// "-\n" +
// "Wide Belt Panel Sander\n" +
// "-\n" +
// "Electric Screwdrivers\n" +
// "-\n" +
// "Intensive Mixer\n" +
// "-\n" +
// "Metal Furniture\n" +
// "-\n" +
// "Hardware Accessories\n" +
// "-\n" +
// "Air Pump\n" +
// "-\n" +
// "Spray Gun\n" +
// "-\n" +
// "PC Sheet\n" +
// "-\n" +
// "Cosmetic In Taiwan\n" +
// "-\n" +
// "Packaging Machine\n" +
// "-\n" +
// "Hook\n" +
// "-\n" +
// "Heated Vest\n" +
// "-\n" +
// "Flow Asia\n" +
// "-\n" +
// "Waterjet Cutting Machine\n" +
// "-\n" +
// "LED\n" +
// "-\n" +
// "AC power inverter\n" +
// "-\n" +
// "LCD Panel\n" +
// "-\n" +
// "EMI/RFI Filters\n" +
// "-\n" +
// "Labels\n" +
// "-\n" +
// "Terminals\n" +
// "-\n" +
// "Sealing Machine\n" +
// "-\n" +
// "Thermometer\n" +
// "-\n" +
// "Steel Cutting Rule\n" +
// "-\n" +
// "LED Manufacturer\n" +
// "-\n" +
// "CNC Machining Parts\n" +
// "-\n" +
// "Bakery Machines\n" +
// "-\n" +
// "Pharmaceutical Machinery\n" +
// "-\n" +
// "Industrial Portable PC\n" +
// "-\n" +
// "Playground Equipment\n" +
// "- |\n" +
// "Embroidery Patch\n" +
// "-\n" +
// "Automatic Doors\n" +
// "-\n" +
// "Children's Furniture\n" +
// "-\n" +
// "IP Camera\n" +
// "-\n" +
// "Shopfitting\n" +
// "-\n" +
// "Power Tools\n" +
// "-\n" +
// "AVR\n" +
// "-\n" +
// "Welding\n" +
// "-\n" +
// "Medical Instrument\n" +
// "-\n" +
// "Giftware\n" +
// "-\n" +
// "Toys\n" +
// "-\n" +
// "Optical Communication\n" +
// "-\n" +
// "CCTV Security\n" +
// "-\n" +
// "Film Capacitor\n" +
// "\n" +
// "More Exposure,\n" +
// "More Buyers Match,\n" +
// "Unlimited Number of Products.\n" +
// "Apply now !\n" +
// "I'm a sales manager of an automobile parts manufacturer in China. I used to post our product ...\n" +
// "Jack Yu\n" +
// "Yon Jiuh Enterprise Co., Ltd.\n" +
// "Hi, your website is very useful. It has brought to me two big business partners during short ...\n" +
// "Helen Zhang\n" +
// "Bears International Co., Ltd.\n" +
// "\n" +
// "Popular Products:\n" +
// "\n" +
// "lock\n" +
// "\n" +
// "container\n" +
// "\n" +
// "connector\n" +
// "\n" +
// "fabric\n" +
// "\n" +
// "furniture\n" +
// "\n" +
// "shoes\n" +
// "\n" +
// "tape\n" +
// "\n" +
// "gps\n" +
// "\n" +
// "cosmetic\n" +
// "\n" +
// "bicycle parts\n" +
// "\n" +
// "brake\n" +
// "\n" +
// "ball valve\n" +
// "\n" +
// "tire\n" +
// "\n" +
// "food service\n" +
// "\n" +
// "screw\n" +
// "\n" +
// "hand tool\n" +
// "\n" +
// "hook\n" +
// "\n" +
// "camera\n" +
// "\n" +
// "spin bike\n" +
// "\n" +
// "packaging machine\n" +
// "\n" +
// "automatic door\n" +
// "\n" +
// "casting\n" +
// "\n" +
// "pump\n" +
// "\n" +
// "splitter\n" +
// "\n" +
// "yoga mat\n" +
// "\n" +
// "circuit breaker\n" +
// "\n" +
// "amplifier\n" +
// "\n" +
// "thermometer\n" +
// "\n" +
// "wiper\n" +
// "\n" +
// "piston ring\n" +
// "\n" +
// "massage chair\n" +
// "\n" +
// "filling machine\n" +
// "\n" +
// "air compressor\n" +
// "\n" +
// "resin\n" +
// "\n" +
// "blow gun\n" +
// "\n" +
// "copper\n" +
// "\n" +
// "stainless steel\n" +
// "\n" +
// "monitor\n" +
// "\n" +
// "lcd display\n" +
// "\n" +
// "screwdriver\n" +
// "\n" +
// "\n" +
// "World's Top 5 Trade Sites\n" +
// "|\n" +
// "Dynamic Optimization Sponsors\n" +
// "AsianNet Global Partners\n" +
// "|\n" +
// "World Business Links\n" +
// "Useful Trade Resources\n" +
// "|\n" +
// "Int'l Trade Shows\n" +
// "Home\n" +
// "-\n" +
// "Global Suppliers\n" +
// "-\n" +
// "Best Products\n" +
// "-\n" +
// "Trade InfoCenter\n" +
// "About AsianNet\n" +
// "|\n" +
// "AsianNet Taiwan\n" +
// "|\n" +
// "AsianNet China\n" +
// "|\n" +
// "Terms of Service\n" +
// "|\n" +
// "Privacy Policy\n" +
// "|\n" +
// "Advertise\n" +
// "|\n" +
// "Link Exchange\n" +
// "|\n" +
// "Contact Us\n" +
// "Copyright (c) 2008 AsianNet Inc. All Rights Reserved.>"));

//     verifyTokens(tokenStream, null);
//   }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestSdTokenStream.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
