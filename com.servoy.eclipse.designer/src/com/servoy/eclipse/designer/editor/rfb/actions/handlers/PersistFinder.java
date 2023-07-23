/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

package com.servoy.eclipse.designer.editor.rfb.actions.handlers;

import static com.servoy.eclipse.model.util.ModelUtils.getEditingFlattenedSolution;

import java.util.Map;

import org.sablo.specification.PropertyDescription;

import com.servoy.eclipse.model.util.WebFormComponentChildType;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.WebComponent;
import com.servoy.j2db.util.UUID;

/**
 * @author user
 *
 */
public class PersistFinder
{

	static
	{
		INSTANCE = new PersistFinder();
	}

	public static PersistFinder INSTANCE;

	private PersistFinder()
	{
	}

	public IPersist searchForPersist(Form form, String fullUUIDA)
	{
		if (fullUUIDA == null) return null;
		String fullUUID = fullUUIDA;
		String uuid = fullUUID, childUUID = null;
		int index = uuid.indexOf('$');
		if (index > 1)
		{
			// this is a form component selection
			String[] uuidParts = fullUUID.split("#");
			uuid = fullUUID = uuidParts[0];
			if (uuidParts.length > 1)
			{
				childUUID = uuidParts[1];
			}

			int start = 0;
			if (uuid.startsWith("_")) start = 1;
			uuid = uuid.substring(start, index).replace('_', '-');
		}
		IPersist searchPersist = getEditingFlattenedSolution(form).searchPersist(uuid);
		if (index > 1)
		{
			searchPersist = new WebFormComponentChildType((WebComponent)searchPersist, fullUUID.substring(index + 1).replace('$', '.'),
				getEditingFlattenedSolution(form));
			if (childUUID != null)
			{
				UUID childId = UUID.fromString(childUUID);
				PropertyDescription pd = ((WebFormComponentChildType)searchPersist).getPropertyDescription();
				Map<String, PropertyDescription> customMap = pd.getCustomJSONProperties();
				for (PropertyDescription customProperty : customMap.values())
				{
					Object customValue = ((WebFormComponentChildType)searchPersist).getProperty(customProperty.getName());
					if (customValue instanceof IPersist)
					{
						if (((IPersist)customValue).getUUID().equals(childUUID))
						{
							return (IPersist)customValue;
						}
					}
					else if (customValue instanceof IPersist[])
					{
						for (IPersist child : (IPersist[])customValue)
						{
							if (child.getUUID().equals(childId))
							{
								return child;
							}
						}
					}
				}
			}
		}

		return searchPersist;

	}

	public boolean checkName(Form form, String compName)
	{
		return getEditingFlattenedSolution(form).getFlattenedForm(form).getFlattenedObjects(
			null).stream().noneMatch(element -> compName.equals(element.getName()));

	}

}
