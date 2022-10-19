/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2022 Servoy BV

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

package com.servoy.eclipse.ui.dialogs.autowizard.nattable;

import org.eclipse.nebula.widgets.nattable.data.convert.DisplayConverter;

import com.servoy.eclipse.ui.dialogs.RelationContentProvider;
import com.servoy.eclipse.ui.dialogs.RelationContentProvider.RelationsWrapper;
import com.servoy.eclipse.ui.util.UnresolvedValue;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Relation;

/**
 * @author emera
 */
public class RelationDisplayConverter extends DisplayConverter
{

	private final FlattenedSolution flattenedSolution;

	public RelationDisplayConverter(FlattenedSolution fs)
	{
		this.flattenedSolution = fs;
	}

	@Override
	public Object canonicalToDisplayValue(Object canonicalValue)
	{
		String relationName = null;
		if (canonicalValue instanceof RelationsWrapper)
		{
			RelationsWrapper relationsWrapper = (RelationsWrapper)canonicalValue;
			Relation relation = relationsWrapper.relations[relationsWrapper.relations.length - 1];
			relationName = relation.getName();
		}
		else if (canonicalValue instanceof String)
		{
			relationName = (String)canonicalValue;
		}
		return relationName;
	}

	@Override
	public Object displayToCanonicalValue(Object displayValue)
	{
		if (displayValue == null)
		{
			return RelationContentProvider.NONE;
		}
		Relation[] relations = flattenedSolution.getRelationSequence((String)displayValue);
		if (relations == null)
		{
			return new UnresolvedValue((String)displayValue);
		}
		return new RelationsWrapper(relations);
	}

}
