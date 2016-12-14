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
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.Utils;

/**
 * @author lvostinar
 *
 */
public class AddFormsToWorkingSet extends Action implements ISelectionChangedListener
{
	private final SolutionExplorerView viewer;

	public AddFormsToWorkingSet(SolutionExplorerView viewer)
	{
		this.viewer = viewer;
		setText("Add to working set");
		setToolTipText("Add form(s) to working set");
	}

	public void selectionChanged(SelectionChangedEvent event)
	{
		setEnabled(isActive());
	}

	@Override
	public boolean isEnabled()
	{
		return isActive();
	}

	@Override
	public void run()
	{
		String solutionName = getParentSolutionName();
		if (solutionName != null)
		{
			List<String> existingWorkingSets = getSolutionWorkingSets(solutionName);
			if (existingWorkingSets != null && existingWorkingSets.size() > 0)
			{
				int option = UIUtils.showOptionDialog(UIUtils.getActiveShell(), "Add form(s) to working set", "Select a working set to add selected forms",
					existingWorkingSets.toArray(new String[0]), -1);
				if (option >= 0)
				{
					String workingSet = existingWorkingSets.get(option);

					List<IFile> formsFile = new ArrayList<IFile>();
					for (SimpleUserNode node : viewer.getSelectedTreeNodes())
					{
						if (!(node.getRealObject() instanceof Form)) continue;
						Pair<String, String> formFilePath = SolutionSerializer.getFilePath((Form)node.getRealObject(), false);
						IFile file = ServoyModel.getWorkspace().getRoot().getFile(new Path(formFilePath.getLeft() + formFilePath.getRight()));
						formsFile.add(file);
						String oldWorkingSet = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveResourcesProject().getContainingWorkingSet(
							((Form)node.getRealObject()).getName(), ServoyModelFinder.getServoyModel().getFlattenedSolution().getSolutionNames());
						if (oldWorkingSet != null)
						{
							IWorkingSet ws = PlatformUI.getWorkbench().getWorkingSetManager().getWorkingSet(oldWorkingSet);
							if (ws != null)
							{
								List<IAdaptable> files = new ArrayList<IAdaptable>(Arrays.asList(ws.getElements()));
								if (files.remove(file))
								{
									ws.setElements(files.toArray(new IAdaptable[0]));
								}
							}
						}
					}
					IWorkingSet ws = PlatformUI.getWorkbench().getWorkingSetManager().getWorkingSet(workingSet);
					if (ws != null)
					{
						List<IAdaptable> files = new ArrayList<IAdaptable>(Arrays.asList(ws.getElements()));
						files.addAll(formsFile);
						ws.setElements(files.toArray(new IAdaptable[0]));
					}
				}
			}
		}
	}

	private boolean isActive()
	{
		List<SimpleUserNode> selectedNodes = viewer.getSelectedTreeNodes();
		if (selectedNodes == null || selectedNodes.size() == 0)
		{
			return false;
		}
		String solutionName = getParentSolutionName();
		if (solutionName == null) return false;

		List<String> existingWorkingSets = getSolutionWorkingSets(solutionName);

		if (existingWorkingSets == null || existingWorkingSets.size() == 0) return false;

		return true;
	}

	private String getParentSolutionName()
	{
		String solutionName = null;
		for (SimpleUserNode node : viewer.getSelectedTreeNodes())
		{
			if (!(node.getRealObject() instanceof Form)) return null;
			ServoyProject activeProject = ((ServoyProject)node.getAncestorOfType(ServoyProject.class).getRealObject());
			if (solutionName == null) solutionName = activeProject.getProject().getName();
			if (!Utils.equalObjects(solutionName, activeProject.getProject().getName())) return null;
		}
		return solutionName;
	}

	private List<String> getSolutionWorkingSets(String solutionName)
	{
		List<String> existingWorkingSets = null;
		if (ServoyModelManager.getServoyModelManager().getServoyModel().getActiveResourcesProject() != null)
		{
			existingWorkingSets = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveResourcesProject().getServoyWorkingSets(
				new String[] { solutionName });
		}
		return existingWorkingSets;
	}
}
