package com.servoy.eclipse.model.tests.plainjunit.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.servoy.eclipse.model.repository.SolutionDeserializer;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.util.Pair;

class SolutionDeserializerStaticsTest
{
	@Test
	@DisplayName("getJSONFileFromJS maps a form js file to its frm file")
	void formJs() throws Exception
	{
		File forms = new File(System.getProperty("java.io.tmpdir"), "forms");
		File js = new File(forms, "f1.js");
		Pair<File, ISupportChilds> result = SolutionDeserializer.getJSONFileFromJS(js, "f1.js", null);
		assertEquals("f1.frm", result.getLeft().getName());
		assertNull(result.getRight());
	}

	@Test
	@DisplayName("getJSONFileFromJS maps a calculations js file to its table node")
	void calculationsJs() throws Exception
	{
		File ds = new File(System.getProperty("java.io.tmpdir"), "datasources");
		File tbl = new File(ds, "tbl");
		File js = new File(tbl, "orders_calculations.js");
		Pair<File, ISupportChilds> result = SolutionDeserializer.getJSONFileFromJS(js, "orders_calculations.js", null);
		assertEquals("orders.tbl", result.getLeft().getName());
	}

	@Test
	@DisplayName("getJSONFileFromJS returns no file for an unrelated js file")
	void otherJs() throws Exception
	{
		File dir = new File(System.getProperty("java.io.tmpdir"), "other");
		File js = new File(dir, "x.js");
		assertNull(SolutionDeserializer.getJSONFileFromJS(js, "x.js", null).getLeft());
	}
}