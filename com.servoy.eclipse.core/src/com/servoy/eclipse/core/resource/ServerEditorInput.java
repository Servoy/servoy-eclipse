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

import com.servoy.j2db.persistence.ServerConfig;

/**
 * Adapter for making a server resource a suitable input for an editor.
 * </p>
 */
public class ServerEditorInput implements IEditorInput
{
	public static final String SERVER_RESOURCE_ID = "com.servoy.eclipse.core.resource.server";
	private final ServerConfig serverConfig;
	private boolean isNew = false;

	/**
	 * Creates a form input.
	 * 
	 * @param solution
	 */

	public ServerEditorInput(ServerConfig serverConfig)
	{
		this.serverConfig = serverConfig;
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
		return serverConfig.getServerName();
	}

	public ServerConfig getServerConfig()
	{
		return serverConfig;
	}

	public boolean getIsNew()
	{
		return isNew;
	}

	public void setIsNew(boolean isNew)
	{
		this.isNew = isNew;
	}

	/*
	 * (non-Javadoc) Method declared on IEditorInput.
	 */
	public IPersistableElement getPersistable()
	{
		return (IPersistableElement)getAdapter(IPersistableElement.class);
	}

// /* (non-Javadoc)
// * Method declared on IStorageEditorInput.
// */
// public IStorage getStorage() {
// return file;
// }

	/*
	 * (non-Javadoc) Method declared on IEditorInput.
	 */
	public String getToolTipText()
	{
		return getName();
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
		result = prime * result + ((serverConfig.getServerName() == null) ? 0 : serverConfig.getServerName().hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		final ServerEditorInput other = (ServerEditorInput)obj;
		if (serverConfig.getServerName() == null)
		{
			if (other.serverConfig.getServerName() != null) return false;
		}
		else if (!serverConfig.getServerName().equals(other.serverConfig.getServerName())) return false;
		return true;
	}
}
