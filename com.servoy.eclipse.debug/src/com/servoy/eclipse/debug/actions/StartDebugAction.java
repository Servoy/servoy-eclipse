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
package com.servoy.eclipse.debug.actions;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.dltk.debug.core.DLTKDebugPlugin;
import org.eclipse.dltk.debug.core.DLTKDebugPreferenceConstants;
import org.eclipse.dltk.debug.ui.DLTKDebugUIPlugin;
import org.eclipse.dltk.internal.debug.core.model.ScriptDebugTarget;
import org.eclipse.dltk.internal.launching.DLTKLaunchingPlugin;
import org.eclipse.dltk.javascript.core.JavaScriptNature;
import org.eclipse.dltk.javascript.launching.JavaScriptLaunchConfigurationConstants;
import org.eclipse.dltk.launching.ScriptLaunchConfigurationConstants;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.ServoyProject;
import com.servoy.eclipse.core.builder.ServoyBuilder;
import com.servoy.eclipse.core.repository.SolutionSerializer;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.preferences.StartupPreferences;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.debug.RemoteDebugScriptEngine;

/**
 * @author jcompagner
 * 
 */
public abstract class StartDebugAction implements IWorkbenchWindowActionDelegate
{
	private Shell shell;
	public static boolean LAUNCH;

	/**
	 * 
	 */
	public StartDebugAction()
	{
		super();
	}

	static long lastTimeStarted;

	/**
	 * 
	 */
	protected boolean testAndStartDebugger()
	{
		ServoyProject[] projects = ServoyModelManager.getServoyModelManager().getServoyModel().getModulesOfActiveProject();
		boolean hasErrors = false;
		// find fatal errors
		for (ServoyProject project : projects)
		{
			IMarker[] markers = null;
			try
			{
				markers = project.getProject().findMarkers(ServoyBuilder.MISSING_MODULES_MARKER_TYPE, false, IResource.DEPTH_INFINITE);
			}
			catch (CoreException e)
			{
			}
			if (markers != null && markers.length > 0)
			{
				displayError(markers[0].getAttribute(IMarker.MESSAGE, "Modules missing from solution."));
				return false;
			}
			try
			{
				markers = project.getProject().findMarkers(ServoyBuilder.MULTIPLE_RESOURCES_PROJECTS_MARKER_TYPE, false, IResource.DEPTH_INFINITE);
			}
			catch (CoreException e)
			{
			}
			if (markers != null && markers.length > 0)
			{
				displayError(markers[0].getAttribute(IMarker.MESSAGE, "Project references multiple resources projects."));
				return false;
			}
			try
			{
				markers = project.getProject().findMarkers(ServoyBuilder.NO_RESOURCES_PROJECTS_MARKER_TYPE, false, IResource.DEPTH_INFINITE);
			}
			catch (CoreException e)
			{
			}
			if (markers != null && markers.length > 0)
			{
				displayError(markers[0].getAttribute(IMarker.MESSAGE, "Project has no resource project attached."));
				return false;
			}
			try
			{
				markers = project.getProject().findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
				if (markers != null && markers.length > 0)
				{
					IEclipsePreferences eclipsePreferences = Activator.getDefault().getEclipsePreferences();
					boolean confirmErrors = eclipsePreferences.getBoolean(StartupPreferences.DEBUG_CLIENT_CONFIRMATION_WHEN_ERRORS,
						StartupPreferences.DEFAULT_ERROR_CONFIRMATION);
					boolean confirmWarnings = eclipsePreferences.getBoolean(StartupPreferences.DEBUG_CLIENT_CONFIRMATION_WHEN_WARNINGS,
						StartupPreferences.DEFAULT_WARNING_CONFIRMATION);
					for (IMarker marker : markers)
					{
						if (marker.getAttribute(IMarker.SEVERITY) != null && marker.getAttribute(IMarker.SEVERITY).equals(IMarker.SEVERITY_ERROR) &&
							confirmErrors)
						{
							hasErrors = true;
							break;
						}
						if (marker.getAttribute(IMarker.SEVERITY) != null && marker.getAttribute(IMarker.SEVERITY).equals(IMarker.SEVERITY_WARNING) &&
							confirmWarnings)
						{
							hasErrors = true;
							break;
						}
					}
				}
			}
			catch (CoreException e)
			{
			}
		}
		if (hasErrors)
		{
			Display.getDefault().syncExec(new Runnable()
			{
				public void run()
				{
					LAUNCH = MessageDialog.openConfirm(Display.getDefault().getActiveShell(), "Errors in project",
						"There are errors/warnings in project. Are you sure you want to launch?");
				}
			});
			if (!LAUNCH) return false;
		}
		IPreferenceStore store = DebugUIPlugin.getDefault().getPreferenceStore();
		final String save = store.getString(IInternalDebugUIConstants.PREF_SAVE_DIRTY_EDITORS_BEFORE_LAUNCH);
		if (!save.equals(MessageDialogWithToggle.NEVER))
		{
			EditorUtil.saveDirtyEditors(shell, save.equals(MessageDialogWithToggle.PROMPT));
		}
		store.setValue(IInternalDebugUIConstants.PREF_CONTINUE_WITH_COMPILE_ERROR, MessageDialogWithToggle.ALWAYS);
		DLTKDebugUIPlugin.getDefault();
		int port = RemoteDebugScriptEngine.startupDebugger();
		if (port == -1)
		{
			MessageDialog.openError(Display.getDefault().getActiveShell(), "Error starting debugger", "Please check your logs");
			return false;
		}
		DLTKDebugPlugin.getDefault().getPluginPreferences().setValue(DLTKDebugPreferenceConstants.PREF_DBGP_REMOTE_PORT, port);

		if (!RemoteDebugScriptEngine.isConnected() || !isScriptDebugTargetLaunched())
		{
			if (lastTimeStarted != 0 && (System.currentTimeMillis() - lastTimeStarted) < 2000)
			{
				return false;
			}
			lastTimeStarted = System.currentTimeMillis();
			ServoyProject activeProject = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject();
			if (activeProject != null && activeProject.getProject() != null)
			{
				IFile script = activeProject.getProject().getFile(SolutionSerializer.GLOBALS_FILE);
				ILaunchConfigurationType configType = DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurationType(
					JavaScriptLaunchConfigurationConstants.ID_JAVASCRIPT_SCRIPT);

				try
				{
					aboutToStartDebugClient();
					final ILaunchConfiguration config = findLaunchConfiguration(script, configType);
					if (config != null)
					{
						Display.getDefault().syncExec(new Runnable()
						{
							public void run()
							{
								DebugUITools.launch(config, "debug");
							}
						});
					}
					else
					{
						ServoyLog.logError("Couldn't start the debugger because there was nog launch config", null);
						return false;
					}
				}
				catch (Exception ex)
				{
					ServoyLog.logError(ex);
					return false;
				}
			}
		}
		return true;
	}

	private static boolean isScriptDebugTargetLaunched()
	{
		boolean isScriptDebugTargetLaunched = false;
		IDebugTarget[] launchedDebugTargats = DebugPlugin.getDefault().getLaunchManager().getDebugTargets();
		if (launchedDebugTargats != null)
		{
			for (IDebugTarget dt : launchedDebugTargats)
			{
				isScriptDebugTargetLaunched = dt instanceof ScriptDebugTarget;
				if (isScriptDebugTargetLaunched) break;
			}
		}

		return isScriptDebugTargetLaunched;
	}

	protected void aboutToStartDebugClient()
	{
		// by default do nothing, see StartSmartClientActionDelegate
	}


	private void displayError(final String message)
	{
		Display.getDefault().syncExec(new Runnable()
		{
			public void run()
			{
				MessageDialog.openError(Display.getDefault().getActiveShell(), "Cannot start client", message);
			}
		});
	}

	/**
	 * Locate a configuration to relaunch for the given type. If one cannot be found, create one.
	 * 
	 * @return a re-useable config or <code>null</code> if none
	 */
	protected ILaunchConfiguration findLaunchConfiguration(IResource script, ILaunchConfigurationType configType)
	{
		List<ILaunchConfiguration> candidateConfigs = Collections.emptyList();
		try
		{
			ILaunchConfiguration[] configs = DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurations(configType);
			candidateConfigs = new ArrayList<ILaunchConfiguration>(configs.length);
			for (ILaunchConfiguration config : configs)
			{
				if (config.getAttribute(ScriptLaunchConfigurationConstants.ATTR_MAIN_SCRIPT_NAME, "").equals(script.getProjectRelativePath().toString()) && //$NON-NLS-1$
					config.getAttribute(ScriptLaunchConfigurationConstants.ATTR_PROJECT_NAME, "").equals(script.getProject().getName())) { //$NON-NLS-1$
					candidateConfigs.add(config);
				}
			}
		}
		catch (CoreException e)
		{
			DLTKLaunchingPlugin.log(e);
		}

		// If there are no existing configs associated with the script, create
		// one.
		// If there is exactly one config associated with the script, return it.
		// Otherwise, if there is more than one config associated with the
		// script, prompt the
		// user to choose one.
		int candidateCount = candidateConfigs.size();
		if (candidateCount < 1)
		{
			return createConfiguration(script);
		}
		else if (candidateCount >= 1)
		{
			return candidateConfigs.get(0);
		}
		return null;
	}

	protected ILaunchConfiguration createConfiguration(IResource script)
	{
		ILaunchConfiguration config = null;
		ILaunchConfigurationWorkingCopy wc = null;
		try
		{
			ILaunchConfigurationType configType = getConfigurationType();
			wc = configType.newInstance(null, getLaunchManager().generateUniqueLaunchConfigurationNameFrom(script.getName()));
			wc.setAttribute(ScriptLaunchConfigurationConstants.ATTR_SCRIPT_NATURE, JavaScriptNature.NATURE_ID);
			wc.setAttribute(ScriptLaunchConfigurationConstants.ATTR_PROJECT_NAME, script.getProject().getName());
			wc.setAttribute(ScriptLaunchConfigurationConstants.ATTR_MAIN_SCRIPT_NAME, script.getProjectRelativePath().toPortableString()/*
																																		 * script.getFullPath().
																																		 * toPortableString ()
																																		 */);
			wc.setAttribute(IDebugUIConstants.ATTR_LAUNCH_IN_BACKGROUND, true);

			wc.setAttribute(ScriptLaunchConfigurationConstants.ATTR_DLTK_DBGP_WAITING_TIMEOUT, 100000);
			wc.setMappedResources(new IResource[] { script.getProject() });
			config = wc.doSave();
		}
		catch (CoreException exception)
		{
			exception.printStackTrace();
		}
		return config;
	}

	protected ILaunchConfigurationType getConfigurationType()
	{
		return getLaunchManager().getLaunchConfigurationType(JavaScriptLaunchConfigurationConstants.ID_JAVASCRIPT_SCRIPT);
	}

	protected ILaunchManager getLaunchManager()
	{
		return DebugPlugin.getDefault().getLaunchManager();
	}


	/**
	 * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#dispose()
	 */
	public void dispose()
	{
	}

	/**
	 * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#init(org.eclipse.ui.IWorkbenchWindow)
	 */
	public void init(IWorkbenchWindow window)
	{
		this.shell = window.getShell();
	}

	/**
	 * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction, org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection)
	{
	}


}