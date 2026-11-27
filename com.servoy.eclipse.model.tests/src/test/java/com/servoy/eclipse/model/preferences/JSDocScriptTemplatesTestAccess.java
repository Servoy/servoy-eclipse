package com.servoy.eclipse.model.preferences;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;

public class JSDocScriptTemplatesTestAccess extends JSDocScriptTemplates
{
	public JSDocScriptTemplatesTestAccess(IEclipsePreferences prefs)
	{
		super(prefs);
	}

	public String replaceVarsPublic(String str)
	{
		return replaceVars(str);
	}
}
