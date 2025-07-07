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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.sablo.specification.Package.IPackageReader;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.nature.ServoyResourcesProject;
import com.servoy.eclipse.model.util.ScrollableDialog;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.util.Pair;

/**
 * @author gganea, gboros
 *
 */
public class ImportZipPackageAsZipAction extends ImportZipPackageAction
{
	private int overrideReturnCode;
	protected final String folderName;

	public ImportZipPackageAsZipAction(SolutionExplorerView viewer, String folder)
	{
		super(viewer, "Import zip Servoy package", "Imports a zip Servoy package (component/service/layout package) into the solution");
		this.folderName = folder;
	}

	@Override
	protected void doImport(String[] fileNames, String filterPath)
	{
		overrideReturnCode = IDialogConstants.YES_TO_ALL_ID;

		final List<String> existingFiles = existingComponents(fileNames);
		if (!existingFiles.isEmpty())
		{
			UIUtils.runInUI(new Runnable()
			{
				public void run()
				{
					ScrollableDialog dialog = new ScrollableDialog(UIUtils.getActiveShell(), IMessageProvider.ERROR, "Error",
						"The folowing files already exist: ", existingFiles.toString());
					List<Pair<Integer, String>> buttonsAndLabels = new ArrayList<Pair<Integer, String>>();
					buttonsAndLabels.add(new Pair<Integer, String>(IDialogConstants.YES_TO_ALL_ID, "Overwrite all"));
					buttonsAndLabels.add(new Pair<Integer, String>(IDialogConstants.CANCEL_ID, "Cancel"));
					dialog.setCustomBottomBarButtons(buttonsAndLabels);
					//shell.setSize(400, 500);
					overrideReturnCode = dialog.open();
				}
			}, true);
		}
		if (overrideReturnCode == IDialogConstants.YES_TO_ALL_ID) addNGPackages(filterPath, fileNames, existingFiles);
	}

	private void addNGPackages(String filterPath, String[] fileNames, List<String> existingFiles)
	{
		IFolder componentsFolder = checkComponentsFolderCreated();
		for (String fileName : fileNames)
		{
			File file = new File(filterPath + File.separator + fileName);
			if (!checkManifestLocation(file)) continue;
			IPackageReader reader = new org.sablo.specification.Package.ZipPackageReader(file, fileName);
			String packageNameToImport = reader.getPackageName();

			if (packageNameToImport != null)
			{
				if (!checkForAlreadyLoadedPackage(packageNameToImport, componentsFolder, fileName, existingFiles)) continue;
				if (file.exists() && file.isFile())
				{
					importZipFileComponent(componentsFolder, file);
				}
			}
			else
			{
				UIUtils.reportError("Import ng package zip",
					"The selected file doesn't seem to be an ng package zip.\nCannot read its package name.\n\nSkipping import.");
			}
		}
	}

	protected boolean checkForAlreadyLoadedPackage(String packageNameToImport, IFolder componentsFolder, String fileName,
		List<String> existingThatWillBeReplacedFiles)
	{
		// see if an active zip or package project with the same package name exists (so we don't end up having two separate locations the same package can be loaded from in the active solution)
		List<Pair<String, File>> loadedSamePackageNameReferences = ServoyModelFinder.getServoyModel().getNGPackageManager().getReferencingProjectsThatLoaded(
			packageNameToImport);
		for (Pair<String, File> loaded : loadedSamePackageNameReferences)
		{
			if (!existingThatWillBeReplacedFiles.contains(loaded.getRight().getName()) ||
				!(new File(componentsFolder.getLocationURI()).equals(loaded.getRight().getParentFile())))
			{
				// so this package name is already loaded in the active solution from a location that will not be replaced right now
				UIUtils.reportError("Cannot import zip package as project",
					"The package with name: '" + packageNameToImport + "' is already used/loaded in currently active solution.\n(by project '" +
						loaded.getLeft() + "' from location '" + loaded.getRight() + "')\n\nSkipping import.");
			}
			return false;

		}
		return true;
	}

	protected File[] getImportFolderEntries(File importFolder)
	{
		return importFolder.listFiles();
	}

	private void importZipFileComponent(IFolder componentsFolder, File javaFile)
	{
		IFile eclipseFile = componentsFolder.getFile(javaFile.getName());

		if (eclipseFile.exists())
		{
			try
			{
				eclipseFile.delete(true, new NullProgressMonitor());
			}
			catch (CoreException e)
			{
				ServoyLog.logError(e);
			}
		}
		eclipseFile = componentsFolder.getFile(javaFile.getName());
		InputStream source;
		try
		{
			source = new FileInputStream(javaFile);
			eclipseFile.create(source, IResource.NONE, null);
		}
		catch (FileNotFoundException e)
		{
			ServoyLog.logError(e);
		}
		catch (CoreException e)
		{
			ServoyLog.logError(e);
		}

	}

	private List<String> existingComponents(String[] fileNames)
	{
		List<String> existing = new ArrayList<String>();
		List<String> incoming = Arrays.asList(fileNames);

		IProject project = getTargetProject();
		if (project.getFolder(folderName).exists())
		{
			try
			{
				IResource[] members = project.getFolder(folderName).members();
				for (IResource iResource : members)
				{
					if (incoming.contains(iResource.getName())) existing.add(iResource.getName());
				}
			}
			catch (CoreException e)
			{
				ServoyLog.logError(e);
			}
		}
		return existing;
	}

	/**
	 *
	 */
	private IFolder checkComponentsFolderCreated()
	{
		IProject project = getTargetProject();

		try
		{
			project.refreshLocal(IResource.DEPTH_ONE, new NullProgressMonitor());
		}
		catch (CoreException e)
		{
			ServoyLog.logError(e);
		}
		IFolder folder = project.getFolder(this.folderName);
		if (!folder.exists())
		{
			try
			{
				folder.create(true, true, new NullProgressMonitor());
			}
			catch (CoreException e)
			{
				ServoyLog.logError(e);
			}
		}
		return folder;
	}

	/**
	 * @return
	 */
	private IProject getTargetProject()
	{

		Object realObject = this.viewer.getSelectedTreeNode().getRealObject();
		if (realObject instanceof Solution)
		{
			Solution solution = (Solution)realObject;
			ServoyProject servoyProject = ServoyModelFinder.getServoyModel().getServoyProject(solution.getName());
			return servoyProject.getProject();
		}
		ServoyProject initialActiveProject = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject();
		ServoyResourcesProject resourcesProject = initialActiveProject.getResourcesProject();
		IProject project = resourcesProject.getProject();
		return project;
	}

	@Override
	public boolean isEnabled()
	{
		SimpleUserNode node = viewer.getSelectedTreeNode();
		if (node != null)
			return node.getType() == UserNodeType.SOLUTION_CONTAINED_AND_REFERENCED_WEB_PACKAGES;
		return false;
	}
}
