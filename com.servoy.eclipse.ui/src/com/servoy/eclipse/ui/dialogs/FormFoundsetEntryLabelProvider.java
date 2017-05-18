/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2015 Servoy BV

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

package com.servoy.eclipse.ui.dialogs;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import com.servoy.eclipse.ui.Activator;

/**
 * Provides just one entry representing the current form's foundset.
 *
 * @author acostescu
 */
public class FormFoundsetEntryLabelProvider extends LabelProvider
{

	private static final Image FORM_IMAGE = Activator.getDefault().loadImageFromBundle("form.png");

	@Override
	public String getText(Object value)
	{
		return (value == FormFoundsetEntryContentProvider.FORM_FOUNDSET) ? "Form foundset" : null;
	}

	@Override
	public Image getImage(Object value)
	{
		return (value == FormFoundsetEntryContentProvider.FORM_FOUNDSET) ? FORM_IMAGE : null;
	}

}
