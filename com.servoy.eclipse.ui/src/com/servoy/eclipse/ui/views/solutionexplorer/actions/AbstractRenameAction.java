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
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.sablo.specification.Package.IPackageReader;
import org.sablo.specification.WebObjectSpecification;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerTreeContentProvider;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;
import com.servoy.j2db.util.StableKeysJSONObject;

/**
 * Action to rename a web object (component, service or layout).
 *
 * @author emera
 */
public abstract class AbstractRenameAction extends Action
{
	private final SolutionExplorerView viewer;
	private final Shell shell;

	public AbstractRenameAction(SolutionExplorerView viewer, Shell shell, UserNodeType nodeType)
	{
		this.viewer = viewer;
		this.shell = shell;
		setText("Rename " + nodeType.toString().toLowerCase());
		setToolTipText("Rename " + nodeType.toString().toLowerCase());
	}

	@Override
	public void run()
	{
		final SimpleUserNode node = viewer.getSelectedTreeNode();
		final String type = node.getType().toString().toLowerCase();

		WebObjectSpecification spec = (WebObjectSpecification)node.getRealObject();
		IContainer[] dirResource;
		try
		{
			IProject resources = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject().getResourcesProject().getProject();
			dirResource = resources.getWorkspace().getRoot().findContainersForLocationURI(spec.getSpecURL().toURI());
			if (dirResource.length == 1 && dirResource[0].getParent().exists())
			{
				final IResource resource = dirResource[0].getParent();

				if (resource != null)
				{
					final String currentName = resource.getName();

					String componentName;

					do
					{
						componentName = UIUtils.showTextFieldDialog(shell, getText(),
							"Please provide a new unique name for the '" + node.getName() + "' " + type +
								".\n\nPlease note that this action does not update references.\nDisplay name will only be changed if it is the same as old name (display name can also be changed directly from the .spec file).\n",
							currentName);
						if (componentName == null) return;
					}
					while (!isNameValid(componentName, type + " name must start with a letter and must contain only alphanumeric characters"));

					final String newName = componentName.trim();

					Job job = new WorkspaceJob("renaming")
					{

						@Override
						public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException
						{
							String displayName;
							try
							{
								IFolder pack = (IFolder)resource;
								IContainer parent = pack.getParent();
								IFile specFile = pack.getFile(currentName + ".spec");
								InputStream is = specFile.getContents();
								StableKeysJSONObject specJSON = new StableKeysJSONObject(IOUtils.toString(is, "UTF-8"));
								String currentComponentName, newComponentName;
								if ("service".equals(type))
								{
									currentComponentName = currentName;
									newComponentName = newName;
								}
								else
								{
									currentComponentName = NewWebObjectAction.getDashedName(currentName);
									newComponentName = NewWebObjectAction.getDashedName(newName);
								}
								specJSON.put("name", specJSON.getString("name").replace("-" + currentComponentName, "-" + newComponentName));
								String definition = specJSON.getString("definition").replace("/" + currentName, "/" + newName);
								if (definition.startsWith(currentName))
								{
									// support for /XXX/compname/compname.xxx and compname/compname.xxx
									definition = newName + definition.substring(currentName.length());
								}
								specJSON.put("definition", definition);

								displayName = specJSON.getString("displayName");
								if (currentName.equals(displayName))
								{
									specJSON.put("displayName", newName);
									displayName = newName;
								}

								renameFiles(pack, currentName, newName);
								IFile newSpecFile = pack.getFile(newName + ".spec");
								newSpecFile.create(new ByteArrayInputStream(specJSON.toString(4).getBytes()), IResource.NONE, new NullProgressMonitor());
								is.close();

								specFile.delete(true, new NullProgressMonitor());
								resource.move(parent.getFolder(new Path(newName)).getFullPath(), IResource.FORCE, new NullProgressMonitor());
								parent.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());

								renameInManifest(newName, resource, parent);
							}
							catch (Exception e)
							{
								ServoyLog.logError(e);
								return Status.CANCEL_STATUS;
							}

							if (viewer != null)
							{
								viewer.getSolExNavigator().revealWhenAvailable(node.parent, new String[] { displayName }, true);
							}
							return Status.OK_STATUS;
						}
					};
					job.setUser(true);
					job.schedule();
				}
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
		}
	}

	/**
	 * @param pack
	 * @param currentName
	 * @param newName
	 * @throws CoreException
	 * @throws IOException
	 */
	protected abstract void renameFiles(IFolder pack, String currentName, String newName) throws CoreException, IOException;

	public void renameInManifest(String newName, IResource resource, IContainer parent) throws CoreException
	{
		OutputStream out = null;
		InputStream in = null;
		try
		{
			IFile m = parent.getFile(new Path("META-INF/MANIFEST.MF"));
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
		SimpleUserNode node = viewer.getSelectedTreeNode();
		IResource packageRoot = SolutionExplorerTreeContentProvider.getResource((IPackageReader)node.parent.getRealObject());
		return (packageRoot instanceof IContainer);
	}
}