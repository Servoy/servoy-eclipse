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

import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.j2db.persistence.IScriptProvider;

/**
 * Property converter that converts the referred script id to the actual selected method id according to form inheritance.
 *
 * @author rgansevles
 *
 */
public class FormInheritenceMethodConverter implements IPropertyConverter<MethodWithArguments, MethodWithArguments>
{
	private final PersistContext persistContext;

	public FormInheritenceMethodConverter(PersistContext persistContext)
	{
		this.persistContext = persistContext;
	}

	public MethodWithArguments convertProperty(Object id, MethodWithArguments value)
	{
		if (value != null)
		{
			IScriptProvider scriptMethod = ModelUtils.getScriptMethod(persistContext.getPersist(), persistContext.getContext(), value.table, value.methodUUID);
			if (scriptMethod != null)
			{
				return MethodWithArguments.create(scriptMethod, value.paramNames, value.arguments);
			}
		}
		return value;
	}

	public MethodWithArguments convertValue(Object id, MethodWithArguments value)
	{
		return value;
	}

}
