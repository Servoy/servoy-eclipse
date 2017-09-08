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
package com.servoy.eclipse.designer.editor;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import com.servoy.eclipse.ui.Activator;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ISupportName;

/**
 * Label provider for setting security checkboxes in form editor security page.
 *
 * @author lvostinar
 */

public class ElementSettingsLabelProvider extends LabelProvider implements ITableLabelProvider
{
	public static final Image TRUE_IMAGE = Activator.getDefault().loadImageFromBundle("check_on.png");
	public static final Image FALSE_IMAGE = Activator.getDefault().loadImageFromBundle("check_off.png");

	private final ElementSettingsModel model;

	public ElementSettingsLabelProvider(ElementSettingsModel model)
	{
		super();
		this.model = model;

	}

	public Image getColumnImage(Object element, int columnIndex)
	{
		if (columnIndex == VisualFormEditorSecurityPage.CI_VIEWABLE)
		{
			if (model.hasRight((IPersist)element, IRepository.VIEWABLE))
			{
				return TRUE_IMAGE;
			}
			else
			{
				return FALSE_IMAGE;
			}
		}
		else if (columnIndex == VisualFormEditorSecurityPage.CI_ACCESSABLE)
		{
			if (model.hasRight((IPersist)element, IRepository.ACCESSIBLE))
			{
				return TRUE_IMAGE;
			}
			else
			{
				return FALSE_IMAGE;
			}
		}
		else
		{
			return null;
		}
	}

	public String getColumnText(Object element, int columnIndex)
	{
		if (columnIndex == VisualFormEditorSecurityPage.CI_NAME)
		{
			String name = ((ISupportName)element).getName();
			if (((IPersist)element).getAncestor(IRepository.FORMS) != model.getForm())
			{
				name += " [" + ((Form)((IPersist)element).getAncestor(IRepository.FORMS)).getName() + "]";
			}
			return name;
		}
		return null;
	}
}
