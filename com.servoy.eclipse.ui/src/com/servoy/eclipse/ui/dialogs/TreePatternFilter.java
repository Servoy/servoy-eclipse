/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

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
package com.servoy.eclipse.ui.dialogs;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.TreeItem;

import com.servoy.eclipse.ui.util.IKeywordChecker;
import com.servoy.eclipse.ui.views.IMaxDepthTreeContentProvider;
import com.servoy.j2db.util.IDelegate;


/**
 * Pattern filter for use in {@link FilteredTreeViewer}.
 * 
 * @author rgansevles
 * 
 */
public class TreePatternFilter extends PatternFilter
{
	public static final String TREE_PATTERN_FILTER_MODE = "treePatternFilterMode"; //$NON-NLS-1$
	public static final String TREE_PATTERN_SEARCH_DEPTH = "treePatternSearchDepth"; //$NON-NLS-1$

	public static final int FILTER_LEAFS = 1;
	public static final int FILTER_PARENTS = 2;

	protected int filterMode = FILTER_LEAFS;
	protected int maxSearchDepth = IMaxDepthTreeContentProvider.DEPTH_DEFAULT;

	private final Map<Object, Boolean> searchKeyCache = new HashMap<Object, Boolean>();

	public TreePatternFilter(int filterMode, int maxSearchDepth)
	{
		this.filterMode = filterMode;
		this.maxSearchDepth = maxSearchDepth;
	}

	public void setFilterMode(int filterMode)
	{
		this.filterMode = filterMode;
	}

	public int getFilterMode()
	{
		return filterMode;
	}

	public void setMaxSearchDepth(int maxSearchDepth)
	{
		this.maxSearchDepth = maxSearchDepth;
	}

	public int getMaxSearchDepth()
	{
		return maxSearchDepth;
	}

	public static int getSavedFilterMode(IDialogSettings settings, int defaultFilterMode)
	{
		if (settings != null)
		{
			try
			{
				return settings.getInt(TREE_PATTERN_FILTER_MODE);
			}
			catch (NumberFormatException e)
			{
			}
		}
		return defaultFilterMode;
	}

	public static int getSavedFilterSearchDepth(IDialogSettings settings, int defaultSearchDepth)
	{
		if (settings != null)
		{
			try
			{
				return settings.getInt(TREE_PATTERN_SEARCH_DEPTH);
			}
			catch (NumberFormatException e)
			{
			}
		}
		return defaultSearchDepth;
	}

	public void saveSettings(IDialogSettings settings)
	{
		if (settings != null)
		{
			settings.put(TREE_PATTERN_FILTER_MODE, filterMode);
			settings.put(TREE_PATTERN_SEARCH_DEPTH, maxSearchDepth);
		}
	}

	/**
	 * Return the first item in the tree that matches this filter pattern.
	 * 
	 * @param items
	 * @return the first matching TreeItem
	 */
	public TreeItem getFirstMatchingItem(Viewer treeViewer, TreeItem[] items)
	{
		for (TreeItem element : items)
		{
			if (element.getData() != null && isLeafMatch(treeViewer, element.getData()) && isElementSelectable(element.getData()))
			{
				return element;
			}
			return getFirstMatchingItem(treeViewer, element.getItems());
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.eclipse.ui.dialogs.PatternFilter#clearCaches()
	 */
	@Override
	protected void clearCaches()
	{
		super.clearCaches();
		searchKeyCache.clear();
	}

	/**
	 * Answers whether the given element in the given viewer matches the filter pattern. This is a default implementation that will show a leaf element in the
	 * tree based on whether the provided filter text matches the text of the given element's text, or that of it's children (if the element has any).
	 * 
	 * Subclasses may override this method.
	 * 
	 * @param viewer the tree viewer in which the element resides
	 * @param element the element in the tree to check for a match
	 * 
	 * @return true if the element matches the filter pattern
	 */
	@Override
	public boolean isElementVisible(Viewer viewer, Object element)
	{
		// limit max depth for searching
		ITreeContentProvider contentProvider = getTreeContentProvider(viewer);
		if (contentProvider instanceof IMaxDepthTreeContentProvider &&
			(((IMaxDepthTreeContentProvider)contentProvider)).searchLimitReached(element, maxSearchDepth * 2))
		{
			return false;
		}

		if (filterMode == FILTER_PARENTS && isAnyParentLeafMatch(viewer, element))
		{
			return true;
		}
		// do not show leaf nodes when mode is FILTER_PARENTS
//			Object[] children = ((ITreeContentProvider) ((AbstractTreeViewer) viewer)
//                .getContentProvider()).getChildren(element);
//			if (children == null || children.length == 0)
//			{
//				return false;
//			}

		Object searchKey = null;
		if (contentProvider instanceof ISearchKeyAdapter)
		{
			searchKey = ((ISearchKeyAdapter)contentProvider).getSearchKey(element);
			if (searchKey != null)
			{
				Boolean b = searchKeyCache.get(searchKey);
				if (b != null)
				{
					return b.booleanValue();
				}
			}
		}

		boolean elementVisible = super.isElementVisible(viewer, element);
		if (searchKey != null)
		{
			searchKeyCache.put(searchKey, elementVisible ? Boolean.TRUE : Boolean.FALSE);
		}
		return elementVisible;
	}

	@Override
	protected boolean isLeafMatch(Viewer viewer, Object element)
	{
		ITreeContentProvider contentProvider = getTreeContentProvider(viewer);
		if (contentProvider instanceof IKeywordChecker && ((IKeywordChecker)contentProvider).isKeyword(element))
		{
			// no isLeafmatch for keyword nodes
			return false;
		}

		// use the most inner label provider for matching when label provider is a delegate label provider
		ILabelProvider labelProvider = ((ILabelProvider)((StructuredViewer)viewer).getLabelProvider());
		while (labelProvider instanceof IDelegate)
		{
			Object delegate = ((IDelegate)labelProvider).getDelegate();
			if (delegate instanceof ILabelProvider)
			{
				labelProvider = (ILabelProvider)delegate;
			}
			else
			{
				break;
			}
		}
		String labelText = labelProvider.getText(element);
		if (labelText == null)
		{
			return false;
		}
		return wordMatches(labelText);
	}

	protected boolean isAnyParentLeafMatch(Viewer viewer, Object element)
	{
		ITreeContentProvider contentProvider = getTreeContentProvider(viewer);
		Object parent = contentProvider.getParent(element);
		while (parent != null)
		{
			if (isLeafMatch(viewer, parent))
			{
				return true;
			}
			parent = contentProvider.getParent(parent);
		}
		return false;
	}

	@Override
	protected String[] getWords(String text)
	{
		// disabled word breaking as performance improvement
		return new String[] { text };
	}

	/**
	 * Should this node in the tree be expanded to show the current match? <br>
	 * when filtering on leafs, expand the entire tree <br>
	 * when filtering on parents, expand if there is a child that matches, do not expand the matching child
	 * 
	 * @param viewer
	 * @param element
	 * @return
	 */
	public boolean shouldExpandNodeForMatch(Viewer viewer, Object element)
	{
		if (filterMode == TreePatternFilter.FILTER_LEAFS)
		{
			return true;
		}

		return !isLeafMatch(viewer, element) && hasLeafMatchChild(viewer, element);
	}

	protected boolean hasLeafMatchChild(Viewer viewer, Object element)
	{
		ITreeContentProvider contentProvider = getTreeContentProvider(viewer);
		Object[] children = contentProvider.getChildren(element);
		if (children != null)
		{
			for (Object child : children)
			{
				if (isLeafMatch(viewer, child) || hasLeafMatchChild(viewer, child))
				{
					return true;
				}
			}
		}
		return false;
	}
}
