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
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ExpandBar;
import org.eclipse.swt.widgets.ExpandItem;

/**
 * An expand bar wrapper that is able to take expanded content's width into account as well when computing default/desired size.
 *
 * @author acostescu
 */
public class ExpandBarWidthAware extends CompositeWithCalculatedPreferredWidth<ExpandBar>
{

	public ExpandBarWidthAware(Composite parent, int style, int expandBarStyle)
	{
		super(parent, style);
		createWrappedControl(expandBarStyle);
	}

	protected int getExpandChildWidth(ExpandItem item, boolean changed)
	{
		Control child = item.getControl();
		return child != null ? child.computeSize(SWT.DEFAULT, SWT.DEFAULT, changed).x : -1;
	}

	protected void createWrappedControl(int expandBarStyle)
	{
		wrapControl(new ExpandBar(this, expandBarStyle));
	}

	@Override
	protected Point adjustComputedPreferredSize(ExpandBar expandBar, boolean changed)
	{
		Point childReportedPreferredSize = expandBar.computeSize(SWT.DEFAULT, SWT.DEFAULT, changed);

		// expand bar ignores the content of expanded items - we need that as well for scrolling parent's content without scolling horizontally inside each expanded item
		int width = childReportedPreferredSize.x; // super computes only text and icon for expanding line, not the expanded content; to this is the max of the expanding lines
		// we must check to see if any of the expanded items needs larger width and use that instead; otherwise super value is good.

		for (int i = 0; i < expandBar.getItemCount(); i++)
		{
			ExpandItem item = expandBar.getItem(i);
			if (item.getExpanded()) width = Math.max(width, getExpandChildWidth(item, changed));
		}

		Rectangle trim = computeTrim(0, 0, width, childReportedPreferredSize.y);
		return new Point(trim.width, childReportedPreferredSize.y);
	}

}