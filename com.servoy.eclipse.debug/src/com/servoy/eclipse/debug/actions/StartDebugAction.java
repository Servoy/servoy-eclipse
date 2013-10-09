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


import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.debug.DebugStarter;
import com.servoy.eclipse.model.builder.ServoyBuilder;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.preferences.StartupPreferences;
import com.servoy.eclipse.ui.util.EditorUtil;

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
			if (EditorUtil.saveDirtyEditors(shell, save.equals(MessageDialogWithToggle.PROMPT)))
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
				MessageDialog.openError(Display.getDefault().getActiveShell(), "Cannot start client", message);
			}
		});
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