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
package com.servoy.eclipse.ui.views.solutionexplorer;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.PlatformObject;

/**
 * A special model object used to represent shallow folders
 */
public class ShallowContainer extends PlatformObject
{

	private final IContainer container;

	public ShallowContainer(IContainer container)
	{
		this.container = container;
	}

	public IContainer getResource()
	{
		return container;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj == this) return true;
		if (obj instanceof ShallowContainer)
		{
			ShallowContainer other = (ShallowContainer)obj;
			return other.getResource().equals(getResource());
		}
		return false;
	}

	@Override
	public int hashCode()
	{
		return getResource().hashCode();
	}

	@Override
	public Object getAdapter(Class adapter)
	{
		if (adapter == IResource.class || adapter == IContainer.class) return container;
		return super.getAdapter(adapter);
	}

}
