/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

package com.servoy.eclipse.debug;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IDebugTarget;
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
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

import com.servoy.eclipse.core.IDebuggerStarter;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.debug.actions.IDebuggerStartListener;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.debug.RemoteDebugScriptEngine;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.util.Utils;

/**
 * @author jcompagner
 * @since 7.0
 */
public class DebugStarter implements IDebuggerStarter
{

	public void testAndStartDebugger()
	{
		startDebugger(null);
	}


	private static long lastTimeStarted = 0;

	public static boolean startDebugger(IDebuggerStartListener listener)
	{
		DLTKDebugUIPlugin.getDefault();
		int port = RemoteDebugScriptEngine.startupDebugger();
		if (port == -1)
		{
			Display.getDefault().asyncExec(new Runnable()
			{
				public void run()
				{
					MessageDialog.openError(UIUtils.getActiveShell(), "Error starting debugger", "Please check your logs");
				}
			});
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
				IFile script = getJavascriptFile(activeProject);
				if (script == null)
				{
					// shouldn't happen
					ServoyLog.logError("Couldn't start the debugger because there was no javascript file found", null);
					return false;
				}
				ILaunchConfigurationType configType = DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurationType(
					JavaScriptLaunchConfigurationConstants.ID_JAVASCRIPT_SCRIPT);

				try
				{
					if (listener != null) listener.aboutToStartDebugClient();
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
						ServoyLog.logError("Couldn't start the debugger because there was no launch config", null);
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

	/**
	 * Locate a configuration to relaunch for the given type. If one cannot be found, create one.
	 *
	 * @return a re-useable config or <code>null</code> if none
	 */
	private static ILaunchConfiguration findLaunchConfiguration(IResource script, ILaunchConfigurationType configType)
	{
		List<ILaunchConfiguration> candidateConfigs = Collections.emptyList();
		try
		{
			ILaunchConfiguration[] configs = DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurations(configType);
			candidateConfigs = new ArrayList<ILaunchConfiguration>(configs.length);
			for (ILaunchConfiguration config : configs)
			{
				if (config.getAttribute(ScriptLaunchConfigurationConstants.ATTR_MAIN_SCRIPT_NAME, "").equals(script.getProjectRelativePath().toString()) &&
					config.getAttribute(ScriptLaunchConfigurationConstants.ATTR_PROJECT_NAME, "").equals(script.getProject().getName()))
				{
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

	private static ILaunchConfiguration createConfiguration(IResource script)
	{
		ILaunchConfiguration config = null;
		ILaunchConfigurationWorkingCopy wc = null;
		try
		{
			ILaunchConfigurationType configType = getConfigurationType();
			wc = configType.newInstance(null, getLaunchManager().generateUniqueLaunchConfigurationNameFrom(script.getName()));
			wc.setAttribute(ScriptLaunchConfigurationConstants.ATTR_SCRIPT_NATURE, JavaScriptNature.NATURE_ID);
			wc.setAttribute(ScriptLaunchConfigurationConstants.ATTR_PROJECT_NAME, script.getProject().getName());
			wc.setAttribute(ScriptLaunchConfigurationConstants.ATTR_MAIN_SCRIPT_NAME,
				script.getProjectRelativePath().toPortableString()/*
																	 * script.getFullPath(). toPortableString ()
																	 */);
			wc.setAttribute(IDebugUIConstants.ATTR_LAUNCH_IN_BACKGROUND, true);

			wc.setAttribute(ScriptLaunchConfigurationConstants.ATTR_DLTK_DBGP_WAITING_TIMEOUT, 100000);
			wc.setMappedResources(new IResource[] { script.getProject() });
			config = wc.doSave();
		}
		catch (CoreException e)
		{
			ServoyLog.logError(e);
		}
		return config;
	}

	private static ILaunchConfigurationType getConfigurationType()
	{
		return getLaunchManager().getLaunchConfigurationType(JavaScriptLaunchConfigurationConstants.ID_JAVASCRIPT_SCRIPT);
	}

	private static ILaunchManager getLaunchManager()
	{
		return DebugPlugin.getDefault().getLaunchManager();
	}

	public static IFile getJavascriptFile(ServoyProject project)
	{
		IFile script = project.getProject().getFile(SolutionSerializer.GLOBALS_FILE);
		if (script.exists())
		{
			return script;
		}
		List<String> scopes = project.getGlobalScopenames();
		if (scopes.size() > 0)
		{
			return project.getProject().getFile(scopes.get(0) + SolutionSerializer.JS_FILE_EXTENSION);
		}
		Iterator<Form> it = project.getSolution().getForms(null, false);
		while (it.hasNext())
		{
			String scriptPath = SolutionSerializer.getScriptPath(it.next(), false);
			script = ServoyModel.getWorkspace().getRoot().getFile(new Path(scriptPath));
			if (script.exists())
			{
				return script;
			}
		}

		it = project.getEditingFlattenedSolution().getForms(false);
		while (it.hasNext())
		{
			String scriptPath = SolutionSerializer.getScriptPath(it.next(), false);
			script = ServoyModel.getWorkspace().getRoot().getFile(new Path(scriptPath));
			if (script.exists())
			{
				return script;
			}
			else if (!it.hasNext())
			{
				// create the form js
				try
				{
					script.create(Utils.getUTF8EncodedStream(""), true, null);
					return script;
				}
				catch (Exception ex)
				{
					ServoyLog.logError(ex);
				}
			}
		}
		return script;

	}
}
