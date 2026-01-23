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

package com.servoy.eclipse.model.util;

import java.util.Iterator;
import java.util.Map;

import org.sablo.specification.PropertyDescription;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.WebComponent;
import com.servoy.j2db.persistence.WebObjectImpl;
import com.servoy.j2db.util.PersistIdentifier;
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
		return searchForPersist(ModelUtils.getEditingFlattenedSolution(form), persistIdentifier);
	}

	/**
	 * It will find the persist from the editing solution based on the given persistIdentifier.<br/><br/>
	 *
	 * This method is mainly meant to form designer and outline view usage - as it will translate persists from inside form component components
	 * into WebFormComponentChildType instances. It can be tweaked in the future using an extra arg (or a different method) if one really needs
	 * the underlying/normal persist - that WebFormComponentChildType should be able to provide via {@link WebFormComponentChildType#getElement()} anyway.
	 *
	 * @param persistIdentifier .persistUUIDAndFCPropAndComponentPath something like ["D9884DBA_C5E7_4395_A934_52030EB8F1F0", "containedForm", "button_1"]; it could also be an array with just one item if we do a simple search for a persist; .customTypeOrComponentTypePropertyUUIDInsidePersist can be null or an UUID
	 */
	public IPersist searchForPersist(FlattenedSolution editingFlattenedSolution, PersistIdentifier persistIdentifier)
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

		IPersist searchPersist = editingFlattenedSolution.searchPersist(uuidAsString);
		if (persistIdentifier.persistUUIDAndFCPropAndComponentPath().length > 1)
		{
			searchPersist = new WebFormComponentChildType((WebComponent)searchPersist, persistIdentifier.persistUUIDAndFCPropAndComponentPath(),
				editingFlattenedSolution);
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

	/**
	 * This is kind of the reverse of {@link PersistFinder#searchForPersist(FlattenedSolution, PersistIdentifier)}.<br/><br/>
	 *
	 * Use this method instead of {@link PersistIdentifier#fromPersist(IPersist)} if the persist you give is from the development environment, so it can be a WebFormComponentChildType.<br/><br/>
	 *
	 * If the persist is not inside a form component and it's not a custom type, it will just use the UUID.<br/>
	 * If it is inside a form component then it will use the correct identification path.<br/>
	 * If it is a custom type (like a column of a table) then it will also give the correct PersistIdemtifier for it (UUID or path + UUID of custom type)...
	 */
	public PersistIdentifier fromPersist(IPersist persist)
	{
		if (persist instanceof WebFormComponentChildType wfcct) return new PersistIdentifier(wfcct.getFcPropAndCompPath(), null);
		else return PersistIdentifier.fromPersist(persist);
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
