package com.servoy.eclipse.model.tests.plainjunit.preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.servoy.eclipse.model.preferences.JSDocScriptTemplatesTestAccess;

class JSDocScriptTemplatesTest
{
	@Test
	@DisplayName("replaceVars leaves strings without ${...} untouched")
	void noVars()
	{
		JSDocScriptTemplatesTestAccess a = new JSDocScriptTemplatesTestAccess(Mockito.mock(IEclipsePreferences.class));
		assertEquals("hello", a.replaceVarsPublic("hello"));
		assertEquals(null, a.replaceVarsPublic(null));
	}

	@Test
	@DisplayName("replaceVars substitutes ${user} and marks unknown keys")
	void vars()
	{
		JSDocScriptTemplatesTestAccess a = new JSDocScriptTemplatesTestAccess(Mockito.mock(IEclipsePreferences.class));
		String user = System.getProperty("user.name");
		assertEquals("hi " + user, a.replaceVarsPublic("hi ${user}"));
		assertEquals("x ??foo?? y", a.replaceVarsPublic("x ${foo} y"));
	}
}