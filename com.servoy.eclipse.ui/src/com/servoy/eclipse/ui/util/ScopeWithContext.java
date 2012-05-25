/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

package com.servoy.eclipse.ui.util;

import java.util.Comparator;

import com.servoy.j2db.persistence.IRootObject;
import com.servoy.j2db.persistence.ScriptVariable;

/**
 * @author hhardut
 *
 */
public class ScopeWithContext
{
	private final String name;
	private final IRootObject rootObject;

	public static final Comparator<ScopeWithContext> SCOPE_COMPARATOR = new Comparator<ScopeWithContext>()
	{
		public int compare(ScopeWithContext sc1, ScopeWithContext sc2)
		{

			String sc1Name = sc1.getName();
			String sc2Name = sc2.getName();
			if (sc1Name.equalsIgnoreCase(sc2Name)) return sc1.getRootObject().getName().compareTo(sc2.getRootObject().getName());
			if (sc1Name.toLowerCase().equals(ScriptVariable.GLOBAL_SCOPE)) return -1;
			if (sc2Name.toLowerCase().equals(ScriptVariable.GLOBAL_SCOPE)) return 1;
			return sc1Name.compareToIgnoreCase(sc2Name);
		}
	};

	public ScopeWithContext(String name, IRootObject rootObject)
	{
		this.name = name;
		this.rootObject = rootObject;
	}

	public String getName()
	{
		return this.name;
	}

	public IRootObject getRootObject()
	{
		return rootObject;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((rootObject == null) ? 0 : rootObject.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		ScopeWithContext other = (ScopeWithContext)obj;
		if (name == null)
		{
			if (other.name != null) return false;
		}
		else if (!name.equals(other.name)) return false;
		if (rootObject == null)
		{
			if (other.rootObject != null) return false;
		}
		else if (!rootObject.equals(other.rootObject)) return false;
		return true;
	}

	@Override
	public String toString()
	{
		// Will be printed when this is used as model in viewers
		return ScriptVariable.SCOPES_DOT_PREFIX + name;
	}

}
