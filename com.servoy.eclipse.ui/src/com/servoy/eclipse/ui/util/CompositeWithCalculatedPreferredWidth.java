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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * A composite that is basically just a wrapper around a child control with the sole purpose
 * of overriding what computeWidth returns for (SWT.DEFAULT, SWT.DEFAULT).<br/><br/>
 *
 * This is helpful because many controls don't allow sub-classing in order to achieve that but
 * they do not provide correct prefferred width/height or they cannot easily be made to adapt to
 * other existing controls in some layouts (for example GridLayout).
 *
 * @author acostescu
 */
public abstract class CompositeWithCalculatedPreferredWidth<CT extends Control> extends Composite
{

	/**
	 * Creates a new wrapper for changing preferred sizes. The extending class must make sure that the child component is
	 * created for this composite and must call {@link #wrapControl(Control)} on it.
	 */
	public CompositeWithCalculatedPreferredWidth(Composite parent, int style)
	{
		super(parent, style);
	}

	/**
	 * Sets up the layout for wrapping the given child component.
	 */
	protected void wrapControl(CT c)
	{
		GridLayout gridLayout = new GridLayout(1, false);
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		gridLayout.verticalSpacing = 0;
		gridLayout.horizontalSpacing = 0;
		setLayout(gridLayout);

		c.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	}

	@Override
	public Point computeSize(int wHint, int hHint, boolean changed)
	{
		CT child = getWrappedControl();

		Point adjustedComputedSize;
		if (wHint == SWT.DEFAULT && hHint == SWT.DEFAULT)
		{
			adjustedComputedSize = adjustComputedPreferredSize(child, changed);
		}
		else adjustedComputedSize = super.computeSize(wHint, hHint, changed);

		return adjustedComputedSize;
	}

	public CT getWrappedControl()
	{
		Control[] children = getChildren();
		if (children == null || children.length != 1) throw new IllegalStateException("CompositeWithCalculatedPreferredWidth must have exactly one child");
		return (CT)children[0];
	}

	protected abstract Point adjustComputedPreferredSize(CT child, boolean changed);

}
