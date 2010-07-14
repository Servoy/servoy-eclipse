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

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.views.properties.PropertyDescriptor;

import com.servoy.eclipse.ui.Messages;
import com.servoy.eclipse.ui.editors.ColorCellEditor;

/**
 * Property controller for selecting a color in Properties view.
 * 
 * @author rgansevles
 *
 */

public class ColorPropertyController extends PropertyDescriptor implements IPropertyController<java.awt.Color, RGB>
{
	public static final PropertyColorConverter PROPERTY_COLOR_CONVERTER = new PropertyColorConverter();
	public static final ColorLabelProvider COLOR_LABEL_PROVIDER = new ColorLabelProvider();

	public ColorPropertyController(String id, String displayName)
	{
		super(id, displayName);
		setLabelProvider(COLOR_LABEL_PROVIDER);
	}

	public PropertyColorConverter getConverter()
	{
		return PROPERTY_COLOR_CONVERTER;
	}

	public boolean supportsReadonly()
	{
		return false;
	}

	public boolean isReadOnly()
	{
		return false;
	}

	public void setReadonly(boolean readonly)
	{
		// ignore
	}

	/**
	 * The <code>ColorPropertyDescriptor</code> implementation of this <code>IPropertyDescriptor</code> method creates and returns a new
	 * <code>ColorCellEditor</code>.
	 * <p>
	 * The editor is configured with the current validator if there is one.
	 * </p>
	 */
	@Override
	public CellEditor createPropertyEditor(Composite parent)
	{
		CellEditor editor = new ColorCellEditor(parent, COLOR_LABEL_PROVIDER, SWT.NONE);
		if (getValidator() != null)
		{
			editor.setValidator(getValidator());
		}
		return editor;
	}

	/**
	 * Converter class for colors, convert AWT to RGB.
	 * 
	 * @author rgansevles
	 * 
	 */
	public static class PropertyColorConverter implements IPropertyConverter<java.awt.Color, RGB>
	{
		public static final RGB NULL_VALUE = null;

		/**
		 * Convert AWT color to SWT color
		 */
		public RGB convertProperty(Object id, java.awt.Color awtcolor)
		{
			if (awtcolor == null) return NULL_VALUE;
			return new RGB(awtcolor.getRed(), awtcolor.getGreen(), awtcolor.getBlue());
		}

		/**
		 * Convert SWT color to AWT color
		 */
		public java.awt.Color convertValue(Object id, RGB rgb)
		{
			if (rgb == null || rgb == NULL_VALUE) return null;
			return new java.awt.Color(rgb.red, rgb.green, rgb.blue);
		}
	}


	/**
	 * Label provider for colors.
	 * 
	 * @author rgansevles
	 * 
	 */
	public static class ColorLabelProvider extends LabelProvider
	{
		@Override
		public String getText(Object element)
		{
			if (element == PropertyColorConverter.NULL_VALUE)
			{
				return Messages.LabelDefault;
			}
			return super.getText(element);
		}

		@Override
		public Image getImage(Object element)
		{
			if (element instanceof RGB && element != PropertyColorConverter.NULL_VALUE)
			{
				ImageData imageData = new ImageData(25, 12, 1, new PaletteData(new RGB[] { (RGB)element }));
				return new Image(Display.getDefault(), imageData);
			}

			return super.getImage(element);
		}
	}
}
