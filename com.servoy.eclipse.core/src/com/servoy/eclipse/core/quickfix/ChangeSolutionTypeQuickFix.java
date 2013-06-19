/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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

import org.eclipse.core.resources.IMarker;
import org.eclipse.ui.IMarkerResolution;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;

/**
 * @author lvostinar
 *
 */
public class ChangeSolutionTypeQuickFix implements IMarkerResolution
{
	private final String solutionName;
	private final int solutionType;

	public ChangeSolutionTypeQuickFix(String solutionName, int solutionType)
	{
		super();
		this.solutionName = solutionName;
		this.solutionType = solutionType;
	}

	public String getLabel()
	{
		String solutionTypeString = null;
		for (int i = 0; i < SolutionMetaData.solutionTypes.length; i++)
		{
			if (solutionType == SolutionMetaData.solutionTypes[i])
			{
				solutionTypeString = SolutionMetaData.solutionTypeNames[i];
				break;
			}
		}
		return "Change solution type of '" + solutionName + "' to '" + solutionTypeString + "'.";
	}

	public void run(IMarker marker)
	{
		ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(solutionName);
		if (servoyProject != null && servoyProject.getSolution() != null)
		{
			try
			{
				Solution solution = servoyProject.getEditingSolution();
				solution.setSolutionType(solutionType);
				servoyProject.saveEditingSolutionNodes(new IPersist[] { solution }, false);
			}
			catch (Exception e)
			{
				ServoyLog.logError(e);
			}
		}
	}
}
