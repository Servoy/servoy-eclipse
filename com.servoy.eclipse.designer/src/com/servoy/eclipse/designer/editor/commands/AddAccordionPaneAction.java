/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2011 Servoy BV

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

import org.eclipse.gef.EditPart;
import org.eclipse.gef.Request;
import org.eclipse.ui.IWorkbenchPart;

import com.servoy.eclipse.designer.property.SetValueCommand;
import com.servoy.eclipse.ui.property.PersistPropertyHandler;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.persistence.TabPanel;
import com.servoy.j2db.server.ngclient.property.PropertyType;

/**
 * @author lvostinar
 *
 */
public class AddAccordionPaneAction extends AddTabpanelAction
{
	/**
	 * @param part
	 */
	public AddAccordionPaneAction(IWorkbenchPart part)
	{
		super(part);
	}

	@Override
	public Request createRequest(EditPart editPart)
	{
		return addSetPropertyValue(
			super.createRequest(editPart),
			StaticContentSpecLoader.PROPERTY_TABORIENTATION.getPropertyName(),
			Integer.valueOf(((PropertyType.ValuesConfig)PersistPropertyHandler.TAB_ORIENTATION_VALUES.getConfig()).getRealIndexOf(Integer.valueOf(TabPanel.ACCORDION_PANEL))));
	}

	/**
	 * @param request
	 * @param propertyName
	 * @param convertProperty
	 */
	protected Request addSetPropertyValue(Request request, String key, Object value)
	{
		if (request != null)
		{
			request.getExtendedData().put(SetValueCommand.REQUEST_PROPERTY_PREFIX + key, value);
		}
		return request;
	}

	@Override
	protected String getDialogTitle()
	{
		return "Select accordion panel form";
	}

	/**
	 * Initializes this action's text and images.
	 */
	@Override
	protected void init()
	{
		super.init();
		setText(DesignerActionFactory.ADD_ACCORDION_TEXT);
		setToolTipText(DesignerActionFactory.ADD_ACCORDION_TOOLTIP);
		setId(DesignerActionFactory.ADD_ACCORDION.getId());
		setImageDescriptor(DesignerActionFactory.ADD_ACCORDION_IMAGE);
	}
}