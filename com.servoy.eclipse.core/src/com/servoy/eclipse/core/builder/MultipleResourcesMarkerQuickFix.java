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
package com.servoy.eclipse.core.builder;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

/**
 * This class is able to quick-fix multiple resources problems markers by letting the user choose the one resources project to remain referenced to the Servoy
 * solution project.
 * 
 * @author acostescu
 */
public class MultipleResourcesMarkerQuickFix extends ChooseResourcesProjectQuickFix
{

	public MultipleResourcesMarkerQuickFix()
	{
		super(
			"Choose one of the referenced resources project.",
			"references more than one Servoy resources project. This in incorrect.\n\nPlease choose one of these projects as the resources project. References to the other resources projects will be removed.");
	}

	@Override
	protected IProject[] getProjectListToFilter(IProject servoyProject) throws CoreException
	{
		return servoyProject.getReferencedProjects();
	}

}