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
package com.servoy.eclipse.ui.actions;

import java.awt.Font;

import org.eclipse.dltk.internal.ui.editor.ScriptEditor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.FontDialog;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.editors.text.TextEditor;

import com.servoy.eclipse.core.ServoyLog;
import com.servoy.j2db.util.PersistHelper;

public class ShowFontChooserActionDelegate implements IWorkbenchWindowActionDelegate
{
	private IWorkbenchWindow window;

	public void run(IAction action)
	{
		if (window != null)
		{
			IWorkbenchPart activePart = window.getPartService().getActivePart();
			if (activePart instanceof IEditorPart)
			{
				IEditorPart editor = (IEditorPart)activePart;
				FontDialog dialog = new FontDialog(editor.getEditorSite().getShell());
				FontData fontData = dialog.open();
				if (fontData != null)
				{
					Font font = new Font(fontData.getName(), fontData.getStyle(), fontData.getHeight());

					if (editor instanceof ScriptEditor)
					{
						ScriptEditor scriptEditor = (ScriptEditor)editor;
						ISourceViewer sv = scriptEditor.getScriptSourceViewer();
						if (sv == null) return;

						IDocument document = sv.getDocument();
						if (document == null) return;

						StyledText st = sv.getTextWidget();
						if (st == null || st.isDisposed()) return;

						Point textSelection = st.getSelectionRange();
						if (textSelection.y <= 0)
						{
							textSelection.x = st.getCaretOffset();
							textSelection.y = 0;
						}
						st.replaceTextRange(textSelection.x, textSelection.y, "'" + PersistHelper.createFontString(font) + "'");
						st.forceFocus();
					}
					else if (editor instanceof TextEditor)
					{
						TextEditor textEditor = (TextEditor)editor;
						IEditorInput input = textEditor.getEditorInput();
						if (textEditor.getDocumentProvider() != null && textEditor.getDocumentProvider().getDocument(input) != null)
						{
							IDocument document = textEditor.getDocumentProvider().getDocument(input);
							ISelection selection = textEditor.getSelectionProvider().getSelection();
							if (selection instanceof TextSelection)
							{
								try
								{
									document.replace(((TextSelection)selection).getOffset(), ((TextSelection)selection).getLength(),
										PersistHelper.createFontCssString(font));
								}
								catch (BadLocationException e)
								{
									ServoyLog.logError(e);
								}
							}
						}
					}
				}
			}
		}
	}

	public void selectionChanged(IAction action, ISelection selection)
	{
		boolean enabled = false;
		if (selection instanceof TextSelection) enabled = true;
		action.setEnabled(enabled);
	}

	public void dispose()
	{
		this.window = null;
	}

	public void init(IWorkbenchWindow window)
	{
		this.window = window;
	}
}
