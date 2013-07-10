package com.servoy.eclipse.jsunit.launch;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;

import com.servoy.eclipse.model.test.TestTarget;

/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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

/**
 * Provider for test launch configurations.
 * @author acostescu
 */
public interface ITestLaunchConfigurationProvider
{

	/**
	 * Finds or creates a launch configuration of the given type (id) that corresponds to the given TestTarget and launchMode.
	 * @param target the targeted tests.
	 * @param launchMode debug/run.
	 * @param launchConfigurationType the id of the required launch configuration type.
	 * @param launch if != null, this is the launch that determined for some reason this search. It might be useful in determining the correct configuration to find.
	 * @return the appropriate launch configuration.
	 */
	ILaunchConfiguration findOrCreateLaunchConfiguration(TestTarget target, String launchMode, String launchConfigurationType, ILaunch launch)
		throws CoreException;

}
