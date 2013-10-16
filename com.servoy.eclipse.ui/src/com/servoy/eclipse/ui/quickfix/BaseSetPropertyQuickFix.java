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

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.util.UUID;

/**
 * Base class for quickfixes for setting/clearing properties.
 * 
 * @author rgansevles
 *
 */
public abstract class BaseSetPropertyQuickFix implements IMarkerResolution
{
	private final String solutionName;
	private final String uuid;
	private final String propertyName;
	private final String displayName;
	private String label;

	public BaseSetPropertyQuickFix(String solutionName, String uuid, String propertyName, String displayName)
	{
		this.solutionName = solutionName;
		this.uuid = uuid;
		this.propertyName = propertyName;
		this.displayName = displayName;
	}

	/**
	 * @return the displayName
	 */
	public String getDisplayName()
	{
		return displayName == null ? propertyName : displayName;
	}

	/**
	 * @param label the label to set
	 */
	public BaseSetPropertyQuickFix setLabel(String label)
	{
		this.label = label;
		return this;
	}

	public String getLabel()
	{
		return label == null ? getDefaultLabel() : label;
	}

	protected String getDefaultLabel()
	{
		return "Set property " + getDisplayName();
	}

	/**
	 * @return the propertyName
	 */
	public String getPropertyName()
	{
		return propertyName;
	}

	/**
	 * @return the uuid
	 */
	public String getUuid()
	{
		return uuid;
	}

	/**
	* @return the solutionName
	*/
	public String getSolutionName()
	{
		return solutionName;
	}

	public void run(IMarker marker)
	{
		if (solutionName != null && uuid != null && propertyName != null)
		{
			ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(solutionName);
			if (servoyProject != null)
			{
				IPersist persist = servoyProject.getEditingPersist(UUID.fromString(uuid));
				if (persist != null)
				{
					// use standard adapter mechanism, it will follow the same logic as set-to-default-value in properties view.
					// Note: for scripts and solutions it will do the saving automatically via SavingPersistPropertySource.
					// For form elements the editor will be opened.
					IPropertySource propertySource = (IPropertySource)Platform.getAdapterManager().getAdapter(persist, IPropertySource.class);
					if (propertySource != null)
					{
						setPropertyValue(propertySource);
					}
				}
			}
		}
	}

	protected abstract void setPropertyValue(IPropertySource propertySource);
}