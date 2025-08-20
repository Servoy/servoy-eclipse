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

import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.TableNode;
import com.servoy.j2db.util.SafeArrayList;
import com.servoy.j2db.util.Utils;

/**
 * Holder for method id and actual arguments
 *
 * @author rgansevles
 *
 */
public class MethodWithArguments
{
	public static final MethodWithArguments METHOD_NONE = new MethodWithArguments("-1", null);
	public static final MethodWithArguments METHOD_DEFAULT = new MethodWithArguments("0", null);

	public final String methodUUID;
	public final SafeArrayList<Object> arguments;
	public final SafeArrayList<String> paramNames;
	public final ITable table;

	public MethodWithArguments(String methodUUID, ITable table)
	{
		this(methodUUID, null, null, table);
	}

	public MethodWithArguments(String methodUUID, SafeArrayList<String> paramNames, SafeArrayList<Object> arguments, ITable table)
	{
		this.methodUUID = methodUUID;
		this.arguments = arguments;
		this.paramNames = paramNames;
		this.table = methodUUID != null ? table : null;
	}

	public static MethodWithArguments create(IPersist script, SafeArrayList<Object> arguments)
	{
		return create(script, new SafeArrayList<String>(), arguments);
	}

	public static MethodWithArguments create(IPersist script, SafeArrayList<String> paramNames, SafeArrayList<Object> arguments)
	{
		if (script == null)
		{
			return null;
		}
		ITable table = null;
		if (script.getParent() instanceof TableNode)
		{
			table = ServoyModelFinder.getServoyModel().getDataSourceManager().getDataSource(((TableNode)script.getParent()).getDataSource());
		}
		return new MethodWithArguments(script.getUUID().toString(), paramNames, arguments, table);
	}

	@Override
	public int hashCode()
	{
		// arguments and/or table doesn't matter for equal/hashcode
		return methodUUID != null ? methodUUID.hashCode() : 0;
	}

	@Override
	public boolean equals(Object obj)
	{
		// arguments and/or table doesn't matter for equal/hashcode
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		return Utils.equalObjects(methodUUID, ((MethodWithArguments)obj).methodUUID);
	}

	public static class UnresolvedMethodWithArguments extends MethodWithArguments
	{
		public final String unresolvedValue;

		public UnresolvedMethodWithArguments(String unresolvedValue)
		{
			super("-1", null);
			this.unresolvedValue = unresolvedValue;
		}

		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + ((unresolvedValue == null) ? 0 : unresolvedValue.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj) return true;
			if (!super.equals(obj)) return false;
			if (getClass() != obj.getClass()) return false;
			final UnresolvedMethodWithArguments other = (UnresolvedMethodWithArguments)obj;
			if (unresolvedValue == null)
			{
				if (other.unresolvedValue != null) return false;
			}
			else if (!unresolvedValue.equals(other.unresolvedValue)) return false;
			return true;
		}

	}

}
