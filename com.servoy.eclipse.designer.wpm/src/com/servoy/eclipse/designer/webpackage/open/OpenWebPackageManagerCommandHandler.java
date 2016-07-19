package com.servoy.eclipse.designer.webpackage.open;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.util.EditorUtil;

public class OpenWebPackageManagerCommandHandler extends AbstractHandler implements IHandler
{

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException
	{
		ISelection selection = HandlerUtil.getActiveMenuSelection(event);
		String solutionName = null;
		if (selection instanceof StructuredSelection && ((StructuredSelection)selection).getFirstElement() instanceof SimpleUserNode)
		{
			SimpleUserNode project = ((SimpleUserNode)((StructuredSelection)selection).getFirstElement()).getAncestorOfType(ServoyProject.class);
			if (project != null)
			{
				solutionName = ((ServoyProject)project.getRealObject()).getSolution().getName();
			}
		}
		EditorUtil.openWebPackageManager(solutionName);
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.core.commands.AbstractHandler#isEnabled()
	 */
	@Override
	public boolean isEnabled()
	{
		return true;
	}

}
