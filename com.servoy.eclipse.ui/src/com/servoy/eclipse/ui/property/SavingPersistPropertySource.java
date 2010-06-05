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
package com.servoy.eclipse.ui.property;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;

import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.ServoyProject;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.RepositoryException;

/**
 * Save all changes straight through.
 * 
 * @author rob
 * 
 */
public class SavingPersistPropertySource implements IPropertySource
{
	private final PersistPropertySource persistProperties;
	private final ServoyProject servoyProject;

	public SavingPersistPropertySource(PersistPropertySource persistProperties, ServoyProject servoyProject)
	{
		this.persistProperties = persistProperties;
		this.servoyProject = servoyProject;
	}

	public Object getEditableValue()
	{
		return persistProperties.getEditableValue();
	}

	public IPropertyDescriptor[] getPropertyDescriptors()
	{
		return persistProperties.getPropertyDescriptors();
	}

	public Object getPropertyValue(Object id)
	{
		return persistProperties.getPropertyValue(id);
	}

	public boolean isPropertySet(Object id)
	{
		return persistProperties.isPropertySet(id);
	}

	public void resetPropertyValue(Object id)
	{
		persistProperties.resetPropertyValue(id);
		try
		{
			servoyProject.saveEditingSolutionNodes(new IPersist[] { persistProperties.getPersist() }, false);
		}
		catch (RepositoryException e)
		{
			ServoyLog.logError(e);
		}
	}

	public void setPropertyValue(Object id, Object value)
	{
		persistProperties.setPropertyValue(id, value);
		try
		{
			servoyProject.saveEditingSolutionNodes(new IPersist[] { persistProperties.getPersist() }, false);
		}
		catch (RepositoryException e)
		{
			ServoyLog.logError(e);
		}
	}

	@Override
	public String toString()
	{
		return persistProperties.toString();
	}
}
