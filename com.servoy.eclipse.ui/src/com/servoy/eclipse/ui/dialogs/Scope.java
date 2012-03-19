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

package com.servoy.eclipse.ui.dialogs;

import com.servoy.j2db.persistence.IRootObject;

/**
 * @author hhardut
 *
 */
public class Scope
{
	private final String name;
	private final IRootObject rootObject;

	public Scope(String name, IRootObject rootObject)
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

	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof Scope)
		{
			Scope scope = (Scope)obj;
			return name.equals(scope.name) && rootObject == scope.rootObject;
		}
		return false;
	}
}
