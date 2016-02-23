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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.sablo.specification.NGPackage;

import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.core.util.UIUtils.ScrollableDialog;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;

/**
 * @author user
 *
 */
public class ImportComponentFolderAction extends ImportComponentAction
{
	private final String entityType;

	/**
	 * @param viewer
	 */
	public ImportComponentFolderAction(SolutionExplorerView viewer, String entity, String folder, String entityType)
	{
		super(viewer, entity, folder);
		this.entityType = entityType;
		setImageDescriptor(Activator.loadImageDescriptorFromBundle("import_folder.gif"));
		setText("Import " + entity + " folder package");
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

	@Override
	protected File[] getImportFolderEntries(File importFolder)
	{
		ArrayList<File> importFolderEntries = new ArrayList<File>();
		NGPackage.DirPackageReader dirReader = new NGPackage.DirPackageReader(importFolder);
		try
		{
			Manifest mf = dirReader.getManifest();
			importFolderEntries.add(new File(importFolder, "META-INF"));
			for (Entry<String, Attributes> entry : mf.getEntries().entrySet())
			{
				if ("true".equalsIgnoreCase((String)entry.getValue().get(new Attributes.Name(entityType))))
				{
					File wcSpecFile = new File(importFolder, entry.getKey());
					if (wcSpecFile.exists()) importFolderEntries.add(wcSpecFile.getParentFile());
				}
			}

			final StringBuilder allComponents = new StringBuilder();
			if (importFolderEntries.size() > 1)
			{
				for (int i = 1; i < importFolderEntries.size(); i++)
				{
					if (allComponents.length() > 0) allComponents.append("\n");
					allComponents.append(importFolderEntries.get(i).getName());
				}
			}
			UIUtils.runInUI(new Runnable()
			{
				public void run()
				{
					ScrollableDialog dialog = new ScrollableDialog(UIUtils.getActiveShell(), IMessageProvider.INFORMATION, "Import " + entity + " folder",
						"The following " + folder + " will be imported: ", allComponents.toString());
					dialog.open();
				}
			}, true);
		}
		catch (final IOException ex)
		{
			ServoyLog.logError(ex);
			UIUtils.runInUI(new Runnable()
			{
				public void run()
				{
					ScrollableDialog dialog = new ScrollableDialog(UIUtils.getActiveShell(), IMessageProvider.ERROR, "Import " + entity + " folder",
						"The following error occured during import: ", ex.toString());
					dialog.open();
				}
			}, true);
		}

		return importFolderEntries.toArray(new File[importFolderEntries.size()]);
	}
}
