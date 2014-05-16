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
import org.eclipse.ui.part.FileEditorInput;

import com.servoy.eclipse.model.repository.DataModelManager;
import com.servoy.eclipse.model.repository.SolutionSerializer;

/**
 * Adapter for making a table resource a suitable input for an editor.
 * </p>
 */
public class TableEditorInput implements IEditorInput
{
	public static final String TABLE_RESOURCE_ID = "com.servoy.eclipse.core.resource.table";
	private final String tableName;
	private final String serverName;

	/**
	 * Creates a form input.
	 * 
	 * @param solution
	 */
	public TableEditorInput(String serverName, String tableName)
	{
		this.serverName = serverName;
		this.tableName = tableName;
	}


	public static TableEditorInput createFromFileEditorInput(FileEditorInput fileEditorInput)
	{
		if (fileEditorInput != null)
		{
			String serverName;
			String tableName;
			String[] segments = fileEditorInput.getFile().getProjectRelativePath().segments();
			// dbi files
			if (segments.length >= 2 && segments[segments.length - 1].endsWith(DataModelManager.COLUMN_INFO_FILE_EXTENSION_WITH_DOT))
			{
				serverName = segments[segments.length - 2];
				tableName = segments[segments.length - 1].substring(0, segments[segments.length - 1].length() -
					DataModelManager.COLUMN_INFO_FILE_EXTENSION_WITH_DOT.length());
			}
			// obj files: datasources: table nodes
			else if (segments.length >= 3 && segments[segments.length - 3].equals(SolutionSerializer.DATASOURCES_DIR_NAME) &&
				segments[segments.length - 1].endsWith(SolutionSerializer.TABLENODE_FILE_EXTENSION))
			{
				serverName = segments[segments.length - 2];
				tableName = segments[segments.length - 1].substring(0, segments[segments.length - 1].length() - SolutionSerializer.JSON_FILE_EXTENSION_SIZE);
			}
			// obj files: datasources: aggregates
			else if (segments.length >= 4 && segments[segments.length - 4].equals(SolutionSerializer.DATASOURCES_DIR_NAME) &&
				segments[segments.length - 1].endsWith(SolutionSerializer.JSON_DEFAULT_FILE_EXTENSION))
			{
				serverName = segments[segments.length - 3];
				tableName = segments[segments.length - 2];
			}
			else
			{
				return null;
			}
			return new TableEditorInput(serverName, tableName);
		}
		// cannot find info for table editor input
		return null;
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
		return tableName;
	}

	public String getServerName()
	{
		return serverName;
	}

	/*
	 * (non-Javadoc) Method declared on IEditorInput.
	 */
	public IPersistableElement getPersistable()
	{
		return (IPersistableElement)getAdapter(IPersistableElement.class);
	}

	/*
	 * (non-Javadoc) Method declared on IEditorInput.
	 */
	public String getToolTipText()
	{
		return serverName + '.' + tableName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return getClass().getName() + "(" + getToolTipText() + ")";
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((serverName == null) ? 0 : serverName.hashCode());
		result = prime * result + ((tableName == null) ? 0 : tableName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		final TableEditorInput other = (TableEditorInput)obj;
		if (serverName == null)
		{
			if (other.serverName != null) return false;
		}
		else if (!serverName.equals(other.serverName)) return false;
		if (tableName == null)
		{
			if (other.tableName != null) return false;
		}
		else if (!tableName.equals(other.tableName)) return false;
		return true;
	}

}
