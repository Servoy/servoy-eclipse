package com.servoy.eclipse.ui.scripting;

import java.util.Arrays;

import org.eclipse.dltk.core.ISourceModule;
import org.eclipse.dltk.core.keyword.IKeywordCategory;
import org.eclipse.dltk.core.keyword.IKeywordProvider;

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

	public String[] getKeywords(IKeywordCategory category, ISourceModule module)
	{
		return keywords;
	}
}
