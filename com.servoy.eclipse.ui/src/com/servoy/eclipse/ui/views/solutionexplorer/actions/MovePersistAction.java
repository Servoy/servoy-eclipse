/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

package com.servoy.eclipse.ui.views.solutionexplorer.actions;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.RunInWorkspaceJob;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.EclipseRepository;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.WorkspaceFileAccess;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.AbstractPersistFactory;
import com.servoy.j2db.persistence.ContentSpec;
import com.servoy.j2db.persistence.DummyValidator;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IPersistVisitor;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IRootObject;
import com.servoy.j2db.persistence.ISupportName;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 * Move for relations, forms and media objects.
 */
public class MovePersistAction extends AbstractMovePersistAction
{
	public MovePersistAction(Shell shell)
	{
		super(shell);
	}

	/**
	 * @see com.servoy.eclipse.ui.views.solutionexplorer.actions.AbstractPersistSelectionAction#doWork(com.servoy.j2db.persistence.Form, java.lang.Object[],
	 *      com.servoy.j2db.persistence.IValidateName)
	 */
	@Override
	protected void doWork(final IPersist[] persistList, IValidateName nameValidator)
	{
		//ask location if not set (in CreateLoginSolutionQuickFix.MoveForm)
		if (location == null && persistList.length > 0)
		{
			location = askForNewLocation(persistList[0], nameValidator);
		}
		if (location == null)
		{
			return;
		}

		final Location destination = location;
		IWorkspaceRunnable moveJob = new IWorkspaceRunnable()
		{

			@Override
			public void run(IProgressMonitor monitor) throws CoreException
			{
				//reset all uuids and update references
				Map<UUID, UUID> oldToNewID = new HashMap<UUID, UUID>();
				Map<UUID, IPersist> duplicates = new HashMap<UUID, IPersist>();
				for (final IPersist persist : persistList)
				{
					IRootObject rootObject = persist.getRootObject();
					if (rootObject instanceof Solution)
					{
						ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(rootObject.getName());
						IPersist editingNode = servoyProject.getEditingPersist(persist.getUUID());
						try
						{
							// reset all uuids
							IPersist duplicate = null;
							final Map<Integer, Integer> idMap;
							if (editingNode instanceof Media)
							{
								duplicate = PersistCloner.duplicatePersist(editingNode, ((Media)editingNode).getName(), destination.getServoyProject(),
									DummyValidator.INSTANCE);
								idMap = Collections.singletonMap(new Integer(editingNode.getID()), new Integer(duplicate.getID()));
								oldToNewID.put(editingNode.getUUID(), duplicate.getUUID());
								duplicates.put(editingNode.getUUID(), duplicate);
								servoyProject.saveEditingSolutionNodes(new IPersist[] { duplicate }, true, false);
							}
							else
							{
								UUID oldID = editingNode.getUUID();
								idMap = AbstractPersistFactory.resetUUIDSRecursively(editingNode, (EclipseRepository)rootObject.getRepository(), true);
								oldToNewID.put(oldID, editingNode.getUUID());
							}
							updateReferences(editingNode, idMap);
						}
						catch (final RepositoryException e)
						{
							Display.getDefault().asyncExec(new Runnable()
							{
								public void run()
								{
									ServoyLog.logError(e);
									MessageDialog.openError(shell, "Cannot move form", persistString + " " + ((ISupportName)persist).getName() +
										"cannot be moved. Reason:\n" + e.getMessage());
								}
							});
						}
					}
				}

				//do the moves
				for (IPersist persist : persistList)
				{
					IRootObject rootObject = persist.getRootObject();
					if (rootObject instanceof Solution)
					{
						ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(rootObject.getName());
						IPersist editingNode = servoyProject.getEditingPersist(oldToNewID.get(persist.getUUID()));
						moveFiles(rootObject, servoyProject, editingNode, duplicates.get(persist.getUUID()), destination);
					}
				}
			}

		};

		RunInWorkspaceJob job = new RunInWorkspaceJob(moveJob);
		job.setName("Moving files");
		job.setRule(ServoyModel.getWorkspace().getRoot());
		job.setUser(false);
		job.schedule();
		location = null;
	}

	private void moveFiles(IRootObject rootObject, ServoyProject servoyProject, final IPersist editingNode, IPersist duplicate, Location destination)
	{
		try
		{
			Pair<String, String> filePairFrom = SolutionSerializer.getFilePath(editingNode, true);
			String[] scriptPathsFrom = SolutionSerializer.getScriptPaths(editingNode, true);

			String relativePathFrom = editingNode.getRootObject().getName() + '/';
			String relativePathTo = destination.getServoyProject().getSolution().getName() + '/';
			if (filePairFrom.getLeft().startsWith(relativePathFrom))
			{
				WorkspaceFileAccess wsa = new WorkspaceFileAccess(ServoyModel.getWorkspace());
				// move object file
				String relativeFilePathTo = relativePathTo + filePairFrom.getLeft().substring(relativePathFrom.length()) + filePairFrom.getRight();
				wsa.delete(relativeFilePathTo);
				if (wsa.move(filePairFrom.getLeft() + filePairFrom.getRight(), relativeFilePathTo))
				{
					// move script files
					if (scriptPathsFrom != null)
					{
						for (String scriptFile : scriptPathsFrom)
						{
							if (scriptFile.startsWith(relativePathFrom))
							{
								wsa.move(scriptFile, relativePathTo + scriptFile.substring(relativePathFrom.length()));
							}
						}
					}
				}
			}

			try
			{
				if (duplicate != null)
				{
					// delete duplicate Media
					((EclipseRepository)rootObject.getRepository()).deleteObject(editingNode);
					servoyProject.saveEditingSolutionNodes(new IPersist[] { editingNode }, true);
				}
				else
				{
					editingNode.getParent().removeChild(editingNode);
				}
			}
			catch (final RepositoryException e)
			{
				Display.getDefault().asyncExec(new Runnable()
				{
					public void run()
					{
						ServoyLog.logError(e);
						MessageDialog.openError(shell, "Cannot move form", persistString + " " + ((ISupportName)editingNode).getName() +
							"cannot be moved. Reason:\n" + e.getMessage());
					}
				});
			}
		}
		catch (IOException e)
		{
			ServoyLog.logError(e);
			UIUtils.reportError("Cannot move " + persistString, "Unexpected error while moving " + persistString + ", check log for failure reason.");
		}
	}

	private void updateReferences(IPersist editingNode, final Map<Integer, Integer> idMap) throws RepositoryException
	{
		boolean saveEditingNode = true;
		ServoyProject editingProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(editingNode.getRootObject().getName());
		for (ServoyProject project : ServoyModelManager.getServoyModelManager().getServoyModel().getModulesOfActiveProject())
		{
			final boolean[] changed = { false };
			project.getEditingSolution().acceptVisitor(new IPersistVisitor()
			{
				public Object visit(IPersist o)
				{
					try
					{
						for (ContentSpec.Element element : Utils.iterate(((EclipseRepository)ServoyModel.getDeveloperRepository()).getContentSpec().getPropertiesForObjectType(
							o.getTypeID())))
						{
							// Don't set meta data properties.
							if (element.isMetaData() || element.isDeprecated()) continue;
							// Get default property value as an object.
							if (element.getTypeID() == IRepository.ELEMENTS)
							{
								Object property_value = ((AbstractBase)o).getProperty(element.getName());
								int id = Utils.getAsInteger(property_value);
								if (id > 0)
								{
									Integer newId = idMap.get(new Integer(id));
									if (newId != null)
									{
										((AbstractBase)o).setProperty(element.getName(), newId);
										changed[0] = true;
									}
								}
							}
						}
					}
					catch (Exception e)
					{
						ServoyLog.logError(e);
					}
					return IPersistVisitor.CONTINUE_TRAVERSAL;
				}
			});

			if (changed[0])
			{
				if (Utils.equalObjects(project.getProject().getName(), editingProject.getProject().getName()))
				{
					saveEditingNode = false;
				}
				project.saveEditingSolutionNodes(new IPersist[] { project.getEditingSolution() }, true, false);
			}
		}
		if (saveEditingNode)
		{
			editingProject.saveEditingSolutionNodes(new IPersist[] { editingNode }, true, false);
		}

	}
}
