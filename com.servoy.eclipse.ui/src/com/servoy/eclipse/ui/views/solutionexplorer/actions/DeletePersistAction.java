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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.ltk.internal.ui.refactoring.RefactoringUIMessages;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.ltk.ui.refactoring.resource.DeleteResourcesWizard;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.nature.ServoyResourcesProject;
import com.servoy.eclipse.model.ngpackages.ILoadedNGPackagesListener;
import com.servoy.eclipse.model.repository.EclipseRepository;
import com.servoy.eclipse.model.repository.StringResourceDeserializer;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.WorkspaceFileAccess;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IRootObject;
import com.servoy.j2db.persistence.MenuItem;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.StringResource;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;

/**
 * Action to delete IPersist objects from a solution or string resources. The selected IPersist objects must be either all resources, or all descendants of the
 * same solution object.
 *
 * @author rgansevles
 */
public class DeletePersistAction extends Action implements ISelectionChangedListener
{
	private final UserNodeType type;

	protected List<IPersist> selectedPersists;

	/**
	 * Creates a new "delete persist" action for the given solution view.
	 */
	public DeletePersistAction(UserNodeType type, String text)
	{
		this.type = type;

		Activator.getDefault();
		setImageDescriptor(Activator.loadImageDescriptorFromBundle("delete.png"));
		setText(text);
		setToolTipText(text);
	}

	public void selectionChanged(SelectionChangedEvent event)
	{
		selectedPersists = null;
		IStructuredSelection sel = (IStructuredSelection)event.getSelection();
		boolean state = (sel.size() > 0);
		if (state)
		{
			Iterator<SimpleUserNode> selit = sel.iterator();
			List<IPersist> selected = new ArrayList<IPersist>(sel.size());
			while (state && selit.hasNext())
			{
				SimpleUserNode node = selit.next();
				state = node.getType() == type;
				if (state)
				{
					IPersist persist = (IPersist)node.getRealObject();
					selected.add(persist);
					// do not allow delete if object is not shown under its own parent (for instance module object)
					if (!(persist instanceof MenuItem) && persist.getParent() != null)
					{
						SimpleUserNode parentNode = node.getAncestorOfType(persist.getParent().getClass());
						if (parentNode != null && parentNode.getRealObject() != null && !parentNode.getRealObject().equals(persist.getParent()))
						{
							state = false;
						}
					}
					if (type == UserNodeType.FORM && persist instanceof Form)
					{
						if (((Form)persist).isFormComponent())
						{
							setText("Delete form component");
							setToolTipText("Delete form component");
						}
						else
						{
							setText("Delete form");
							setToolTipText("Delete form");
						}
					}
				}
			}
			if (state)
			{
				selectedPersists = selected;
			}
		}
		setEnabled(state);
	}

	protected void performDeletion(List<IPersist> selectedPersistItems)
	{
		performDeletionStatic(selectedPersistItems, RefactoringUIMessages.DeleteResourcesHandler_title);
	}

	public static void performDeletionStatic(List<IPersist> selectedPersistItems, String formDeleteDialogTitle)
	{
		List<IPersist> toDelete = selectedPersistItems;
		List<IPersist> refItems = selectedPersistItems;

		try
		{
			if (toDelete.size() > 0)
			{
				List<Form> formsToDelete = new ArrayList<>();
				boolean isTemplateChange = false;
				for (IPersist persist : refItems)
				{
					boolean closeEditor = true;
					IRootObject rootObject = persist.getRootObject();

					if (rootObject instanceof Solution)
					{
						if (persist instanceof Form frm)
						{
							//make a list of forms to delete them all at once using Delete Resources Wizard
							formsToDelete.add(frm);
							closeEditor = false;
						}
						else
						{
							ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(rootObject.getName());
							EclipseRepository repository = (EclipseRepository)rootObject.getRepository();

							IPersist editingNode = servoyProject.getEditingPersist(persist.getUUID());

							repository.deleteObject(editingNode);
							if (editingNode instanceof MenuItem)
							{
								editingNode = editingNode.getAncestor(IRepository.MENUS);
							}
							servoyProject.saveEditingSolutionNodes(new IPersist[] { editingNode }, true);
						}
					}
					else if (rootObject instanceof StringResource)
					{
						ServoyResourcesProject resourcesProject = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveResourcesProject();
						if (resourcesProject != null)
						{
							IProject p = resourcesProject.getProject();
							try
							{
								if (persist instanceof StringResource)
								{
									StringResourceDeserializer.deleteStringResource((StringResource)persist, new WorkspaceFileAccess(p.getWorkspace()),
										p.getName());
								}
								else
								{
									ServoyLog.logError("Trying to delete a selection that is only partially made of resources (they must all be resources)",
										null);
								}
							}
							catch (IOException e)
							{
								ServoyLog.logError(e);
							}
						}
						else
						{
							ServoyLog.logError("Cannot delete resources when no resources project is active", null);
						}
					}
					if (closeEditor) EditorUtil.closeEditor(persist);
					if (!isTemplateChange && persist.getTypeID() == IRepository.TEMPLATES)
					{
						isTemplateChange = true;
					}
				}
				if (isTemplateChange)
				{
					// TODO how nice is this to force-call this just to refresh templates?
					ServoyModelFinder.getServoyModel().getNGPackageManager().ngPackagesChanged(ILoadedNGPackagesListener.CHANGE_REASON.RELOAD, false);
				}

				if (!formsToDelete.isEmpty())
				{
					EclipseRepository rep = (EclipseRepository)ApplicationServerRegistry.get().getDeveloperRepository();
					HashMap<Form, List<String>> persists = rep.getAllFilesForPersists(formsToDelete);

					Set<Form> keys = persists.keySet();
					Iterator<Form> it = keys.iterator();
					ArrayList<IFile> resources = new ArrayList<IFile>();
					while (it.hasNext())
					{
						List<String> filePaths = persists.get(it.next());
						for (String path : filePaths)
						{
							resources.add(ServoyModel.getWorkspace().getRoot().getFile(new Path(path)));
						}
					}

					DeleteResourcesWizard wizard = new DeleteResourcesWizard(resources.toArray(new IFile[resources.size()]));

					RefactoringWizardOpenOperation op = new RefactoringWizardOpenOperation(wizard);
					try
					{
						op.run(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), formDeleteDialogTitle);
					}
					catch (InterruptedException e)
					{
						// do nothing
					}
				}
			}
		}
		catch (RepositoryException e)
		{
			ServoyLog.logError("Could not delete persist from solution", e);
		}
	}

	/**
	 * Retrieves the dependency table, that specifies which form extends another form;
	 *
	 * @param formsToDelete
	 * @return
	 */
	private Map<Integer, List<String>> retrieveDeleteFormRelations(List<IPersist> formsToDelete)
	{
		Map<Integer, List<String>> map = new HashMap<Integer, List<String>>();

		//build a list of the selected form ids;
		List<Integer> formsIds = new ArrayList<Integer>();
		for (IPersist form : formsToDelete)
		{
			formsIds.add(new Integer(form.getID()));
		}

		//retrieve the servoy model
		IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();

		//retrieve all the forms;
		Iterator<Form> it = servoyModel.getFlattenedSolution().getForms(false);

		//start iterating through all the forms;
		while (it.hasNext())
		{
			Form itForm = it.next();

			int extendedFormId = itForm.getExtendsID();
			if (extendedFormId > 0)
			{
				Integer itExtendsFormID = new Integer(extendedFormId);

				//if we found a form that has an extended class in the list of the ones that we wish to delete then...
				if (formsIds.contains(itExtendsFormID))
				{
					if (map.containsKey(itExtendsFormID))
					{
						List<String> list = map.get(itExtendsFormID);
						list.add(itForm.getName());
					}
					else
					{
						List<String> list = new ArrayList<String>();
						list.add(itForm.getName());
						map.put(itExtendsFormID, list);
					}
				}
			}
		}
		return map;
	}


	/**
	 * Builds the message to be displayed in the case of having forms that extend other forms;
	 *
	 * @param map
	 * @return
	 */
	private String buildMessageFromRelationsTable(Map<Integer, List<String>> map)
	{
		String message = "";
		//retrieve the servoy model
		IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();

		for (Integer currentKey : map.keySet())
		{
			List<String> childFormNames = map.get(currentKey);

			String baseFormName = servoyModel.getFlattenedSolution().getForm(currentKey.intValue()).getName();

			message += "Form: [" + baseFormName + "] has the following children: ";
			for (Object object : childFormNames)
			{
				String childName = (String)object;

				if (childFormNames.indexOf(object) == childFormNames.size() - 1)
				{
					message += childName + ";";
				}
				else
				{
					message += childName + ",";
				}
			}

			message += "\n";
		}
		return message;
	}

	/**
	 * Search for a given id in the list of forms to be deleted;
	 *
	 * @param id
	 * @param formsToDelete
	 * @return true/false whether is found or not;
	 */
	private boolean idInFormsToDelete(int id, List<IPersist> formsToDelete)
	{
		for (IPersist persist : formsToDelete)
		{
			if (persist.getID() == id) return true;
		}
		return false;
	}


	/**
	 * Checks is all the selected forms for deletion are in the same set; That means, there is no other form that is NOT selected for delete and has a parent in
	 * the delete list;
	 *
	 * @param formsToDelete
	 * @return
	 */
	private boolean allFormsAreInTheSameSet(List<IPersist> formsToDelete)
	{
		IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
		Iterator<Form> it = servoyModel.getFlattenedSolution().getForms(false);

		while (it.hasNext())
		{
			Form form = it.next();

			if (form.getExtendsID() > 0)
			{
				int id = form.getID();
				int extendedFormId = form.getExtendsID();

				if (!idInFormsToDelete(id, formsToDelete) && idInFormsToDelete(extendedFormId, formsToDelete)) return false;
			}
		}

		return true;
	}


	@Override
	public void run()
	{
		if (selectedPersists == null) return;
		List<IPersist> deleteItems = selectedPersists;

		Map<Integer, List<String>> map = new HashMap<Integer, List<String>>();
		String message = "";

		if (deleteItems.size() > 0 && (deleteItems.get(0).getRootObject() instanceof Solution))
		{
			map = retrieveDeleteFormRelations(deleteItems);

			//that means that we have relationships
			if (map.size() > 0)
			{
				if (allFormsAreInTheSameSet(deleteItems))
				{
					if (selectedPersists.get(0) instanceof Form) //we'll later use the delete resources wizard for forms, and that already contains question on deletion
					{
						performDeletion(selectedPersists);
					}
					else if (MessageDialog.openConfirm(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), getText(),
						"Are you sure you want to delete?"))
					{
						performDeletion(selectedPersists);
					}
				}
				else
				{
					message = buildMessageFromRelationsTable(map);
					MessageDialog.openError(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), getText(),
						message + "You are not allowed to delete!");
				}
			}
			else
			{
				if (selectedPersists.get(0) instanceof Form) //we'll later use the delete resources wizard for forms, and that already contains question on deletion
				{
					performDeletion(selectedPersists);
				}
				else if (MessageDialog.openConfirm(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), getText(),
					"Are you sure you want to delete?"))
				{
					performDeletion(selectedPersists);
				}
			}
		}
		else if (deleteItems.size() > 0 && (deleteItems.get(0).getRootObject() instanceof StringResource))
		{
			if (MessageDialog.openConfirm(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), getText(), "Are you sure you want to delete?"))
			{
				performDeletion(selectedPersists);
			}
		}
	}

}