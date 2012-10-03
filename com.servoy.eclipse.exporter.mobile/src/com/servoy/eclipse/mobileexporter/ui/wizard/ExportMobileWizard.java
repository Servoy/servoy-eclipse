/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

package com.servoy.eclipse.mobileexporter.ui.wizard;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;

import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.wizards.FinishPage;

public class ExportMobileWizard extends Wizard implements IExportWizard
{

	private final FinishPage finishPage = new FinishPage("lastPage")
	{
		@Override
		public boolean isPageComplete()
		{
			return super.isCurrentPage();
		}

		@Override
		public boolean canFlipToNextPage()
		{
			return false;
		}
	};
	private final PhoneGapApplicationPage pgAppPage = new PhoneGapApplicationPage("PhoneGap Application", finishPage);

	public ExportMobileWizard()
	{
		IDialogSettings workbenchSettings = Activator.getDefault().getDialogSettings();
		IDialogSettings section = workbenchSettings.getSection("MobileExportWizard");//$NON-NLS-1$
		if (section == null)
		{
			section = workbenchSettings.addNewSection("MobileExportWizard");//$NON-NLS-1$
		}
		setDialogSettings(section);
		finishPage.setTitle("Export finished");
		setWindowTitle("Mobile Export");
	}

	private WarExportPage selectionPage;

	public void init(IWorkbench workbench, IStructuredSelection selection)
	{
		selectionPage = new WarExportPage("outputPage", "Choose output", null, finishPage, pgAppPage);
	}

	@Override
	public boolean performFinish()
	{
		return true;
	}

	@Override
	public void addPages()
	{
		addPage(selectionPage);
		addPage(finishPage);
		addPage(pgAppPage);
	}

}
