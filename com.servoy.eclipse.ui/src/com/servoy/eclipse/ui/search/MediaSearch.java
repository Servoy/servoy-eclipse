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

import java.util.regex.Pattern;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.search.core.text.TextSearchEngine;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.text.FileTextSearchScope;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.util.Utils;

/**
 * An {@link ISearchQuery} implementation for finding Medias in frm and js files.
 *
 * @author obuligan
 * @since 7.4
 */
public class MediaSearch extends AbstractPersistSearch
{
	private final Media mediaImage;

	public MediaSearch(Media image)
	{
		this.mediaImage = image;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.search.ui.ISearchQuery#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IStatus run(IProgressMonitor monitor) throws OperationCanceledException
	{
		IResource[] scopes = getScopes((Solution)mediaImage.getRootObject());
		if (ServoyModelManager.getServoyModelManager().getServoyModel().getActiveResourcesProject() != null)
		{
			IResource resourceProject = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveResourcesProject().getProject();
			scopes = Utils.arrayAdd(scopes, resourceProject, true);
		}
		TextSearchResultCollector collector = getResultCollector();

		FileTextSearchScope scope = FileTextSearchScope.newSearchScope(scopes, new String[] { "*.frm", "*.js" }, true);
		TextSearchEngine.create().search(scope, collector, Pattern.compile(mediaImage.getUUID().toString()), monitor);

		scope = FileTextSearchScope.newSearchScope(scopes, new String[] { "*.js", "*.css", "*.less" }, true);
		TextSearchEngine.create().search(scope, collector, Pattern.compile("\\bmedia:///" + mediaImage.getName() + "\\b"), monitor);

		return Status.OK_STATUS;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.search.ui.ISearchQuery#getLabel()
	 */
	public String getLabel()
	{
		return "Searching references to media '" + mediaImage.getName() + "'";
	}


}
