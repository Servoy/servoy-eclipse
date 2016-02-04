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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.json.JSONObject;
import org.sablo.specification.WebObjectSpecification;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.views.solutionexplorer.PlatformSimpleUserNode;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;

/**
 * Action to rename a component.
 * @author emera
 */
public class RenameComponentAction extends Action
{
	private final SolutionExplorerView viewer;
	private final Shell shell;

	public RenameComponentAction(SolutionExplorerView viewer, Shell shell, UserNodeType nodeType)
	{
		this.viewer = viewer;
		this.shell = shell;
		setText("Rename " + nodeType.toString().toLowerCase());
		setToolTipText("Rename " + nodeType.toString().toLowerCase());
	}

	@Override
	public void run()
	{
		PlatformSimpleUserNode node = (PlatformSimpleUserNode)viewer.getSelectedTreeNode();
		String type = node.getType().toString().toLowerCase();

		String componentName = UIUtils.showTextFieldDialog(shell, getText(), "Provide the new name for the " + node.getName() + " " + type +
			".\n Please note that this action does not update references.");
		if (componentName == null) return;
		while (!isNameValid(componentName, type + " name must start with a letter and must contain only alphanumeric characters"))
		{
			componentName = UIUtils.showTextFieldDialog(shell, getText(), "Please provide the " + type.toLowerCase() + " name.");
			if (componentName == null) return;
		}
		componentName = componentName.trim();

		WebObjectSpecification spec = (WebObjectSpecification)node.getRealObject();
		IContainer[] dirResource;
		IResource resource = null;
		try
		{
			IProject resources = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject().getResourcesProject().getProject();
			dirResource = resources.getWorkspace().getRoot().findContainersForLocationURI(spec.getSpecURL().toURI());
			if (dirResource.length == 1 && dirResource[0].getParent().exists()) resource = dirResource[0].getParent();

			if (resource != null)
			{
				IFolder pack = (IFolder)resource;
				IFolder parent = (IFolder)pack.getParent();

				String currentName = resource.getName();
				IFile specFile = pack.getFile(currentName + ".spec");
				InputStream is = specFile.getContents();
				JSONObject specJSON = new JSONObject(IOUtils.toString(is, "UTF-8"));
				specJSON.put("name", specJSON.getString("name").replace("-" + currentName, "-" + componentName));
				specJSON.put("definition", specJSON.getString("definition").replace("/" + currentName, "/" + componentName));
				specJSON.put("displayName", componentName);
				IFile newSpecFile = pack.getFile(componentName + ".spec");
				newSpecFile.create(new ByteArrayInputStream(specJSON.toString(3).getBytes()), IResource.NONE, new NullProgressMonitor());
				is.close();

				IFile defFile = pack.getFile(currentName + ".js");
				is = defFile.getContents();
				String text = IOUtils.toString(is, "UTF-8");
				String moduleName = pack.getParent().getName() + componentName.substring(0, 1).toUpperCase() + componentName.substring(1);
				String oldName = pack.getParent().getName() + currentName.substring(0, 1).toUpperCase() + currentName.substring(1);
				text = text.replaceAll(oldName, moduleName);
				text = text.replaceAll(currentName + "/" + currentName + ".html", componentName + "/" + componentName + ".html");
				IFile newDefFile = pack.getFile(componentName + ".js");
				newDefFile.create(new ByteArrayInputStream(text.getBytes()), IResource.NONE, new NullProgressMonitor());
				is.close();

				IFile htmlFile = pack.getFile(currentName + ".html");
				if (htmlFile.exists())
				{
					IFile newHTML = pack.getFile(componentName + ".html");
					is = htmlFile.getContents();
					newHTML.create(is, IResource.NONE, new NullProgressMonitor());
					is.close();
					htmlFile.delete(true, new NullProgressMonitor());
				}
				defFile.delete(true, new NullProgressMonitor());
				specFile.delete(true, new NullProgressMonitor());
				resource.move(parent.getFolder(componentName).getFullPath(), IResource.FORCE, new NullProgressMonitor());
				parent.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());

				renameInManifest(componentName, resource, parent);
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
		}
	}

	public void renameInManifest(String newName, IResource resource, IFolder parent) throws CoreException
	{
		OutputStream out = null;
		InputStream in = null;
		try
		{
			IFile m = parent.getFile("META-INF/MANIFEST.MF");
			m.refreshLocal(IResource.DEPTH_ONE, new NullProgressMonitor());
			Manifest manifest = new Manifest(m.getContents());
			Attributes attributes = manifest.getEntries().remove(resource.getName() + "/" + resource.getName() + ".spec");
			manifest.getEntries().put(newName + "/" + newName + ".spec", attributes);
			out = new ByteArrayOutputStream();
			manifest.write(out);
			in = new ByteArrayInputStream(out.toString().getBytes());
			m.setContents(in, IResource.FORCE, new NullProgressMonitor());
		}
		catch (IOException e)
		{
			ServoyLog.logError(e);
		}
		finally
		{
			try
			{
				if (out != null) out.close();
				if (in != null) in.close();
			}
			catch (IOException e)
			{
				ServoyLog.logError(e);
			}
		}
	}

	private boolean isNameValid(String value, String message)
	{
		if (!value.matches("^[a-zA-Z][0-9a-zA-Z]*$"))
		{
			if (message != null)
			{
				MessageDialog.openError(shell, getText(), message);
			}
			return false;
		}
		return true;
	}


	@Override
	public boolean isEnabled()
	{
		PlatformSimpleUserNode node = (PlatformSimpleUserNode)viewer.getSelectedTreeNode();
		IResource packageRoot = (IResource)node.parent.getRealObject();
		return (packageRoot instanceof IFolder);
	}
}