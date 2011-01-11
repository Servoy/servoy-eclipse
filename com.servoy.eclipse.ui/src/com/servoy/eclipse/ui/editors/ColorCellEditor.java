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
package com.servoy.eclipse.ui.editors;

import java.awt.Color;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import com.servoy.eclipse.ui.property.ColorPropertyController;
import com.servoy.eclipse.ui.resource.ColorResource;

/**
 * A cell editor that manages a color field. The cell editor's value is the color (an SWT <code>RBG</code>).
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @author rgansevles
 */
public class ColorCellEditor extends TextDialogCellEditor
{

	/**
	 * Creates a new color cell editor parented under the given control. The cell editor value is black (<code>RGB(0,0,0)</code>) initially.
	 * 
	 * @param parent the parent control
	 * @param style the style bits
	 */
	public ColorCellEditor(Composite parent, ILabelProvider labelProvider, int style)
	{
		super(parent, style, labelProvider);
	}

	@Override
	public Object openDialogBox(Control cellEditorWindow)
	{
		java.awt.Color color = ColorPropertyController.PROPERTY_COLOR_CONVERTER.convertValue(null, (String)getValue());
		if (color == null)
		{
			color = Color.black;
		}
		ColorDialog dialog = new ColorDialog(cellEditorWindow.getShell());
		dialog.setRGB(ColorResource.ColorAwt2Rgb(color));

		if (dialog.open() == null) return CANCELVALUE;

		return ColorPropertyController.PROPERTY_COLOR_CONVERTER.convertProperty(null, ColorResource.ColoRgb2Awt(dialog.getRGB()));
	}

}
