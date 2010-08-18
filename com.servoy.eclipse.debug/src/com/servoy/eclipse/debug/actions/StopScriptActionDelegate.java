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

import com.servoy.eclipse.core.ServoyLog;

/**
 * 
 * @author jcompagner
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
	@SuppressWarnings("nls")
	public void run(IAction action)
	{
		if (stackFrame != null && stackFrame.isSuspended())
		{
			IScriptEvaluationResult syncEvaluate = stackFrame.getScriptThread().getEvaluationEngine().syncEvaluate("!stop_current_script!", stackFrame);
			System.err.println(syncEvaluate);
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
