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

import java.io.File;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorMatchingStrategy;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.FileEditorInput;

import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.repository.SolutionDeserializer;
import com.servoy.eclipse.core.repository.SolutionSerializer;
import com.servoy.j2db.util.UUID;

/**
 * Match input for persist editors against file editor input.
 * 
 * @author rgansevles
 * 
 */
public class PersistMatchingStrategy implements IEditorMatchingStrategy
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
		if (editorInput instanceof PersistEditorInput && input instanceof FileEditorInput)
		{
			// open a file, see if it matches a persist editor
			FileEditorInput fileInput = (FileEditorInput)input;
			// FUTURE: remove this if when form methods are edited in form editor
			if (!fileInput.getFile().getFileExtension().equals(SolutionSerializer.JS_FILE_EXTENSION_WITHOUT_DOT))
			{
				return findUuidParent(fileInput.getFile().getWorkspace().getRoot().getLocation().toFile(), fileInput.getFile().getLocation().toFile(),
					((PersistEditorInput)editorInput).getUuid()) != null;
			}
		}

		return input.equals(editorInput);
	}

	protected File findUuidParent(File workspace, File file, UUID uuid)
	{
		if (uuid == null || file == null)
		{
			return null;
		}
		if (uuid.equals(SolutionDeserializer.getUUID(file)))
		{
			return file;
		}
		File parentFile = SolutionSerializer.getParentFile(workspace, file);
		return findUuidParent(workspace, parentFile, uuid);
	}
}
