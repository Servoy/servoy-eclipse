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
package com.servoy.eclipse.core.builder;

import java.beans.PropertyDescriptor;

import org.eclipse.core.resources.IMarker;
import org.eclipse.ui.IMarkerResolution;

import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.ServoyProject;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.util.UUID;

public class ClearEventMethod implements IMarkerResolution
{
	private final String uuid;
	private final String solutionName;
	private final String eventName;

	public ClearEventMethod(String uuid, String solName, String eventName)
	{
		this.uuid = uuid;
		this.solutionName = solName;
		this.eventName = eventName;
	}

	public String getLabel()
	{
		return "Clear invalid event method (set to none).";
	}

	public void run(IMarker marker)
	{
		if (uuid != null)
		{
			UUID id = UUID.fromString(uuid);
			ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(solutionName);
			if (servoyProject != null)
			{
				try
				{
					IPersist persist = servoyProject.getEditingPersist(id);
					if (persist != null)
					{
						PropertyDescriptor descriptor = new PropertyDescriptor(eventName, persist.getClass());
						descriptor.getWriteMethod().invoke(persist, -1);
						servoyProject.saveEditingSolutionNodes(new IPersist[] { persist }, true);
					}
				}
				catch (Exception e)
				{
					ServoyLog.logError(e);
				}
			}
		}

	}
}