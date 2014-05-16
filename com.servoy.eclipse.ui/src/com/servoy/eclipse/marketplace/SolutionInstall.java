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

package com.servoy.eclipse.marketplace;

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.ui.wizards.ImportSolutionWizard;

/**
 * Class representing an installable solution from the Servoy Marketplace
 * @author gboros
 *
 */
public class SolutionInstall implements InstallItem
{
	private final File solutionFile;

	public SolutionInstall(File solutionFile)
	{
		this.solutionFile = solutionFile;
	}

	public void install(IProgressMonitor monitor) throws Exception
	{
		Display.getDefault().syncExec(new Runnable()
		{
			public void run()
			{
				IStructuredSelection selection = StructuredSelection.EMPTY;
				ImportSolutionWizard importSolutionWizard = new ImportSolutionWizard("Extension install - import solution");
				importSolutionWizard.setSolutionFilePath(solutionFile.getAbsolutePath());
				importSolutionWizard.setAskForImportServerName(true);
				importSolutionWizard.init(PlatformUI.getWorkbench(), selection);
				WizardDialog dialog = new WizardDialog(UIUtils.getActiveShell(), importSolutionWizard);
				dialog.create();
				dialog.open();
			}
		});
	}

	public String getName()
	{
		return solutionFile.getName();
	}
}
