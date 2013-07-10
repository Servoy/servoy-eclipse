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

package com.servoy.eclipse.exporter.mobile.action;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.exporter.mobile.launch.IMobileLaunchConstants;
import com.servoy.eclipse.jsunit.launch.ITestLaunchConfigurationProvider;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.test.TestTarget;

/**
 * Provides launch configurations for mobile solutions.
 * @author acostescu
 */
public class MobileTestConfigurationProvider implements ITestLaunchConfigurationProvider
{

	@Override
	public ILaunchConfiguration findOrCreateLaunchConfiguration(TestTarget target, String launchMode, String launchConfigurationType, ILaunch oldLaunch)
		throws CoreException
	{
		ILaunchConfiguration found = null;

		ServoyModel model = ServoyModelManager.getServoyModelManager().getServoyModel();
		String solutionName = ((oldLaunch != null) ? oldLaunch.getLaunchConfiguration().getAttribute(IMobileLaunchConstants.SOLUTION_NAME, "") : null);
		ServoyProject sp = model.getActiveProject();
		if (solutionName == null || solutionName.equals(sp.getProject().getName()))
		{
			boolean nodebug = ((oldLaunch != null)
				? Boolean.valueOf(oldLaunch.getLaunchConfiguration().getAttribute(IMobileLaunchConstants.NODEBUG, "true")).booleanValue() : true);
			found = nodebug ? new RunMobileTestsHandler().findLaunchConfiguration(target, sp) : new DebugMobileTestsHandler().findLaunchConfiguration(target,
				sp);
		} // otherwise we can't run tests on non-active solution
		return found;
	}
}
