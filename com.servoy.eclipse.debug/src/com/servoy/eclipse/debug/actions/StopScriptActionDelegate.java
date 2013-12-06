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

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.ISuspendResume;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.contexts.DebugContextEvent;
import org.eclipse.debug.ui.contexts.IDebugContextListener;
import org.eclipse.debug.ui.contexts.IDebugContextService;
import org.eclipse.dltk.debug.core.eval.IScriptEvaluationResult;
import org.eclipse.dltk.debug.core.model.IScriptStackFrame;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IActionDelegate2;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;

import com.servoy.eclipse.model.util.ServoyLog;

/**
 * An ActionDelegate that is attached to the debug views toolbar that stops the executing of the current script
 * but doesnt stop the whole process.
 * 
 * @author jcompagner
 * @since 6.0
 */
public class StopScriptActionDelegate implements IViewActionDelegate, IDebugContextListener, IActionDelegate2
{
	private IAction actionDelegate;
	private IViewPart view;
	private IScriptStackFrame stackFrame;

	public StopScriptActionDelegate()
	{
		super();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IViewActionDelegate#init(org.eclipse.ui.IViewPart)
	 */
	public void init(IViewPart view)
	{
		this.view = view;
		IDebugContextService contextService = DebugUITools.getDebugContextManager().getContextService(view.getSite().getWorkbenchWindow());
		contextService.addDebugContextListener(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	public void run(IAction action)
	{
		if (stackFrame != null && stackFrame.isSuspended())
		{
			IScriptEvaluationResult syncEvaluate = stackFrame.getScriptThread().getEvaluationEngine().syncEvaluate("!stop_current_script!", stackFrame);
			if ("!stopped!".equals(syncEvaluate.getValue().getRawValue()))
			{
				try
				{
					stackFrame.resume();
				}
				catch (DebugException e)
				{
					ServoyLog.logError(e);
				}
			}
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction, org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection)
	{
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.debug.ui.contexts.IDebugContextListener#debugContextChanged(org.eclipse.debug.ui.contexts.DebugContextEvent)
	 */
	public void debugContextChanged(DebugContextEvent event)
	{
		ISelection context = event.getContext();
		if (context instanceof IStructuredSelection)
		{
			Object selected = ((IStructuredSelection)context).getFirstElement();
			if (selected instanceof ISuspendResume)
			{
				if (selected instanceof IScriptStackFrame)
				{
					stackFrame = (IScriptStackFrame)selected;
				}
				actionDelegate.setEnabled(((ISuspendResume)selected).isSuspended());
			}
			else
			{
				actionDelegate.setEnabled(false);
			}
		}
		else
		{
			actionDelegate.setEnabled(false);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IActionDelegate2#init(org.eclipse.jface.action.IAction)
	 */
	public void init(IAction action)
	{
		this.actionDelegate = action;
		action.setEnabled(false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IActionDelegate2#dispose()
	 */
	public void dispose()
	{
		IDebugContextService contextService = DebugUITools.getDebugContextManager().getContextService(view.getSite().getWorkbenchWindow());
		contextService.removeDebugContextListener(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IActionDelegate2#runWithEvent(org.eclipse.jface.action.IAction, org.eclipse.swt.widgets.Event)
	 */
	public void runWithEvent(IAction action, Event event)
	{
		run(action);
	}

}
