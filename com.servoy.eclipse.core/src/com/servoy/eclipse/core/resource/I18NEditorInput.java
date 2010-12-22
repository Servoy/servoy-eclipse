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

import com.servoy.eclipse.model.repository.EclipseMessages;
import com.servoy.eclipse.model.util.ModelUtils;

/**
 * Adapter for making a i18n resource a suitable input for an editor.
 * </p>
 */
public class I18NEditorInput implements IEditorInput
{
	public static final String I18N_RESOURCE_ID = "com.servoy.eclipse.core.resource.i18n";
	private final String i18nServer;
	private final String i18nTable;

	public static I18NEditorInput createFromFileEditorInput(FileEditorInput fileEditorInput)
	{
		if (fileEditorInput != null)
		{
			String fileName = fileEditorInput.getName();
			if (fileName.endsWith(EclipseMessages.MESSAGES_EXTENSION))
			{
				String[] fileNameTokens = ModelUtils.getTokenElements(fileName, ".", true);
				if (fileNameTokens != null && fileNameTokens.length > 1)
				{
					return new I18NEditorInput(fileNameTokens[0], fileNameTokens[1]);
				}
			}
		}
		// cannot find info for i18n editor input
		return null;
	}

	public I18NEditorInput(String i18nServer, String i18nTable)
	{
		this.i18nServer = i18nServer;
		this.i18nTable = i18nTable;
	}

	public String getServer()
	{
		return i18nServer;
	}

	public String getTable()
	{
		return i18nTable;
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
		return new StringBuffer("I18N ").append(i18nServer).append('.').append(i18nTable).toString();
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
		return getName();
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((i18nServer == null) ? 0 : i18nServer.hashCode());
		result = prime * result + ((i18nTable == null) ? 0 : i18nTable.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		final I18NEditorInput other = (I18NEditorInput)obj;
		if (i18nServer == null)
		{
			if (other.i18nServer != null) return false;
		}
		else if (!i18nServer.equals(other.i18nServer)) return false;
		if (i18nTable == null)
		{
			if (other.i18nTable != null) return false;
		}
		else if (!i18nTable.equals(other.i18nTable)) return false;
		return true;
	}
}
