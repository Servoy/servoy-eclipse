package com.servoy.eclipse.ui.scripting;

import java.util.Arrays;

import org.eclipse.dltk.core.keyword.IKeywordCategory;
import org.eclipse.dltk.ui.coloring.IKeywordColorProvider;

public class KeywordColorProvider implements IKeywordColorProvider
{
	public String getColorKey(IKeywordCategory category, String keyword)
	{
		if (Arrays.binarySearch(KeywordProvider.keywords, keyword) >= 0)
		{
			return keyword;
		}
		return null;
	}
}
