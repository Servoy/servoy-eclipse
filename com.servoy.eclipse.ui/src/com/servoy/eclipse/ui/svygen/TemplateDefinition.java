/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2025 Servoy BV

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

package com.servoy.eclipse.ui.svygen;

import java.util.HashMap;

import org.json.JSONObject;

/**
 * The definition that maps an AI template to real components... etc. in Servoy.
 *
 * @author acostescu
 */
public class TemplateDefinition
{

	private final HashMap<String, String> propertyAIToRealMap;
	private final String name;
	private final String realSpecType;
	private final String templateStyleToAddToSolution;
	private final String styleHookRoot;

	public TemplateDefinition(String templateStyleToAddToSolution, String templateJSONString)
	{
		this.templateStyleToAddToSolution = templateStyleToAddToSolution;

		JSONObject templateJSON = new JSONObject(templateJSONString);

		this.name = templateJSON.getString("name");
		this.realSpecType = templateJSON.getString("componentRef");

		JSONObject propertyMapJSON = templateJSON.getJSONObject("propertyMap");
		this.propertyAIToRealMap = new HashMap<>();
		propertyMapJSON.keys().forEachRemaining(aiProp -> this.propertyAIToRealMap.put(aiProp, propertyMapJSON.getString(aiProp)));

		JSONObject styleHooks = templateJSON.optJSONObject("styleHooks");
		this.styleHookRoot = ((styleHooks != null) ? styleHooks.optString("root", null) : null);
	}

	public String getRealPropertyFor(String aiPropertyName)
	{
		return propertyAIToRealMap.get(aiPropertyName);
	}

	public String getRealSpecType()
	{
		return realSpecType;
	}

	public String getName()
	{
		return name;
	}

	public String getTemplateStyleToAddToSolution()
	{
		return templateStyleToAddToSolution;
	}

	public String getStyleHookRoot()
	{
		return styleHookRoot;
	}

}
