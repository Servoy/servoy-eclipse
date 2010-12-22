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
package com.servoy.eclipse.core.resource;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.PlatformUI;

import com.servoy.j2db.util.UUID;

/**
 * Adapter for making a solution resource a suitable input for an editor.
 * </p>
 */
public class PersistEditorInput implements IEditorInput
{
	public static final String FORM_RESOURCE_ID = "com.servoy.eclipse.core.resource.form";
	public static final String VALUELIST_RESOURCE_ID = "com.servoy.eclipse.core.resource.valuelist";
	public static final String RELATION_RESOURCE_ID = "com.servoy.eclipse.core.resource.relation";
	public static final String MEDIA_RESOURCE_ID = "com.servoy.eclipse.core.resource.media";

	private final String name;
	private final UUID uuid;
	private final String solutionName;

	/**
	 * Creates a persist input.
	 * 
	 * @param solution
	 */
	public PersistEditorInput(String name, String solutionName, UUID uuid)
	{
		this.name = name;
		this.uuid = uuid;
		this.solutionName = solutionName;
	}

	/*
	 * (non-Javadoc) Method declared on IEditorInput.
	 */
	public boolean exists()
	{
		return true;
	}

	/*
	 * (non-Javadoc) Method declared on IAdaptable.
	 */
	public Object getAdapter(Class adapter)
	{
		return Platform.getAdapterManager().getAdapter(this, adapter);
	}

	/*
	 * (non-Javadoc) Method declared on IEditorInput.
	 */
	public ImageDescriptor getImageDescriptor()
	{
		return PlatformUI.getWorkbench().getEditorRegistry().getImageDescriptor(null);
	}

	/*
	 * (non-Javadoc) Method declared on IEditorInput.
	 */
	public String getName()
	{
		return name;
	}


	public UUID getUuid()
	{
		return uuid;
	}

	public String getSolutionName()
	{
		return solutionName;
	}

	/*
	 * (non-Javadoc) Method declared on IEditorInput.
	 */
	public String getToolTipText()
	{
		return solutionName + '.' + name;
	}

	/*
	 * (non-Javadoc) Method declared on IEditorInput.
	 */
	public IPersistableElement getPersistable()
	{
		return (IPersistableElement)getAdapter(IPersistableElement.class);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return getClass().getName() + "(" + getToolTipText() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		final PersistEditorInput other = (PersistEditorInput)obj;
		if (uuid == null)
		{
			if (other.uuid != null) return false;
		}
		else if (!uuid.equals(other.uuid)) return false;
		return true;
	}

}
