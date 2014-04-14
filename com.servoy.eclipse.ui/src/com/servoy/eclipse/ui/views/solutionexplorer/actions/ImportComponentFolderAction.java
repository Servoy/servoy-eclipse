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

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.DirectoryDialog;

import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;

/**
 * @author user
 *
 */
public class ImportComponentFolderAction extends ImportComponentAction
{

	/**
	 * @param viewer
	 */
	public ImportComponentFolderAction(SolutionExplorerView viewer)
	{
		super(viewer);
		setImageDescriptor(Activator.loadImageDescriptorFromBundle("import_folder.gif"));
		setText("Import component folder");
		setToolTipText(getText());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.eclipse.ui.views.solutionexplorer.actions.ImportComponentAction#run()
	 */
	@Override
	public void run()
	{
		DirectoryDialog ddDialog = new DirectoryDialog(viewer.getSite().getShell(), SWT.OPEN | SWT.MULTI);
		String absoluteFolderPath = ddDialog.open();
		if (absoluteFolderPath != null)
		{
			Path absolutePath = new Path(absoluteFolderPath);
			String lastSegment = absolutePath.lastSegment();
			String[] fileNames = new String[] { lastSegment };
			String filterPath = ddDialog.getFilterPath();
			if (filterPath.equals(absoluteFolderPath))
			{
				Path eclipseFilterPath = new Path(filterPath);
				IPath eclipseCorrectFilterPath = eclipseFilterPath.removeLastSegments(1);
				filterPath = eclipseCorrectFilterPath.toOSString();

			}
			checkIfOverwriteAndDoImport(fileNames, filterPath);
		}
	}

}
