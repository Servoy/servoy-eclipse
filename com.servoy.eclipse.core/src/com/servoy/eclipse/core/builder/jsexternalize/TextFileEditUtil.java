/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2011 Servoy BV

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

package com.servoy.eclipse.core.builder.jsexternalize;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.TextEdit;

import com.servoy.eclipse.model.util.ServoyLog;

/**
 * Utility class for editing text files
 * @author gboros
 */
public class TextFileEditUtil
{
	public static void applyTextEdit(IFile scriptFile, TextEdit textEdit)
	{
		ITextFileBufferManager textFileBufferManager = FileBuffers.getTextFileBufferManager();
		try
		{
			textFileBufferManager.connect(scriptFile.getFullPath(), LocationKind.IFILE, null);
		}
		catch (CoreException e)
		{
			ServoyLog.logError(e);
			return;
		}

		try
		{
			ITextFileBuffer textFileBuffer = textFileBufferManager.getTextFileBuffer(scriptFile.getFullPath(), LocationKind.IFILE);
			IDocument document = textFileBuffer.getDocument();

			try
			{
				textEdit.apply(document);
			}
			catch (Exception e)
			{
				ServoyLog.logError(e);
			}
		}
		finally
		{
			try
			{
				textFileBufferManager.disconnect(scriptFile.getFullPath(), LocationKind.IFILE, null);
			}
			catch (CoreException e)
			{
				ServoyLog.logError(e);
			}
		}
	}
}
