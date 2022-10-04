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

package com.servoy.eclipse.ui.property.types;

import org.eclipse.jface.viewers.IFilter;

import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.util.IDataSourceWrapper;
import com.servoy.eclipse.ui.dialogs.CombinedTreeContentProvider;
import com.servoy.eclipse.ui.dialogs.FormFoundsetEntryContentProvider;
import com.servoy.eclipse.ui.dialogs.RelationContentProvider.RelationsWrapper;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.ITable;

public class FormFoundsetSelectionFilter implements IFilter
{
	private final ITable foreignTableForRelation;

	public FormFoundsetSelectionFilter(ITable foreignTableForRelation)
	{
		this.foreignTableForRelation = foreignTableForRelation;
	}

	public boolean select(Object toTest)
	{
		if (toTest == CombinedTreeContentProvider.NONE || toTest == FormFoundsetEntryContentProvider.FORM_FOUNDSET)
		{
			return true;
		}
		if (toTest instanceof RelationsWrapper)
		{
			return ((RelationsWrapper)toTest).relations != null && ((RelationsWrapper)toTest).relations.length > 0 && foreignTableForRelation == null ||
				foreignTableForRelation.equals(ServoyModelFinder.getServoyModel().getDataSourceManager().getDataSource(
					((RelationsWrapper)toTest).relations[((RelationsWrapper)toTest).relations.length - 1].getForeignDataSource()));
		}
		else if (toTest instanceof IDataSourceWrapper)
		{
			return ((IDataSourceWrapper)toTest).getTableName() != null && ((IDataSourceWrapper)toTest).getTableName().length() > 0;
		}
		else if (toTest instanceof Form)
		{
			return true;
		}
		return false;
	}
}