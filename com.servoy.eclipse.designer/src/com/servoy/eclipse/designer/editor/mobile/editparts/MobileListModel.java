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

package com.servoy.eclipse.designer.editor.mobile.editparts;

import java.awt.Dimension;
import java.awt.Point;

import com.servoy.base.persistence.IMobileProperties;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportBounds;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.ISupportSize;
import com.servoy.j2db.util.Utils;

/**
 * Model for list in mobile form editor.
 * This class holds all items used for the list.
 * 
 * @author rgansevles
 *
 */
public class MobileListModel implements ISupportBounds
{
	public final Form form;
	public final ISupportChilds component;
	public final GraphicalComponent header;
	public final GraphicalComponent button;
	public final GraphicalComponent subtext;
	public final Field countBubble;
	public final Field image;

	/**
	 * For Inset Lists
	 * @param tabPanel
	 * @param tab
	 * @param containedForm
	 * @param header
	 * @param button
	 * @param subtext
	 * @param countBubble
	 * @param image
	 */
	public MobileListModel(Form form, ISupportChilds component, GraphicalComponent header, GraphicalComponent button, GraphicalComponent subtext,
		Field countBubble, Field image)
	{
		this.form = form;
		this.component = component;
		this.header = header;
		this.button = button;
		this.subtext = subtext;
		this.countBubble = countBubble;
		this.image = image;
	}

	/**
	 * For Form Lists
	 * @param button
	 * @param subtext
	 * @param countBubble
	 * @param image
	 */
	public MobileListModel(Form form, GraphicalComponent button, GraphicalComponent subtext, Field countBubble, Field image)
	{
		this.form = form;
		this.component = null;
		this.header = null;
		this.button = button;
		this.subtext = subtext;
		this.countBubble = countBubble;
		this.image = image;
	}

	public void setSize(Dimension d)
	{
		if (component instanceof ISupportSize) ((ISupportSize)component).setSize(d);
	}

	public Dimension getSize()
	{
		return component instanceof ISupportSize ? ((ISupportSize)component).getSize() : null;
	}

	public void setLocation(Point p)
	{
		if (component instanceof ISupportBounds) ((ISupportBounds)component).setLocation(p);
	}

	public Point getLocation()
	{
		return component instanceof ISupportBounds ? ((ISupportBounds)component).getLocation() : null;
	}

	public static MobileListModel create(Form form, ISupportChilds parent)
	{
		GraphicalComponent header = null;
		GraphicalComponent button = null;
		GraphicalComponent subtext = null;
		Field countBubble = null;
		Field image = null;
		for (IPersist elem : Utils.iterate(parent.getAllObjects()))
		{
			if (elem instanceof GraphicalComponent &&
				((GraphicalComponent)elem).getCustomMobileProperty(IMobileProperties.LIST_ITEM_HEADER.propertyName) != null)
			{
				header = (GraphicalComponent)elem;
			}
			else if (elem instanceof GraphicalComponent &&
				((GraphicalComponent)elem).getCustomMobileProperty(IMobileProperties.LIST_ITEM_BUTTON.propertyName) != null)
			{
				button = (GraphicalComponent)elem;
			}
			else if (elem instanceof GraphicalComponent &&
				((GraphicalComponent)elem).getCustomMobileProperty(IMobileProperties.LIST_ITEM_SUBTEXT.propertyName) != null)
			{
				subtext = (GraphicalComponent)elem;
			}
			else if (elem instanceof Field && ((Field)elem).getCustomMobileProperty(IMobileProperties.LIST_ITEM_COUNT.propertyName) != null)
			{
				countBubble = (Field)elem;
			}
			else if (elem instanceof Field && ((Field)elem).getCustomMobileProperty(IMobileProperties.LIST_ITEM_IMAGE.propertyName) != null)
			{
				image = (Field)elem;
			}
		}

		return new MobileListModel(form, parent instanceof Form ? null : parent, header, button, subtext, countBubble, image);
	}
}
