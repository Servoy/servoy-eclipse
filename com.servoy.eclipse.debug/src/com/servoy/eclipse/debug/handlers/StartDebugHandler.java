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
package com.servoy.eclipse.debug.handlers;


import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.dltk.debug.ui.DLTKDebugUIPlugin;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.debug.DebugStarter;
import com.servoy.eclipse.debug.actions.IDebuggerStartListener;
import com.servoy.eclipse.model.builder.ServoyBuilder;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.preferences.StartupPreferences;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.eclipse.ui.util.EditorUtil.SaveDirtyEditorsOutputEnum;

/**
 * @author jcompagner
 *
 */
public abstract class StartDebugHandler extends AbstractHandler implements IHandler
{
	public static boolean LAUNCH;

	/**
	 *
	 */
	public StartDebugHandler()
	{
		super();
	}

	static long lastTimeStarted;

	protected void makeSureNeededPluginsAreStarted()
	{
		//make sure the plugins are loaded
		DLTKDebugUIPlugin.getDefault();
		DebugPlugin.getDefault();
	}

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
				ServoyLog.logError(e);
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
				ServoyLog.logError(e);
			}
			if (markers != null && markers.length > 0)
			{
				displayError(markers[0].getAttribute(IMarker.MESSAGE, "Project references multiple resources projects."));
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
						if (confirmErrors && marker.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO) == IMarker.SEVERITY_ERROR)
						{
							hasErrors = true;
							break;
						}
						if (confirmWarnings && marker.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO) == IMarker.SEVERITY_WARNING)
						{
							hasErrors = true;
							break;
						}
					}
				}
			}
			catch (CoreException e)
			{
				ServoyLog.logError(e);
			}
		}
		if (hasErrors)
		{
			Display.getDefault().syncExec(new Runnable()
			{
				public void run()
				{
					LAUNCH = MessageDialog.openConfirm(UIUtils.getActiveShell(), "Errors in project",
						"There are errors/warnings in project. Are you sure you want to launch?");
				}
			});
			if (!LAUNCH) return false;
		}
		IPreferenceStore store = DebugUIPlugin.getDefault().getPreferenceStore();
		final String save = store.getString(IInternalDebugUIConstants.PREF_SAVE_DIRTY_EDITORS_BEFORE_LAUNCH);
		if (!save.equals(MessageDialogWithToggle.NEVER))
		{
			IWorkbenchWindow[] workbenchWindows = PlatformUI.getWorkbench().getWorkbenchWindows();
			if ((workbenchWindows.length > 0) &&
				SaveDirtyEditorsOutputEnum.CANCELED == EditorUtil.saveDirtyEditors(workbenchWindows[0].getShell(), save.equals(MessageDialogWithToggle.PROMPT)))
			{
				// there where dirty editors and the user canceled it.
				return false;
			}
		}
		store.setValue(IInternalDebugUIConstants.PREF_CONTINUE_WITH_COMPILE_ERROR, MessageDialogWithToggle.ALWAYS);
		return DebugStarter.startDebugger(getDebuggerAboutToStartListener());
	}

	protected IDebuggerStartListener getDebuggerAboutToStartListener()
	{
		return null;
	}

	private void displayError(final String message)
	{
		Display.getDefault().syncExec(new Runnable()
		{
			public void run()
			{
				MessageDialog.openError(UIUtils.getActiveShell(), "Cannot start client", message);
			}
		});
	}
}