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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.servoy.base.persistence.IMobileProperties;
import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.ISupportFormElement;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.util.Utils;

/**
 * Edit part for footer in mobile form editor.
 * 
 * @author rgansevles
 *
 */
public class MobileFooterGraphicalEditPart extends MobilePartGraphicalEditPart
{
	public MobileFooterGraphicalEditPart(IApplication application, BaseVisualFormEditor editorPart, Part model)
	{
		super(application, editorPart, model);
	}

	@Override
	protected List<ISupportFormElement> getModelChildren()
	{
		return getFooterModelChildren(application, getEditorPart().getForm());
	}

	public static List<ISupportFormElement> getFooterModelChildren(IApplication application, Form form)
	{
		List<ISupportFormElement> list = new ArrayList<ISupportFormElement>(5);

		for (ISupportFormElement persist : Utils
			.iterate(new Form.FormTypeIterator(application.getFlattenedSolution().getFlattenedForm(form).getAllObjectsAsList(),
				new Comparator<ISupportFormElement>()
				{
					// sort elements by x position
					public int compare(ISupportFormElement element1, ISupportFormElement element2)
					{
						return element1.getLocation().x - element2.getLocation().x;
					}
				})))
		{
			if (persist instanceof AbstractBase && ((AbstractBase)persist).getCustomMobileProperty(IMobileProperties.FOOTER_ITEM.propertyName) != null)
			{
				list.add(persist);
			}
		}

		return list;
	}

}
