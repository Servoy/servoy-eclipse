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
package com.servoy.eclipse.ui.views.solutionexplorer.actions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.widgets.Shell;

import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.util.Utils;

/**
 * Action for adding the selected solution(s) as (a) module(s) of one of the already active modules.
 *
 * @author acostescu
 */
public class AddAsModuleAction extends AddAsSolutionReference
{

	/**
	 * Creates a new add as module action.
	 *
	 * @param shell shell that might be used to display a dialog to the user (too choose a parent solution).
	 */
	public AddAsModuleAction(Shell shell)
	{
		super(shell, UserNodeType.SOLUTION_ITEM_NOT_ACTIVE_MODULE);
		setText("Add as module");
		setToolTipText("Add as a module to an already active module");
		setImageDescriptor(Activator.loadImageDescriptorFromBundle("add_as_module.png"));
	}

	@Override
	public void run()
	{
		if (selectedProjects.size() > 0)
		{
			// let the user choose the active module to add the selected projects to (as modules)
			ServoyProject activeModule = askUserForActiveModuleToUse();
			if (activeModule == null) return;
			Solution editingSolution = activeModule.getEditingSolution();
			if (editingSolution != null)
			{
				String[] modules = Utils.getTokenElements(editingSolution.getModulesNames(), ",", true);
				List<String> modulesList = new ArrayList<String>(Arrays.asList(modules));
				for (String module : selectedProjects)
				{
					if (!modulesList.contains(module))
					{
						modulesList.add(module);
					}
				}
				String modulesTokenized = Utils.getTokenValue(modulesList.toArray(new String[] { }), ",");
				editingSolution.setModulesNames(modulesTokenized);

				try
				{
					activeModule.saveEditingSolutionNodes(new IPersist[] { editingSolution }, false);
				}
				catch (RepositoryException e)
				{
					ServoyLog.logError("Cannot save new module list for active module " + activeModule.getProject().getName(), e);
				}
			}
		}
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event)
	{
		super.selectionChanged(event);
		if (selectedProjects.size() > 1)
		{
			setText("Add as modules");
			setToolTipText("Add as modules to an already active module");
		}
		else if (selectedProjects.size() == 1)
		{
			setText("Add as module");
			setToolTipText("Add as a module to an already active module");
		}
	}


	@Override
	protected String solutionChooseDialogMessage()
	{
		return "You may add modules to the active solution or to any of it's modules.\nPlease choose one of these solutions.";
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.eclipse.ui.views.solutionexplorer.actions.AddAsSolutionReference#getSelectedNodeName(com.servoy.eclipse.ui.node.SimpleUserNode)
	 */
	@Override
	protected String getSelectedNodeName(SimpleUserNode node)
	{
		return ((ServoyProject)node.getRealObject()).getProject().getName();
	}

}