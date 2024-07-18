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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.servoy.j2db.documentation.ClientSupport;
import com.servoy.j2db.documentation.IFunctionDocumentation;
import com.servoy.j2db.documentation.IParameterDocumentation;

/**
 * @author jcompagner
 *
 */
public class FunctionTemplateModel
{

	private final IFunctionDocumentation obj;
	private final Function<Class, String> cg;
	private final Class cls;
	private final boolean ngOnly;

	/**
	 * @param obj
	 */
	public FunctionTemplateModel(IFunctionDocumentation obj, Function<Class, String> cg, Class cls, boolean ngOnly)
	{
		this.obj = obj;
		this.cg = cg;
		this.cls = cls;
		this.ngOnly = ngOnly;
	}

	public String getSummary()
	{
		String summary = obj.getSummary(ClientSupport.Default);
		if (summary == null) summary = getDescription();
		return summary.replace('\n', ' ');
	}

	public String getDescription()
	{
		String description = obj.getDescription(ClientSupport.Default);
		if (description == null)
		{
			return "";
		}
		return description.replace("<br>", "").replace("<br/>", "");
	}

	public String getReturnType()
	{
		String returnType = cg.apply(obj.getReturnedType());
		if (returnType == null)
		{
			returnType = "void";
		}
		return returnType;
	}

	public String getReturnTypeDescription()
	{
		return obj.getReturnDescription();
	}

	public List<Parameter> getParameters()
	{
		LinkedHashMap<String, IParameterDocumentation> arguments = obj.getArguments();
		Class< ? >[] argumentsTypes = obj.getArgumentsTypes();
		if (arguments.size() > 0 || (argumentsTypes != null && argumentsTypes.length > 0))
		{
			List<Parameter> parameters = new ArrayList<>();
			if (arguments.size() == 0 && argumentsTypes.length > 0)
			{
				for (Class< ? > argumentType : argumentsTypes)
				{
					String argType = cg.apply(argumentType);
					if ("void".equals(argType)) argType = "Object";
					parameters.add(new Parameter(argType));
				}
			}
			else
			{
				Iterator<IParameterDocumentation> iterator = obj.getArguments().values().iterator();
				while (iterator.hasNext())
				{
					IParameterDocumentation paramDoc = iterator.next();
					String description = paramDoc.getDescription();
					description = description == null ? " ;" : description;
					String paramType = cg.apply(paramDoc.getType());
					if ("void".equals(paramType))
					{
						if (paramDoc.getJSType() != null)
						{
							paramType = paramDoc.getJSType();
							parameters.add(new Parameter(paramDoc.getName(), paramType, description));
							continue;
						}
						else
						{
							paramType = "Object";
						}
					}
					if (!iterator.hasNext() && obj.isVarargs() && paramType != null)
					{
						paramType = paramType.replace("[]", "...");
					}
					parameters.add(new Parameter(paramDoc.getName(), paramType, description));
				}
			}
			return parameters;
		}
		return null;
	}

	public String getFullFunctionName()
	{
		String functionName = obj.getMainName();
		if (obj.getType() == IFunctionDocumentation.TYPE_FUNCTION || obj.getType() == IFunctionDocumentation.TYPE_EVENT)
		{
			StringBuilder sb = new StringBuilder(functionName);
			sb.append("(");
			if (obj.getArguments().size() == 0 && obj.getArgumentsTypes() != null && obj.getArgumentsTypes().length > 0)
			{
				System.err.println("missing parameter notation of: " + obj.getFullSignature() + " of " + cls);
				for (Class< ? > paramName : obj.getArgumentsTypes())
				{
					sb.append(paramName.getSimpleName());
					sb.append(", ");
				}
			}
			else
			{
				for (String paramName : obj.getArguments().keySet())
				{
					sb.append(paramName);
					sb.append(", ");
				}
			}
			if (sb.charAt(sb.length() - 2) == ',') sb.setLength(sb.length() - 2);
			sb.append(")");
			functionName = sb.toString();
		}
		return functionName;
	}

	public String getAnchoredName()
	{
		String fullName = getFullFunctionName().toLowerCase();
		return fullName.replace(" ", "-").replace("()", "").replace("(", "-").replace(")", "").replace(",", "");
	}

	public String getSupportedClients()
	{
		if (ngOnly) return null;
		return ConfluenceGenerator.getSupportedClientsList(obj.getClientSupport()).stream().collect(Collectors.joining(","));
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