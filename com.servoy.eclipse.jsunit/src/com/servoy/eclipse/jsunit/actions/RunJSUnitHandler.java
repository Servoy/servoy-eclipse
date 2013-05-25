package com.servoy.eclipse.jsunit.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.jsunit.SolutionUnitTestTarget;
import com.servoy.eclipse.jsunit.launch.JSUnitLaunchConfigurationDelegate;
import com.servoy.eclipse.jsunit.runner.TestTarget;
import com.servoy.eclipse.model.util.ServoyLog;

public class RunJSUnitHandler extends AbstractHandler
{

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException
	{
		try
		{
			ILaunchConfiguration configuration = findSmartClientTestLaunchConfiguration(getTestTarget(HandlerUtil.getActiveWorkbenchWindow(event).getActivePage().getSelection()));
			//this call will end up in launch method of ServoyJsLaunchConfigurationDelegate
			DebugUITools.launch(configuration, ILaunchManager.DEBUG_MODE);
		}
		catch (CoreException e)
		{
			ServoyLog.logError(e);
		}
		return null;
	}

	protected TestTarget getTestTarget(ISelection iSelection)
	{
		if (iSelection instanceof IStructuredSelection)
		{
			IStructuredSelection structuredSelection = (IStructuredSelection)iSelection;
			if (structuredSelection != null)
			{
				if (structuredSelection.size() == 1)
				{
					Object fe = Platform.getAdapterManager().getAdapter(structuredSelection.getFirstElement(), SolutionUnitTestTarget.class);
					if (fe != null)
					{
						return ((SolutionUnitTestTarget)fe).getTestTarget();
					}
				}
			}
		}
		return null;
	}

	/**
	 * Finds a suitable launch configuration for the current active solution. (for example mobile launchers are contributed
	 * via extension point)
	 */
	public ILaunchConfiguration findSmartClientTestLaunchConfiguration(TestTarget target) throws CoreException
	{
		if (target == null)
		{
			// use currently active solution
			target = new TestTarget(ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject().getSolution());
		}

		ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfigurationType type = manager.getLaunchConfigurationType(JSUnitLaunchConfigurationDelegate.LAUNCH_CONFIGURATION_TYPE_ID);
		ILaunchConfiguration[] configurations;
		configurations = manager.getLaunchConfigurations(type);
		int count = 0;
		for (ILaunchConfiguration configuration : configurations)
		{
			//was this target already launched before? if yes reuse the launch configuration
			if (target.convertToString().equals(
				configuration.getAttribute(JSUnitLaunchConfigurationDelegate.LAUNCH_CONFIG_INSTANCE, TestTarget.activeProjectTarget().convertToString())))
			{
				return configuration;
			}
			count++;
			if (count > JSUnitLaunchConfigurationDelegate.MAX_CONFIG_INSTANCES) configuration.delete();
		}

		//create a launch configuration copy 
		ILaunchConfigurationWorkingCopy workingCopy = type.newInstance(null, JSUnitLaunchConfigurationDelegate.generateLaunchConfigName(target));
		JSUnitLaunchConfigurationDelegate.prepareLaunchConfigForTesting(workingCopy);

		workingCopy.setAttribute(JSUnitLaunchConfigurationDelegate.LAUNCH_CONFIG_INSTANCE, target.convertToString());
		ILaunchConfiguration configuration = workingCopy.doSave();
		return configuration;
	}

}
