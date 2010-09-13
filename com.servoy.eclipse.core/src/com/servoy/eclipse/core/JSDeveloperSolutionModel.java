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

package com.servoy.eclipse.core;

import java.util.List;

import org.eclipse.core.resources.ResourcesPlugin;

import com.servoy.eclipse.core.repository.SolutionSerializer;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.util.Debug;

/**
 * @author jcompagner
 *
 */
public class JSDeveloperSolutionModel
{
	public void js_save()
	{
		final IFileAccess wfa = new WorkspaceFileAccess(ResourcesPlugin.getWorkspace());
		Solution solutionCopy = Activator.getDefault().getDebugJ2DBClient().getFlattenedSolution().getSolutionCopy();
		try
		{
			List<IPersist> allObjectsAsList = solutionCopy.getAllObjectsAsList();
			for (IPersist persist : allObjectsAsList)
			{
				SolutionSerializer.writePersist(persist, wfa, ServoyModel.getDeveloperRepository(), true, false, true);
			}
		}
		catch (RepositoryException e)
		{
			Debug.error(e);
		}
	}
}
