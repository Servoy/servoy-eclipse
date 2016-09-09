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
import org.eclipse.jface.viewers.IToolTipProvider;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;

import com.servoy.j2db.persistence.IPersist;

/**
 * Base delegate label provider that use the context of a persist.
 *
 * @author rgansevles
 *
 */
public abstract class AbstractPersistContextDelegateLabelProvider extends DelegateLabelProvider
	implements IFontProvider, IColorProvider, IPersistLabelProvider, IToolTipProvider
{
	private final IPersist context;

	public AbstractPersistContextDelegateLabelProvider(IPersistLabelProvider labelProvider, IPersist context)
	{
		super(labelProvider);
		this.context = context;
	}

	/**
	 * @return the context
	 */
	public IPersist getContext()
	{
		return context;
	}

	/**
	 * @see IFontProvider
	 *
	 */
	@Override
	public Font getFont(Object element)
	{
		if (getLabelProvider() instanceof IFontProvider)
		{
			return ((IFontProvider)getLabelProvider()).getFont(element);
		}
		return null;
	}

	/**
	 * @see IColorProvider
	 *
	 */
	@Override
	public Color getBackground(Object element)
	{
		if (getLabelProvider() instanceof IColorProvider)
		{
			return ((IColorProvider)getLabelProvider()).getBackground(element);
		}
		return null;
	}

	/**
	 * @see IColorProvider
	 *
	 */
	@Override
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

	@Override
	public String getToolTipText(Object element)
	{
		if (getLabelProvider() instanceof IToolTipProvider)
		{
			return ((IToolTipProvider)getLabelProvider()).getToolTipText(element);
		}
		return null;
	}
}
