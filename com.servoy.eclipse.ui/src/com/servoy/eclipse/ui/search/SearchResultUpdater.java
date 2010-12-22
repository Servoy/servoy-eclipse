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

package com.servoy.eclipse.ui.search;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.search.ui.IQueryListener;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.Match;

import com.servoy.eclipse.model.util.ServoyLog;

/**
 * Class that handles resource changes to update the search results.
 * 
 * @author jcompagner
 * @since 6.0
 */
public class SearchResultUpdater implements IResourceChangeListener, IQueryListener
{
	private final AbstractTextSearchResult fResult;

	public SearchResultUpdater(AbstractTextSearchResult result)
	{
		fResult = result;
		NewSearchUI.addQueryListener(this);
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
	}

	public void resourceChanged(IResourceChangeEvent event)
	{
		IResourceDelta delta = event.getDelta();
		if (delta != null) handleDelta(delta);
	}

	private void handleDelta(IResourceDelta d)
	{
		try
		{
			d.accept(new IResourceDeltaVisitor()
			{
				public boolean visit(IResourceDelta delta) throws CoreException
				{
					switch (delta.getKind())
					{
						case IResourceDelta.ADDED :
							return false;
						case IResourceDelta.REMOVED :
							IResource res = delta.getResource();
							if (res instanceof IFile)
							{
								Match[] matches = fResult.getMatches(res);
								fResult.removeMatches(matches);
							}
							break;
						case IResourceDelta.CHANGED :
							// handle changed resource
							break;
					}
					return true;
				}
			});
		}
		catch (CoreException e)
		{
			ServoyLog.logError(e);
		}
	}

	public void queryAdded(ISearchQuery query)
	{
		// don't care
	}

	public void queryRemoved(ISearchQuery query)
	{
		if (fResult.equals(query.getSearchResult()))
		{
			ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
			NewSearchUI.removeQueryListener(this);
		}
	}

	public void queryStarting(ISearchQuery query)
	{
		// don't care
	}

	public void queryFinished(ISearchQuery query)
	{
		// don't care
	}
}