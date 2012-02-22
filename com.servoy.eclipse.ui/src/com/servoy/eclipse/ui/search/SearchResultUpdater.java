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

import java.io.IOException;

import org.apache.commons.io.IOUtils;
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
	private final String searchText;

	public SearchResultUpdater(AbstractTextSearchResult result)
	{
		fResult = result;
		searchText = null;
		NewSearchUI.addQueryListener(this);
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
	}

	public SearchResultUpdater(AbstractTextSearchResult result, String searchText)
	{
		fResult = result;
		this.searchText = searchText;
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
				@SuppressWarnings("restriction")
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
							IResourceDelta[] ird = delta.getAffectedChildren();
							if (ird.length == 0)
							{
								Match[] matches = fResult.getMatches(delta.getResource());
								String fileText = null;
								try
								{
									if (delta.getResource() instanceof IFile)
									{
										fileText = IOUtils.toString(((IFile)delta.getResource()).getContents(), "UTF-8");
									}
								}
								catch (IOException e)
								{
									ServoyLog.logError(e);
									break;
								}

								if (searchText == null || fileText == null) break;

								if (matches.length > 0)
								{
									for (Match m : matches)
									{
										int startOfLine = -1; //start of line in the file
										int lengthToSearch = -1; //length of the old line match
										String lineElementContents = "";

										if (m instanceof com.servoy.eclipse.ui.search.FileMatch)
										{
											com.servoy.eclipse.ui.search.LineElement svyle = ((com.servoy.eclipse.ui.search.FileMatch)m).getLineElement();
											startOfLine = svyle.getOffset();
											lengthToSearch = svyle.getLength();
											lineElementContents = svyle.getContents();
										}
										else
										{
											org.eclipse.search.internal.ui.text.LineElement le = ((org.eclipse.search.internal.ui.text.FileMatch)m).getLineElement();
											startOfLine = le.getOffset();
											lengthToSearch = le.getLength();
											lineElementContents = le.getContents();
										}

										//get start of line element
										int ind1 = lineElementContents.indexOf(searchText) - 1;
										int ind2 = lineElementContents.indexOf(searchText) + searchText.length() + 1;
										if (startOfLine < 0 || ind1 < 0) continue;

										//safety: make sure we don't take out unnecessary stuff when doubleclicking on a match
										String newLineOfText = fileText.substring(startOfLine, startOfLine + lengthToSearch);
										if (newLineOfText.trim().equals(lineElementContents.trim())) continue;

										String theMatch = lineElementContents.substring(ind1, ind2);

										//search if new line in the file (where old match was) still contains the match (=search text + 1char before and 1after it)
										//a match is changed if it differs one char before and one after the searchtext in the line string
										if (!newLineOfText.contains(theMatch)) fResult.removeMatch(m);
									}
								}
							}
							else for (IResourceDelta id : ird)
							{
								if (id.getKind() != IResourceDelta.CHANGED) continue;
								handleDelta(id);
							}

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