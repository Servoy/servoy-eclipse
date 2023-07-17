package com.servoy.eclipse.ui.actions;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.persistence.Solution;

public class ConfigureThemeCommandHandler extends AbstractHandler implements IHandler
{

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException
	{
		ISelection selection = HandlerUtil.getActiveMenuSelection(event);
		Solution solution = null;
		if (selection instanceof StructuredSelection && ((StructuredSelection)selection).getFirstElement() instanceof SimpleUserNode)
		{
			SimpleUserNode node = ((SimpleUserNode)((StructuredSelection)selection).getFirstElement()).getAncestorOfType(ServoyProject.class);
			solution = node != null ? node.getSolution() : null;
		}
		else
		{
			ServoyProject project = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject();
			solution = project != null ? project.getSolution() : null;
		}
		if (solution != null)
		{
			EditorUtil.openThemeEditor(solution);
		}
		return null;
	}

	@Override
	public boolean isEnabled()
	{
		return ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject() != null;
	}
}
