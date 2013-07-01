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
import org.eclipse.gef.requests.CreateRequest;
import org.eclipse.swt.graphics.Point;

import com.servoy.base.persistence.IMobileProperties;
import com.servoy.eclipse.core.elements.ElementFactory;
import com.servoy.eclipse.designer.editor.commands.BaseFormPlaceElementCommand;
import com.servoy.eclipse.designer.property.SetValueCommand;
import com.servoy.eclipse.ui.property.MobileListModel;
import com.servoy.eclipse.ui.property.PersistPropertySource;
import com.servoy.j2db.FormController;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.ISupportFormElements;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.StaticContentSpecLoader;

/**
 * Command to modify the current form as a list form.
 * 
 * @author rgansevles
 *
 */
@SuppressWarnings("nls")
public class AddFormListCommand extends CompoundCommand
{
	public AddFormListCommand(IApplication application, Form form, CreateRequest request)
	{
		add(SetValueCommand.createSetvalueCommand(
			"",
			PersistPropertySource.createPersistPropertySource(form, false),
			StaticContentSpecLoader.PROPERTY_VIEW.getPropertyName(),
			PersistPropertySource.VIEW_TYPE_CONTOLLER.getConverter().convertProperty(StaticContentSpecLoader.PROPERTY_VIEW.getPropertyName(),
				Integer.valueOf(FormController.LOCKED_TABLE_VIEW))));
		add(new AddFormListemsCommand(application, form, request));
	}

	private static class AddFormListemsCommand extends BaseFormPlaceElementCommand
	{
		public AddFormListemsCommand(IApplication application, Form form, CreateRequest request)
		{
			super(application, form, null, request.getType(), null, null, request.getLocation().getSWTPoint(), null, form);
		}

		@Override
		protected Object[] placeElements(Point location) throws RepositoryException
		{
			if (parent instanceof Form)
			{
				// add items for properties
				MobileListModel model = addlistItems((Form)parent, null, location);
				return new Object[] { model.button, model.subtext, model.countBubble, model.image };
			}

			return null;
		}
	}

	public static MobileListModel addlistItems(Form form, ISupportFormElements component, Point location) throws RepositoryException
	{
		int x = 0;
		int y = 0;
		if (location != null)
		{
			x = location.x;
			y = location.y;
		}
		ISupportFormElements parent = component == null ? form : component;

		// image
		Field image = ElementFactory.createField(parent, null, new Point(x + 0, y + 10));
		image.putCustomMobileProperty(IMobileProperties.LIST_ITEM_IMAGE.propertyName, Boolean.TRUE);
		image.setEditable(false); // for debug in developer

		// button
		GraphicalComponent button = ElementFactory.createButton(parent, null, "list", new Point(x + 10, y + 20));
		button.setDisplaysTags(false);
		button.putCustomMobileProperty(IMobileProperties.LIST_ITEM_BUTTON.propertyName, Boolean.TRUE);

		// subtext
		GraphicalComponent subtext = ElementFactory.createLabel(parent, null, new Point(x + 20, y + 30));
		subtext.setDisplaysTags(false);
		subtext.putCustomMobileProperty(IMobileProperties.LIST_ITEM_SUBTEXT.propertyName, Boolean.TRUE);

		// countBubble
		Field countBubble = ElementFactory.createField(parent, null, new Point(x + 40, y + 40));
		countBubble.putCustomMobileProperty(IMobileProperties.LIST_ITEM_COUNT.propertyName, Boolean.TRUE);
		countBubble.setEditable(false); // for debug in developer

		return new MobileListModel(form, button, subtext, countBubble, image);
	}
}