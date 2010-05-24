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

import org.eclipse.core.resources.IMarker;
import org.eclipse.ui.IMarkerResolution;

import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.ServoyProject;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.util.UUID;

public class ClearMissingStyleQuickFix implements IMarkerResolution
{
	private final String uuid;
	private final String solutionName;
	private final boolean clearStyle;

	public ClearMissingStyleQuickFix(String uuid, String solName, boolean clearStyle)
	{
		this.uuid = uuid;
		this.solutionName = solName;
		this.clearStyle = clearStyle;
	}

	public String getLabel()
	{
		if (clearStyle)
		{
			return "Clear invalid style in form.";
		}
		else
		{
			return "Clear invalid style class.";
		}
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
						if (clearStyle)
						{
							if (persist instanceof Form)
							{
								((Form)persist).setStyleName(null);
							}
						}
						else
						{
							if (persist instanceof Form)
							{
								((Form)persist).setStyleClass(null);
							}
							else if (persist instanceof BaseComponent)
							{
								((BaseComponent)persist).setStyleClass(null);
							}
						}
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