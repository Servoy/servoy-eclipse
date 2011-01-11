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


import java.awt.Font;

import org.eclipse.jface.viewers.ICellEditorValidator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FontDialog;

import com.servoy.eclipse.ui.property.PropertyFontConverter;
import com.servoy.eclipse.ui.util.IDefaultValue;

/**
 * A cell editor that manages a font field.
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 */
public class FontCellEditor extends TextDialogCellEditor
{
	private static final FontStringValidator FONT_STRING_VALIDATOR = new FontStringValidator();
	private final IDefaultValue<String> defaultValue;

	/**
	 * Creates a new font cell editor parented under the given control.
	 * 
	 * @param parent the parent control
	 */
	public FontCellEditor(Composite parent, IDefaultValue<String> defaultValue)
	{
		super(parent, SWT.NONE, null);
		this.defaultValue = defaultValue;
		setValidator(FONT_STRING_VALIDATOR);
	}

	@Override
	public Object openDialogBox(Control cellEditorWindow)
	{
		FontDialog dialog = new FontDialog(cellEditorWindow.getShell());
		String value = (String)getValue();
		if ((value == null || value.trim().length() == 0) && defaultValue != null)
		{
			value = defaultValue.getValue();
		}
		Font awtfont = PropertyFontConverter.INSTANCE.convertValue(null, value);
		if (awtfont != null)
		{
			dialog.setFontList(new FontData[] { new FontData(awtfont.getFamily(), awtfont.getSize(), awtfont.getStyle()) });
		}
		FontData fontData = dialog.open();
		if (fontData == null)
		{
			return CANCELVALUE;
		}
		return PropertyFontConverter.INSTANCE.convertProperty(null, new java.awt.Font(fontData.getName(), fontData.getStyle(), fontData.getHeight()));
	}

	/**
	 * Validate font strings for the text cell editor.
	 * 
	 * @author rgansevles
	 *
	 */
	public static class FontStringValidator implements ICellEditorValidator
	{
		public String isValid(Object value)
		{
			String fontString = (String)value;
			if (fontString != null && fontString.trim().length() > 0 && PropertyFontConverter.INSTANCE.convertValue(null, fontString) == null)
			{
				return "Cannot parse font value \"" + value + '"';
			}

			// valid
			return null;
		}
	}
}
