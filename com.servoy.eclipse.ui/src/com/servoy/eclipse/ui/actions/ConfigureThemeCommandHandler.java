package com.servoy.eclipse.ui.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import com.servoy.eclipse.core.IActiveProjectListener;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.persistence.IMediaProvider;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.server.ngclient.less.resources.ThemeResourceLoader;

public class ConfigureThemeCommandHandler extends AbstractHandler implements IHandler, IActiveProjectListener
{
	public ConfigureThemeCommandHandler()
	{
		super();
		ServoyModelManager.getServoyModelManager().getServoyModel().addActiveProjectListener(this);
	}

	@Override
	public void dispose()
	{
		ServoyModelManager.getServoyModelManager().getServoyModel().removeActiveProjectListener(this);
		super.dispose();
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException
	{
		Solution solution = getSolution(event);
		if (solution != null)
		{
			EditorUtil.openThemeEditor(ModelUtils.getEditingFlattenedSolution(solution));
		}
		return null;
	}

	private Solution getSolution(ExecutionEvent event)
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
		return solution;
	}

	@Override
	public void setEnabled(Object evaluationContext)
	{
		ServoyProject activeProject = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject();
		if (activeProject != null)
		{
			IMediaProvider solution = ModelUtils.getEditingFlattenedSolution(activeProject.getSolution());
			setBaseEnabled(solution.getMedia(ThemeResourceLoader.CUSTOM_PROPERTIES_LESS) != null ||
				solution.getMedia(ThemeResourceLoader.SOLUTION_PROPERTIES_LESS) != null);
		}
		else
		{
			setBaseEnabled(false);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.eclipse.core.IActiveProjectListener#activeProjectWillChange(com.servoy.eclipse.model.nature.ServoyProject,
	 * com.servoy.eclipse.model.nature.ServoyProject)
	 */
	@Override
	public boolean activeProjectWillChange(ServoyProject activeProject, ServoyProject toProject)
	{
		return true;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.eclipse.core.IActiveProjectListener#activeProjectChanged(com.servoy.eclipse.model.nature.ServoyProject)
	 */
	@Override
	public void activeProjectChanged(ServoyProject activeProject)
	{
		setEnabled(null);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.eclipse.core.IActiveProjectListener#activeProjectUpdated(com.servoy.eclipse.model.nature.ServoyProject, int)
	 */
	@Override
	public void activeProjectUpdated(ServoyProject activeProject, int updateInfo)
	{
		if (updateInfo == IActiveProjectListener.STYLES_ADDED_OR_REMOVED ||
			updateInfo == IActiveProjectListener.RESOURCES_UPDATED_BECAUSE_ACTIVE_PROJECT_CHANGED ||
			updateInfo == IActiveProjectListener.MODULES_UPDATED)
		{
			setEnabled(null);
		}
	}
}
