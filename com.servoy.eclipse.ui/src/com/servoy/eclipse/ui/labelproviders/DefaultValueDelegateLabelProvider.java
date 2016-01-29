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
package com.servoy.eclipse.ui.labelproviders;

import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.osgi.util.NLS;

import com.servoy.eclipse.ui.Messages;

/**
 * Label provider delegate that handles the default value when actual value is null.
 *
 * @author rgansevles
 */

public class DefaultValueDelegateLabelProvider extends DelegateLabelProvider
{
	private final Object defaultValue;

	public DefaultValueDelegateLabelProvider(IBaseLabelProvider labelProvider)
	{
		this(labelProvider, null);
	}

	public DefaultValueDelegateLabelProvider(IBaseLabelProvider labelProvider, Object defaultValue)
	{
		super(labelProvider);
		this.defaultValue = defaultValue;
	}

	@Override
	public String getText(Object element)
	{
		if (element == null)
		{
			return defaultValue == null ? Messages.LabelDefault : NLS.bind(Messages.LabelDefault_arg, defaultValue);
		}

		return super.getText(element);
	}
}
