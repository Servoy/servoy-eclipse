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
package com.servoy.eclipse.ui.quickfix;

import org.eclipse.core.resources.IMarker;
import org.eclipse.ui.IMarkerResolution;

import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.ServoyProject;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;

/**
 * Quickfix for solution settings that should only be used if then solution is declares as webclient-only.
 * @author rob
 *
 */
public class MarkSolutionAsWebclientOnlyQuickFix implements IMarkerResolution
{
	private final String solutionName;

	public MarkSolutionAsWebclientOnlyQuickFix(String solutionName)
	{
		this.solutionName = solutionName;
	}

	public String getLabel()
	{
		return "Mark solution as webclient-only.";
	}

	public void run(IMarker marker)
	{
		if (solutionName != null)
		{
			ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(solutionName);
			if (servoyProject != null)
			{
				Solution solution = servoyProject.getEditingSolution();
				if (solution != null)
				{
					solution.setSolutionType(SolutionMetaData.WEB_CLIENT_ONLY);
					try
					{
						servoyProject.saveEditingSolutionNodes(new IPersist[] { solution }, false);
					}
					catch (RepositoryException e)
					{
						ServoyLog.logError(e);
					}
				}
			}
		}
	}
}