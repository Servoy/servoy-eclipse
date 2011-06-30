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
package com.servoy.eclipse.ui.resource;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.part.FileEditorInput;

import com.servoy.eclipse.ui.preferences.DesignerPreferences;

/**
 * Factory for persisted editor inputs.
 * Editors state is saved over sessions when configured in preferences.
 * 
 * @author rgansevles
 */

public class FileEditorInputFactory extends org.eclipse.ui.part.FileEditorInputFactory
{
	public static FileEditorInput createFileEditorInput(IFile file)
	{
		return new FileEditorInput(file)
		{
			@Override
			public IPersistableElement getPersistable()
			{
				if (!(new DesignerPreferences().getCloseEditorOnExit()))
				{
					return super.getPersistable();
				}
				return null;
			}
		};
	}

	/**
	 * Tag for the IFile.fullPath of the file resource.
	 */
	private static final String TAG_PATH = "path"; //$NON-NLS-1$

	/*
	 * (non-Javadoc) Method declared on IElementFactory.
	 */
	@Override
	public IAdaptable createElement(IMemento memento)
	{
		// Get the file name.
		String fileName = memento.getString(TAG_PATH);
		if (fileName == null)
		{
			return null;
		}

		// Get a handle to the IFile...which can be a handle
		// to a resource that does not exist in workspace
		IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(fileName));
		if (file != null)
		{
			return createFileEditorInput(file);
		}
		else
		{
			return null;
		}
	}
}
