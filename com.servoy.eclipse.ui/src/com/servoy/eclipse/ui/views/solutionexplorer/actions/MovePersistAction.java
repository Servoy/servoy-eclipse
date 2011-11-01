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


import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.OptionDialog;
import com.servoy.eclipse.core.util.UIUtils.ExtendedInputDialog;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.EclipseRepository;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Activator;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.ContentSpec;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IPersistVisitor;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IRootObject;
import com.servoy.j2db.persistence.ISupportName;
import com.servoy.j2db.persistence.ISupportUpdateableName;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.ValidatorSearchContext;
import com.servoy.j2db.util.Utils;

/**
 * Action for duplicating the selected persist(s).
 * 
 * 
 */
public class MovePersistAction extends AbstractMovePersistAction
{
	protected Location location;

	/**
	 * Creates a new "duplicate persist" action.
	 */
	public MovePersistAction(Shell shell)
	{
		super(shell);
		setImageDescriptor(Activator.loadImageDescriptorFromBundle("moveform.gif"));

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

		location = null;
	}

	@Override
	protected Location askForNewFormLocation(IPersist persist, IValidateName nameValidator)
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
			String[] moduleNames = modules.toArray(new String[] { });
			final OptionDialog optionDialog = new OptionDialog(shell, "Select destination module", null, "Select module where to move " + persistString + " " +
				((ISupportName)persist).getName(), MessageDialog.INFORMATION, new String[] { "OK", "Cancel" }, 0, moduleNames, 0);
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
		if (location != null)
		{
			try
			{
				ServoyModelManager.getServoyModelManager().getServoyModel().getNameValidator().checkName(((ISupportName)persist).getName(), persist.getID(),
					new ValidatorSearchContext(getPersistType()), false);
			}
			catch (RepositoryException ex)
			{
				MessageDialog.openError(shell, persistString + " already exists", persistString + " " + ((ISupportName)persist).getName() +
					" already exists, it won't be moved to another module");
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
	protected void doWork(final IPersist persist, final Location location, IValidateName nameValidator) throws RepositoryException
	{
		String tempName = persist.getUUID().toString();
		String persistName = null;
		if (persist instanceof ISupportName) persistName = ((ISupportName)persist).getName();
		final IPersist duplicate = intelligentClonePersist(persist, tempName, location.getServoyProject(), nameValidator, false);
		if (duplicate != null)
		{
			IRootObject rootObject = persist.getRootObject();
			final int oldId = persist.getID();
			if (rootObject instanceof Solution)
			{
				ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(rootObject.getName());
				EclipseRepository repository = (EclipseRepository)rootObject.getRepository();

				IPersist editingNode = servoyProject.getEditingPersist(persist.getUUID());
				repository.deleteObject(editingNode);

				servoyProject.saveEditingSolutionNodes(new IPersist[] { editingNode }, true);
			}

			if (duplicate instanceof ISupportUpdateableName)
			{
				((ISupportUpdateableName)duplicate).updateName(nameValidator, persistName);
			}
			else if (duplicate instanceof Media)
			{
				((Media)duplicate).setName(persistName);
			}

			for (ServoyProject project : ServoyModelManager.getServoyModelManager().getServoyModel().getModulesOfActiveProject())
			{
				project.getEditingSolution().acceptVisitor(new IPersistVisitor()
				{
					public Object visit(IPersist o)
					{
						try
						{
							EclipseRepository er = (EclipseRepository)ServoyModel.getDeveloperRepository();
							Iterator<ContentSpec.Element> iterator = er.getContentSpec().getPropertiesForObjectType(o.getTypeID());
							while (iterator.hasNext())
							{
								final ContentSpec.Element element = iterator.next();
								// Don't set meta data properties.
								if (element.isMetaData() || element.isDeprecated()) continue;
								// Get default property value as an object.
								final int typeId = element.getTypeID();
								if (typeId == IRepository.ELEMENTS)
								{
									Object property_value = ((AbstractBase)o).getProperty(element.getName());
									final int element_id = Utils.getAsInteger(property_value);
									if (element_id > 0 && element_id == oldId)
									{
										((AbstractBase)o).setProperty(element.getName(), new Integer(duplicate.getID()));
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

				project.saveEditingSolutionNodes(new IPersist[] { project.getEditingSolution() }, true);
			}
		}
	}
}