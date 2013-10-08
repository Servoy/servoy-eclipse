/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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


import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.DecoratingStyledCellLabelProvider;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

/** Delegate label provider for DecoratingStyledCellLabelProviders.
 * 
 * @author rgansevles
 *
 */
public class DelegatingDecoratingStyledCellLabelProvider extends DecoratingStyledCellLabelProvider implements ILabelProvider, IFontProvider
{
	private final IBaseLabelProvider labelProvider;

	/**
	 * @param labelProvider
	 */
	public DelegatingDecoratingStyledCellLabelProvider(IBaseLabelProvider labelProvider)
	{
		super(labelProvider instanceof ILabelProvider && labelProvider instanceof IStyledLabelProvider ? (IStyledLabelProvider)labelProvider
			: new DelegateLabelProvider(labelProvider), null, null);
		this.labelProvider = labelProvider;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.DecoratingStyledCellLabelProvider#getBackground(java.lang.Object)
	 */
	@Override
	public Color getBackground(Object element)
	{
		if (labelProvider instanceof DecoratingStyledCellLabelProvider)
		{
			return ((DecoratingStyledCellLabelProvider)labelProvider).getBackground(element);
		}
		return super.getBackground(element);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.DecoratingStyledCellLabelProvider#getForeground(java.lang.Object)
	 */
	@Override
	public Color getForeground(Object element)
	{
		if (labelProvider instanceof DecoratingStyledCellLabelProvider)
		{
			return ((DecoratingStyledCellLabelProvider)labelProvider).getForeground(element);
		}
		return super.getForeground(element);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.CellLabelProvider#getToolTipBackgroundColor(java.lang.Object)
	 */
	@Override
	public Color getToolTipBackgroundColor(Object object)
	{
		if (labelProvider instanceof CellLabelProvider)
		{
			return ((CellLabelProvider)labelProvider).getToolTipBackgroundColor(object);
		}
		return super.getToolTipBackgroundColor(object);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.CellLabelProvider#getToolTipDisplayDelayTime(java.lang.Object)
	 */
	@Override
	public int getToolTipDisplayDelayTime(Object object)
	{
		if (labelProvider instanceof CellLabelProvider)
		{
			return ((CellLabelProvider)labelProvider).getToolTipDisplayDelayTime(object);
		}
		return super.getToolTipDisplayDelayTime(object);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.CellLabelProvider#getToolTipFont(java.lang.Object)
	 */
	@Override
	public Font getToolTipFont(Object object)
	{
		if (labelProvider instanceof CellLabelProvider)
		{
			return ((CellLabelProvider)labelProvider).getToolTipFont(object);
		}
		return super.getToolTipFont(object);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.CellLabelProvider#getToolTipForegroundColor(java.lang.Object)
	 */
	@Override
	public Color getToolTipForegroundColor(Object object)
	{
		if (labelProvider instanceof CellLabelProvider)
		{
			return ((CellLabelProvider)labelProvider).getToolTipForegroundColor(object);
		}
		return super.getToolTipForegroundColor(object);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.CellLabelProvider#getToolTipImage(java.lang.Object)
	 */
	@Override
	public Image getToolTipImage(Object object)
	{
		if (labelProvider instanceof CellLabelProvider)
		{
			return ((CellLabelProvider)labelProvider).getToolTipImage(object);
		}
		return super.getToolTipImage(object);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.CellLabelProvider#getToolTipShift(java.lang.Object)
	 */
	@Override
	public Point getToolTipShift(Object object)
	{
		if (labelProvider instanceof CellLabelProvider)
		{
			return ((CellLabelProvider)labelProvider).getToolTipShift(object);
		}
		return super.getToolTipShift(object);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.CellLabelProvider#getToolTipStyle(java.lang.Object)
	 */
	@Override
	public int getToolTipStyle(Object object)
	{
		if (labelProvider instanceof CellLabelProvider)
		{
			return ((CellLabelProvider)labelProvider).getToolTipStyle(object);
		}
		return super.getToolTipStyle(object);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.CellLabelProvider#getToolTipText(java.lang.Object)
	 */
	@Override
	public String getToolTipText(Object element)
	{
		if (labelProvider instanceof CellLabelProvider)
		{
			return ((CellLabelProvider)labelProvider).getToolTipText(element);
		}
		return super.getToolTipText(element);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.CellLabelProvider#getToolTipTimeDisplayed(java.lang.Object)
	 */
	@Override
	public int getToolTipTimeDisplayed(Object object)
	{
		if (labelProvider instanceof CellLabelProvider)
		{
			return ((CellLabelProvider)labelProvider).getToolTipTimeDisplayed(object);
		}
		return super.getToolTipTimeDisplayed(object);
	}

	@Override
	public String getText(Object element)
	{
		return ((ILabelProvider)getStyledStringProvider()).getText(element);
	}
}
