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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;

import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.dialogs.ModuleListSelectionDialog;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;


/**
 * @author gganea@servoy.com
 *
 */
public abstract class AddAsSolutionReference extends Action implements ISelectionChangedListener
{
	protected final List<String> selectedProjects = new ArrayList<String>();
	protected Shell shell;
	protected final UserNodeType targetNodeType;

	public AddAsSolutionReference(Shell shell, UserNodeType nodeType)
	{
		this.shell = shell;
		targetNodeType = nodeType;
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event)
	{
		IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
		selectedProjects.clear();
		IStructuredSelection sel = (IStructuredSelection)event.getSelection();
		boolean state = (sel.size() > 0) && (servoyModel.getActiveProject() != null);
		if (state)
		{
			Iterator<SimpleUserNode> selit = sel.iterator();
			while (state && selit.hasNext())
			{
				SimpleUserNode node = selit.next();
				state = (node.getRealType() == targetNodeType);
				if (state) selectedProjects.add(getSelectedNodeName(node));
			}
			if (!state)
			{
				selectedProjects.clear();
			}
		}
		setEnabled(state);
	}

	protected abstract String getSelectedNodeName(SimpleUserNode node);

	protected ServoyProject askUserForActiveModuleToUse()
	{
		IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
		ServoyProject[] activeModules = servoyModel.getModulesOfActiveProject();
		if (activeModules.length == 1)
		{
			return activeModules[0];
		}
		else if (activeModules.length > 1)
		{
			ServoyProject activeProject = servoyModel.getActiveProject();

			ModuleListSelectionDialog nameDialog = new ModuleListSelectionDialog(shell, "Choose parent solution");
			nameDialog.setInitialSelections(activeProject.getSolution().getName());
			int res = nameDialog.open();
			if (res == Window.OK)
			{
				String moduleSelectd = nameDialog.getFirstResult().toString();
				for (final ServoyProject project : activeModules)
				{
					if (moduleSelectd == project.getEditingSolution().getName())
					{
						return project;
					}
				}
			}
			return null;
		}
		else
		{
			ServoyLog.logError("Trying to add modules while there is no active solution!", null);
			return null;
		}
	}

	protected abstract String solutionChooseDialogMessage();
}
