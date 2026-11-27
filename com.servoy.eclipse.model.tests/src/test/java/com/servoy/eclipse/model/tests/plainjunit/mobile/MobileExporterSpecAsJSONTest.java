package com.servoy.eclipse.model.tests.plainjunit.mobile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileWriter;
import java.net.URL;

import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.servoy.eclipse.model.mobile.exporter.MobileExporter;
import org.sablo.specification.WebObjectSpecification;

class MobileExporterSpecAsJSONTest
{
	private static File specFile(String content) throws Exception
	{
		File f = File.createTempFile("spec", ".json");
		f.deleteOnExit();
		try (FileWriter w = new FileWriter(f))
		{
			w.write(content);
		}
		return f;
	}

	@Test
	@DisplayName("getSpecAsJSON returns an empty object for null input")
	void nullInput() throws Exception
	{
		MobileExporter me = new MobileExporter(null);
		assertEquals(0, me.getSpecAsJSON(null).length());
	}

	@Test
	@DisplayName("getSpecAsJSON strips defaults/tags/values from model properties")
	void strips() throws Exception
	{
		String spec = "{\"model\":{\"foo\":{\"type\":\"string\",\"default\":\"x\",\"tags\":[\"t\"],\"values\":[1]},\"bar\":\"int\"},\"api\":{}}";
		URL url = specFile(spec).toURI().toURL();

		WebObjectSpecification wos = mock(WebObjectSpecification.class);
		when(wos.getName()).thenReturn("comp");
		when(wos.getSpecURL()).thenReturn(url);

		MobileExporter me = new MobileExporter(null);
		JSONObject result = me.getSpecAsJSON(new WebObjectSpecification[] { wos });
		JSONObject model = result.getJSONObject("comp").getJSONObject("model");
		JSONObject foo = model.getJSONObject("foo");
		assertEquals("string", foo.getString("type"));
		assertTrue(!foo.has("default"));
		assertTrue(!foo.has("tags"));
		assertTrue(!foo.has("values"));
		JSONObject bar = model.getJSONObject("bar");
		assertEquals("int", bar.getString("type"));
	}
}