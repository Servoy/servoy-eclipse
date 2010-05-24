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
package com.servoy.eclipse.ui.quickfix;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.views.properties.IPropertySource;

import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.ServoyProject;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.util.UUID;

/**
 * Quickfix for properties by resetting them.
 * 
 * @author rob
 *
 */
public class ClearPropertyQuickFix implements IMarkerResolution
{
	private final String solutionName;
	private final String uuid;
	private final String propertyName;
	private final String displayName;

	public ClearPropertyQuickFix(String solutionName, String uuid, String propertyName, String displayName)
	{
		this.solutionName = solutionName;
		this.uuid = uuid;
		this.propertyName = propertyName;
		this.displayName = displayName;
	}

	public String getLabel()
	{
		return "Clear property " + (displayName == null ? propertyName : displayName);
	}

	public void run(IMarker marker)
	{
		if (solutionName != null && uuid != null && propertyName != null)
		{
			ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(solutionName);
			if (servoyProject != null)
			{
				IPersist persist;
				try
				{
					persist = servoyProject.getEditingPersist(UUID.fromString(uuid));
				}
				catch (RepositoryException e)
				{
					ServoyLog.logError(e);
					return;
				}
				if (persist != null)
				{
					// use standard adapter mechanism, it will follow the same logic as set-to-default-value in properties view.
					// Note: for scripts and solutions it will do the saving automatically via SavingPersistPropertySource.
					// For form elements the editor will be opened.
					IPropertySource propertySource = (IPropertySource)Platform.getAdapterManager().getAdapter(persist, IPropertySource.class);
					if (propertySource != null)
					{
						propertySource.resetPropertyValue(propertyName);
					}
				}
			}
		}
	}
}