/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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

package com.servoy.eclipse.jsunit.actions;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Path;
import org.eclipse.dltk.core.ModelException;
import org.eclipse.dltk.internal.ui.editor.EditorUtility;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.model.util.ServoyLog;

/**
 * This action is created in response to a double click on the stack trace of ScriptUnit view.
 * Callback from  {@link com.servoy.eclipse.jsunit.scriptunit.JSUnitTestRunnerUI}
 * @author obuligan
 *
 */
public class OpenEditorAtLineAction extends Action
{
	protected final int lineNumber;
	protected final String scriptFilePath;
	private final boolean absolutePath;

	public OpenEditorAtLineAction(String scriptFilePath, int lineNumber)
	{
		this(scriptFilePath, true, lineNumber);
	}

	/**
	 *  If lineNumber is <1 it just opens the editor and doesn't try to move the selection to the line number 
	 */
	public OpenEditorAtLineAction(String scriptFilePath, boolean absolutePath, int lineNumber)
	{
		this.scriptFilePath = scriptFilePath;
		this.absolutePath = absolutePath;
		this.lineNumber = lineNumber;
	}

	@Override
	public void run()
	{
		IFile globalsNodeFile;
		if (absolutePath) globalsNodeFile = ServoyModel.getWorkspace().getRoot().getFileForLocation(new Path(scriptFilePath));
		else globalsNodeFile = ServoyModel.getWorkspace().getRoot().getFile(new Path(scriptFilePath));

		try
		{
			IEditorPart openInEditor = EditorUtility.openInEditor(globalsNodeFile, true);
			if (openInEditor instanceof ITextEditor && lineNumber != -1)
			{
				ITextEditor editor = (ITextEditor)openInEditor;

				IDocumentProvider provider = editor.getDocumentProvider();
				IDocument document = provider.getDocument(editor.getEditorInput());
				int start = document.getLineOffset(lineNumber - 1);
				editor.selectAndReveal(start, document.getLineLength(lineNumber - 1));
			}
		}
		catch (PartInitException e)
		{
			ServoyLog.logError(e);
		}
		catch (ModelException e)
		{
			ServoyLog.logError(e);
		}
		catch (BadLocationException e)
		{
			ServoyLog.logError(e);
		}
	}
}
