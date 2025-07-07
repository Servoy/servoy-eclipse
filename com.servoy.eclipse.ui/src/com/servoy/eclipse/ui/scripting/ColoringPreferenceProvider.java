/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2011 Servoy BV

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

package com.servoy.eclipse.ui.scripting;

import org.eclipse.dltk.ui.coloring.IColoringPreferenceProvider;
import org.eclipse.dltk.ui.coloring.IColoringPreferenceRequestor;
import org.eclipse.swt.graphics.RGB;

import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.scripting.IExecutingEnviroment;

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
		requestor.addPreference(IExecutingEnviroment.TOPLEVEL_CLIENTUTILS, IExecutingEnviroment.TOPLEVEL_CLIENTUTILS, new RGB(0, 200, 0));
		requestor.addPreference(IExecutingEnviroment.TOPLEVEL_SECURITY, IExecutingEnviroment.TOPLEVEL_SECURITY, new RGB(0, 200, 0));
		requestor.addPreference("elements", "elements", new RGB(100, 200, 0));
		requestor.addPreference("controller", "controller", new RGB(50, 200, 0));
		requestor.addPreference("currentcontroller", "currentcontroller", new RGB(50, 200, 0));
		requestor.addPreference(IExecutingEnviroment.TOPLEVEL_APPLICATION, IExecutingEnviroment.TOPLEVEL_APPLICATION, new RGB(0, 200, 0));
		requestor.addPreference(IExecutingEnviroment.TOPLEVEL_DATABASE_MANAGER, IExecutingEnviroment.TOPLEVEL_DATABASE_MANAGER, new RGB(0, 200, 0));
		requestor.addPreference(IExecutingEnviroment.TOPLEVEL_DATASOURCES, IExecutingEnviroment.TOPLEVEL_DATASOURCES, new RGB(0, 200, 0));
		requestor.addPreference(IExecutingEnviroment.TOPLEVEL_SOLUTION_MODIFIER, IExecutingEnviroment.TOPLEVEL_SOLUTION_MODIFIER, new RGB(0, 200, 0));
		requestor.addPreference(ScriptVariable.GLOBAL_SCOPE, ScriptVariable.GLOBAL_SCOPE, new RGB(0, 200, 50));
		requestor.addPreference(IExecutingEnviroment.TOPLEVEL_SCOPES, IExecutingEnviroment.TOPLEVEL_SCOPES, new RGB(0, 200, 50));
		requestor.addPreference(IExecutingEnviroment.TOPLEVEL_FORMS, IExecutingEnviroment.TOPLEVEL_FORMS, new RGB(0, 200, 100));
		requestor.addPreference(IExecutingEnviroment.TOPLEVEL_HISTORY, IExecutingEnviroment.TOPLEVEL_HISTORY, new RGB(0, 200, 0));
		requestor.addPreference(IExecutingEnviroment.TOPLEVEL_MENUS, IExecutingEnviroment.TOPLEVEL_MENUS, new RGB(0, 200, 0));
		requestor.addPreference(IExecutingEnviroment.TOPLEVEL_EVENTTYPES, IExecutingEnviroment.TOPLEVEL_EVENTTYPES, new RGB(0, 200, 0));
		requestor.addPreference(IExecutingEnviroment.TOPLEVEL_PLUGINS, IExecutingEnviroment.TOPLEVEL_PLUGINS, new RGB(255, 0, 0));
		requestor.addPreference("_super", "_super", new RGB(0, 200, 50));
	}
}
