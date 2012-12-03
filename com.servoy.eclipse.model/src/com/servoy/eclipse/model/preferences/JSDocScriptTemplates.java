/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

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
package com.servoy.eclipse.model.preferences;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.prefs.BackingStoreException;

import com.servoy.eclipse.model.Activator;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.util.ServoyLog;

/**
 * Preferences for templates in jsdoc for new methods and variables.
 * Are stored at project level.
 * 
 * @author rgansevles
 * 
 * @since 6.1
 * 
 */
public class JSDocScriptTemplates
{
	private final IEclipsePreferences settingsNode;

	public final static String JSDOC_SCRIPT_TEMPLATES_NODE = Activator.PLUGIN_ID + "/jsdoctemplates"; //$NON-NLS-1$
	public static final String METHOD_TEMPLATE_SETTING = "methodTemplate";
	public static final String VARIABLE_TEMPLATE_SETTING = "variableTemplate";

	/**
	 * Can be called from anywhere , returns the JSdocScriptTemplates of the active project 
	 * or of the workspace if the active project doesn't have project specific templates defined
	 * @return  JSDocScriptTemplates 
	 */
	public static JSDocScriptTemplates getTemplates()
	{
		IProject project = null;
		if (ServoyModelFinder.getServoyModel().getActiveProject() != null)
		{
			project = ServoyModelFinder.getServoyModel().getActiveProject().getProject();
		}
		return getTemplates(project, true);

	}

	/**
	 * 
	 * @param project the project specific templates to return  or <b>null</b> if workspace settings is desired
	 * @param inherit this parameter is used to search for templates in the workspace settings
	 * @return 
	 */
	public static JSDocScriptTemplates getTemplates(IProject project, boolean inherit)
	{
		if (project != null)
		{ //project specific preferences
			IEclipsePreferences preferences = new ProjectScope(project).getNode(JSDOC_SCRIPT_TEMPLATES_NODE);
			try
			{
				if (preferences.keys().length > 0 || !inherit) return new JSDocScriptTemplates(preferences);
			}
			catch (BackingStoreException e)
			{
				ServoyLog.logError(e);
			}
		}
		//get the workspace preferences
		return new JSDocScriptTemplates(InstanceScope.INSTANCE.getNode(JSDOC_SCRIPT_TEMPLATES_NODE));
	}

	private JSDocScriptTemplates(IEclipsePreferences eclipsePrefferences)
	{
		settingsNode = eclipsePrefferences;
	}

	public String getMethodTemplate()
	{
		return replaceVars(getMethodTemplateProperty());
	}

	public String getVariableTemplate()
	{
		return replaceVars(getVariableTemplateProperty());
	}

	public String getMethodTemplateProperty()
	{
		return settingsNode.get(METHOD_TEMPLATE_SETTING, "");
	}

	public void setMethodTemplateProperty(String template)
	{
		setProperty(METHOD_TEMPLATE_SETTING, template);
	}

	public String getVariableTemplateProperty()
	{
		return settingsNode.get(VARIABLE_TEMPLATE_SETTING, "");
	}

	public void setVariableTemplateProperty(String template)
	{
		setProperty(VARIABLE_TEMPLATE_SETTING, template);
	}


	/**
	 * Replace some ${variable} with system values.
	 */
	protected String replaceVars(String str)
	{
		if (str == null || str.indexOf("${") < 0)
		{
			return str;
		}

		// replace ${key} with substitutions(key)
		Matcher matcher = Pattern.compile("\\$\\{(\\w+)\\}").matcher(str);
		StringBuffer stringBuffer = new StringBuffer(str.length() + 50);
		while (matcher.find())
		{
			String key = matcher.group(1);

			String value = null;
			if ("user".equals(key))
			{
				value = System.getProperty("user.name");
			}
			// TODO: add more keys

			matcher.appendReplacement(stringBuffer, value == null ? "??" + key + "??" : value);
		}
		matcher.appendTail(stringBuffer);
		return stringBuffer.toString();
	}


	protected void setProperty(String key, String value)
	{
		if (value == null || value.length() == 0)
		{
			settingsNode.remove(key);
		}
		else
		{
			settingsNode.put(key, value);
		}
	}

	public void save()
	{
		try
		{
			settingsNode.flush();
		}
		catch (BackingStoreException e)
		{
			ServoyLog.logError(e);
		}
	}

	public void clear()
	{
		try
		{
			settingsNode.clear();
		}
		catch (BackingStoreException e)
		{
			ServoyLog.logError(e);
		}
	}

	public IEclipsePreferences getSettingsNode()
	{
		return settingsNode;
	}
}
