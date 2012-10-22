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
package com.servoy.eclipse.designer.editor.commands;

import java.beans.BeanInfo;
import java.util.Map;

import org.eclipse.gef.commands.Command;
import org.eclipse.swt.graphics.Point;

import com.servoy.eclipse.core.elements.ElementFactory;
import com.servoy.eclipse.core.elements.IFieldPositioner;
import com.servoy.eclipse.core.util.TemplateElementHolder;
import com.servoy.eclipse.designer.editor.BaseVisualFormEditor.RequestType;
import com.servoy.eclipse.designer.editor.VisualFormEditor;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.ISupportFormElements;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.TabPanel;

/**
 * Command to place an element in the form designer.
 * 
 * @author rgansevles
 */

@SuppressWarnings("nls")
public class FormPlaceElementCommand extends BaseFormPlaceElementCommand
{
	/**
	 * Command to add a field.
	 * 
	 * @param parent
	 * @param location
	 * @param object
	 * @param size 
	 * @param
	 */
	public FormPlaceElementCommand(IApplication application, ISupportChilds parent, Object object, Object requestType, Map<Object, Object> objectProperties,
		IFieldPositioner fieldPositioner, Point defaultLocation, org.eclipse.draw2d.geometry.Dimension size, IPersist context)
	{
		super(application, parent, object, requestType, objectProperties, fieldPositioner, defaultLocation, size, context);
	}

	@Override
	protected Object[] placeElements(Point location) throws RepositoryException
	{
		return placeElements(this, application, parent, object, requestType, objectProperties, location);
	}

	public static Object[] placeElements(Command command, IApplication application, ISupportChilds parent, Object object, Object requestType,
		Map<Object, Object> objectProperties, Point location) throws RepositoryException
	{
		if (requestType instanceof RequestType)
		{
			if (((RequestType)requestType).type == RequestType.TYPE_TAB)
			{
				command.setLabel("place tabpanel");
				return ElementFactory.createTabs(application, parent, (Object[])object, location, TabPanel.DEFAULT, objectProperties == null ? null
					: (String)objectProperties.get(ElementFactory.NAME_HINT_PROPERTY));
			}

			if (parent instanceof ISupportFormElements)
			{
				if (((RequestType)requestType).type == RequestType.TYPE_BUTTON)
				{
					command.setLabel("place button");
					return toArrAy(ElementFactory.createButton((ISupportFormElements)parent, null, (object instanceof String) ? (String)object : "button",
						location));
				}
			}
		}

		if (parent instanceof Form && VisualFormEditor.REQ_PLACE_BEAN.equals(requestType))
		{
			command.setLabel("place bean");
			String beanClassname;
			if (object instanceof BeanInfo)
			{
				beanClassname = ((BeanInfo)object).getBeanDescriptor().getBeanClass().getName();
			}
			else
			{
				beanClassname = (String)object;
			}

			return toArrAy(ElementFactory.createBean((Form)parent, beanClassname, location));
		}

		if (parent instanceof ISupportFormElements)
		{
			if (VisualFormEditor.REQ_PLACE_MEDIA.equals(requestType))
			{
				command.setLabel("place image");
				return toArrAy(ElementFactory.createImage((ISupportFormElements)parent, (Media)object, location));
			}

			if (VisualFormEditor.REQ_PLACE_LABEL.equals(requestType) || object instanceof String)
			{
				command.setLabel("place label");
				return toArrAy(ElementFactory.createLabel((ISupportFormElements)parent, (object instanceof String) ? (String)object : "type", location));
			}

			if (VisualFormEditor.REQ_PLACE_RECT_SHAPE.equals(requestType))
			{
				command.setLabel("place shape");
				return toArrAy(ElementFactory.createRectShape((ISupportFormElements)parent, location));
			}
		}

		if (parent instanceof Form && VisualFormEditor.REQ_PLACE_TEMPLATE.equals(requestType))
		{
			command.setLabel("place template");
			return ElementFactory.applyTemplate((Form)parent, (TemplateElementHolder)object, location, false);
		}

		return null;
	}
}
