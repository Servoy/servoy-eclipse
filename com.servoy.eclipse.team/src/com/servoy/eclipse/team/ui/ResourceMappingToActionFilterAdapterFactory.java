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
package com.servoy.eclipse.team.ui;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.ui.IActionFilter;

import com.servoy.eclipse.team.Activator;

public class ResourceMappingToActionFilterAdapterFactory implements IAdapterFactory
{
	private final IActionFilter stpActionFilter = new IActionFilter()
	{
		public boolean testAttribute(Object target, String name, String value)
		{
			return true;
		}
	};

	public Object getAdapter(Object adaptableObject, Class adapterType)
	{
		ResourceMapping resourceMapping = (ResourceMapping)adaptableObject;
		boolean isSTPProject = true;

		for (IProject resourceProject : resourceMapping.getProjects())
			isSTPProject = isSTPProject && (RepositoryProvider.getProvider(resourceProject, Activator.NATURE_ID) != null);

		return isSTPProject ? stpActionFilter : null;
	}

	public Class[] getAdapterList()
	{
		return new Class[] { IActionFilter.class };
	}
}
