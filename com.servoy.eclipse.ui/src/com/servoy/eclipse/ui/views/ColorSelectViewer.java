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
package com.servoy.eclipse.ui.views;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

import com.servoy.eclipse.ui.resource.ColorResource;

/**
 * Viewer for selecting a color value
 * 
 * @author rgansevles
 * 
 */
public class ColorSelectViewer extends TreeSelectViewer
{
	public ColorSelectViewer(Composite parent, int style)
	{
		super(parent, style);
		setButtonText("");
	}

	@Override
	protected Text createTextField(Composite parent)
	{
		// button only
		return null;
	}

	@Override
	protected void internalRefresh(Object element)
	{
		Object value = getValue();

		Image image = null;
		ILabelProvider labelProvider = getTextLabelProvider();
		if (labelProvider != null)
		{
			image = labelProvider.getImage(value);
		}
		if (image == null)
		{
			Point buttonSize = button.getSize();
			image = ColorResource.INSTANCE.getColorImage(buttonSize.x > 10 ? buttonSize.x - 8 : buttonSize.x, buttonSize.y > 10 ? buttonSize.y - 8
				: buttonSize.y, 1, (RGB)value);
		}
		button.setImage(image);
	}

	@Override
	protected IStructuredSelection openDialogBox(Control control)
	{
		// show color select dialog
		ColorDialog dialog = new ColorDialog(control.getShell());
		IStructuredSelection selection = (IStructuredSelection)getSelection();
		if (!selection.isEmpty())
		{
			dialog.setRGB((RGB)selection.getFirstElement());
		}
		dialog.open();
		RGB rgb = dialog.getRGB();
		return rgb == null ? StructuredSelection.EMPTY : new StructuredSelection(rgb);
	}
}
