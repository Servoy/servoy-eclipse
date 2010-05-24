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


import java.lang.reflect.InvocationTargetException;
import java.util.List;

import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.dltk.debug.ui.DLTKDebugUIPlugin;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;

import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.ServoyProject;
import com.servoy.eclipse.debug.Activator;
import com.servoy.eclipse.debug.FlattenedSolutionDebugListener;
import com.servoy.eclipse.debug.Activator.ShortcutDefinition;
import com.servoy.j2db.IDebugJ2DBClient;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.util.ITagResolver;

/**
 * @author jcompagner,jblok
 * 
 */
public class StartSmartClientActionDelegate extends StartDebugAction implements IRunnableWithProgress
{
	public static ITagResolver noReplacementResolver = new ITagResolver()
	{
		public String getStringValue(String name)
		{
			return "%%" + name + "%%"; //$NON-NLS-1$ //$NON-NLS-2$
		}
	};

	public static ITagResolver simpleReplacementResolver = new ITagResolver()
	{
		public String getStringValue(String name)
		{
			if ("prefix".equals(name)) return "forms.customer."; //$NON-NLS-1$//$NON-NLS-2$
			else if ("elementName".equals(name)) return ".elements.customer_id"; //$NON-NLS-1$ //$NON-NLS-2$
			else return "%%" + name + "%%"; //$NON-NLS-1$ //$NON-NLS-2$
		}
	};

	private IWorkbenchWindow window;

	private List<ShortcutDefinition> shortCuts;

	@Override
	public void init(IWorkbenchWindow w)
	{
		this.window = w;
	}

	/**
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	public void run(IAction action)
	{
		//make sure the plugins are loaded
		DLTKDebugUIPlugin.getDefault();
		DebugPlugin.getDefault();
		try
		{
			// run in the progress service thread in stead of the main swt thread.
			// needed to make sure the main swt thread is not busy when the debug smart client
			// is created, see ApplicationServer.DebugClientHandler.
			window.getWorkbench().getProgressService().run(true, false, this);
		}
		catch (InvocationTargetException e)
		{
			ServoyLog.logError(e);
		}
		catch (InterruptedException e)
		{
			ServoyLog.logError(e);
		}
	}

	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
	{
		monitor.beginTask("Smart client start", 5); //$NON-NLS-1$
		monitor.worked(1);
		try
		{
			ServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
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
							MessageDialog.openError(Display.getDefault().getActiveShell(), "Solution type problem" ,"Cant open this solution type in this client"); //$NON-NLS-1$
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
								ServoyLog.logWarning("Could not parse keystroke '" + def.keystroke + '\'', null); //$NON-NLS-1$
							}
							else
							{
								im.put(keyStroke, def.name);
								am.put(def.name, def.action);
							}
						}
					}

					if (debugJ2DBClient.getFlattenedSolution().getDebugListener() == null)
					{
						debugJ2DBClient.getFlattenedSolution().registerDebugListener(new FlattenedSolutionDebugListener());
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
}
