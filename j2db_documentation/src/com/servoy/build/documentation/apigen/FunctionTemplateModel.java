/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2016 Servoy BV

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

package com.servoy.build.documentation.apigen;

import com.servoy.j2db.documentation.ClientSupport;
import com.servoy.j2db.documentation.IFunctionDocumentation;

/**
 * @author jcompagner
 *
 */
public class FunctionTemplateModel
{

	private final IFunctionDocumentation obj;
	private final ConfluenceGenerator cg;

	/**
	 * @param obj
	 */
	public FunctionTemplateModel(IFunctionDocumentation obj, ConfluenceGenerator cg)
	{
		this.obj = obj;
		this.cg = cg;
	}

	public String getSummary()
	{
		String summary = obj.getSummary(ClientSupport.Default);
		if (summary == null) return getDescription();
		return summary;
	}

	public String getDescription()
	{
		String description = obj.getDescription(ClientSupport.Default);
		if (description == null)
		{
			return "";
		}
		return description;
	}

	public String getReturnType()
	{
		return cg.getPublicName(obj.getReturnedType());
	}

	/**
	 * @return
	 */
	public String getSampleCode()
	{
		String sample = obj.getSample(ClientSupport.Default);
		if (sample == null) return "";
		return sample.replace("%%prefix%%", "");
	}

}