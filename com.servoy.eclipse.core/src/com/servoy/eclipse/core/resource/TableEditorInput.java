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

import com.servoy.eclipse.model.util.ResourcesUtils;
import com.servoy.j2db.util.DataSourceUtils;

/**
 * Adapter for making a table resource a suitable input for an editor.
 * </p>
 */
public class TableEditorInput implements IEditorInput
{
	public static final String TABLE_RESOURCE_ID = "com.servoy.eclipse.core.resource.table";
	private final String dataSource;

	/**
	 * Creates a form input.
	 *
	 * @param solution
	 */
	public TableEditorInput(String dataSource)
	{
		this.dataSource = dataSource;
	}


	public static TableEditorInput createFromFileEditorInput(FileEditorInput fileEditorInput)
	{
		if (fileEditorInput != null)
		{
			String dataSource = ResourcesUtils.getParentDatasource(fileEditorInput.getFile(), true);
			if (dataSource != null)
			{
				return new TableEditorInput(dataSource);
			}
		}
		// cannot find info for table editor input
		return null;
	}

	@Override
	public String getName()
	{
		return dataSource;
	}


	/**
	 * @return
	 */
	public String getDataSource()
	{
		return dataSource;
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

	public IPersistableElement getPersistable()
	{
		return (IPersistableElement)getAdapter(IPersistableElement.class);
	}

	public String getToolTipText()
	{
		String[] snt = DataSourceUtils.getDBServernameTablename(dataSource);
		if (snt != null) return snt[0] + '.' + snt[1];
		return dataSource;
	}

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
		result = prime * result + ((dataSource == null) ? 0 : dataSource.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		final TableEditorInput other = (TableEditorInput)obj;
		if (dataSource == null)
		{
			if (other.dataSource != null) return false;
		}
		else if (!dataSource.equals(other.dataSource)) return false;
		return true;
	}
}
