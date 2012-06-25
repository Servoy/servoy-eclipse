/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

package com.servoy.eclipse.ui.editors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;

import com.servoy.eclipse.model.repository.SolutionDeserializer;
import com.servoy.j2db.persistence.IPersist;

/**
 * 
 * @author hhardut
 *
 */
public class EditorPartAdapter implements IAdapterFactory
{

	public Object getAdapter(Object adaptableObject, Class adapterType)
	{
		if (adaptableObject instanceof IEditorPart && adapterType.equals(IPersist.class))
		{
			IEditorPart ep = (IEditorPart)adaptableObject;
			IEditorInput edInput = ep.getEditorInput();
			if (edInput != null)
			{
				IFile file = (IFile)edInput.getAdapter(IFile.class);
				if (file != null)
				{
					return SolutionDeserializer.findPersistFromFile(file);
				}
			}
		}
		return null;
	}

	public Class[] getAdapterList()
	{
		return new Class[] { IPersist.class };
	}

}
