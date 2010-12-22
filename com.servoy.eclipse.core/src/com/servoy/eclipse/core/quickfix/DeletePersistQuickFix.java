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

import org.eclipse.core.resources.IMarker;
import org.eclipse.ui.IMarkerResolution;

import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.EclipseRepository;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRootObject;
import com.servoy.j2db.persistence.ISupportName;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.util.Pair;

public class DeletePersistQuickFix implements IMarkerResolution
{
	private final IPersist persist;
	private final ServoyProject project;

	public DeletePersistQuickFix(IPersist persist, ServoyProject project)
	{
		this.persist = persist;
		this.project = project;
	}

	public String getLabel()
	{
		String message = null;
		if (persist != null && persist instanceof ISupportName && ((ISupportName)persist).getName() != null)
		{
			message = "Delete persist '" + ((ISupportName)persist).getName() + "'.";
		}
		else if (persist != null)
		{
			Pair<String, String> pathPair = SolutionSerializer.getFilePath(persist, true);
			message = "Delete persist from file '" + pathPair.getRight() + "'.";
		}
		return message;
	}

	public void run(IMarker marker)
	{
		try
		{
			IRootObject rootObject = persist.getRootObject();
			if (rootObject instanceof Solution)
			{
				EclipseRepository repository = (EclipseRepository)rootObject.getRepository();
				IPersist editingNode = project.getEditingPersist(persist.getUUID());
				repository.deleteObject(editingNode);

				project.saveEditingSolutionNodes(new IPersist[] { editingNode }, true);
			}
		}
		catch (Exception ex)
		{
			ServoyLog.logError(ex);
		}

	}

}
