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
package com.servoy.eclipse.ui.labelproviders;


import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;

import com.servoy.eclipse.ui.Messages;
import com.servoy.eclipse.ui.property.PersistPropertySource;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.util.Utils;

/**
 * Delegate label provider that adds the form inheritance context to the label.
 * 
 * @author rob
 * 
 */
public class FormInheritenceDelegateLabelProvider extends DelegateLabelProvider implements IFontProvider
{
	//public static final Image INHERITED_IMAGE = Activator.loadImageDescriptorFromBundle("inherited.gif").createImage();

	private final Form form;
	private final Object propertyId;

	public FormInheritenceDelegateLabelProvider(Form form, ILabelProvider labelProvider, Object propertyId)
	{
		super(labelProvider);
		this.form = form;
		this.propertyId = propertyId;
	}

	/**
	 * Mark the entry with an inherited image hen there is no image yet
	 */
	@Override
	public Image getImage(Object value)
	{
		Image image = super.getImage(value);
//		if (image == null && form != null && form.getExtendsFormID() > 0)
//		{
//			Object inheritedValue = new PersistProperties(form, null, true).getInheritedPropertyValue(value, propertyId);
//			if (!Utils.equalObjects(value, inheritedValue))
//			{
//				return INHERITED_IMAGE;
//			}
//		}
		return image;
	}

	/**
	 * show the actually used value per form inheritance.
	 */
	@Override
	public String getText(Object value)
	{
		if (form != null && form.getExtendsFormID() > 0)
		{
			Object inheritedValue = new PersistPropertySource(form, null, true).getInheritedPropertyValue(value, propertyId);
			if (!Utils.equalObjects(value, inheritedValue))
			{
				return super.getText(inheritedValue) + " (" + Messages.LabelInherited + ')';
			}
		}

		return super.getText(value);
	}

	/**
	 * @see IFontProvider
	 * 
	 */
	public Font getFont(Object value)
	{
		if (getLabelProvider() instanceof IFontProvider)
		{
			return ((IFontProvider)getLabelProvider()).getFont(value);
		}
		return null;
	}

}
