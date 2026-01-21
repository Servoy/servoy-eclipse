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

package com.servoy.eclipse.core.builder;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import com.servoy.eclipse.core.resource.PersistEditorInput;
import com.servoy.eclipse.core.resource.ServerEditorInput;
import com.servoy.eclipse.model.builder.ServoyBuilder;
import com.servoy.eclipse.model.extensions.IMarkerAttributeContributor;
import com.servoy.eclipse.model.util.PersistFinder;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.ScriptVariable;

/**
 * Implementation for a com.servoy.eclipse.model extension point.
 * @author acostescu
 */
public class OpenWithEditor implements IMarkerAttributeContributor
{

	public void contributeToMarker(IMarker marker, IPersist persist)
	{
		String contentTypeIdentifier = null;
		try
		{
			if (marker.getType().equals(ServoyBuilder.MISSING_DRIVER))
			{
				contentTypeIdentifier = ServerEditorInput.SERVER_RESOURCE_ID;
			}
			else if (persist.getAncestor(IRepository.FORMS) instanceof Form && !(persist instanceof ScriptVariable) && !(persist instanceof ScriptMethod))
			{
				contentTypeIdentifier = PersistEditorInput.FORM_RESOURCE_ID;
			}
			else if (persist.getAncestor(IRepository.RELATIONS) != null)
			{
				contentTypeIdentifier = PersistEditorInput.RELATION_RESOURCE_ID;
			}
			else if (persist.getAncestor(IRepository.VALUELISTS) != null)
			{
				contentTypeIdentifier = PersistEditorInput.VALUELIST_RESOURCE_ID;
			}
			else if (persist.getAncestor(IRepository.MEDIA) != null)
			{
				contentTypeIdentifier = PersistEditorInput.MEDIA_RESOURCE_ID;
			}
		}
		catch (CoreException e1)
		{
			ServoyLog.logError(e1);
		}
		if (contentTypeIdentifier != null)
		{
			try
			{
				marker.setAttribute(
					IDE.EDITOR_ID_ATTR,
					PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(null, Platform.getContentTypeManager().getContentType(contentTypeIdentifier))
						.getId());

				if (persist != null)
				{
					marker.setAttribute("persistIdentifier", PersistFinder.INSTANCE.fromPersist(persist).toJSONString());
				}
			}
			catch (CoreException e)
			{
				ServoyLog.logError(e);
			}
		}
	}

}