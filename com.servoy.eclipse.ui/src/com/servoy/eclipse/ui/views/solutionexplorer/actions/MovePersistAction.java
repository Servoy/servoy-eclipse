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
package com.servoy.eclipse.ui.views.solutionexplorer.actions;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.OptionDialog;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.core.util.UIUtils.ExtendedInputDialog;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.EclipseRepository;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.WorkspaceFileAccess;
import com.servoy.eclipse.ui.Activator;
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
import com.servoy.j2db.persistence.ValidatorSearchContext;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 * Action for moving the selected persist(s).
 * 
 * 
 */
public class MovePersistAction extends AbstractMovePersistAction
{
	protected Location location;

	/**
	 * Creates a new "move persist" action.
	 */
	public MovePersistAction(Shell shell)
	{
		super(shell);
		setImageDescriptor(Activator.loadImageDescriptorFromBundle("moveform.gif"));

	}

	@Override
	public void run()
	{
		WorkspaceJob saveJob = new WorkspaceJob("Moving files")
		{

			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException
			{
				MovePersistAction.super.run();
				return Status.OK_STATUS;
			}

		};
		saveJob.setUser(false);
		saveJob.setRule(ResourcesPlugin.getWorkspace().getRoot());
		saveJob.schedule();
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event)
	{
		super.selectionChanged(event);
		setText("Move " + persistString);
		setToolTipText("Moves the " + persistString + " to a different solution/module");

		ServoyProject[] activeModules = ServoyModelManager.getServoyModelManager().getServoyModel().getModulesOfActiveProject();
		//excluding the active project, so we get the exact number of modules
		if ((activeModules.length - 1) == 0) setEnabled(false);

		if (!isMoving) location = null; // we need to keep the location until all the selected items are moved
	}

	@Override
	protected Location askForNewFormLocation(final IPersist persist, IValidateName nameValidator)
	{
		if (location == null)
		{
			final ServoyProject[] activeModules = ServoyModelManager.getServoyModelManager().getServoyModel().getModulesOfActiveProject();
			List<String> modules = new ArrayList<String>();
			for (ServoyProject project : activeModules)
			{
				if (!project.getProject().getName().equals(persist.getRootObject().getName()))
				{
					modules.add(project.getProject().getName());
				}
			}
			if (modules.size() == 0) return null;

			Collections.sort(modules);
			final String[] moduleNames = modules.toArray(new String[] { });
			Display.getDefault().syncExec(new Runnable()
			{
				public void run()
				{
					final OptionDialog optionDialog = new OptionDialog(shell, "Select destination module", null, "Select module where to move " +
						persistString + " " + ((ISupportName)persist).getName(), MessageDialog.INFORMATION, new String[] { "OK", "Cancel" }, 0, moduleNames, 0);
					int retval = optionDialog.open();
					String selectedProject = null;
					if (retval == Window.OK)
					{
						selectedProject = moduleNames[optionDialog.getSelectedOption()];
					}
					if (selectedProject != null)
					{
						ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(selectedProject);
						location = new Location(null, servoyProject);
					}
				}
			});
		}
		if (location != null)
		{
			try
			{
				ServoyModelManager.getServoyModelManager().getServoyModel().getNameValidator().checkName(((ISupportName)persist).getName(), persist.getID(),
					new ValidatorSearchContext(getPersistType()), false);
			}
			catch (RepositoryException ex)
			{
				Display.getDefault().syncExec(new Runnable()
				{
					public void run()
					{
						MessageDialog.openError(shell, persistString + " already exists", persistString + " " + ((ISupportName)persist).getName() +
							" already exists, it won't be moved to another module");
					}
				});
				isMoving = false;
				return null;
			}
		}
		return location;
	}

	@Override
	protected ExtendedInputDialog<String> createDialog(IPersist persist, IValidateName nameValidator, String[] solutionNames, String initialSolutionName)
	{
		// do not use, renaming is too hackish
		return null;
	}

	/**
	 * @see com.servoy.eclipse.ui.views.solutionexplorer.actions.AbstractMovePersistAction#doWork(com.servoy.j2db.persistence.Form, java.lang.Object[],
	 *      com.servoy.j2db.persistence.IValidateName)
	 */
	@Override
	protected void doWork(IPersist[] persistList, IValidateName nameValidator)
	{
		//ask location if not set (in CreateLoginSolutionQuickFix.MoveForm)
		if (location == null && persistList.length > 0)
		{
			location = askForNewFormLocation(persistList[0], nameValidator);
		}
		if (location == null)
		{
			return;
		}

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
						duplicate = duplicatePersist(editingNode, ((Media)editingNode).getName(), location.getServoyProject(), DummyValidator.INSTANCE);
						idMap = Collections.singletonMap(new Integer(editingNode.getID()), new Integer(duplicate.getID()));
						oldToNewID.put(editingNode.getUUID(), duplicate.getUUID());
						duplicates.put(editingNode.getUUID(), duplicate);
						servoyProject.saveEditingSolutionNodes(new IPersist[] { duplicate }, true, false);
					}
					else
					{
						UUID oldID = editingNode.getUUID();
						idMap = AbstractPersistFactory.resetUUIDSRecursively(editingNode, (EclipseRepository)rootObject.getRepository(), true);
						servoyProject.saveEditingSolutionNodes(new IPersist[] { editingNode }, true, false);
						oldToNewID.put(oldID, editingNode.getUUID());
					}
					updateReferences(idMap);
				}
				catch (final RepositoryException e)
				{
					Display.getDefault().syncExec(new Runnable()
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
				moveFiles(rootObject, servoyProject, editingNode, duplicates.get(persist.getUUID()));
			}
		}

		//clear location when move is done
		location = null;
	}

	private void moveFiles(IRootObject rootObject, ServoyProject servoyProject, final IPersist editingNode, IPersist duplicate)
	{
		try
		{
			Pair<String, String> filePairFrom = SolutionSerializer.getFilePath(editingNode, true);
			String[] scriptPathsFrom = SolutionSerializer.getScriptPaths(editingNode, true);

			String relativePathFrom = editingNode.getRootObject().getName() + '/';
			String relativePathTo = location.getServoyProject().getSolution().getName() + '/';
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

			if (duplicate != null)
			{

				try
				{
					// delete duplicate Media
					((EclipseRepository)rootObject.getRepository()).deleteObject(editingNode);
					servoyProject.saveEditingSolutionNodes(new IPersist[] { editingNode }, true);
				}
				catch (final RepositoryException e)
				{
					Display.getDefault().syncExec(new Runnable()
					{
						public void run()
						{
							ServoyLog.logError(e);
							MessageDialog.openError(shell, "Cannot move form", persistString + " " + ((ISupportName)editingNode).getName() +
								"cannot be moved. Reason:\n" + e.getMessage());
						}
					});
					isMoving = false;
				}
			}
		}
		catch (IOException e)
		{
			ServoyLog.logError(e);
			UIUtils.reportError("Cannot move " + persistString, "Unexpected error while moving " + persistString + ", check log for failure reason.");
		}
	}

	private void updateReferences(final Map<Integer, Integer> idMap) throws RepositoryException
	{
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
				project.saveEditingSolutionNodes(new IPersist[] { project.getEditingSolution() }, true, false);
			}
		}
	}
}