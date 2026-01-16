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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.widgets.Shell;

import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.RootObjectReference;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.util.Utils;

/**
 * Action for removing the selected module from one of the active modules that contains it.
 *
 * @author acostescu
 */
public class RemoveModuleAction extends Action implements ISelectionChangedListener
{

	private ServoyProject selectedProject = null;
	private final Shell shell;

	/**
	 * Creates a new remove module action.
	 *
	 * @param shell shell that might be used to display a dialog to the user (too choose a parent solution).
	 */
	public RemoveModuleAction(Shell shell)
	{
		this.shell = shell;
		setText("Remove module...");
		setToolTipText("Remove module from one of the active modules that contains it (the solution itself will not be deleted)");
		setImageDescriptor(Activator.loadImageDescriptorFromBundle("remove_module.png"));
	}

	@Override
	public void run()
	{
		if (selectedProject != null)
		{
			ServoyProject parentProject = askUserForParentProject();
			if (parentProject == null) return;
			Solution editingSolution = parentProject.getEditingSolution();
			if (editingSolution != null)
			{
				String[] modules = Utils.getTokenElements(editingSolution.getModulesNames(), ",", true);
				List<String> modulesList = new ArrayList<String>(Arrays.asList(modules));
				modulesList.remove(selectedProject.getProject().getName());
				String modulesTokenized = Utils.getTokenValue(modulesList.toArray(new String[] { }), ",");
				editingSolution.setModulesNames(modulesTokenized);
				try
				{
					parentProject.saveEditingSolutionNodes(new IPersist[] { editingSolution }, false);
				}
				catch (RepositoryException e)
				{
					ServoyLog.logError("Cannot save new module list for solution " + parentProject.getProject().getName(), e);
				}
			}
		}
	}

	private ServoyProject askUserForParentProject()
	{
		IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
		ServoyProject[] activeModules = servoyModel.getModulesOfActiveProject();
		ArrayList<ServoyProject> activeParentModules = new ArrayList<ServoyProject>();
		// see which ones are parents of the selected module
		for (ServoyProject p : activeModules)
		{
			if (p != selectedProject)
			{
				Solution s = p.getSolution();
				if (s != null)
				{
					try
					{
						List<RootObjectReference> modulesOfSolution = s.getReferencedModules(null);
						for (RootObjectReference r : modulesOfSolution)
						{
							if (selectedProject.getProject().getName().equals(r.getName()))
							{
								activeParentModules.add(p);
								break;
							}
						}
					}
					catch (RepositoryException e)
					{
						ServoyLog.logError(e);
					}
				}
			}
		}

		if (activeParentModules.size() == 1)
		{
			return activeParentModules.get(0);
		}
		else if (activeParentModules.size() > 1)
		{
			ServoyProject activeProject = servoyModel.getActiveProject();
			int defaultIndex = 0;
			String options[] = new String[activeParentModules.size()];
			for (int i = activeParentModules.size() - 1; i >= 0; i--)
			{
				if (activeParentModules.get(i) == activeProject)
				{
					defaultIndex = i;
				}
				options[i] = activeParentModules.get(i).getProject().getName();
			}
			int selectedProject = UIUtils.showOptionDialog(shell, "Choose parent solution",
				"You may remove the selected solution (as module) from any of the following parent solutions (all of them are modules of the active solution that declare the selected solution as a module).\nPlease choose one of these solutions.",
				options, defaultIndex);

			if (selectedProject >= 0)
			{
				// the user selected a project
				return activeParentModules.get(selectedProject);
			}
			else
			{
				// the user canceled the dialog
				return null;
			}
		}
		else
		{
			ServoyLog.logError("Trying to remove modules while there is no active modules that contain them!", null);
			return null;
		}
	}

	public void selectionChanged(SelectionChangedEvent event)
	{
		IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();

		selectedProject = null;
		IStructuredSelection sel = (IStructuredSelection)event.getSelection();
		boolean state = (sel.size() == 1) && (servoyModel.getActiveProject() != null);
		if (state)
		{
			SimpleUserNode node = (SimpleUserNode)sel.getFirstElement();
			state = (node.getType() == UserNodeType.SOLUTION_ITEM) && (node.getRealObject() != servoyModel.getActiveProject());
			if (state) selectedProject = (ServoyProject)node.getRealObject();
		}
		setEnabled(state);
	}
}