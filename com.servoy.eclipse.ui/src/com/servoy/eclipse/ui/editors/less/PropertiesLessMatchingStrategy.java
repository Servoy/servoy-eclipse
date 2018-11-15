/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2018 Servoy BV

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

package com.servoy.eclipse.ui.editors.less;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorMatchingStrategy;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.FileEditorInput;

import com.servoy.eclipse.model.util.ServoyLog;

/**
 * Match input for less properties editor against file editor input.
 * @author emera
 */
public class PropertiesLessMatchingStrategy implements IEditorMatchingStrategy
{

	public boolean matches(IEditorReference editorRef, IEditorInput input)
	{
		if (input == null)
		{
			return false;
		}

		IEditorInput editorInput = null;
		try
		{
			// this may activate the editor plugin
			editorInput = editorRef.getEditorInput();
		}
		catch (PartInitException e)
		{
			ServoyLog.logError(e);
		}
		if (editorInput instanceof PropertiesLessEditorInput && input instanceof FileEditorInput)
		{
			return editorInput.equals(PropertiesLessEditorInput.createFromFileEditorInput((FileEditorInput)input));
		}

		return input.equals(editorInput);
	}
}
