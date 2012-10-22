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

import org.eclipse.gef.commands.Command;
import org.eclipse.swt.graphics.Point;

import com.servoy.eclipse.designer.editor.BaseVisualFormEditor.RequestType;
import com.servoy.eclipse.designer.editor.commands.ISupportModels;
import com.servoy.j2db.persistence.Form;

/**
 * RAGTEST doc
 * @author rgansevles
 *
 */
public class MobileAddFooterItemCommand extends Command implements ISupportModels
{
	private final Form form;
	private final RequestType requestType;

	protected Object[] models;


	public MobileAddFooterItemCommand(Form form, RequestType requestType)
	{
		this.form = form;
		this.requestType = requestType;
	}

	public Object[] getModels()
	{
		return models;
	}

	@Override
	public void execute()
	{
		models = null;
//		try
//		{
		Point location; // RAGTEST 
//			models = FormPlaceElementCommand.placeElements(this, null, form, null, requestType, null, location);

		if (models != null)
		{
			for (Object model : models)
			{

			}
		}

//			GraphicalComponent button = ElementFactory.createButton(form, null, snapType == MobileSnapType.HeaderLeftButton ? "left" : "right", null);
//			button.putCustomMobileProperty("headeritem", Boolean.TRUE);
//			button.putCustomMobileProperty(snapType == MobileSnapType.HeaderLeftButton ? "headerLeftButton" : "headerRightButton", Boolean.TRUE);

//			models = new IPersist[] { button };


//			if (parent instanceof ISupportFormElements)
//			{
//				if (((RequestType)requestType).type == RequestType.TYPE_BUTTON)
//				{
//					setLabel("place button");
//					models = new IPersist[] { ElementFactory.createButton((ISupportFormElements)parent, null, "button", null) };
//				}
//			}

//			models = placeElements(getNextLocation());
//			// set data in request.getExtendedData map as properties in the created persists
//			if (models != null)
//			{
//				if (size != null)
//				{
//					// resize all created models relative to the current bounding box
//					applySizeToModels(size, models);
//				}
//				for (Object model : models)
//				{
//					if (objectProperties != null && objectProperties.size() > 0)
//					{
//						Command setPropertiesCommand = SetValueCommand.createSetPropertiesComnmand(
//							(IPropertySource)Platform.getAdapterManager().getAdapter(model, IPropertySource.class), objectProperties);
//						if (setPropertiesCommand != null)//FIXME: why size?
//						{
//							setPropertiesCommand.execute();
//						}
//					}
//					ServoyModelManager.getServoyModelManager().getServoyModel().firePersistChanged(false, model, true);
//				}
//			}
//		}
//		catch (RepositoryException ex)
//		{
//			ServoyLog.logError(ex);
//		}
	}
}
