/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2026 Servoy BV

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

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.dialogs.TagsAndI18NTextDialog;

/**
 * @author gabi
 *
 */
public class ReplaceWithI18NTag implements IEditorActionDelegate, ISelectionChangedListener
{
	private ITextEditor textEditor;
	private IAction action;

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	@Override
	public void run(IAction action)
	{
		if (textEditor != null)
		{
			ISelection selection = textEditor.getSelectionProvider().getSelection();
			if (selection instanceof ITextSelection)
			{
				ITextSelection textSelection = (ITextSelection)selection;
				String selectedText = textSelection.getText();
				// Open dialog with selected text
				TagsAndI18NTextDialog dialog = new TagsAndI18NTextDialog(textEditor.getSite().getShell(), null,
					null, null, selectedText,
					"Edit Text",
					com.servoy.eclipse.core.Activator.getDefault().getDesignClient(), false);
				dialog.setOpenI18nTab(true);
				dialog.open();

				if (dialog.getReturnCode() != Window.CANCEL)
				{
					// Replace selected text with processed result
					try
					{
						textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput())
							.replace(textSelection.getOffset(), textSelection.getLength(), dialog.getValue().toString());
					}
					catch (BadLocationException ex)
					{
						ServoyLog.logError(ex);
					}
				}
			}
		}

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction, org.eclipse.jface.viewers.ISelection)
	 */
	@Override
	public void selectionChanged(IAction action, ISelection selection)
	{
		this.action = action;
		// Enable action only when text is selected
		updateActionEnablement(selection);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.ui.IEditorActionDelegate#setActiveEditor(org.eclipse.jface.action.IAction, org.eclipse.ui.IEditorPart)
	 */
	@Override
	public void setActiveEditor(IAction action, IEditorPart targetEditor)
	{
		// Unregister from previous editor
		if (textEditor != null && textEditor.getSelectionProvider() != null)
		{
			textEditor.getSelectionProvider().removeSelectionChangedListener(this);
		}

		if (targetEditor instanceof ITextEditor)
		{
			this.textEditor = (ITextEditor)targetEditor;
			this.action = action;

			// Register as selection listener to properly receive selection changes
			if (textEditor.getSelectionProvider() != null)
			{
				textEditor.getSelectionProvider().addSelectionChangedListener(this);
				// Initialize action enablement based on current selection
				ISelection currentSelection = textEditor.getSelectionProvider().getSelection();
				updateActionEnablement(currentSelection);
			}
		}
		else
		{
			this.textEditor = null;
			this.action = null;
		}
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event)
	{
		if (action != null)
		{
			updateActionEnablement(event.getSelection());
		}
	}

	private void updateActionEnablement(ISelection selection)
	{
		boolean enabled = false;
		if (selection instanceof ITextSelection)
		{
			ITextSelection textSelection = (ITextSelection)selection;
			enabled = textSelection.getLength() > 0;
		}
		action.setEnabled(enabled);
	}
}
