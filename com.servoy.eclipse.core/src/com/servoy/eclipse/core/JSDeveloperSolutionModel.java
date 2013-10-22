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

package com.servoy.eclipse.core;

import java.util.List;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.core.resource.PersistEditorInput;
import com.servoy.eclipse.core.util.RunInWorkspaceJob;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.repository.EclipseRepository;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.IFileAccess;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.WorkspaceFileAccess;
import com.servoy.j2db.ClientState;
import com.servoy.j2db.IForm;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.scripting.solutionmodel.JSForm;
import com.servoy.j2db.util.Debug;

/**
 * Class that is a special interface in javascript only there in the developer that bridges between the runtime client and the developers workspace
 * 
 * @author jcompagner
 * @since 6.0
 */
@ServoyDocumented(category = ServoyDocumented.RUNTIME, publicName = "servoyDeveloper", scriptingName = "servoyDeveloper")
public class JSDeveloperSolutionModel
{

	private final ClientState state;


	public JSDeveloperSolutionModel(ClientState state)
	{
		this.state = state;
	}

	/**
	 * Saves all changes made through the solution model into the workspace.
	 */
	public void js_save()
	{
		IWorkspaceRunnable saveJob = new IWorkspaceRunnable()
		{
			@Override
			public void run(IProgressMonitor monitor) throws CoreException
			{
				final IFileAccess wfa = new WorkspaceFileAccess(ResourcesPlugin.getWorkspace());
				Solution solutionCopy = state.getFlattenedSolution().getSolutionCopy();
				try
				{
					EclipseRepository eclipseRepository = (EclipseRepository)ServoyModel.getDeveloperRepository();
					eclipseRepository.loadForeignElementsIDs(solutionCopy);
					List<IPersist> allObjectsAsList = solutionCopy.getAllObjectsAsList();
					for (IPersist persist : allObjectsAsList)
					{
						checkParent(persist);
						SolutionSerializer.writePersist(persist, wfa, ServoyModel.getDeveloperRepository(), true, false, true);
						if (persist instanceof AbstractBase)
						{
							((AbstractBase)persist).setParent(solutionCopy);
						}
					}
					eclipseRepository.clearForeignElementsIds();
				}
				catch (RepositoryException e)
				{
					Debug.error(e);
				}
			}
		};
		RunInWorkspaceJob job = new RunInWorkspaceJob("Save solution data", saveJob);
		job.setRule(ServoyModel.getWorkspace().getRoot());
		job.setUser(false);
		job.schedule();
	}

	/**
	 * Saves just the give form into the developers workspace.
	 * This must be a solution created or altered form.
	 * 
	 * @param form The formname or JSForm object to save.
	 */
	public void js_save(Object form)
	{
		String name = null;
		if (form instanceof String)
		{
			name = (String)form;
		}
		else if (form instanceof JSForm)
		{
			name = ((JSForm)form).getName();
		}
		if (name != null)
		{
			final String formName = name;
			WorkspaceJob saveJob = new WorkspaceJob("Save solution data")
			{
				@Override
				public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException
				{
					final IFileAccess wfa = new WorkspaceFileAccess(ResourcesPlugin.getWorkspace());
					Solution solutionCopy = state.getFlattenedSolution().getSolutionCopy();
					try
					{
						Form frm = solutionCopy.getForm(formName);
						if (frm == null) throw new IllegalArgumentException("JSForm is not a solution model created/altered form"); //$NON-NLS-1$

						checkParent(frm);

						EclipseRepository eclipseRepository = (EclipseRepository)ServoyModel.getDeveloperRepository();
						eclipseRepository.loadForeignElementsIDs(frm);
						SolutionSerializer.writePersist(frm, wfa, ServoyModel.getDeveloperRepository(), true, false, true);
						eclipseRepository.clearForeignElementsIds();

						frm.setParent(solutionCopy);
					}
					catch (RepositoryException e)
					{
						Debug.error(e);
					}
					return Status.OK_STATUS;
				}
			};
			saveJob.setUser(false);
			saveJob.setRule(ServoyModel.getWorkspace().getRoot());
			saveJob.schedule();
		}
	}

	/**
	 * @param persist
	 */
	private void checkParent(IPersist persist)
	{
		IPersist realPersist = ServoyModelFinder.getServoyModel().getActiveProject().getEditingSolution().getChild(persist.getUUID());
		if (realPersist == null)
		{
			// the changed form could be in a module.
			Solution[] modules = ServoyModelFinder.getServoyModel().getActiveProject().getModules();
			for (Solution module : modules)
			{
				realPersist = module.getChild(persist.getUUID());
				if (realPersist instanceof AbstractBase)
				{
					// it is found in a module, now rebase the form to that parent so that it is saved in the right location
					((AbstractBase)persist).setParent(realPersist.getParent());
					break;
				}
			}
		}
	}

	/**
	 * Opens the form FormEditor in the developer.
	 * 
	 * @param form The form name or JSForm object to open in an editor.
	 */
	public void js_openForm(Object form)
	{
		String name = null;
		if (form instanceof String)
		{
			name = (String)form;
		}
		else if (form instanceof JSForm)
		{
			name = ((JSForm)form).getName();
		}
		else if (form instanceof IForm)
		{
			name = ((IForm)form).getName();
		}
		if (name != null)
		{
			final Form frm = ServoyModelFinder.getServoyModel().getFlattenedSolution().getForm(name);
			if (frm != null)
			{
				Display.getDefault().asyncExec(new Runnable()
				{
					public void run()
					{
						try
						{
							PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(
								new PersistEditorInput(frm.getName(), frm.getSolution().getName(), frm.getUUID()).setNew(false),
								PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(null,
									Platform.getContentTypeManager().getContentType(PersistEditorInput.FORM_RESOURCE_ID)).getId());
						}
						catch (PartInitException ex)
						{
							ServoyLog.logError(ex);
						}
					}
				});
			}
			else
			{
				throw new IllegalArgumentException("form " + name + " is not a workspace stored (blueprint) form"); //$NON-NLS-1$//$NON-NLS-2$
			}
		}
	}
}
