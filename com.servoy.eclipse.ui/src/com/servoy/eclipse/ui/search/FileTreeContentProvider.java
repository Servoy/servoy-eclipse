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

package com.servoy.eclipse.ui.search;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.Match;

/**
 * The tree based content provider for the file based search results.
 * 
 * @author jcompagner
 * @since 6.0
 */
public class FileTreeContentProvider implements ITreeContentProvider, IFileSearchContentProvider
{

	private final Object[] EMPTY_ARR = new Object[0];

	private AbstractTextSearchResult fResult;
	private final FileSearchPage fPage;
	private final AbstractTreeViewer fTreeViewer;
	private Map<Object, Set<Object>> fChildrenMap;

	FileTreeContentProvider(FileSearchPage page, AbstractTreeViewer viewer)
	{
		fPage = page;
		fTreeViewer = viewer;
	}

	public Object[] getElements(Object inputElement)
	{
		Object[] children = getChildren(inputElement);
		int elementLimit = getElementLimit();
		if (elementLimit != -1 && elementLimit < children.length)
		{
			Object[] limitedChildren = new Object[elementLimit];
			System.arraycopy(children, 0, limitedChildren, 0, elementLimit);
			return limitedChildren;
		}
		return children;
	}

	private int getElementLimit()
	{
		return fPage.getElementLimit().intValue();
	}

	public void dispose()
	{
		// nothing to do
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
	{
		if (newInput instanceof FileSearchResult)
		{
			initialize((FileSearchResult)newInput);
		}
	}

	private synchronized void initialize(AbstractTextSearchResult result)
	{
		fResult = result;
		fChildrenMap = new HashMap<Object, Set<Object>>();

		if (result != null)
		{
			Object[] elements = result.getElements();
			for (Object element : elements)
			{
				Match[] matches = result.getMatches(element);
				for (Match matche : matches)
				{
					insert(((FileMatch)matche).getLineElement(), false);
				}
			}
		}
	}

	private void insert(Object child, boolean refreshViewer)
	{
		Object walker = child;
		Object parent = getParent(walker);
		while (parent != null)
		{
			if (insertChild(parent, walker))
			{
				if (refreshViewer) fTreeViewer.add(parent, walker);
			}
			else
			{
				if (refreshViewer) fTreeViewer.refresh(parent);
				return;
			}
			walker = parent;
			parent = getParent(walker);
		}
		if (insertChild(fResult, walker))
		{
			if (refreshViewer) fTreeViewer.add(fResult, walker);
		}
	}

	/**
	 * Adds the child to the parent.
	 * 
	 * @param parent the parent
	 * @param child the child
	 * @return <code>true</code> if this set did not already contain the specified element

	 */
	private boolean insertChild(Object parent, Object child)
	{
		Set<Object> children = fChildrenMap.get(parent);
		if (children == null)
		{
			children = new HashSet<Object>();
			fChildrenMap.put(parent, children);
		}
		return children.add(child);
	}

	private boolean hasChild(Object parent, Object child)
	{
		Set<Object> children = fChildrenMap.get(parent);
		return children != null && children.contains(child);
	}


	private void remove(Object element, boolean refreshViewer)
	{
		// precondition here:  fResult.getMatchCount(child) <= 0

		if (hasChildren(element))
		{
			if (refreshViewer) fTreeViewer.refresh(element);
		}
		else
		{
			if (!hasMatches(element))
			{
				fChildrenMap.remove(element);
				Object parent = getParent(element);
				if (parent != null)
				{
					removeFromSiblings(element, parent);
					remove(parent, refreshViewer);
				}
				else
				{
					removeFromSiblings(element, fResult);
					if (refreshViewer) fTreeViewer.refresh();
				}
			}
			else
			{
				if (refreshViewer)
				{
					fTreeViewer.refresh(element);
				}
			}
		}
	}

	private boolean hasMatches(Object element)
	{
		if (element instanceof LineElement)
		{
			LineElement lineElement = (LineElement)element;
			return lineElement.getNumberOfMatches(fResult) > 0;
		}
		return fResult.getMatchCount(element) > 0;
	}


	private void removeFromSiblings(Object element, Object parent)
	{
		Set<Object> siblings = fChildrenMap.get(parent);
		if (siblings != null)
		{
			siblings.remove(element);
		}
	}

	public Object[] getChildren(Object parentElement)
	{
		Set<Object> children = fChildrenMap.get(parentElement);
		if (children == null) return EMPTY_ARR;
		return children.toArray();
	}

	public boolean hasChildren(Object element)
	{
		return getChildren(element).length > 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.search.internal.ui.text.IFileSearchContentProvider#elementsChanged(java.lang.Object[])
	 */
	public synchronized void elementsChanged(Object[] updatedElements)
	{
		for (int i = 0; i < updatedElements.length; i++)
		{
			if (!(updatedElements[i] instanceof LineElement))
			{
				// change events to elements are reported in file search
				if (fResult.getMatchCount(updatedElements[i]) > 0) insert(updatedElements[i], true);
				else remove(updatedElements[i], true);
			}
			else
			{
				// change events to line elements are reported in text search
				LineElement lineElement = (LineElement)updatedElements[i];
				int nMatches = lineElement.getNumberOfMatches(fResult);
				if (nMatches > 0)
				{
					if (hasChild(lineElement.getParent(), lineElement))
					{
						fTreeViewer.update(new Object[] { lineElement, lineElement.getParent() }, null);
					}
					else
					{
						insert(lineElement, true);
					}
				}
				else
				{
					remove(lineElement, true);
				}
			}
		}
	}

	public void clear()
	{
		initialize(fResult);
		fTreeViewer.refresh();
	}

	public Object getParent(Object element)
	{
		if (element instanceof IProject) return null;
		if (element instanceof IResource)
		{
			IResource resource = (IResource)element;
			return resource.getParent();
		}
		if (element instanceof LineElement)
		{
			return ((LineElement)element).getParent();
		}

		if (element instanceof FileMatch)
		{
			FileMatch match = (FileMatch)element;
			return match.getLineElement();
		}
		return null;
	}
}
