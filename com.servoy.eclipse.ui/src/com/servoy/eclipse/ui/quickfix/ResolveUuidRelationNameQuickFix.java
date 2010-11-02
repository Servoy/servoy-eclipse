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

import org.eclipse.ui.views.properties.IPropertySource;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.ServoyProject;
import com.servoy.eclipse.ui.dialogs.RelationContentProvider.RelationsWrapper;
import com.servoy.eclipse.ui.util.UnresolvedValue;
import com.servoy.j2db.persistence.AbstractRepository;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;


/**
 * Quickfix to resolve relation names from uuid.
 * 
 * During import of old (3.5) solutions,the relation uuid is changed to relation name, when the relation cannot be found at that stage the uuid is not resolved.
 * This quickfix tries to resolve the relation again.
 * 
 * @author rgansevles
 *
 */
public class ResolveUuidRelationNameQuickFix extends BaseSetPropertyQuickFix
{
	public ResolveUuidRelationNameQuickFix(String solutionName, String uuid, String propertyName, String displayName)
	{
		super(solutionName, uuid, propertyName, displayName);
	}

	public String getLabel()
	{
		return "Resolve " + getDisplayName() + " from uuid.";
	}

	@Override
	protected void setPropertyValue(IPropertySource propertySource)
	{
		Object oldValue = propertySource.getPropertyValue(getPropertyName());
		if (!(oldValue instanceof UnresolvedValue))
		{
			return;
		}

		UUID relationUuid = Utils.getAsUUID(((UnresolvedValue)oldValue).getUnresolved(), false);
		if (relationUuid == null)
		{
			return;
		}

		// search in the active solution and its modules.
		ServoyProject[] modulesOfActiveProject = ServoyModelManager.getServoyModelManager().getServoyModel().getModulesOfActiveProject();
		for (ServoyProject project : modulesOfActiveProject)
		{
			IPersist searchPersist = AbstractRepository.searchPersist(project.getEditingSolution(), relationUuid);
			if (searchPersist instanceof Relation)
			{
				propertySource.setPropertyValue(getPropertyName(), new RelationsWrapper(new Relation[] { (Relation)searchPersist }));
				return;
			}
		}

		// TODO: show dialog that relation was not found?
	}
}
