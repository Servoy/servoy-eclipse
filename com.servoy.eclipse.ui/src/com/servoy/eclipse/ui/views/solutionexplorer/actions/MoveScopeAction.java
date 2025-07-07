package com.servoy.eclipse.ui.views.solutionexplorer.actions;

import java.io.IOException;
import java.util.List;

import org.eclipse.core.resources.IProjectNature;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;

import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.WorkspaceFileAccess;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.util.Pair;

public class MoveScopeAction extends Action implements ISelectionChangedListener
{
	private final SolutionExplorerView viewer;

	/**
	 * Creates a new "create new variable" action for the given solution view.
	 *
	 * @param sev the solution view to use.
	 */
	public MoveScopeAction(SolutionExplorerView sev)
	{
		viewer = sev;

		setText("Move scope");
		setToolTipText(getText());
		setImageDescriptor(Activator.loadDefaultImageDescriptorFromBundle("move_form.png"));
	}

	public void selectionChanged(SelectionChangedEvent event)
	{
		boolean haveModules = false;
		IStructuredSelection sel = (IStructuredSelection)event.getSelection();
		if (sel.size() == 1 && ((SimpleUserNode)sel.getFirstElement()).getType() == UserNodeType.GLOBALS_ITEM)
		{
			SimpleUserNode project = ((SimpleUserNode)sel.getFirstElement()).getAncestorOfType(IProjectNature.class);
			List<String> modules = NewScopeAction.getModules((ServoyProject)project.getRealObject());
			if (modules.size() > 0) haveModules = true;
		}

		setEnabled(haveModules);
	}


	@Override
	public void run()
	{
		SimpleUserNode node = viewer.getSelectedTreeNode();
		if (node != null)
		{
			SimpleUserNode project = node.getAncestorOfType(IProjectNature.class);
			if (project == null)
			{
				return;
			}

			Pair<Solution, String> pair = (Pair<Solution, String>)node.getRealObject();
			String fileName = pair.getRight();

			ServoyProject solution = NewScopeAction.askNewProject(viewer.getViewSite().getShell(), fileName, (ServoyProject)project.getRealObject());
			if (solution == null || solution.equals(project.getRealObject()))
			{
				return;
			}

			Solution oldSolution = (((ServoyProject)project.getRealObject()).getSolution());
			Solution newSolution = solution.getSolution();

			WorkspaceFileAccess wsfa = new WorkspaceFileAccess(((IProjectNature)solution).getProject().getWorkspace());
			String oldScriptPath = SolutionSerializer.getRelativePath(oldSolution, false) + fileName + SolutionSerializer.JS_FILE_EXTENSION;
			String newScriptPath = SolutionSerializer.getRelativePath(newSolution, false) + fileName + SolutionSerializer.JS_FILE_EXTENSION;
			if (!wsfa.exists(newScriptPath))
			{
				try
				{
					wsfa.move(oldScriptPath, newScriptPath);
				}
				catch (IOException e)
				{
					ServoyLog.logError("Could not move global scope " + fileName + " in project  " + newSolution.getName(), e);
				}
			}
			else
			{
				MessageDialog.openInformation(
					viewer.getViewSite().getShell(),
					"Move global scope file",
					"File '" + fileName + "' was not moved, because in project '" + newSolution.getName() + "' already exist a file with that name!");
			}
		}
	}
}