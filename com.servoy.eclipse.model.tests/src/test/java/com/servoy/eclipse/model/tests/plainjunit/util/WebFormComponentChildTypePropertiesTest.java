package com.servoy.eclipse.model.tests.plainjunit.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.servoy.eclipse.model.util.UtilTestAccess;
import com.servoy.eclipse.model.util.WebFormComponentChildType;
import com.servoy.j2db.persistence.IBasicWebObject;

class WebFormComponentChildTypePropertiesTest
{
	private static final String ROOT_PROPERTY = "containedForm";

	private WebFormComponentChildType child()
	{
		JSONObject rootValue = new JSONObject();
		rootValue.put("svy_form", "7C783D6E-8E26-40B9-8BDA-E2DC4F2ECDF9");
		JSONObject childBlob = new JSONObject();
		childBlob.put("text", "hello");
		rootValue.put("n1", childBlob);
		IBasicWebObject parent = (IBasicWebObject)mock(com.servoy.j2db.persistence.WebComponent.class,
			org.mockito.Mockito.withSettings().extraInterfaces(IBasicWebObject.class));
		when(parent.getProperty(ROOT_PROPERTY)).thenReturn(rootValue);
		when(((com.servoy.j2db.persistence.WebComponent)parent).getAncestor(com.servoy.j2db.persistence.Form.class))
			.thenReturn(mock(com.servoy.j2db.persistence.Form.class));
		return UtilTestAccess.unresolvedFormComponentChild(parent,
			new String[] { "7C783D6E-8E26-40B9-8BDA-E2DC4F2ECDF8", ROOT_PROPERTY, "n1" }, null);
	}

	@Test
	@DisplayName("getProperty and hasProperty read the child's json")
	void read()
	{
		WebFormComponentChildType c = child();
		assertEquals("hello", c.getProperty("text"));
		assertTrue(c.hasProperty("text"));
		assertFalse(c.hasProperty("missing"));
	}

	@Test
	@DisplayName("getPropertiesMap returns the child's properties")
	void map()
	{
		assertEquals("hello", child().getPropertiesMap().get("text"));
	}

}