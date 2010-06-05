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
package com.servoy.eclipse.ui.views.solutionexplorer.actions;

import java.util.Iterator;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.widgets.Shell;

import com.servoy.eclipse.core.repository.EclipseMessages;
import com.servoy.eclipse.core.util.CoreUtils;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;

public class DeleteI18NAction extends Action implements ISelectionChangedListener
{
	private final Shell shell;
	private IStructuredSelection selection;

	/**
	 * Creates a new delete i18n action.
	 */
	public DeleteI18NAction(Shell shell)
	{
		this.shell = shell;
		setText("Delete I18N"); //$NON-NLS-1$
		setToolTipText("Delete I18N"); //$NON-NLS-1$
	}

	@Override
	public void run()
	{
		if (selection != null && MessageDialog.openConfirm(shell, getText(), "Are you sure you want to delete?")) //$NON-NLS-1$
		{
			Iterator<SimpleUserNode> it = selection.iterator();
			String[] i18nName;
			while (it.hasNext())
			{
				i18nName = CoreUtils.getTokenElements(it.next().getName(), ".", true); //$NON-NLS-1$
				EclipseMessages.deleteMessageFileNames(i18nName[0], i18nName[1]);
			}
		}
	}

	public void selectionChanged(SelectionChangedEvent event)
	{
		// allow multiple selection
		selection = null;
		IStructuredSelection sel = (IStructuredSelection)event.getSelection();
		boolean state = true;
		Iterator<SimpleUserNode> it = sel.iterator();
		while (it.hasNext() && state)
		{
			SimpleUserNode node = it.next();
			state = (node.getType() == UserNodeType.I18N_FILE_ITEM) && !node.isEnabled();
		}
		if (state)
		{
			selection = sel;
		}
		setEnabled(state);
	}

}
