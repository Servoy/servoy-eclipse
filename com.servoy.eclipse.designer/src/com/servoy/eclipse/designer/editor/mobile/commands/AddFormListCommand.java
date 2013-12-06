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

import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.swt.graphics.Point;

import com.servoy.base.persistence.constants.IFormConstants;
import com.servoy.eclipse.core.elements.ElementFactory;
import com.servoy.eclipse.designer.editor.commands.BaseFormPlaceElementCommand;
import com.servoy.eclipse.designer.editor.commands.FormElementDeleteCommand;
import com.servoy.eclipse.designer.property.SetValueCommand;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.ui.property.PersistPropertySource;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.debug.layout.MobileFormLayout;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportBounds;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.util.Utils;

/**
 * Command to modify the current form as a list form.
 * 
 * @author rgansevles
 *
 */
@SuppressWarnings("nls")
public class AddFormListCommand extends CompoundCommand
{
	public AddFormListCommand(IApplication application, Form form, Object requestType, Point defaultLocation)
	{
		if (form.getView() != IFormConstants.VIEW_TYPE_TABLE_LOCKED)
		{
			add(SetValueCommand.createSetvalueCommand(
				"",
				PersistPropertySource.createPersistPropertySource(form, false),
				StaticContentSpecLoader.PROPERTY_VIEW.getPropertyName(),
				PersistPropertySource.VIEW_TYPE_CONTOLLER.getConverter().convertProperty(StaticContentSpecLoader.PROPERTY_VIEW.getPropertyName(),
					Integer.valueOf(IFormConstants.VIEW_TYPE_TABLE_LOCKED))));

			// delete all form elements except header/footer
			FlattenedSolution editingFlattenedSolution = ModelUtils.getEditingFlattenedSolution(form);
			for (ISupportBounds elem : MobileFormLayout.getBodyElementsForRecordView(editingFlattenedSolution, editingFlattenedSolution.getFlattenedForm(form)))
			{
				if (elem instanceof IPersist)
				{
					add(new FormElementDeleteCommand((IPersist)elem));
				}
				else if (elem instanceof FormElementGroup)
				{
					for (IPersist persist : Utils.iterate(((FormElementGroup)elem).getElements()))
					{
						add(new FormElementDeleteCommand(persist));
					}
				}
			}

			// add list items
			add(new AddFormListemsCommand(application, form, requestType, defaultLocation));
		}
	}

	private static class AddFormListemsCommand extends BaseFormPlaceElementCommand
	{
		public AddFormListemsCommand(IApplication application, Form form, Object requestType, Point defaultLocation)
		{
			super(application, form, null, requestType, null, null, defaultLocation, null, form);
		}

		@Override
		protected Object[] placeElements(Point location) throws RepositoryException
		{
			if (parent instanceof Form)
			{
				// add items for properties
				return ElementFactory.addFormListItems((Form)parent, null, location);
			}

			return null;
		}

	}


}