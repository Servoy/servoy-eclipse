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
package com.servoy.eclipse.designer.property;

import com.servoy.j2db.persistence.IPersist;

public class PersistContext
{
	private final IPersist persist;
	private final IPersist context;

	public PersistContext(IPersist persist, IPersist context)
	{
		this.persist = persist;
		this.context = context;
	}

	public IPersist getPersist()
	{
		return persist;
	}

	public IPersist getContext()
	{
		return context;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((context == null) ? 0 : context.hashCode());
		result = prime * result + ((persist == null) ? 0 : persist.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		final PersistContext other = (PersistContext)obj;
		if (context == null)
		{
			if (other.context != null) return false;
		}
		else if (!context.equals(other.context)) return false;
		if (persist == null)
		{
			if (other.persist != null) return false;
		}
		else if (!persist.equals(other.persist)) return false;
		return true;
	}

	@Override
	public String toString()
	{
		return "PersistContext(" + (persist == null ? "NULL" : persist.toString()) + ", " + (context == null ? "NULL" : context.toString()) + ')';
	}

}
