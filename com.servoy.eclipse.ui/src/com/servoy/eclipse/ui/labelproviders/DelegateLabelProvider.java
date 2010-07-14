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


import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;

import com.servoy.j2db.util.IDelegate;

/**
 * Base delegate label provider.
 * 
 * @author rgansevles
 * 
 */
public abstract class DelegateLabelProvider implements ILabelProvider, IDelegate
{
	private final IBaseLabelProvider labelProvider;

	public DelegateLabelProvider(IBaseLabelProvider labelProvider)
	{
		this.labelProvider = labelProvider;
	}

	public IBaseLabelProvider getLabelProvider()
	{
		return labelProvider;
	}

	public void addListener(ILabelProviderListener listener)
	{
		labelProvider.addListener(listener);
	}

	public void dispose()
	{
		labelProvider.dispose();
	}

	public boolean isLabelProperty(Object element, String property)
	{
		return labelProvider.isLabelProperty(element, property);
	}

	public void removeListener(ILabelProviderListener listener)
	{
		labelProvider.removeListener(listener);
	}

	public Image getImage(Object element)
	{
		if (labelProvider instanceof ILabelProvider)
		{
			return ((ILabelProvider)labelProvider).getImage(element);
		}
		return null;
	}

	public String getText(Object element)
	{
		if (labelProvider instanceof ILabelProvider)
		{
			return ((ILabelProvider)labelProvider).getText(element);
		}
		return element == null ? "" : element.toString();
	}

	public Object getDelegate()
	{
		return labelProvider;
	}

}
