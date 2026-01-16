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

package com.servoy.eclipse.debug.script;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FunctionTypeParser
{
	public enum ParsedParameterKind
	{
		NORMAL,
		VARARGS
	}

	// Inner record for a single parameter (optional; you can keep only the top-level ones)
	public static record ParsedParameter(String name, String type, boolean optional, ParsedParameterKind kind)
	{

		// Compact canonical constructor to normalize the input
		public ParsedParameter
		{
			// Strip leading "..." from type if present; set kind accordingly
			if (type != null && type.startsWith("..."))
			{
				type = type.substring(3).trim();
				kind = ParsedParameterKind.VARARGS;
			}
			// If kind wasn't set (e.g., caller passes null), default to NORMAL
			if (kind == null)
			{
				kind = ParsedParameterKind.NORMAL;
			}
		}

		@Override
		public String toString()
		{
			return "ParsedParameter[name=%s, type=%s, optional=%s, kind=%s]"
				.formatted(name, type, optional, kind);
		}

	}

	// Inner record for the entire function declaration (optional; you can keep only the top-level ones)
	public static record ParsedFunctionDeclaration(String returnType, List<ParsedParameter> parameters)
	{
	}

	/**
	 * Parses a function type declaration string into a structured FunctionDeclaration record.
	 * Examples:
	 *   "{(param1:String,param2:Number)=>String}"
	 *   "{(record:JSRecord,recordIndex:Number,foundset:JSFoundset)=>Object}"
	 *   "{(a:Number)=>void}"
	 *   "{(event:JSEvent,customArguments:...Object)=>Object}"
	 *
	 * Accepts both arrow forms: "=>" and "=&gt;" and optional outer braces.
	 */

	public static ParsedFunctionDeclaration parseFunctionType(String declaration)
	{
		if (declaration == null)
		{
			return new ParsedFunctionDeclaration("void", Collections.emptyList());
		}

		// Normalize entities
		String parsed = declaration.trim()
			.replace("&gt;", ">")
			.replace("&lt;", "<");

		// If there are braces, work ONLY on what's inside them
		int open = parsed.indexOf('{');
		int close = parsed.lastIndexOf('}');
		if (open >= 0 && close > open)
		{
			parsed = parsed.substring(open + 1, close).trim();
		}

		// Now parse the inner "(...)=>(...)" only
		String regex = "^\\s*\\{?\\s*\\((.*?)\\)\\s*(?:=>|=&gt;)\\s*(.*?)\\s*\\}?\\s*$";

		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(parsed);

		if (!matcher.find())
		{
			return new ParsedFunctionDeclaration("void", Collections.emptyList());
		}

		String paramListString = matcher.group(1).trim();
		String returnType = matcher.group(2).trim();

		List<ParsedParameter> parameters = new ArrayList<>();

		if (!paramListString.isEmpty())
		{
			// Simple comma split; upgrade to a depth-aware splitter if you expect nested generics with commas.
			String[] rawParams = paramListString.split(",");

			for (String rawParam : rawParams)
			{
				String trimmedParam = rawParam.trim();
				if (trimmedParam.isEmpty()) continue;

				// Split name and type by the first ':'
				String[] nameAndType = trimmedParam.split(":", 2);
				String rawName = nameAndType[0].trim();
				String rawType = (nameAndType.length > 1) ? nameAndType[1].trim() : "UnknownType";

				boolean optional = false;
				String name = rawName;
				String type = rawType;

				// Optional if name? : type  OR name : type?
				if (name.endsWith("?"))
				{
					optional = true;
					name = name.substring(0, name.length() - 1).trim();
				}
				if (type.endsWith("?"))
				{
					optional = true;
					type = type.substring(0, type.length() - 1).trim();
				}

				parameters.add(new ParsedParameter(name, type, optional, null));
			}
		}


		return new ParsedFunctionDeclaration(returnType, Collections.unmodifiableList(parameters));
	}

	public static void main(String[] args)
	{
		String declaration = "{(event:JSEvent,customArguments:...Object)=>Object}";
		String declaration1 = "@param callback {(record:JSRecord,recordIndex?:Number,foundset?:JSFoundSet)=>Object} The callback function to be called for each loaded record in the foundset. Can receive three parameters: the record to be processed, the index of the record in the foundset, and the foundset that is traversed.";

		ParsedFunctionDeclaration parsedFunction = parseFunctionType(declaration);

		System.out.println("--- STRUCTURED PARSING RESULTS ---");
		System.out.println("Return Type: " + parsedFunction.returnType());
		System.out.println("Total Parameters: " + parsedFunction.parameters().size());

		System.out.println("Parameters Detail:");
		for (ParsedParameter param : parsedFunction.parameters())
		{
			System.out.printf("  - Name: %s, Type: %s%n", param.name(), param.type());
		}
	}
}
