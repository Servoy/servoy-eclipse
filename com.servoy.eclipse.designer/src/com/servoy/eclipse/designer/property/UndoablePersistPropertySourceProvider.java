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
package com.servoy.eclipse.designer.property;

import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.IPropertySourceProvider;

import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.ui.property.PersistPropertySource;
import com.servoy.j2db.persistence.IPersist;

/**
 * IPropertySourceProvider implementation that provides UndoablePropertySource wrapper around PersistPropertySource.
 * Properties are set via the command stack, this enables undo/redo.
 * 
 * @author rgansevles
 */

public class UndoablePersistPropertySourceProvider implements IPropertySourceProvider
{
	private final BaseVisualFormEditor editorPart;

	public UndoablePersistPropertySourceProvider(BaseVisualFormEditor editorPart)
	{
		this.editorPart = editorPart;
	}

	public IPropertySource getPropertySource(Object object)
	{
		if (object instanceof IPersist)
		{
			return new UndoablePropertySource(PersistPropertySource.createPersistPropertySource(((IPersist)object), editorPart.getForm(), false), editorPart);
		}
		return null;
	}
}
