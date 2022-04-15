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

package com.servoy.eclipse.ui.dialogs.autowizard;

import java.util.ArrayList;
import java.util.List;

import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.ui.dialogs.RelationContentProvider;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ITable;

/**
 * @author emera
 */
public class RelatedFormsContentProvider extends RelationContentProvider
{
	public RelatedFormsContentProvider(FlattenedSolution flattenedSolution, IPersist context)
	{
		super(flattenedSolution, context);
	}

	@Override
	public Object[] getChildrenUncached(Object parentElement)
	{
		Object[] children = super.getChildrenUncached(parentElement);
		if (parentElement instanceof RelationsWrapper)
		{
			RelationsWrapper wrapper = (RelationsWrapper)parentElement;
			ITable foreignTable = ServoyModelFinder.getServoyModel().getDataSourceManager().getDataSource(
				wrapper.relations[wrapper.relations.length - 1].getForeignDataSource());
			List<IPersist> forms = new ArrayList<>();
			flattenedSolution.getForms(foreignTable.getDataSource(), true).forEachRemaining(forms::add);
			Object[] result = new Object[children.length + forms.size()];
			System.arraycopy(forms.toArray(), 0, result, 0, forms.size());
			System.arraycopy(children, 0, result, result.length - 1, children.length);
			return result;
		}
		return children;
	}

	@Override
	public boolean hasChildren(Object element)
	{
		if (element instanceof RelationsWrapper)
		{
			RelationsWrapper wrapper = (RelationsWrapper)element;
			ITable foreignTable = ServoyModelFinder.getServoyModel().getDataSourceManager().getDataSource(
				wrapper.relations[wrapper.relations.length - 1].getForeignDataSource());
			return super.hasChildren(element) || foreignTable != null && flattenedSolution.getForms(foreignTable.getDataSource(), true).hasNext();
		}
		return false;
	}
}
