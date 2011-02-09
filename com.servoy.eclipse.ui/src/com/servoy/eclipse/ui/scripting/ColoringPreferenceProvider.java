package com.servoy.eclipse.ui.scripting;

import org.eclipse.dltk.ui.coloring.IColoringPreferenceProvider;
import org.eclipse.dltk.ui.coloring.IColoringPreferenceRequestor;
import org.eclipse.swt.graphics.RGB;

import com.servoy.j2db.scripting.IExecutingEnviroment;

@SuppressWarnings("nls")
public class ColoringPreferenceProvider implements IColoringPreferenceProvider
{

	private static String SERVOY_CAT = "Servoy Keywords";

//	private static String SERVOY_OP = "Math Operators";
//	private static String SERVOY_COMPARE_OPERATORS = "Compare operators";
//	private static String SERVOY_AUX_OPERATOR = "Additional";


	public ColoringPreferenceProvider()
	{
	}

	public void providePreferences(IColoringPreferenceRequestor requestor)
	{
		requestor.enterCategory(SERVOY_CAT);

		requestor.addPreference(IExecutingEnviroment.TOPLEVEL_JSUNIT, IExecutingEnviroment.TOPLEVEL_JSUNIT, new RGB(0, 200, 0));
		requestor.addPreference(IExecutingEnviroment.TOPLEVEL_UTILS, IExecutingEnviroment.TOPLEVEL_UTILS, new RGB(0, 200, 0));
		requestor.addPreference(IExecutingEnviroment.TOPLEVEL_SECURITY, IExecutingEnviroment.TOPLEVEL_SECURITY, new RGB(0, 200, 0));
		requestor.addPreference("elements", "elements", new RGB(100, 200, 0));
		requestor.addPreference("controller", "controller", new RGB(50, 200, 0));
		requestor.addPreference("currentcontroller", "currentcontroller", new RGB(50, 200, 0));
		requestor.addPreference(IExecutingEnviroment.TOPLEVEL_APPLICATION, IExecutingEnviroment.TOPLEVEL_APPLICATION, new RGB(0, 200, 0));
		requestor.addPreference(IExecutingEnviroment.TOPLEVEL_DATABASE_MANAGER, IExecutingEnviroment.TOPLEVEL_DATABASE_MANAGER, new RGB(0, 200, 0));
		requestor.addPreference(IExecutingEnviroment.TOPLEVEL_SOLUTION_MODIFIER, IExecutingEnviroment.TOPLEVEL_SOLUTION_MODIFIER, new RGB(0, 200, 0));
		requestor.addPreference("globals", "globals", new RGB(0, 200, 50));
		requestor.addPreference(IExecutingEnviroment.TOPLEVEL_FORMS, IExecutingEnviroment.TOPLEVEL_FORMS, new RGB(0, 200, 100));
		requestor.addPreference(IExecutingEnviroment.TOPLEVEL_HISTORY, IExecutingEnviroment.TOPLEVEL_HISTORY, new RGB(0, 200, 0));
		requestor.addPreference(IExecutingEnviroment.TOPLEVEL_PLUGINS, IExecutingEnviroment.TOPLEVEL_PLUGINS, new RGB(255, 0, 0));
		requestor.addPreference("_super", "_super", new RGB(0, 200, 50));
	}
}
