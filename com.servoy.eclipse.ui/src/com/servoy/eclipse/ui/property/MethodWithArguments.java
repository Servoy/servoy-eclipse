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

import com.servoy.j2db.util.SafeArrayList;

/**
 * Holder for method id and actual arguments
 * 
 * @author rgansevles
 * 
 */
public class MethodWithArguments
{
	public final int methodId;
	public final SafeArrayList<Object> arguments;

	public MethodWithArguments(int methodId)
	{
		this(methodId, null);
	}

	public MethodWithArguments(int methodId, SafeArrayList<Object> arguments)
	{
		this.methodId = methodId;
		this.arguments = arguments;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + methodId;
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		// equal on method id only
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		final MethodWithArguments other = (MethodWithArguments)obj;
		if (methodId != other.methodId) return false;
		return true;
	}

	public static class UnresolvedMethodWithArguments extends MethodWithArguments
	{
		public final String unresolvedValue;

		public UnresolvedMethodWithArguments(String unresolvedValue)
		{
			super(-1, null);
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
