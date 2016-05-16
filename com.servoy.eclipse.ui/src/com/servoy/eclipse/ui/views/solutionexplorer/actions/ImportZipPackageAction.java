/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

import java.util.Set;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;

import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;
import com.servoy.j2db.server.ngclient.startup.resourceprovider.ResourceProvider;

/**
 * @author gboros
 *
 */
public abstract class ImportZipPackageAction extends Action
{
	protected final SolutionExplorerView viewer;

	public ImportZipPackageAction(SolutionExplorerView viewer)
	{
		this.viewer = viewer;
		setImageDescriptor(Activator.loadImageDescriptorFromOldLocations("import.gif"));
		setText("Import zip web package");
		setToolTipText(getText());
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.jface.action.Action#run()
	 */
	@Override
	public void run()
	{
		FileDialog fd = new FileDialog(viewer.getSite().getShell(), SWT.OPEN | SWT.MULTI);
		fd.open();
		String[] fileNames = fd.getFileNames();
		String filterPath = fd.getFilterPath();

		checkForDefaultPackageNameConflict(fileNames);
		doImport(fileNames, filterPath);
	}

	protected void checkForDefaultPackageNameConflict(String[] fileNames)
	{
		if (fileNames == null || fileNames.length == 0)
		{
			return;
		}
		Set<String> defaultPackageNames = ResourceProvider.getDefaultPackageNames();
		for (String fName : fileNames)
		{
			if (defaultPackageNames.contains(fName.split("\\.")[0]))
			{
				MessageDialog.openError(UIUtils.getActiveShell(), "Error", fName + " is a default " + " package.");
				return;
			}
		}
	}

	protected abstract void doImport(String[] fileNames, String filterPath);
}
