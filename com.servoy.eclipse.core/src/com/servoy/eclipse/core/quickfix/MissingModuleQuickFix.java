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
package com.servoy.eclipse.core.quickfix;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.ui.IMarkerResolution;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.util.Utils;

public class MissingModuleQuickFix implements IMarkerResolution
{
	private final String solutionName;
	private final String moduleName;

	public MissingModuleQuickFix(String solutionName, String moduleName)
	{
		super();
		this.solutionName = solutionName;
		this.moduleName = moduleName;
	}

	public String getLabel()
	{
		return "Remove '" + moduleName + "' as module of '" + solutionName + "'.";
	}

	public void run(IMarker marker)
	{
		ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(solutionName);
		if (servoyProject != null && servoyProject.getSolution() != null)
		{
			try
			{
				Solution solution = servoyProject.getEditingSolution();
				String[] modules = Utils.getTokenElements(servoyProject.getSolution().getModulesNames(), ",", true);
				List<String> modulesList = new ArrayList<String>(Arrays.asList(modules));
				if (modulesList.contains(moduleName))
				{
					modulesList.remove(moduleName);
					String modulesTokenized = Utils.getTokenValue(modulesList.toArray(new String[] {}), ",");
					solution.setModulesNames(modulesTokenized);
					servoyProject.saveEditingSolutionNodes(new IPersist[] { solution }, false);
				}
			}
			catch (Exception e)
			{
				ServoyLog.logError(e);
			}
		}
	}
}