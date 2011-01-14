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
import org.eclipse.team.internal.ui.ProjectSetImporter;
import org.eclipse.ui.PlatformUI;
import org.w3c.dom.Node;

import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.wizards.ImportSolutionWizard;

/**
 * Class representing an installable solution from the Servoy Marketplace
 * @author gabi
 *
 */
public class SolutionInstall extends InstallItem
{
	public static final String destinationDir = "solutions/marketplace"; //$NON-NLS-1$
	private final ImportSolutionWizard importSolutionWizard;

	public SolutionInstall(Node entryNode)
	{
		super(entryNode);
		importSolutionWizard = new ImportSolutionWizard();
	}

	@Override
	public void install(IProgressMonitor monitor) throws Exception
	{
		final String dType = getDownloadType();
		if (DOWNLOAD_TYPE_ZIP.equals(dType) || DOWNLOAD_TYPE_SERVOY.equals(dType) || DOWNLOAD_TYPE_PSF.equals(dType))
		{
			File downloadedFile = downloadURL(DOWNLOAD_TYPE_ZIP.equals(dType) ? "" : destinationDir, monitor, "Installing solution " + getName() + " ..."); //$NON-NLS-1$
			if (downloadedFile != null)
			{
				final String downloadFileCanonicalPath = downloadedFile.getCanonicalPath();

				if (DOWNLOAD_TYPE_PSF.equals(dType))
				{
					ProjectSetImporter.importProjectSet(downloadFileCanonicalPath, UIUtils.getActiveShell(), monitor);
					new File(downloadFileCanonicalPath).delete();
				}
				else
				{
					Display.getDefault().syncExec(new Runnable()
					{
						public void run()
						{
							IStructuredSelection selection = StructuredSelection.EMPTY;
							importSolutionWizard.setSolutionFilePath(downloadFileCanonicalPath);
							importSolutionWizard.setAskForImportServerName(true);
							importSolutionWizard.init(PlatformUI.getWorkbench(), selection);
							WizardDialog dialog = new WizardDialog(UIUtils.getActiveShell(), importSolutionWizard);
							dialog.create();
							dialog.open();
						}
					});
				}
			}
		}
		else
		{
			ServoyLog.logWarning("Maketplace unknown download type for solution install " + getDownloadType(), null);
		}
	}
}
