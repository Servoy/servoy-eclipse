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

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.NewVariableAction;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ISupportDataProviderID;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.ScopesUtils;
import com.servoy.j2db.util.UUID;

/**
 * @author lvostinar
 *
 */
public class CreateVariableReferenceQuickFix implements IMarkerResolution
{
	private final String uuid;
	private final String solutionName;

	public CreateVariableReferenceQuickFix(String uuid, String solName)
	{
		this.uuid = uuid;
		this.solutionName = solName;
	}

	public String getLabel()
	{
		return "Create variable for invalid DataProviderID."; //$NON-NLS-1$
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
					IPersist parent = null;
					String variableName = null;
					if (persist instanceof ISupportDataProviderID)
					{
						variableName = ((ISupportDataProviderID)persist).getDataProviderID();
						Pair<String, String> scope = ScopesUtils.getVariableScope(variableName);
						String scopeName = scope.getLeft();
						if (scopeName != null)
						{
							variableName = scope.getRight();
							parent = persist.getAncestor(IRepository.SOLUTIONS);
						}
						else
						{
							parent = persist.getAncestor(IRepository.FORMS);
						}
						if (parent != null)
						{
							NewVariableAction.createNewVariable(UIUtils.getActiveShell(), parent, scopeName, variableName, IColumnTypes.TEXT, null);
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
