package com.servoy.eclipse.ui.search;

import org.eclipse.jface.action.Action;

/**
 * {@link Action} to sort the search results in the tableview.
 * 
 * @author jcompagner
 * @since 6.0
 */
public class SortAction extends Action
{
	private final int fSortOrder;
	private final FileSearchPage fPage;

	public SortAction(String label, FileSearchPage page, int sortOrder)
	{
		super(label);
		fPage = page;
		fSortOrder = sortOrder;
	}

	@Override
	public void run()
	{
		fPage.setSortOrder(fSortOrder);
	}

	public int getSortOrder()
	{
		return fSortOrder;
	}
}
