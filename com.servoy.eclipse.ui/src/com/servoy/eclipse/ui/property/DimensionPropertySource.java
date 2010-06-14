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

import java.awt.Dimension;
import java.util.StringTokenizer;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.properties.IPropertyDescriptor;

import com.servoy.eclipse.ui.property.ConvertorObjectCellEditor.IObjectTextConverter;

public class DimensionPropertySource extends ComplexPropertySource<java.awt.Dimension>
{
	private static final String HEIGHT = "height";
	private static final String WIDTH = "width";

	private static IObjectTextConverter dimensionTextConverter = new DimensionTextConverter();
	private static ILabelProvider dimensionLabelProvider;

	private final Dimension defaultDimension;

	public DimensionPropertySource(ComplexProperty<java.awt.Dimension> dimension, Dimension defaultDimension)
	{
		super(dimension);
		this.defaultDimension = defaultDimension;
	}

	@Override
	public IPropertyDescriptor[] createPropertyDescriptors()
	{
		// make sure sub-properties are sorted in defined order
		return PropertyController.applySequencePropertyComparator(new IPropertyDescriptor[] { new NumberTypePropertyDescriptor(NumberCellEditor.DOUBLE, WIDTH,
			WIDTH), new NumberTypePropertyDescriptor(NumberCellEditor.DOUBLE, HEIGHT, HEIGHT) });
	}

	@Override
	public Object getPropertyValue(Object id)
	{
		java.awt.Dimension dim = getEditableValue();
		if (dim == null)
		{
			if (WIDTH.equals(id))
			{
				return new Double(defaultDimension.getWidth());
			}
			if (HEIGHT.equals(id))
			{
				return new Double(defaultDimension.getHeight());
			}
		}
		if (WIDTH.equals(id))
		{
			return new Double(dim.getWidth());
		}
		if (HEIGHT.equals(id))
		{
			return new Double(dim.getHeight());
		}
		return null;
	}

	@Override
	public Object resetComplexPropertyValue(Object id)
	{
		if (WIDTH.equals(id))
		{
			return new Double(140);
		}
		if (HEIGHT.equals(id))
		{
			return new Double(20);
		}
		return super.resetComplexPropertyValue(id);
	}

	@Override
	protected java.awt.Dimension setComplexPropertyValue(Object id, Object v)
	{
		java.awt.Dimension dim = (getEditableValue() == null) ? defaultDimension : getEditableValue();
		if (WIDTH.equals(id))
		{
			dim.setSize(((Double)v).doubleValue(), dim.getHeight());
		}
		if (HEIGHT.equals(id))
		{
			dim.setSize(dim.getWidth(), ((Double)v).doubleValue());
		}
		return dim;
	}

	public static CellEditor createPropertyEditor(Composite parent)
	{
		return new ConvertorObjectCellEditor(parent, dimensionTextConverter);
	}

	public static ILabelProvider getLabelProvider()
	{
		if (dimensionLabelProvider == null)
		{
			dimensionLabelProvider = new LabelProvider()
			{
				@Override
				public String getText(Object element)
				{
					return dimensionTextConverter.convertToString(element);
				}
			};
		}
		return dimensionLabelProvider;
	}

	public static class DimensionTextConverter implements IObjectTextConverter
	{

		public String isCorrectString(String value)
		{
			if (value != null && value.trim().length() > 0 && convertToObject(value) == null)
			{
				return "Expecting 2 \"width,height\"";
			}
			return null;
		}

		public Object convertToObject(String value)
		{
			if (value == null)
			{
				return null;
			}
			StringTokenizer tok = new StringTokenizer(value, ",");
			if (tok.countTokens() != 2)
			{
				return null;
			}
			int width;
			int height;
			try
			{
				width = Integer.parseInt(tok.nextToken().trim());
				height = Integer.parseInt(tok.nextToken().trim());
			}
			catch (NumberFormatException e)
			{
				return null;
			}
			return new java.awt.Dimension(width, height);
		}

		public String isCorrectObject(Object value)
		{
			if (value == null || (value instanceof java.awt.Dimension))
			{
				return null;
			}
			return "Object is not " + java.awt.Dimension.class.getName();
		}

		public String convertToString(Object value)
		{
			if (value == null)
			{
				return "";
			}
			return (int)((java.awt.Dimension)value).getWidth() + "," + (int)((java.awt.Dimension)value).getHeight();
		}

	}

}
