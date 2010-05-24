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
package com.servoy.eclipse.core.nature;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.ServoyProject;
import com.servoy.eclipse.core.ServoyResourcesProject;

public class ToggleServoyResourcesNatureAction extends ToggleNatureAction
{

	public ToggleServoyResourcesNatureAction()
	{
		super(ServoyResourcesProject.NATURE_ID, null);
	}

	@Override
	protected void toggleNature(IProject project)
	{
		try
		{
			if (project.hasNature(ServoyProject.NATURE_ID) && !project.hasNature(ServoyResourcesProject.NATURE_ID))
			{
				MessageDialog.openError(Display.getDefault().getActiveShell(), "Cannot add resources nature",
					"Project already has solution nature, it has to be removed before adding resources nature (cannot have both natures).");
				return;
			}
			super.toggleNature(project);
		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
		}
	}
}
