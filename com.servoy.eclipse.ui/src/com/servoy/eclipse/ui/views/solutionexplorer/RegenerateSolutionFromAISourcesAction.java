/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2025 Servoy BV

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

package com.servoy.eclipse.ui.views.solutionexplorer;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;

import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.svygen.AISolutionGenerator;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.DeletePersistAction;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.DeleteScopeAction;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.Solution;

/**
 * @author acostescu
 */
public class RegenerateSolutionFromAISourcesAction extends Action
{

	public RegenerateSolutionFromAISourcesAction()
	{
		setImageDescriptor(Activator.loadImageDescriptorFromBundle("refresh.png"));
		setText("(re)Generate solution from AI");
		setToolTipText("Deletes all solution content & generates it again based on source json from AI.");
	}

	@Override
	public void run()
	{
		ServoyProject activeProject = ServoyModelFinder.getServoyModel().getActiveProject();
		if (activeProject != null)
		{
			// delete old unneeded content
			Solution solution = activeProject.getEditingSolution();
			List<IPersist> allPersistsToDelete = new ArrayList<>();
			solution.getAllObjects().forEachRemaining((p) -> {
				if (!(p instanceof Media))
				{
					allPersistsToDelete.add(p);
				}
			});

			DeletePersistAction.performDeletionStatic(allPersistsToDelete);
			solution.getScopeNames().forEach((sn) -> DeleteScopeAction.deleteScript(activeProject, sn));

			// now (re)generate new content
			AISolutionGenerator.generateSolutionFromAIContent(activeProject);
		}


	}

}
