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


import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;

import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;

/**
 * Delegate label provider that adds the form context to the label.
 * 
 * @author rgansevles
 * 
 */
public class FormContextDelegateLabelProvider extends DelegateLabelProvider implements IFontProvider, IColorProvider, IPersistLabelProvider
{
	private final IPersist context;

	public FormContextDelegateLabelProvider(IPersistLabelProvider labelProvider, IPersist context)
	{
		super(labelProvider);
		this.context = context;
	}

	@Override
	public String getText(Object value)
	{
		String baseText = super.getText(value);
		if (context != null && value != null)
		{
			IPersist persist = getPersist(value);
			if (persist != null && persist.getParent() instanceof Form)
			{
				// form method
				Form contextForm = (Form)context.getAncestor(IRepository.FORMS);
				if (contextForm != null && !persist.getParent().getUUID().equals(contextForm.getUUID()))
				{
					return baseText + " [" + ((Form)persist.getParent()).getName() + ']';
				}
			}
		}

		return baseText;
	}

	/**
	 * @see IFontProvider
	 * 
	 */
	public Font getFont(Object element)
	{
		if (getLabelProvider() instanceof IFontProvider)
		{
			return ((IFontProvider)getLabelProvider()).getFont(element);
		}
		return null;
	}


	public Color getBackground(Object element)
	{
		if (getLabelProvider() instanceof IColorProvider)
		{
			return ((IColorProvider)getLabelProvider()).getBackground(element);
		}
		return null;
	}

	public Color getForeground(Object element)
	{
		if (getLabelProvider() instanceof IColorProvider)
		{
			return ((IColorProvider)getLabelProvider()).getForeground(element);
		}
		return null;
	}

	public IPersist getPersist(Object value)
	{
		return ((IPersistLabelProvider)getLabelProvider()).getPersist(value);
	}

}
