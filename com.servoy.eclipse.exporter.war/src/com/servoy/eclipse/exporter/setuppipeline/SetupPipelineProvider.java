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

package com.servoy.eclipse.exporter.setuppipeline;

import org.eclipse.jface.action.IAction;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.OpenWizardAction;
import com.servoy.eclipse.ui.wizards.IExportSolutionWizardProvider;
import com.servoy.eclipse.warexporter.Activator;
import com.servoy.j2db.persistence.SolutionMetaData;

/**
 * @author Diana
 *
 */
public class SetupPipelineProvider implements IExportSolutionWizardProvider
{

	public IAction getExportAction()
	{
		ServoyProject activeProject = ServoyModelFinder.getServoyModel().getActiveProject();
		if (activeProject != null && (activeProject.getSolutionMetaData().getSolutionType() == SolutionMetaData.SOLUTION ||
			activeProject.getSolutionMetaData().getSolutionType() == SolutionMetaData.SMART_CLIENT_ONLY ||
			activeProject.getSolutionMetaData().getSolutionType() == SolutionMetaData.WEB_CLIENT_ONLY ||
			activeProject.getSolutionMetaData().getSolutionType() == SolutionMetaData.NG_CLIENT_ONLY ||
			activeProject.getSolutionMetaData().getSolutionType() == SolutionMetaData.SERVICE))
		{
			return new OpenWizardAction(SetupPipelineWizard.class, AbstractUIPlugin.imageDescriptorFromPlugin(Activator.PLUGIN_ID, "$nl$/icons/war_export.png"),
				"Setup Pipeline");
		}
		return null;
	}

}
