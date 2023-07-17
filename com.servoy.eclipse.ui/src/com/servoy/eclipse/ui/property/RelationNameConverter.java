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
package com.servoy.eclipse.ui.property;

import com.servoy.eclipse.core.util.EclipseDatabaseUtils;
import com.servoy.eclipse.ui.dialogs.RelationContentProvider;
import com.servoy.eclipse.ui.dialogs.RelationContentProvider.RelationsWrapper;
import com.servoy.eclipse.ui.util.UnresolvedValue;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Relation;

/**
 * Convert relation to name and vv.
 *
 * @author rgansevles
 *
 */
public class RelationNameConverter implements IPropertyConverter<String, Object> // not <String, Relation> because we may get RelationContentProvider.NONE
{
	private final FlattenedSolution flattenedSolution;

	public RelationNameConverter(FlattenedSolution flattenedSolution)
	{
		this.flattenedSolution = flattenedSolution;
	}

	public Object convertProperty(Object id, String value)
	{
		if (value == null)
		{
			return RelationContentProvider.NONE;
		}
		Relation[] relations = flattenedSolution.getRelationSequence(value);
		if (relations == null)
		{
			return new UnresolvedValue(value);
		}
		return new RelationsWrapper(relations);
	}

	public String convertValue(Object id, Object value)
	{
		if (value instanceof RelationsWrapper)
		{
			return EclipseDatabaseUtils.getRelationsString(((RelationsWrapper)value).relations);
		}
		if (value instanceof String)
		{
			return (String)value;
		}
		return null;
	}
}
