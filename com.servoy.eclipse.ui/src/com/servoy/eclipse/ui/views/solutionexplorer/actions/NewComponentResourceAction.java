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
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.widgets.Shell;
import org.json.JSONObject;
import org.sablo.specification.Package.IPackageReader;
import org.sablo.specification.WebObjectSpecification;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.nature.ServoyResourcesProject;
import com.servoy.eclipse.model.util.ResourcesUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.util.EditorUtil;

/**
 * Create new component resource file
 * @author emera
 */
public class NewComponentResourceAction extends Action implements ISelectionChangedListener
{

	private final Shell shell;
	private SimpleUserNode selectedNode;

	public NewComponentResourceAction(Shell shell)
	{
		setText("New file");
		setToolTipText("New file");
		this.shell = shell;
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event)
	{
		selectedNode = null;
		IStructuredSelection sel = (IStructuredSelection)event.getSelection();
		boolean state = (sel.size() == 1);
		if (state)
		{
			selectedNode = ((SimpleUserNode)sel.getFirstElement());
			UserNodeType type = selectedNode.getType();
			if (type == UserNodeType.WEB_OBJECT_FOLDER || type == UserNodeType.COMPONENTS_PROJECT_PACKAGE || type == UserNodeType.SERVICES_PROJECT_PACKAGE ||
				type == UserNodeType.LAYOUT_PROJECT_PACKAGE)
			{
				state = true;
			}
			else if (type == UserNodeType.COMPONENT || type == UserNodeType.SERVICE || type == UserNodeType.LAYOUT)
			{
				WebObjectSpecification spec = ((WebObjectSpecification)((SimpleUserNode)sel.getFirstElement()).getRealObject());
				state = "file".equals(spec.getSpecURL().getProtocol());
			}
		}
		setEnabled(state);
		if (!isEnabled()) selectedNode = null;
	}

	@Override
	public void run()
	{
		if (selectedNode != null)
		{
			String newFileName = UIUtils.showTextFieldDialog(shell, getText(), "Please provide a file name.");
			if (newFileName == null) return;

			ServoyProject initialActiveProject = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject();
			ServoyResourcesProject resourcesProject = initialActiveProject.getResourcesProject();
			IProject project = resourcesProject.getProject();

			IContainer folder = null;
			UserNodeType type = selectedNode.getType();
			if (type == UserNodeType.WEB_OBJECT_FOLDER)
			{
				folder = (IFolder)selectedNode.getRealObject();
			}
			else if (type == UserNodeType.COMPONENTS_PROJECT_PACKAGE || type == UserNodeType.SERVICES_PROJECT_PACKAGE ||
				type == UserNodeType.LAYOUT_PROJECT_PACKAGE)
			{
				folder = ServoyModel.getWorkspace().getRoot().getProject(((IPackageReader)selectedNode.getRealObject()).getPackageName());
			}
			else
			{
				try
				{
					IFile specFile = ResourcesUtils.findFileWithShortestPathForLocationURI(
						((WebObjectSpecification)selectedNode.getRealObject()).getSpecURL().toURI());
					if (specFile != null)
					{
						folder = specFile.getParent();
					}
					else
					{
						MessageDialog.openError(shell, getText(), "Could not create file. Spec file location incorrect.");
					}
				}
				catch (URISyntaxException e)
				{
					ServoyLog.logError(e);
				}
			}

			if (folder != null)
			{
				IFile file = folder.getFile(new Path(newFileName));
				if (!file.exists())
				{
					try
					{
						file.create(new ByteArrayInputStream(new byte[0]), true, new NullProgressMonitor());
						EditorUtil.openFileEditor(file);
						if (newFileName.indexOf("_server.js") > -1)
						{
							String specFileName = folder.getName() + ".spec";
							IFile specFile = folder.getFile(new Path(specFileName));
							try (InputStream is = specFile.getContents())
							{
								String text = IOUtils.toString(is, "UTF-8");
								JSONObject obj = new JSONObject(text);
								obj.put("serverscript", file.getFullPath().toString());
								specFile.setContents(new ByteArrayInputStream(obj.toString().getBytes()), IResource.NONE, new NullProgressMonitor());
							}
							catch (IOException e)
							{
								MessageDialog.openError(shell, getText(), "Could not update the .spec file.");
							}


						}
					}
					catch (CoreException e)
					{
						MessageDialog.openError(shell, getText(), "Could not create file.");
						ServoyLog.logError(e);
					}
				}
				else
				{
					MessageDialog.openError(shell, getText(), "The file " + newFileName + " already exists.");
				}
			}
			else
			{
				MessageDialog.openError(shell, getText(), "Could not create file. Spec file location incorrect.");
			}
		}
	}

}
