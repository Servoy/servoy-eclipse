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
package com.servoy.eclipse.ui.property;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.osgi.util.NLS;

import com.servoy.eclipse.ui.Messages;
import com.servoy.j2db.util.Utils;

/**
 * Simple combobox model with static real and display values.
 *
 * @author rgansevles
 */

public class ComboboxPropertyModel<T> implements IComboboxPropertyModel<T>
{
	private T[] real;
	private String[] display;
	private int defaultValueIndex = -1;

	/**
	 * Display and real values are the same.
	 *
	 * @param display
	 */
	public ComboboxPropertyModel(T[] real)
	{
		this(real, (ILabelProvider)null);
	}

	public ComboboxPropertyModel<T> addDefaultValue()
	{
		return addDefaultValue(null, null);
	}

	public ComboboxPropertyModel<T> addDefaultValue(T defaultReal)
	{
		return addDefaultValue(defaultReal, null);
	}

	public ComboboxPropertyModel<T> addDefaultValue(T defaultReal, String defaultDisplay)
	{
		// get the display value to show for the default real value
		String displayForDefault = Messages.LabelDefault;
		if (defaultDisplay != null)
		{
			Object disp = getDisplayValue(defaultReal, real, display);
			if (disp instanceof String && ((String)disp).length() > 0)
			{
				displayForDefault = NLS.bind(Messages.LabelDefault_arg, disp);
			}
		}

		real = Utils.arrayAdd(real, defaultReal, false);
		display = Utils.arrayAdd(display, displayForDefault, false);
		defaultValueIndex = 0;
		return this;
	}


	private static <T> Object getDisplayValue(T realValue, T[] real, String[] display)
	{
		for (int i = 0; real != null && i < real.length; i++)
		{
			if (realValue == real[i] || realValue != null && realValue.equals(real[i]))
			{
				return display[i];
			}
		}

		// not found
		return null;
	}

	@Override
	public int getDefaultValueIndex()
	{
		return defaultValueIndex;
	}

	/**
	 * Display and real values are the same.
	 *
	 * @param display
	 */
	public ComboboxPropertyModel(T[] real, ILabelProvider labelProvider)
	{
		this.real = real;
		this.display = new String[real.length];
		for (int i = 0; i < real.length; i++)
		{
			if (labelProvider == null)
			{
				display[i] = real[i] == null ? "" : real[i].toString();
			}
			else
			{
				display[i] = labelProvider.getText(real[i]);
			}
		}
	}


	/**
	 * Display and real values are different.
	 *
	 * @param display
	 */
	public ComboboxPropertyModel(T[] real, String[] display)
	{
		this.real = real;
		this.display = display;
	}

	public String[] getDisplayValues()
	{
		return display;
	}

	public T[] getRealValues()
	{
		return real;
	}
}
