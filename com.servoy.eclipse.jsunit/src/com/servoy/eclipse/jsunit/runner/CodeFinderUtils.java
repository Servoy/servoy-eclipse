/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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

package com.servoy.eclipse.jsunit.runner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

import de.berlios.jsunit.JsUnitException;

/**
 * Utility class for loading JS code from files in jars.
 * 
 * @author acostescu
 */
public class CodeFinderUtils
{

	public static String getScriptAsStringFromResource(String repeatSafeguardId, Class< ? > locatorClass, String name)
	{
		StringWriter writer = new StringWriter(125 * 1024);
		writer.append("if (typeof(" + repeatSafeguardId + ") == 'undefined') {\n" + repeatSafeguardId + " = 1;\n");
		loadScriptFromResource(locatorClass, name, writer);
		writer.append("\n}");
		return writer.toString();
	}

	// these methods containing "fixes" were also sent as an SVN patch to the author of jsunit (see http://jsunit.berlios.de/index.html)
	@SuppressWarnings("nls")
	public static String getFixedJSUtilScriptFromResource()
	{
		// @formatter:off
		return CodeFinderUtils.getScriptAsStringFromResource("this.JsUtilLoaded", JsUnitException.class, "/JsUtil.js")
			.replace(
			    "var r = /function (\\w+)(",
			    "var r = /function *(\\w*)(\\("); // if you had "function(){}" with no space after "function", a wrong function name could appear in the call stack 
		// @formatter:on
	}

	@SuppressWarnings("nls")
	public static String getFixedJSUnitScriptFromResource()
	{
		// @formatter:off
		return CodeFinderUtils.getScriptAsStringFromResource("this.TestCaseLoaded", JsUnitException.class, "/JsUnit.js")
			.replace(
			    "( usermsg ? usermsg + \" \" : \"\" ) + msg, stack );",
			    "( usermsg ? usermsg + \" \" : \"\" ) + msg, stack ? stack : new CallStack());"); // if you would simply use jsunit.fail() you would get no stack... 
		// @formatter:on
	}

	private static void loadScriptFromResource(Class< ? > locatorClass, final String name, final Writer writer)
	{
		final InputStream is = locatorClass.getResourceAsStream(name);
		if (is != null)
		{
			try
			{
				final Reader reader = new InputStreamReader(is, "ISO-8859-1");
				copy(reader, writer);
			}
			catch (final UnsupportedEncodingException e)
			{
				throw new InternalError("Missing standard character set ISO-8859-1");
			}
			catch (final IOException e)
			{
				throw new InternalError("Cannot load resource " + name);
			}
			finally
			{
				close(is);
			}
		}
		else
		{
			throw new InternalError("Cannot find resource " + name);
		}
	}

	private static void copy(final Reader reader, final Writer writer) throws IOException
	{
		final Reader in = new BufferedReader(reader, 1024 * 16);
		int i;
		while ((i = in.read()) != -1)
		{
			writer.write(i);
		}
	}

	private static void close(final InputStream is)
	{
		try
		{
			is.close();
		}
		catch (final IOException e)
		{
			// ignore
		}
	}

}
