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

import java.util.Iterator;
import java.util.Map;

import org.sablo.specification.PropertyDescription;

import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.WebFormComponentChildType;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.WebComponent;
import com.servoy.j2db.persistence.WebObjectImpl;
import com.servoy.j2db.server.ngclient.template.PersistIdentifier;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

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

	/**
	 * @param persistIdentifier .persistUUIDAndFCPropAndComponentPath something like ["D9884DBA_C5E7_4395_A934_52030EB8F1F0", "containedForm", "button_1"]; it could also be an array with just one item if we do a simple search for a persist; .customTypeOrComponentTypePropertyUUIDInsidePersist can be null or an UUID
	 */
	public IPersist searchForPersist(Form form, PersistIdentifier persistIdentifier)
	{
		if (persistIdentifier == null) return null;
		String uuidAsString = persistIdentifier.persistUUIDAndFCPropAndComponentPath()[0];

		if (persistIdentifier.persistUUIDAndFCPropAndComponentPath().length > 1)
		{
			// this is a selection of a component (or a custom type prop or component prop inside a component) that is inside a form component container
			int start = 0;

			// there is similar code in FormElement.getName(rawName); but this code should now no longer use a "name", but a PersistIdentifier that is not (directly) based on names...
			if (uuidAsString.startsWith("_")) start = 1; // TODO why is this needed with _ prefix and _ to - ? explain here
			uuidAsString = uuidAsString.substring(start).replace('_', '-');
		}

		IPersist searchPersist = ModelUtils.getEditingFlattenedSolution(form).searchPersist(uuidAsString);
		if (persistIdentifier.persistUUIDAndFCPropAndComponentPath().length > 1)
		{
			searchPersist = new WebFormComponentChildType((WebComponent)searchPersist, persistIdentifier.persistUUIDAndFCPropAndComponentPath(),
				ModelUtils.getEditingFlattenedSolution(form));
		}

		if (persistIdentifier.customTypeOrComponentTypePropertyUUIDInsidePersist() != null)
		{
			UUID childPropUUID = UUID.fromString(persistIdentifier.customTypeOrComponentTypePropertyUUIDInsidePersist()); // this is I think meant for custom types (maybe component child types) inside components that are inside form component containers
			PropertyDescription pd = null;

			if (searchPersist instanceof WebFormComponentChildType fcChild) pd = fcChild.getPropertyDescription();
			if (searchPersist instanceof WebComponent wc)
				pd = (wc.getImplementation() instanceof WebObjectImpl wcImpl ? wcImpl.getPropertyDescription() : null);

			if (pd != null)
			{
				Map<String, PropertyDescription> customMap = pd.getCustomJSONProperties();
				for (PropertyDescription customProperty : customMap.values())
				{
					Object customValue = ((AbstractBase)searchPersist).getProperty(customProperty.getName());
					if (customValue instanceof IPersist)
					{
						if (((IPersist)customValue).getUUID().equals(childPropUUID))
						{
							return (IPersist)customValue;
						}
					}
					else if (customValue instanceof IPersist[])
					{
						for (IPersist child : (IPersist[])customValue)
						{
							if (child.getUUID().equals(childPropUUID))
							{
								return child;
							}
						}
					}
				}
			} // else should never happen
		}

		return searchPersist;
	}


	public boolean checkName(Form form, String compName)
	{
		Iterator<IFormElement> fields = ModelUtils.getEditingFlattenedSolution(form).getFlattenedForm(form).getFlattenedObjects(
			null).iterator();
		for (IFormElement element : Utils.iterate(fields))
		{
			if (compName.equals(element.getName()))
			{
				return false;
			}
		}
		return true;
	}


}
