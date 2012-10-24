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

import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportBounds;
import com.servoy.j2db.persistence.Tab;
import com.servoy.j2db.persistence.TabPanel;

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
	public final TabPanel tabPanel;
	public final Tab tab;
	public final Form containedForm;
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
	public MobileListModel(Form form, TabPanel tabPanel, Tab tab, Form containedForm, GraphicalComponent header, GraphicalComponent button,
		GraphicalComponent subtext, Field countBubble, Field image)
	{
		this.form = form;
		this.tabPanel = tabPanel;
		this.tab = tab;
		this.containedForm = containedForm;
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
		this.tabPanel = null;
		this.tab = null;
		this.containedForm = null;
		this.header = null;
		this.button = button;
		this.subtext = subtext;
		this.countBubble = countBubble;
		this.image = image;
	}

	public void setSize(Dimension d)
	{
		if (tabPanel != null) tabPanel.setSize(d);
	}

	public Dimension getSize()
	{
		return tabPanel == null ? null : tabPanel.getSize();
	}

	public void setLocation(Point p)
	{
		if (tabPanel != null) tabPanel.setLocation(p);
	}

	public Point getLocation()
	{
		return tabPanel == null ? null : tabPanel.getLocation();
	}

	public static MobileListModel create(IApplication application, Form form, TabPanel tabPanel, Tab tab, Form containedForm)
	{
		GraphicalComponent header = null;
		GraphicalComponent button = null;
		GraphicalComponent subtext = null;
		Field countBubble = null;
		Field image = null;
		for (IPersist elem : application.getFlattenedSolution().getFlattenedForm(containedForm).getAllObjectsAsList())
		{
			if (elem instanceof GraphicalComponent && ((GraphicalComponent)elem).getCustomMobileProperty("listitemHeader") != null)
			{
				header = (GraphicalComponent)elem;
			}
			else if (elem instanceof GraphicalComponent && ((GraphicalComponent)elem).getCustomMobileProperty("listitemButton") != null)
			{
				button = (GraphicalComponent)elem;
			}
			else if (elem instanceof GraphicalComponent && ((GraphicalComponent)elem).getCustomMobileProperty("listitemSubtext") != null)
			{
				subtext = (GraphicalComponent)elem;
			}
			else if (elem instanceof Field && ((Field)elem).getCustomMobileProperty("listitemCount") != null)
			{
				countBubble = (Field)elem;
			}
			else if (elem instanceof Field && ((Field)elem).getCustomMobileProperty("listitemImage") != null)
			{
				image = (Field)elem;
			}
		}

		return new MobileListModel(form, tabPanel, tab, containedForm, header, button, subtext, countBubble, image);

	}
}
