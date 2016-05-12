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

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.core.util.UIUtils.ScrollableDialog;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.nature.ServoyResourcesProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.util.Pair;

/**
 * @author gganea, gboros
 *
 */
public class ImportComponentAsZipAction extends ImportComponentAction
{
	private int overrideReturnCode;
	protected final String folderName;

	public ImportComponentAsZipAction(SolutionExplorerView viewer, String entity, String folder)
	{
		super(viewer, entity);
		this.folderName = folder;
	}

	/**
	 * @param fileNames
	 * @param filterPath
	 */
	@Override
	protected void doImport(String[] fileNames, String filterPath)
	{
		overrideReturnCode = IDialogConstants.YES_TO_ALL_ID;

		final List<String> existingComponents = existingComponents(fileNames);

		if (!existingComponents.isEmpty())
		{
			UIUtils.runInUI(new Runnable()
			{
				public void run()
				{
					ScrollableDialog dialog = new ScrollableDialog(UIUtils.getActiveShell(), IMessageProvider.ERROR, "Error",
						"The folowing " + entity + " files already exist: ", existingComponents.toString());
					List<Pair<Integer, String>> buttonsAndLabels = new ArrayList<Pair<Integer, String>>();
					buttonsAndLabels.add(new Pair<Integer, String>(IDialogConstants.YES_TO_ALL_ID, "Overwrite all"));
					buttonsAndLabels.add(new Pair<Integer, String>(IDialogConstants.CANCEL_ID, "Cancel"));
					dialog.setCustomBottomBarButtons(buttonsAndLabels);
					//shell.setSize(400, 500);
					overrideReturnCode = dialog.open();
				}
			}, true);
		}
		if (overrideReturnCode == IDialogConstants.YES_TO_ALL_ID) addComponents(filterPath, fileNames);
	}

	/**
	 * @param fileNames
	 */
	private void addComponents(String filterPath, String[] fileNames)
	{
		IFolder componentsFolder = checkComponentsFolderCreated();
		for (String fileName : fileNames)
		{
			File javaFile = new File(filterPath + File.separator + fileName);
			if (javaFile.exists() && javaFile.isFile())
			{
				importZipFileComponent(componentsFolder, javaFile);
			}
		}
	}

	protected File[] getImportFolderEntries(File importFolder)
	{
		return importFolder.listFiles();
	}

	/**
	 * @param javaFile
	 */
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
				e.printStackTrace();
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
		catch (CoreException e1)
		{
			e1.printStackTrace();
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
}
