package com.servoy.eclipse.aibridge.actions;


import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import com.servoy.eclipse.aibridge.AiBridgeView;
import com.servoy.eclipse.aibridge.dto.Completion;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.Solution;

public class OpenSourceAction extends Action implements ISelectionListener
{
	private Completion completion;
	private IPersist persist;
	private String scopeName;

	public OpenSourceAction()
	{
		super("Open Source", SWT.PUSH);
		ImageDescriptor splitViewImageDescriptor = AbstractUIPlugin.imageDescriptorFromPlugin("com.servoy.eclipse.ui", "icons/open.png");
		setImageDescriptor(splitViewImageDescriptor);
		setEnabled(true);
	}

	@Override
	public void run()
	{
		Display display = Display.getCurrent();
		if (display != null)
		{
			display.asyncExec(() -> display.timerExec(300, () -> setChecked(false)));
		}
		if (completion == null) return;
		String[] pathParts = extractPathParts(completion.getSourcePath());
		Solution targetSolution = locateTargetSolution(pathParts[0]);
		assignPersistAndScopeName(pathParts, targetSolution);
		EditorUtil.openScriptEditor(persist, scopeName, true);

	}

	private String[] extractPathParts(String selectionPath)
	{
		return selectionPath.substring(1).split("/");
	}

	private Solution locateTargetSolution(String solutionName)
	{
		Solution activeSolution = getActiveSolution();
		if (isActiveSolution(solutionName, activeSolution))
		{
			return activeSolution;
		}
		return findTargetSolutionInModules(solutionName);
	}

	private Solution getActiveSolution()
	{
		return ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject().getSolution();
	}

	private boolean isActiveSolution(String solutionName, Solution activeSolution)
	{
		return activeSolution.getName().equals(solutionName);
	}

	private Solution findTargetSolutionInModules(String solutionName)
	{
		FlattenedSolution flSol = ServoyModelManager.getServoyModelManager().getServoyModel().getFlattenedSolution();
		for (Solution module : flSol.getModules())
		{
			if (module.getName().equals(solutionName))
			{
				return module;
			}
		}
		return null;
	}

	private void assignPersistAndScopeName(String[] parts, Solution targetSolution)
	{
		scopeName = parts[parts.length - 1];
		boolean isForm = (parts.length > 2 && parts[1].equals("forms"));
		if (scopeName.endsWith(".js"))
		{
			scopeName = scopeName.substring(0, scopeName.length() - 3);
		}
		if (isForm)
		{
			persist = targetSolution.getForm(scopeName);
			scopeName = null;
		}
		else
		{
			persist = targetSolution;
		}
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection)
	{
		if (part instanceof AiBridgeView)
		{
			this.completion = null;
			if (!selection.isEmpty() && selection instanceof IStructuredSelection structuredSelection)
			{
				this.completion = (Completion)structuredSelection.getFirstElement();
			}
			setEnabled(completion != null ? true : false);
		}
	}
}
