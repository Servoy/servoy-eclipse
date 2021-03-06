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
package com.servoy.eclipse.model.nature;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;

/**
 * Project nature for Servoy NG Package projects (that contain custom NG web components and services).
 *
 * @author acostescu
 */
public class ServoyNGPackageProject implements IProjectNature
{
	/**
	 * ID of this project nature
	 */
	public static final String NATURE_ID = "com.servoy.eclipse.core.ServoyNGPackage";

	private IProject project;

	public ServoyNGPackageProject()
	{
	}

	public ServoyNGPackageProject(IProject project)
	{
		this.project = project;
	}

	public void configure() throws CoreException
	{
	}

	public void deconfigure() throws CoreException
	{
	}

	public IProject getProject()
	{
		return project;
	}

	public void setProject(IProject project)
	{
		this.project = project;
	}

	@Override
	public String toString()
	{
		return (project != null ? project.getName() : null);
	}

}