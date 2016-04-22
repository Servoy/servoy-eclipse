/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2016 Servoy BV

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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.wizards.IWizardDescriptor;

import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;
import com.servoy.eclipse.ui.wizards.NewPackageProjectWizard;
import com.servoy.j2db.util.Debug;

/**
 * @author gganea@servoy.com
 *
 */
public class NewPackageProjectAction extends Action implements ISelectionChangedListener
{

	private final SolutionExplorerView viewer;
	private final Shell shell;
	private IStructuredSelection selection;

	/**
	 * @param text - the label of the menu entry
	 */
	public NewPackageProjectAction(SolutionExplorerView solutionExplorerView, Shell shell, String text)
	{
		this.viewer = solutionExplorerView;
		this.shell = shell;
		setText(text);
	}

	@Override
	public void run()
	{
		IWizardDescriptor descriptor = PlatformUI.getWorkbench().getNewWizardRegistry().findWizard("com.servoy.eclipse.ui.NewPackageProjectWizard");
		try
		{
			if (descriptor != null)
			{
				NewPackageProjectWizard wizard = (NewPackageProjectWizard)descriptor.createWizard();
				wizard.init(PlatformUI.getWorkbench(), selection);
				wizard.setSolutionExplorerView(viewer);
				WizardDialog wd = new WizardDialog(shell, wizard);
				wd.setTitle(wizard.getWindowTitle());
				wd.open();
			}
		}
		catch (CoreException e)
		{
			Debug.log(e);
		}
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event)
	{
		if (event.getSelection() instanceof IStructuredSelection) this.selection = (IStructuredSelection)event.getSelection();
	}

}
