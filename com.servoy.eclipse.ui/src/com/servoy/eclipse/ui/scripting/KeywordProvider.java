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

import java.util.Arrays;

import org.eclipse.dltk.core.ISourceModule;
import org.eclipse.dltk.core.keyword.IKeywordCategory;
import org.eclipse.dltk.core.keyword.IKeywordProvider;
import org.eclipse.dltk.javascript.core.JSKeywordCategory;

import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.scripting.IExecutingEnviroment;

@SuppressWarnings("nls")
public class KeywordProvider implements IKeywordProvider
{

	public static final String[] keywords;

	static
	{
		keywords = new String[18];
		int n = 0;

		keywords[n++] = IExecutingEnviroment.TOPLEVEL_JSUNIT;
		keywords[n++] = IExecutingEnviroment.TOPLEVEL_UTILS;
		keywords[n++] = IExecutingEnviroment.TOPLEVEL_SECURITY;
		keywords[n++] = "elements";
		keywords[n++] = "controller";
		keywords[n++] = "currentcontroller";
		keywords[n++] = IExecutingEnviroment.TOPLEVEL_APPLICATION;
		keywords[n++] = IExecutingEnviroment.TOPLEVEL_DATABASE_MANAGER;
		keywords[n++] = IExecutingEnviroment.TOPLEVEL_SOLUTION_MODIFIER;
		keywords[n++] = ScriptVariable.GLOBAL_SCOPE;
		keywords[n++] = IExecutingEnviroment.TOPLEVEL_SCOPES;
		keywords[n++] = IExecutingEnviroment.TOPLEVEL_FORMS;
		keywords[n++] = IExecutingEnviroment.TOPLEVEL_HISTORY;
		keywords[n++] = IExecutingEnviroment.TOPLEVEL_PLUGINS;
		keywords[n++] = "_super";

		keywords[n++] = "FIXME";
		keywords[n++] = "CHECKME";
		keywords[n++] = "TODO";

		Arrays.sort(keywords);
	}

	public KeywordProvider()
	{
	}

	public String[] getKeywords(IKeywordCategory category, ISourceModule module)
	{
		if (category == JSKeywordCategory.CODE)
		{
			return keywords;
		}
		if (category == JSKeywordCategory.JS_DOC_TAG)
		{
			return new String[] { "@AllowToRunInFind" };
		}
		return null;
	}
}
