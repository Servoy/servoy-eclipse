/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2026 Servoy BV

 This program is free software; you can redistribute it and/or modify it under
 the terms of the GNU Affero General Public License as published by the Free
 Software Foundation; either version 3 of the License, or (at your option) any
 later version.

 This program is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along
 with this program; if not, see http://www.gnu.org/licenses or write to the Free
 Software Foundation,Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
*/

package com.servoy.eclipse.model.tests.plainjunit.war;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.servoy.eclipse.model.war.exporter.WarExporter;
import com.servoy.eclipse.model.war.exporter.WarExporterTestAccess;

/**
 * SVY-21642 - tests the Tomcat precompressed DefaultServlet injection into the
 * generated web.xml ({@code WarExporter.injectPrecompressedServlet},
 * package-private, reached through {@link WarExporterTestAccess}).
 */
class WarExporterPrecompressedWebXmlTest {
	private static final String CLOSE = "</web-app>";
	private static String snippet;
	private static String template;

	@BeforeAll
	static void init() throws Exception {
		snippet = WarExporterTestAccess.precompressedServletSnippet();
		try (InputStream is = WarExporter.class.getResourceAsStream("resources/web.xml")) {
			assertNotNull(is, "web.xml template resource must be reachable");
			template = new String(is.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	private static String inject(String s) {
		return WarExporterTestAccess.injectPrecompressedServlet(s);
	}

	private static int count(String s, String what) {
		int c = 0;
		int i = -1;
		while ((i = s.indexOf(what, i + 1)) != -1)
			c++;
		return c;
	}

	private static Document parse(String xml) throws Exception {
		DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
		f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		f.setFeature("http://xml.org/sax/features/external-general-entities", false);
		f.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
		f.setXIncludeAware(false);
		f.setExpandEntityReferences(false);
		return f.newDocumentBuilder()
				.parse(new InputSource(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))));
	}

	@Test
	@DisplayName("snippet is inserted exactly once, right before the last closing web-app tag, rest is untouched")
	void insertsOnceBeforeLastClose() throws Exception {
		String result = inject(template);
		int last = template.lastIndexOf(CLOSE);
		assertAll( //
				() -> assertEquals(1, count(result, snippet), "snippet must occur exactly once"), //
				() -> assertEquals(template.substring(0, last) + snippet + template.substring(last), result,
						"only the snippet is added"), //
				() -> assertEquals(count(template, CLOSE), count(result, CLOSE), "no closing tag added or removed"));
	}

	@Test
	@DisplayName("result is well-formed XML and declares the default servlet with precompressed br=.br,gzip=.gz")
	void resultIsWellFormedWithDefaultServlet() throws Exception {
		Document doc = parse(inject(template));
		NodeList servlets = doc.getDocumentElement().getElementsByTagName("servlet");
		Element def = null;
		for (int i = 0; i < servlets.getLength(); i++) {
			Element s = (Element) servlets.item(i);
			if ("default".equals(s.getElementsByTagName("servlet-name").item(0).getTextContent().trim()))
				def = s;
		}
		assertNotNull(def, "servlet named default must be declared");
		assertEquals("org.apache.catalina.servlets.DefaultServlet",
				def.getElementsByTagName("servlet-class").item(0).getTextContent().trim());
		Map<String, String> params = new HashMap<>();
		NodeList ips = def.getElementsByTagName("init-param");
		for (int i = 0; i < ips.getLength(); i++) {
			Element ip = (Element) ips.item(i);
			params.put(ip.getElementsByTagName("param-name").item(0).getTextContent().trim(),
					ip.getElementsByTagName("param-value").item(0).getTextContent().trim());
		}
		assertAll( //
				() -> assertEquals("br=.br,gzip=.gz", params.get("precompressed")), //
				() -> assertEquals("0", params.get("debug")), //
				() -> assertEquals("false", params.get("listings")), //
				() -> assertEquals(3, params.size(), "exactly the three init-params"));
	}

	@Test
	@DisplayName("template version comment stays version:7 (checkWebXML is a strict equality check)")
	void versionCommentUnchanged() throws Exception {
		String result = inject(template);
		assertAll( //
				() -> assertTrue(template.contains("servoy web.xml version:7"), "template is version 7"), //
				() -> assertTrue(result.contains("servoy web.xml version:7"), "injection keeps version 7"), //
				() -> assertEquals(0, count(result, "servoy web.xml version:8")));
	}

	@Test
	@DisplayName("input without closing web-app tag is rejected with IllegalArgumentException")
	void missingClosingTagThrows() {
		Exception e = assertThrows(Exception.class, () -> inject("<web-app><display-name>x</display-name>"));
		assertInstanceOf(IllegalArgumentException.class, e, "missing </web-app> must be reported");
	}

	@Test
	@DisplayName("only the last closing tag is used when the text appears more than once")
	void usesLastClosingTag() throws Exception {
		String input = "<a><!-- </web-app> --></a>\n</web-app>";
		int last = input.lastIndexOf(CLOSE);
		assertEquals(input.substring(0, last) + snippet + CLOSE, inject(input));
	}

	@Test
	@DisplayName("template itself has no DefaultServlet declaration")
	void templateHasNoDefaultServlet() {
		assertEquals(0, count(template, "org.apache.catalina.servlets.DefaultServlet"));
	}
}
