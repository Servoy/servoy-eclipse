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
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IActionDelegate;

import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.actions.Openable.OpenableForm;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.persistence.Form;

/**
 * @author gerzse
 */
public class OpenFormEditorAction extends Action implements IActionDelegate
{

	protected IStructuredSelection selection;

	public OpenFormEditorAction()
	{
		setImageDescriptor(Activator.loadImageDescriptorFromBundle("designer.gif")); //$NON-NLS-1$
		setText("Open in Form Editor"); //$NON-NLS-1$
		setToolTipText(getText());
	}

	@Override
	public void run()
	{
		if (selection != null)
		{
			Iterator< ? > it = selection.iterator();
			while (it.hasNext())
			{
				Form form = ((OpenableForm)it.next()).getData();
				if (form != null)
				{
					EditorUtil.openFormDesignEditor(form);
				}
			}
		}
	}

	@Override
	public void run(IAction action)
	{
		run();
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection)
	{
		if (selection instanceof IStructuredSelection)
		{
			this.selection = (IStructuredSelection)selection;
			Iterator< ? > it = ((IStructuredSelection)selection).iterator();
			boolean state = it.hasNext();
			while (it.hasNext())
			{
				state = it.next() instanceof OpenableForm;
			}
			setEnabled(state);
		}
		else setEnabled(false);
	}

}
