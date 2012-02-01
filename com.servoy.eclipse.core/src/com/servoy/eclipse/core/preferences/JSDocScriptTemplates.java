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
package com.servoy.eclipse.core.preferences;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.servoy.eclipse.model.nature.ServoyProject;

/**
 * Preferences for templates in jsdoc for new methods and variables.
 * Are stored at project level.
 * 
 * @author rgansevles
 * 
 * @since 6.1
 * 
 */
public class JSDocScriptTemplates extends ProjectPreferences
{
	public static final String METHOD_TEMPLATE_SETTING = "methodTemplate";
	public static final String VARIABLE_TEMPLATE_SETTING = "variableTemplate";

	public JSDocScriptTemplates(ServoyProject project)
	{
		super(project, "jsdoctemplates");
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
		return getProperty(METHOD_TEMPLATE_SETTING, "");
	}

	public void setMethodTemplateProperty(String template)
	{
		setProperty(METHOD_TEMPLATE_SETTING, template);
	}

	public String getVariableTemplateProperty()
	{
		return getProperty(VARIABLE_TEMPLATE_SETTING, "");
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
}
