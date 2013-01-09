/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

package com.servoy.eclipse.designer.editor.mobile.commands;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CompoundCommand;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.designer.editor.mobile.editparts.MobileListModel;
import com.servoy.eclipse.designer.property.SetValueCommand;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.property.PersistPropertySource;
import com.servoy.j2db.IForm;
import com.servoy.j2db.persistence.IDeveloperRepository;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.StaticContentSpecLoader;

/**
 * Command to delete inset list (tabpanel) and containing form.
 * 
 * @author rgansevles
 *
 */
public class DeleteListCommand extends CompoundCommand
{
	public DeleteListCommand(MobileListModel model)
	{
		if (model.component == null)
		{
			// form list, set view type back to record view
			add(SetValueCommand.createSetvalueCommand(
				"",
				PersistPropertySource.createPersistPropertySource(model.form, false),
				StaticContentSpecLoader.PROPERTY_VIEW.getPropertyName(),
				PersistPropertySource.VIEW_TYPE_CONTOLLER.getConverter().convertProperty(StaticContentSpecLoader.PROPERTY_VIEW.getPropertyName(),
					Integer.valueOf(IForm.LOCKED_RECORD_VIEW))));
		}
		add(new DeleteListItemsCommand(model));
	}

	public static class DeleteListItemsCommand extends Command
	{
		private final MobileListModel model;

		/**
		 * @param model
		 */
		public DeleteListItemsCommand(MobileListModel model)
		{
			this.model = model;
			setLabel("delete list");
		}

		@Override
		public void execute()
		{
			IDeveloperRepository repository = (IDeveloperRepository)model.form.getRootObject().getRepository();

			List<IPersist> changes = new ArrayList<IPersist>(2);

			try
			{
				if (model.component != null)
				{
					// inset lists
					repository.deleteObject(model.component);
					changes.add(model.component);
				}
				else
				{
					// form list
					if (model.button != null)
					{
						repository.deleteObject(model.button);
						changes.add(model.button);
					}
					if (model.header != null)
					{
						repository.deleteObject(model.header);
						changes.add(model.header);
					}
					if (model.subtext != null)
					{
						repository.deleteObject(model.subtext);
						changes.add(model.subtext);
					}
					if (model.countBubble != null)
					{
						repository.deleteObject(model.countBubble);
						changes.add(model.countBubble);
					}
					if (model.image != null)
					{
						repository.deleteObject(model.image);
						changes.add(model.image);
					}
				}
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError("Could not delete list", e);
			}

			ServoyModelManager.getServoyModelManager().getServoyModel().firePersistsChanged(false, changes);
		}

		@Override
		public void undo()
		{
			IDeveloperRepository repository = (IDeveloperRepository)model.form.getRootObject().getRepository();
			List<IPersist> changes = new ArrayList<IPersist>(2);
			try
			{
				if (model.component != null)
				{
					// inset list
					repository.undeleteObject(model.form, model.component);
					changes.add(model.component);
				}
				else
				{
					// form list
					if (model.header != null)
					{
						repository.undeleteObject(model.form, model.header);
						changes.add(model.header);
					}
					if (model.button != null)
					{
						repository.undeleteObject(model.form, model.button);
						changes.add(model.button);
					}
					if (model.subtext != null)
					{
						repository.undeleteObject(model.form, model.subtext);
						changes.add(model.subtext);
					}
					if (model.countBubble != null)
					{
						repository.undeleteObject(model.form, model.countBubble);
						changes.add(model.countBubble);
					}
					if (model.image != null)
					{
						repository.undeleteObject(model.form, model.image);
						changes.add(model.image);
					}
				}
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError("Could not restore list", e);
			}

			ServoyModelManager.getServoyModelManager().getServoyModel().firePersistsChanged(false, changes);
		}
	}
}
