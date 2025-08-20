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
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.OptionDialog;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ValidatorSearchContext;

/**
 * Super class for move persist actions.
 */
public abstract class AbstractMovePersistAction extends AbstractPersistSelectionAction
{
	protected Location location;

	/**
	 * Creates a new "move persist" action.
	 */
	public AbstractMovePersistAction(Shell shell)
	{
		super(shell);
		setImageDescriptor(Activator.loadImageDescriptorFromBundle("move_form.png"));
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event)
	{
		super.selectionChanged(event);
		setText("Move " + persistString);
		setToolTipText("Moves the " + persistString + " to a different solution/module");

		ServoyProject[] activeModules = ServoyModelManager.getServoyModelManager().getServoyModel().getModulesOfActiveProject();
		//excluding the active project, so we get the exact number of modules
		if ((activeModules.length - 1) <= 0) setEnabled(false);
	}

	@Override
	protected boolean isEnabledForNode(UserNodeType type)
	{
		return type == UserNodeType.RELATION || type == UserNodeType.VALUELIST_ITEM || type == UserNodeType.MEDIA_IMAGE || type == UserNodeType.FORM ||
			type == UserNodeType.INMEMORY_DATASOURCE;
	}

	protected Location askForNewLocation(final IPersist persist, IValidateName nameValidator)
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
			final OptionDialog optionDialog = new OptionDialog(shell, "Select destination module", null,
				"Select module where to move " + persistString + " " + getName(persist), MessageDialog.INFORMATION, new String[] { "OK", "Cancel" }, 0,
				moduleNames, 0);
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
				nameValidator.checkName(getName(persist), persist.getUUID(), new ValidatorSearchContext(getPersistType()), false);
			}
			catch (RepositoryException ex)
			{
				MessageDialog.openError(shell, persistString + " already exists",
					persistString + " " + getName(persist) + " already exists, it won't be moved to another module");
				return null;
			}
		}
		return location;
	}
}