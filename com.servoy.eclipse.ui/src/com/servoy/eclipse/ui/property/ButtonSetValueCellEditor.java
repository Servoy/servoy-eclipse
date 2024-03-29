/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2023 Servoy BV

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

package com.servoy.eclipse.ui.property;

import org.eclipse.swt.widgets.Composite;

/**
 * Helpful for building 1-button cell editors to set a value.
 *
 * @author rgansevles
 */
public abstract class ButtonSetValueCellEditor extends ButtonCellEditor
{
	// constructors similar to super
	public ButtonSetValueCellEditor()
	{
	}

	public ButtonSetValueCellEditor(Composite parent)
	{
		super(parent);
	}

	public ButtonSetValueCellEditor(Composite parent, int style)
	{
		super(parent, style);
	}

	/**
	 * Triggered when the button is clicked. You can do anything you'd like here.
	 * If you return oldValue then the returned value will not be set to the property. Else the returned value will be set
	 *
	 * @return
	 */
	protected abstract Object getValueToSetOnClick(Object oldPropertyValue);

	@Override
	protected void buttonClicked()
	{
		applyValue(getValueToSetOnClick(doGetValue()));
	}
}
