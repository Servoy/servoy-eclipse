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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;
import com.servoy.eclipse.ui.wizards.NewPackageProjectWizard;

/**
 * @author gganea@servoy.com
 *
 */
public class NewPackageProjectAction extends Action implements ISelectionChangedListener
{

	private final SolutionExplorerView viewer;
	private final Shell shell;
	private IStructuredSelection selection;
	private final String packageType;

	/**
	 * @param text - the label of the menu entry
	 */
	public NewPackageProjectAction(SolutionExplorerView solutionExplorerView, Shell shell, String text, String packageType)
	{
		this.viewer = solutionExplorerView;
		this.shell = shell;
		this.packageType = packageType;
		setText(text);
	}

	@Override
	public void run()
	{
		NewPackageProjectWizard wizard = new NewPackageProjectWizard(packageType);
		wizard.init(PlatformUI.getWorkbench(), selection);
		wizard.setSolutionExplorerView(viewer);
		WizardDialog wd = new WizardDialog(shell, wizard);
		wd.setTitle(wizard.getWindowTitle());
		wd.open();
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event)
	{
		if (event.getSelection() instanceof IStructuredSelection) this.selection = (IStructuredSelection)event.getSelection();
	}

}
