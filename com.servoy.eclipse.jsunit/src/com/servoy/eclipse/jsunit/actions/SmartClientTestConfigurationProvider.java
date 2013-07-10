package com.servoy.eclipse.jsunit.actions;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.jsunit.launch.ITestLaunchConfigurationProvider;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.test.TestTarget;


public class SmartClientTestConfigurationProvider implements ITestLaunchConfigurationProvider
{

	@Override
	public ILaunchConfiguration findOrCreateLaunchConfiguration(TestTarget target, String launchMode, String launchConfigurationType, ILaunch launch)
		throws CoreException
	{
		ILaunchConfiguration found = null;

		ServoyModel model = ServoyModelManager.getServoyModelManager().getServoyModel();
		String solutionName = target != null ? target.getActiveSolution().getName() : null;
		ServoyProject sp = model.getActiveProject();
		if (solutionName == null || solutionName.equals(sp.getProject().getName()))
		{
			found = new RunJSUnitHandler().findSmartClientTestLaunchConfiguration(target);
		} // otherwise we can't run tests on non-active solution
		return found;
	}

}
