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

import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportName;

public class FormDisplayConverter extends DisplayConverter
{
	private final PersistContext context;

	public FormDisplayConverter(PersistContext context)
	{
		this.context = context;
	}

	@Override
	public Object displayToCanonicalValue(Object displayValue)
	{
		Form frm = ModelUtils.getEditingFlattenedSolution(context.getPersist(), context.getContext())
			.getForm(displayValue != null ? displayValue.toString() : null);
		return (frm == null) ? null : frm.getUUID().toString();
	}

	@Override
	public Object canonicalToDisplayValue(Object canonicalValue)
	{
		if (canonicalValue instanceof String)
		{
			IPersist persist = ModelUtils.getEditingFlattenedSolution(context.getPersist(), context.getContext())
				.searchPersist((String)canonicalValue);
			if (persist instanceof ISupportName)
			{
				return ((ISupportName)persist).getName();
			}
		}
		return null;
	}
}