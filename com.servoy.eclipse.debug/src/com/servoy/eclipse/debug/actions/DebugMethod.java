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
package com.servoy.eclipse.debug.actions;

import java.io.File;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.dltk.core.IMethod;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;

import com.servoy.eclipse.core.Activator;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.SolutionDeserializer;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.WorkspaceFileAccess;
import com.servoy.j2db.IDebugClientHandler;
import com.servoy.j2db.persistence.AbstractRepository;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.util.UUID;

public class DebugMethod implements IViewActionDelegate
{
	private IMethod sm;

	/**
	 * @see org.eclipse.ui.IViewActionDelegate#init(org.eclipse.ui.IViewPart)
	 */
	public void init(IViewPart view)
	{
	}

	/**
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	public void run(IAction action)
	{
		IDebugClientHandler debugClientHandler = Activator.getDefault().getDebugClientHandler();
		if (sm != null && debugClientHandler.getDebugReadyClient() != null)
		{
			if (debugClientHandler.getDebugWebClient() != null)
			{
				//make sure the web client gets the latest changes
				debugClientHandler.getDebugWebClient().checkForChanges();
			}
			IPath path = sm.getPath();
			String[] segments = path.segments();
			String solutionName = segments[0];
			ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(solutionName);
			if (servoyProject != null && servoyProject.getProject().isOpen())
			{
				Solution sol = servoyProject.getSolution();
				File projectFile = new WorkspaceFileAccess(ResourcesPlugin.getWorkspace()).getProjectFile(sol.getName());
				File parentFile = SolutionSerializer.getParentFile(projectFile, new File(projectFile.getParentFile(), path.toString()));
				UUID uuid = SolutionDeserializer.getUUID(parentFile);

				IPersist parent = AbstractRepository.searchPersist(sol, uuid);
				if (parent instanceof ISupportChilds)
				{
					String scopeName = null;
					if (parent instanceof Solution)
					{
						String basename = segments[segments.length - 1];
						scopeName = basename.substring(0, basename.length() - 3); // remove .js
					}
					debugClientHandler.executeMethod((ISupportChilds)parent, scopeName, sm.getElementName());
				}
			}
		}
		else if (debugClientHandler.getDebugReadyClient() == null)
		{
			MessageDialog.openError(UIUtils.getActiveShell(), "Debug Method Problem", "Cannot debug method; please start a debug client first."); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	/**
	 * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction, org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection)
	{
		sm = null;
		boolean enable = false;
		if (selection instanceof IStructuredSelection)
		{
			Object o = ((IStructuredSelection)selection).getFirstElement();
			if (o instanceof IMethod)
			{
				sm = (IMethod)o;
				enable = true;
			}
			else
			{
				enable = false;
			}
		}
		action.setEnabled(enable);
	}
}