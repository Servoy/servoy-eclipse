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
import org.eclipse.jface.viewers.ICellEditorValidator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.properties.PropertyDescriptor;

import com.servoy.eclipse.ui.Messages;
import com.servoy.eclipse.ui.editors.ColorCellEditor;
import com.servoy.eclipse.ui.resource.ColorResource;
import com.servoy.j2db.util.TreeBidiMap;
import com.servoy.j2db.util.Utils;

/**
 * Property controller for selecting a color in Properties view.
 * 
 * @author rgansevles
 *
 */

public class ColorPropertyController extends PropertyDescriptor implements IPropertyController<java.awt.Color, String>
{
	public static final PropertyColorConverter PROPERTY_COLOR_CONVERTER = new PropertyColorConverter();
	public static final ColorLabelProvider COLOR_LABEL_PROVIDER = new ColorLabelProvider();
	public static final ColorStringValidator COLOR_STRING_VALIDATOR = new ColorStringValidator();

	public ColorPropertyController(String id, String displayName)
	{
		super(id, displayName);
		setLabelProvider(COLOR_LABEL_PROVIDER);
		setValidator(COLOR_STRING_VALIDATOR);
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
	public static class PropertyColorConverter implements IPropertyConverter<java.awt.Color, String>
	{
		private static final TreeBidiMap<String, String> colorsMap = new TreeBidiMap<String, String>();
		static
		{
			// The 17 standard css colors
			colorsMap.put("black", "#000000");
			colorsMap.put("silver", "#c0c0c0");
//			colorsMap.put("gray", "#808080");
			colorsMap.put("grey", "#808080");
			colorsMap.put("white", "#ffffff");
			colorsMap.put("maroon", "#800000");
			colorsMap.put("red", "#ff0000");
			colorsMap.put("purple", "#800080");
			colorsMap.put("fuchsia", "#ff00ff");
			colorsMap.put("green", "#008000");
			colorsMap.put("lime", "#00ff00");
			colorsMap.put("olive", "#808000");
			colorsMap.put("yellow", "#ffff00");
			colorsMap.put("navy", "#000080");
			colorsMap.put("blue", "#0000ff");
			colorsMap.put("teal", "#008080");
			colorsMap.put("aqua", "#00ffff");
		}

		/**
		 * Convert AWT color to SWT color
		 */
		public static String getColorString(java.awt.Color awtcolor)
		{
			if (awtcolor == null) return null;
			String hexString = Integer.toHexString(awtcolor.getRGB() & 0x00ffffff);
			if (hexString.length() < 6)
			{
				hexString = "000000".substring(hexString.length()) + hexString;
			}
			return '#' + hexString;
		}

		public String convertProperty(Object id, java.awt.Color awtcolor)
		{
			if (awtcolor == null) return null;
			String colorString = getColorString(awtcolor);
			String named = colorsMap.getKey(colorString);
			if (named != null)
			{
				return Utils.stringInitCap(named);
			}
			return colorString;
		}

		/**
		 * Convert SWT color to AWT color
		 */
		public java.awt.Color convertValue(Object id, String string)
		{
			if (string == null || string.trim().length() == 0) return null;
			String hex = colorsMap.get("gray".equalsIgnoreCase(string) ? "grey" : string.toLowerCase());
			if (hex == null)
			{
				hex = string;
			}
			if (hex.startsWith("#") && hex.length() == 4)
			{
				// #rgb -> #rrggbb
				hex = new StringBuilder("#").append(hex.charAt(1)).append(hex.charAt(1)).append(hex.charAt(2)).append(hex.charAt(2)).append(hex.charAt(3)).append(
					hex.charAt(3)).toString();
			}
			return java.awt.Color.decode(hex);
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
			if (element == null)
			{
				return Messages.LabelDefault;
			}
			return super.getText(element);
		}

		@Override
		public Image getImage(Object element)
		{
			if (element instanceof String)
			{
				return ColorResource.INSTANCE.getColorImage(12, 12, 1, ColorResource.ColorAwt2Rgb(PROPERTY_COLOR_CONVERTER.convertValue(null, (String)element)));
			}

			return super.getImage(element);
		}
	}

	/**
	 * Validate color strings for the text cell editor.
	 * 
	 * @author rgansevles
	 *
	 */
	public static class ColorStringValidator implements ICellEditorValidator
	{
		public String isValid(Object value)
		{
			try
			{
				PROPERTY_COLOR_CONVERTER.convertValue(null, (String)value);
				// valid
				return null;
			}
			catch (Exception e)
			{
				return "Cannot parse color value \"" + value + '"';
			}

		}
	}

}
