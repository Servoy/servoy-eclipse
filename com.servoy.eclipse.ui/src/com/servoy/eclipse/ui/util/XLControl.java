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
 * An wrapper around a control that will always report extra-width and extra-height as preferred size (so for computeSize(SWT.DEFAULT, SWT.DEFAULT, ...)).<br/><br/>
 *
 * Useful if you want controls to end up occupying more space then they need in some layouts. Or for working around SWT native integration bugs that report one size but then they wrap content
 * when being given that size.
 *
 * @author acostescu
 */
public class XLControl<CT extends Control> extends CompositeWithCalculatedPreferredWidth<CT>
{

	protected int extraWidth = 0;
	protected int extraHeight = 0;

	/**
	 * The caller Should create the (child) wrapped control and call {@link #wrapControl(Control)} with it.
	 */
	public XLControl(Composite parent, int style)
	{
		super(parent, style);
	}

	public void setExtraWidth(int extraWidth)
	{
		this.extraWidth = extraWidth;
	}

	public void setExtraHeight(int extraHeight)
	{
		this.extraHeight = extraHeight;
	}

	@Override
	public void wrapControl(CT c)
	{
		super.wrapControl(c);
	}

	@Override
	protected Point adjustComputedPreferredSize(CT wrappedControl, boolean changed)
	{
		Point normalPreferred = wrappedControl.computeSize(SWT.DEFAULT, SWT.DEFAULT, changed);
		return new Point(normalPreferred.x + extraWidth, normalPreferred.y + extraHeight); // workaround to make a label inside a ScrolledComposite with setExpandHorizontal(true) be able to wrap to the width of the other components in that composite
	}

}