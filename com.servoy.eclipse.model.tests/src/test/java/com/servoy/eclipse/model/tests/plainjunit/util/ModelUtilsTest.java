package com.servoy.eclipse.model.tests.plainjunit.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.servoy.eclipse.model.util.ModelUtils;

class ModelUtilsTest
{
	@Test
	@DisplayName("the UI-disabled flag defaults to false and can be toggled")
	void uiDisabled()
	{
		assertFalse(ModelUtils.isUIDisabled());
		ModelUtils.setUIDisabled(true);
		assertTrue(ModelUtils.isUIDisabled());
		ModelUtils.setUIDisabled(false);
		assertFalse(ModelUtils.isUIDisabled());
	}

	@Test
	@DisplayName("getAdapter returns the object when it is already assignable")
	void adapter()
	{
		assertEquals("x", ModelUtils.getAdapter("x", CharSequence.class));
		assertEquals(null, ModelUtils.getAdapter(null, String.class));
	}
}
