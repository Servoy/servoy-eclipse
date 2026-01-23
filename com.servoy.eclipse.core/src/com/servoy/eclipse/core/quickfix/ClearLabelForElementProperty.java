/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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
import com.servoy.eclipse.model.util.PersistFinder;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportName;
import com.servoy.j2db.util.PersistIdentifier;

/**
 * @author acostache
 *
 */
public class ClearLabelForElementProperty implements IMarkerResolution
{

	private final PersistIdentifier persistIdentifier;
	private final String solutionName;

	public ClearLabelForElementProperty(PersistIdentifier persistIdentifier, String solutionName)
	{
		this.persistIdentifier = persistIdentifier;
		this.solutionName = solutionName;
	}

	private IPersist getElementAsPersist()
	{
		IPersist element = null;
		if (persistIdentifier != null)
		{
			ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(solutionName);
			if (servoyProject != null)
			{
				try
				{
					element = PersistFinder.INSTANCE.searchForPersist(servoyProject.getEditingFlattenedSolution(), persistIdentifier);
				}
				catch (Exception e)
				{
					ServoyLog.logError(e);
				}
			}
		}
		return element;
	}

	public String getLabel()
	{
		String message = "Clear 'labelFor' property of element ";
		IPersist persist = getElementAsPersist();
		if (persist instanceof ISupportName)
		{
			String elemName = ((ISupportName)persist).getName();
			if (elemName != null && !elemName.equals("")) return message + "'" + elemName + "'";
		}
		return message + "with UUID: '" + persist.getUUID() + "'";
	}

	public void run(IMarker marker)
	{
		IPersist persist = getElementAsPersist();
		if (persist instanceof GraphicalComponent)
		{
			((GraphicalComponent)persist).setLabelFor(null);

			ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(solutionName);
			try
			{
				servoyProject.saveEditingSolutionNodes(new IPersist[] { persist }, true);
			}
			catch (Exception e)
			{
				ServoyLog.logError(e);
			}
		}
	}

}
