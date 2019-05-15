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

package com.servoy.eclipse.cheatsheets.actions;

import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.core.util.IntroCloseWelcomePage;
import com.servoy.eclipse.ui.wizards.NewSolutionWizard;
import com.servoy.j2db.persistence.SolutionMetaData;

/**
 * Starts the New Solution wizard.
 *
 * @author gerzse
 */
public class CreateNewSolutionAction extends IntroCloseWelcomePage
{
	@Override
	public void run()
	{
		super.run();
		NewSolutionWizard wizard = new NewSolutionWizard()
		{
			@Override
			public void createPageControls(Composite pageContainer)
			{
				super.createPageControls(pageContainer);
				configPage.setSolutionTypes(SolutionMetaData.solutionTypes, 10, false);
			}
		};
		wizard.init(PlatformUI.getWorkbench(), null);
		WizardDialog dialog = new WizardDialog(PlatformUI.getWorkbench().getDisplay().getActiveShell(), wizard);
		dialog.create();
		dialog.open();
	}
}
