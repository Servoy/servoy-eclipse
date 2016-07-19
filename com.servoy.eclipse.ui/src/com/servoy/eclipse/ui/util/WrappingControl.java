/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2016 Servoy BV

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

package com.servoy.eclipse.ui.util;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * An wrapper around a control that can wrap content such as label; it makes the child not have a preferred width, wrapping to whatever width the layout gives it.<br/>
 * It is useful when you need labels or other controls with large text not to expand a layout when using in a ScrolledComposite - but wrap according to the other existing components in the layout instead.
 *
 * @author acostescu
 */
public class WrappingControl<CT extends Control> extends CompositeWithCalculatedPreferredWidth<CT>
{

	protected int minWidth = 0;

	/**
	 * The caller Should create the (child) wrapped control and call {@link #wrapControl(Control)} with it.
	 */
	public WrappingControl(Composite parent, int style)
	{
		super(parent, style);
	}

	public void setMinWidth(int minWidth)
	{
		this.minWidth = minWidth;
	}

	@Override
	public void wrapControl(CT c)
	{
		super.wrapControl(c);
	}

	@Override
	protected Point adjustComputedPreferredSize(CT wrappedControl, boolean changed)
	{
		return new Point(minWidth, wrappedControl.computeSize(minWidth > 0 ? minWidth : SWT.DEFAULT, SWT.DEFAULT, changed).y); // workaround to make a label inside a ScrolledComposite with setExpandHorizontal(true) be able to wrap to the width of the other components in that composite
	}

}