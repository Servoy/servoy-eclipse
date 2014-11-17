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
import java.net.URISyntaxException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.widgets.Shell;
import org.sablo.specification.WebComponentSpecification;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.nature.ServoyResourcesProject;
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
	private WebComponentSpecification spec;

	public NewComponentResourceAction(Shell shell)
	{
		setText("New file");
		setToolTipText("New file");
		this.shell = shell;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged(org.eclipse.jface.viewers.SelectionChangedEvent)
	 */
	@Override
	public void selectionChanged(SelectionChangedEvent event)
	{
		spec = null;
		IStructuredSelection sel = (IStructuredSelection)event.getSelection();
		boolean state = (sel.size() == 1);
		if (state)
		{
			UserNodeType type = ((SimpleUserNode)sel.getFirstElement()).getType();
			state = (type == UserNodeType.COMPONENT) || (type == UserNodeType.SERVICE);
			if (state)
			{
				spec = ((WebComponentSpecification)((SimpleUserNode)sel.getFirstElement()).getRealObject());
				state = "file".equals(spec.getSpecURL().getProtocol());
			}
		}
		setEnabled(state);
	}

	@Override
	public void run()
	{
		if (spec != null)
		{
			String newFileName = UIUtils.showTextFieldDialog(shell, getText(), "Please provide a file name.");
			if (newFileName == null) return;

			ServoyProject initialActiveProject = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject();
			ServoyResourcesProject resourcesProject = initialActiveProject.getResourcesProject();
			IProject project = resourcesProject.getProject();

			try
			{
				IFile[] specFile = project.getWorkspace().getRoot().findFilesForLocationURI(spec.getSpecURL().toURI());
				if (specFile.length == 1)
				{
					IFolder folder = (IFolder)specFile[0].getParent();
					IFile file = folder.getFile(newFileName);
					if (!file.exists())
					{
						try
						{
							file.create(new ByteArrayInputStream(new byte[0]), true, new NullProgressMonitor());
							EditorUtil.openComponentFileEditor(file);
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
			catch (URISyntaxException e)
			{
				ServoyLog.logError(e);
			}
		}
	}
}
