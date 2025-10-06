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

public class KeywordProvider implements IKeywordProvider
{

	public static final String[] keywords;

	static
	{
		Arrays.sort(keywords = new String[] { //

			IExecutingEnviroment.TOPLEVEL_JSUNIT, //
			IExecutingEnviroment.TOPLEVEL_UTILS, //
			IExecutingEnviroment.TOPLEVEL_CLIENTUTILS, //
			IExecutingEnviroment.TOPLEVEL_SECURITY, //
			"elements", //
			"controller", //
			"currentcontroller", //
			"developerBridge", //
			IExecutingEnviroment.TOPLEVEL_APPLICATION, //
			IExecutingEnviroment.TOPLEVEL_DATABASE_MANAGER, //
			IExecutingEnviroment.TOPLEVEL_DATASOURCES, //
			IExecutingEnviroment.TOPLEVEL_SOLUTION_MODIFIER, //
			ScriptVariable.GLOBAL_SCOPE, //
			IExecutingEnviroment.TOPLEVEL_SCOPES, //
			IExecutingEnviroment.TOPLEVEL_FORMS, //
			IExecutingEnviroment.TOPLEVEL_HISTORY, //
			IExecutingEnviroment.TOPLEVEL_MENUS, //
			IExecutingEnviroment.TOPLEVEL_EVENTTYPES, //
			IExecutingEnviroment.TOPLEVEL_JSPERMISSION, //
			IExecutingEnviroment.TOPLEVEL_JSVALUELIST, //
			IExecutingEnviroment.TOPLEVEL_JSFORM, //
			IExecutingEnviroment.TOPLEVEL_PLUGINS, //
			"_super", //

			"FIXME", //
			"CHECKME", //
			"TODO" //
		});
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
			return new String[] { "@AllowToRunInFind", "@enum", "@override", "@parse" };
		}
		return null;
	}
}
