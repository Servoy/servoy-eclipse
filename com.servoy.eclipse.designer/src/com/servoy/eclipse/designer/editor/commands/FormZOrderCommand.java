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

import java.awt.Dimension;

import com.servoy.eclipse.designer.editor.BaseRestorableCommand;
import com.servoy.eclipse.designer.editor.VisualFormEditor;
import com.servoy.eclipse.ui.property.PersistPropertySource;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.StaticContentSpecLoader;

/**
 * Command to change the z-ordering (stacking order) of elements in the form designer.
 * 
 * @author rgansevles
 */

public class FormZOrderCommand extends BaseRestorableCommand implements ISupportModels
{
	private final Object requestType;
	private final Object[] models;
	private final Form form;

	/**
	 * Command to change the z-order of a number of persists.
	 * 
	 * @param parent
	 * @param location
	 * @param object
	 */
	public FormZOrderCommand(Object requestType, Form form, IPersist[] models)
	{
		super(determineLabel(requestType));
		this.requestType = requestType;
		this.form = form;
		this.models = models;
	}

	public Object[] getModels()
	{
		return models;
	}

	protected static String determineLabel(Object requestType)
	{
		if (VisualFormEditor.REQ_BRING_TO_FRONT.equals(requestType))
		{
			return "bring to front";
		}
		if (VisualFormEditor.REQ_SEND_TO_BACK.equals(requestType))
		{
			return "send to back";
		}
		return null;
	}

	@Override
	public void execute()
	{

		Dimension min_max = form.getMinMaxUsedFormIndex();
		int indexFirstToUse = 0;
		if (VisualFormEditor.REQ_SEND_TO_BACK.equals(requestType))
		{
			indexFirstToUse = min_max.width - 1;//==min
		}
		else if (VisualFormEditor.REQ_BRING_TO_FRONT.equals(requestType))
		{
			indexFirstToUse = min_max.height + 1;//==max
		}

		for (int i = 0; i < models.length; i++)
		{
			if (models[i] instanceof IFormElement)
			{
				IFormElement formElement = (IFormElement)models[i];

				int formIndex;
				if (VisualFormEditor.REQ_SEND_TO_BACK.equals(requestType))
				{
					formIndex = indexFirstToUse - i;
				}
				else if (VisualFormEditor.REQ_BRING_TO_FRONT.equals(requestType))
				{
					formIndex = indexFirstToUse + i;
				}
				else
				{
					continue;
				}
				setPropertyValue(new PersistPropertySource((IPersist)formElement, form, false), StaticContentSpecLoader.PROPERTY_FORMINDEX.getPropertyName(),
					new Integer(formIndex));
			}
		}
	}
}
