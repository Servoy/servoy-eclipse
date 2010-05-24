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
package com.servoy.eclipse.ui.quickfix;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IWorkbenchWizard;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.ui.wizards.SynchronizeDBIWithDBWizard;

public class DBIQuickFixShowSyncWizard implements IMarkerResolution
{

	public String getLabel()
	{
		return "Open synchronize with database wizard";
	}

	public void run(IMarker marker)
	{
		IWorkbenchWizard wizard = new SynchronizeDBIWithDBWizard();

		IStructuredSelection selection = StructuredSelection.EMPTY;
		wizard.init(PlatformUI.getWorkbench(), selection);

		// Instantiates the wizard container with the wizard and opens it
		WizardDialog dialog = new WizardDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), wizard);
		dialog.create();
		dialog.open();
		wizard.dispose();
	}

}