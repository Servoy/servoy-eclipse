/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2011 Servoy BV

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

package com.servoy.eclipse.exporter.apps.mobile;

import java.io.File;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.app.IApplicationContext;

import com.servoy.eclipse.exporter.apps.common.AbstractWorkspaceExporter;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.mobile.exporter.MobileExporter;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;

/**
 * Eclipse application that can be used for exporting servoy solutions in .servoy format (that can be used to import solutions afterwards in developer/app. server).
 *
 * @author acostescu
 */
public class MobileWorkspaceExporter extends AbstractWorkspaceExporter<MobileArgumentChest>
{

	@Override
	protected MobileArgumentChest createArgumentChest(IApplicationContext context)
	{
		return new MobileArgumentChest((String[])context.getArguments().get(IApplicationContext.APPLICATION_ARGS));
	}

	@Override
	protected void exportActiveSolution(MobileArgumentChest configuration)
	{
		ServoyProject activeProject = ServoyModelFinder.getServoyModel().getActiveProject();
		Solution solution = activeProject.getSolution();

		if (solution != null)
		{
			if (solution.getSolutionType() == SolutionMetaData.MOBILE)
			{
				MobileExporter exporter = new MobileExporter(null); // TODO should this be the CommandLineExporterModel ???
				exporter.setSolutionName(solution.getName());
				exporter.setOutputFolder(new File(configuration.getExportFilePath()));
				exporter.setServerURL(configuration.getServerURL());
				exporter.setTimeout(configuration.getSyncTimeout());
				exporter.setServiceSolutionName(configuration.getServiceSolutionName());
				if (configuration.shouldExportForTesting()) exporter.useTestWar(null, configuration.useLongTestMethodNames());
//			exporter.setSkipConnect(..allow user to specify license and check it..); // TODO a separate case was created for this: SVY-4807
				try
				{
					exporter.doExport(false, new NullProgressMonitor());
				}
				catch (Exception ex)
				{
					outputError("Error while exporting solution - '" + activeProject.getProject().getName() + "': " + ex.getMessage());
					exitCode = EXIT_EXPORT_FAILED;
				}
			}
			else
			{
				outputError("Solution '" + activeProject.getProject().getName() + "' is not a mobile solution. EXPORT FAILED for this solution.");
				exitCode = EXIT_EXPORT_FAILED;
			}
		}
		else
		{
			outputError("Solution in project '" + activeProject.getProject().getName() + "' is not valid. EXPORT FAILED for this solution.");
			exitCode = EXIT_EXPORT_FAILED;
		}
	}
}