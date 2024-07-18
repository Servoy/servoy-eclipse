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


import java.lang.reflect.InvocationTargetException;
import java.util.List;

import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;

import com.servoy.base.util.ITagResolver;
import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.debug.Activator;
import com.servoy.eclipse.debug.Activator.ShortcutDefinition;
import com.servoy.eclipse.debug.actions.IDebuggerStartListener;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.IDebugJ2DBClient;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.Utils;

/**
 * @author jcompagner,jblok
 *
 */
public class StartSmartClientHandler extends StartDebugHandler implements IRunnableWithProgress, IDebuggerStartListener
{
	public static ITagResolver noReplacementResolver = new ITagResolver()
	{
		public String getStringValue(String name)
		{
			return "%%" + name + "%%";
		}
	};

	public static ITagResolver simpleReplacementResolver = new ITagResolver()
	{
		public String getStringValue(String name)
		{
			if ("prefix".equals(name)) return "forms.customer.";
			else if ("elementName".equals(name)) return ".elements.customer_id";
			else return "%%" + name + "%%";
		}
	};

	private List<ShortcutDefinition> shortCuts;

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException
	{
		makeSureNeededPluginsAreStarted();

		Job job = new Job("Smart client launch")
		{
			@Override
			protected IStatus run(IProgressMonitor monitor)
			{
				try
				{
					StartSmartClientHandler.this.run(monitor);
				}
				catch (InvocationTargetException e)
				{
					ServoyLog.logError(e);
				}
				catch (InterruptedException e)
				{
					ServoyLog.logError(e);
				}
				return Status.OK_STATUS;
			}
		};
		job.setUser(true);
		job.schedule();
		return null;
	}

	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
	{
		StartClientHandler.setLastCommand(StartClientHandler.START_SMART_CLIENT);
		monitor.beginTask("Smart client launch", 5);
		monitor.worked(1);
		try
		{
			IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
			ServoyProject activeProject = servoyModel.getActiveProject();
			if (activeProject != null && activeProject.getSolution() != null)
			{
				final Solution solution = activeProject.getSolution();
				if (solution.getSolutionType() == SolutionMetaData.WEB_CLIENT_ONLY)
				{
					Display.getDefault().asyncExec(new Runnable()
					{
						public void run()
						{
							MessageDialog.openError(UIUtils.getActiveShell(), "Solution type problem",
								"Cant open this solution type in this client");
						}
					});
					return;
				}
				monitor.worked(2);
				if (testAndStartDebugger())
				{
					IDebugJ2DBClient debugJ2DBClient = com.servoy.eclipse.core.Activator.getDefault().getDebugJ2DBClient();

					if (shortCuts == null)
					{
						// register shortcuts defined in extension point.
						shortCuts = Activator.getDefault().getDebugSmartclientShortcuts();
						JRootPane rootPane = debugJ2DBClient.getMainApplicationFrame().getRootPane();
						ActionMap am = rootPane.getActionMap();
						InputMap im = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
						for (ShortcutDefinition def : shortCuts)
						{
							KeyStroke keyStroke = KeyStroke.getKeyStroke(def.keystroke);
							if (keyStroke == null)
							{
								ServoyLog.logWarning("Could not parse keystroke '" + def.keystroke + '\'', null);
							}
							else
							{
								im.put(keyStroke, def.name);
								am.put(def.name, def.action);
							}
						}
					}
					debugJ2DBClient.show();
				}
			}
		}
		finally
		{
			monitor.done();
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.eclipse.debug.actions.StartDebugAction#getDebuggerAboutToStartListener()
	 */
	@Override
	protected IDebuggerStartListener getDebuggerAboutToStartListener()
	{
		return this;
	}

	public void aboutToStartDebugClient()
	{
		// add some delay, this seems to fix grey screens in developer on the mac.
		// If action returns and at the same time the frame is shown, awt/swt events get mixed up?
		long delay = Utils.getAsLong(Settings.getInstance().getProperty("servoy.developer.startsc.delay", Utils.isAppleMacOS() ? "1000" : "0"));
		if (delay > 0)
		{
			try
			{
				Thread.sleep(delay);
			}
			catch (InterruptedException e)
			{
				ServoyLog.logError(e);
			}
		}
	}

}
