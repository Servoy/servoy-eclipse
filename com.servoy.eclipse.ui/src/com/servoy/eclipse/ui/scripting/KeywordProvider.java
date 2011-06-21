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

import com.servoy.j2db.scripting.IExecutingEnviroment;

@SuppressWarnings("nls")
public class KeywordProvider implements IKeywordProvider
{

	public static final String[] keywords;

	static
	{
		keywords = new String[17];

		keywords[0] = IExecutingEnviroment.TOPLEVEL_JSUNIT;
		keywords[1] = IExecutingEnviroment.TOPLEVEL_UTILS;
		keywords[2] = IExecutingEnviroment.TOPLEVEL_SECURITY;
		keywords[3] = "elements";
		keywords[4] = "controller";
		keywords[5] = "currentcontroller";
		keywords[6] = IExecutingEnviroment.TOPLEVEL_APPLICATION;
		keywords[7] = IExecutingEnviroment.TOPLEVEL_DATABASE_MANAGER;
		keywords[8] = IExecutingEnviroment.TOPLEVEL_SOLUTION_MODIFIER;
		keywords[9] = "globals";
		keywords[10] = IExecutingEnviroment.TOPLEVEL_FORMS;
		keywords[11] = IExecutingEnviroment.TOPLEVEL_HISTORY;
		keywords[12] = IExecutingEnviroment.TOPLEVEL_PLUGINS;
		keywords[13] = "_super";

		keywords[14] = "FIXME";
		keywords[15] = "CHECKME";
		keywords[16] = "TODO";

		Arrays.sort(keywords);
	}

	public KeywordProvider()
	{
	}

	/**
	 * @test
	 * @see test
	 */
	public String[] getKeywords(IKeywordCategory category, ISourceModule module)
	{
		if (category == JSKeywordCategory.CODE)
		{
			return keywords;
		}
		else if (category == JSKeywordCategory.JS_DOC_TAG)
		{
			return new String[] { "@AllowToRunInFind" };
		}
		return null;
	}
}
