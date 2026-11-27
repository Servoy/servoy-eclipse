package com.servoy.eclipse.model.tests.plainjunit.preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.servoy.eclipse.model.preferences.JSDocScriptTemplates;
import com.servoy.eclipse.model.preferences.JSDocScriptTemplatesTestAccess;

class JSDocScriptTemplatesTest
{
	@Test
	@DisplayName("replaceVars leaves strings without ${...} untouched")
	void noVars()
	{
		JSDocScriptTemplatesTestAccess a = new JSDocScriptTemplatesTestAccess(mock(IEclipsePreferences.class));
		assertEquals("hello", a.replaceVarsPublic("hello"));
		assertEquals(null, a.replaceVarsPublic(null));
	}

	@Test
	@DisplayName("replaceVars substitutes ${user} and marks unknown keys")
	void vars()
	{
		JSDocScriptTemplatesTestAccess a = new JSDocScriptTemplatesTestAccess(mock(IEclipsePreferences.class));
		String user = System.getProperty("user.name");
		assertEquals("hi " + user, a.replaceVarsPublic("hi ${user}"));
		assertEquals("x ??foo?? y", a.replaceVarsPublic("x ${foo} y"));
	}

	@Test
	@DisplayName("template properties are read from preferences and resolved via replaceVars")
	void templates()
	{
		IEclipsePreferences prefs = mock(IEclipsePreferences.class);
		when(prefs.get(JSDocScriptTemplates.METHOD_TEMPLATE_SETTING, "")).thenReturn("method ${user}");
		when(prefs.get(JSDocScriptTemplates.VARIABLE_TEMPLATE_SETTING, "")).thenReturn("var");
		JSDocScriptTemplates t = new JSDocScriptTemplatesTestAccess(prefs);
		assertEquals("method " + System.getProperty("user.name"), t.getMethodTemplate());
		assertEquals("var", t.getVariableTemplate());
		assertEquals("method ${user}", t.getMethodTemplateProperty());
	}

	@Test
	@DisplayName("set*TemplateProperty removes empty values and stores non-empty ones")
	void setProperties()
	{
		IEclipsePreferences prefs = mock(IEclipsePreferences.class);
		JSDocScriptTemplates t = new JSDocScriptTemplatesTestAccess(prefs);
		t.setMethodTemplateProperty("");
		t.setVariableTemplateProperty("v");
		verify(prefs).remove(JSDocScriptTemplates.METHOD_TEMPLATE_SETTING);
		verify(prefs).put(JSDocScriptTemplates.VARIABLE_TEMPLATE_SETTING, "v");
	}
}