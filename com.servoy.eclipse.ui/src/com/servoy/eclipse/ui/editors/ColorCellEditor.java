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

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * A cell editor that manages a color field. The cell editor's value is the color (an SWT <code>RBG</code>).
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 */
public class ColorCellEditor extends DialogCellEditor
{

	/**
	 * Creates a new color cell editor parented under the given control. The cell editor value is black (<code>RGB(0,0,0)</code>) initially, and has no
	 * validator.
	 * 
	 * @param parent the parent control
	 * @param style the style bits
	 */
	public ColorCellEditor(Composite parent, ILabelProvider labelProvider, int style)
	{
		super(parent, labelProvider, null, false, style);
	}

	/*
	 * (non-Javadoc) Method declared on DialogCellEditor.
	 */
	@Override
	protected Object openDialogBox(Control cellEditorWindow)
	{
		ColorDialog dialog = new ColorDialog(cellEditorWindow.getShell());
		Object value = getValue();
		if (value != null)
		{
			dialog.setRGB((RGB)value);
		}
		else
		{
			dialog.setRGB(new RGB(0, 0, 0));
		}
		value = dialog.open();
		if (value == null) return null;
		return dialog.getRGB();
	}

}
