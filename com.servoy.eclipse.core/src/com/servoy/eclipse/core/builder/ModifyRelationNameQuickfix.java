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
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportDataProviderID;
import com.servoy.j2db.persistence.Portal;
import com.servoy.j2db.util.UUID;

public class ModifyRelationNameQuickfix implements IMarkerResolution
{
	private final String uuid;
	private final String solutionName;
	private final boolean changePortal;

	public ModifyRelationNameQuickfix(String uuid, String solName, boolean changePortal)
	{
		this.uuid = uuid;
		this.solutionName = solName;
		this.changePortal = changePortal;
	}

	public String getLabel()
	{
		if (!changePortal)
		{
			return "Apply portal relation name to element.";
		}
		else
		{
			return "Apply element dataprovider relation(s) to portal relationName.";
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
					if (persist.getParent() instanceof Portal)
					{
						Portal portal = (Portal)persist.getParent();
						if (persist instanceof ISupportDataProviderID)
						{
							String dataProviderId = ((ISupportDataProviderID)persist).getDataProviderID();
							int indx = dataProviderId.lastIndexOf('.');
							if (indx > 0)
							{
								String rel_name = dataProviderId.substring(0, indx);
								if (!portal.getRelationName().equals(rel_name))
								{
									if (changePortal)
									{
										portal.setRelationName(rel_name);
										servoyProject.saveEditingSolutionNodes(new IPersist[] { portal }, true);
									}
									else
									{
										dataProviderId = dataProviderId.replaceFirst(rel_name, portal.getRelationName());
										if (persist instanceof Field)
										{
											((Field)persist).setDataProviderID(dataProviderId);
										}
										else if (persist instanceof GraphicalComponent)
										{
											((GraphicalComponent)persist).setDataProviderID(dataProviderId);
										}
										servoyProject.saveEditingSolutionNodes(new IPersist[] { persist }, true);
									}
								}
							}
						}
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