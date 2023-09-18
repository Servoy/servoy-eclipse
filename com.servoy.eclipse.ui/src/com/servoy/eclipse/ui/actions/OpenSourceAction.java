package com.servoy.eclipse.ui.actions;


import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.Solution;

public class OpenSourceAction extends Action
{
	private String scopeName;
	private IPersist persist;
	private boolean run = false;

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
			display.asyncExec(() -> {
				display.timerExec(300, () -> {
					setChecked(false);
				});
			});
		}
		if (!run) return;
		EditorUtil.openScriptEditor(persist, scopeName, true);

	}

	public void processDataForScripEditor(String selectionPath)
	{
		System.out.println("DEBUG### - processDataForScriptEditor");
		run = false;
		if (selectionPath == null || selectionPath.trim().isEmpty()) return;

		String[] parts = selectionPath.substring(1).split("/");
		String solutionName = parts[0];
		boolean isForm = (parts.length > 2 && parts[1].equals("forms"));

		Solution activeSolution = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject().getSolution();

		Solution targetSolution = null;

		if (activeSolution.getName().equals(solutionName))
		{
			targetSolution = activeSolution;
		}
		else
		{
			FlattenedSolution flSol = ServoyModelManager.getServoyModelManager().getServoyModel().getFlattenedSolution();
			Solution[] modules = flSol.getModules();

			for (Solution module : modules)
			{
				if (module.getName().equals(solutionName))
				{
					targetSolution = module;
					break;
				}
			}
		}

		persist = targetSolution;
		scopeName = parts[parts.length - 1];
		if (scopeName.endsWith(".js"))
			scopeName = scopeName.substring(0, scopeName.length() - 3);

		if (isForm)
		{
			persist = targetSolution.getForm(scopeName);
			scopeName = null;
		}

		run = true;
	}

}
