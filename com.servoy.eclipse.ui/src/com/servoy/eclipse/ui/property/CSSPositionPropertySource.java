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

import java.text.NumberFormat;
import java.util.StringTokenizer;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.properties.IPropertyDescriptor;

import com.servoy.eclipse.ui.property.ConvertorObjectCellEditor.IObjectTextConverter;
import com.servoy.j2db.persistence.CSSPosition;

/**
 * IPropertySource for css position  show top, right, bottom , left width and height subproperties.
 *
 * @author lvostinar
 */

public class CSSPositionPropertySource extends ComplexPropertySourceWithStandardReset<CSSPosition>
{
	private static final String RIGHT = "right";
	private static final String BOTTOM = "bottom";
	private static final String LEFT = "left";
	private static final String TOP = "top";
	private static final String WIDTH = "width";
	private static final String HEIGHT = "height";

	private static IObjectTextConverter cssPositionTextConverter = new CSSPositionTextConverter();
	private static ILabelProvider cssPositionLabelProvider;

	public CSSPositionPropertySource(ComplexProperty<CSSPosition> cssPosition)
	{
		super(cssPosition);
	}

	@Override
	public IPropertyDescriptor[] createPropertyDescriptors()
	{
		// make sure sub-properties are sorted in defined order
		return PropertyController.applySequencePropertyComparator(new IPropertyDescriptor[] { new NumberTypePropertyDescriptor(NumberCellEditor.INTEGER, TOP,
			TOP, CSSPositionLabelProvider.INSTANCE), new NumberTypePropertyDescriptor(NumberCellEditor.INTEGER, LEFT, LEFT,
				CSSPositionLabelProvider.INSTANCE), new NumberTypePropertyDescriptor(NumberCellEditor.INTEGER, BOTTOM, BOTTOM,
					CSSPositionLabelProvider.INSTANCE), new NumberTypePropertyDescriptor(NumberCellEditor.INTEGER, RIGHT, RIGHT,
						CSSPositionLabelProvider.INSTANCE), new NumberTypePropertyDescriptor(NumberCellEditor.INTEGER, WIDTH, WIDTH,
							CSSPositionLabelProvider.INSTANCE), new NumberTypePropertyDescriptor(NumberCellEditor.INTEGER, HEIGHT, HEIGHT,
								CSSPositionLabelProvider.INSTANCE) });
	}

	@Override
	public Object getPropertyValue(Object id)
	{
		CSSPosition position = getEditableValue();
		if (position == null)
		{
			return new Integer(0);
		}
		if (TOP.equals(id))
		{
			return position.top;
		}
		if (LEFT.equals(id))
		{
			return position.left;
		}
		if (BOTTOM.equals(id))
		{
			return position.bottom;
		}
		if (RIGHT.equals(id))
		{
			return position.right;
		}
		if (WIDTH.equals(id))
		{
			return position.width;
		}
		if (HEIGHT.equals(id))
		{
			return position.height;
		}
		return null;
	}

	@Override
	public Object resetComplexPropertyValue(Object id)
	{
		if (BOTTOM.equals(id) || RIGHT.equals(id))
		{
			return Integer.valueOf(-1);
		}
		if (WIDTH.equals(id))
		{
			return Integer.valueOf(80);
		}
		if (HEIGHT.equals(id))
		{
			return Integer.valueOf(20);
		}
		return Integer.valueOf(0);
	}

	@Override
	protected CSSPosition setComplexPropertyValue(Object id, Object v)
	{
		CSSPosition position = (getEditableValue() == null) ? new CSSPosition(0, 0, 0, 0, 0, 0) : getEditableValue();
		if (TOP.equals(id))
		{
			position.top = (Integer)v;
		}
		if (LEFT.equals(id))
		{
			position.left = (Integer)v;
		}
		if (BOTTOM.equals(id))
		{
			position.bottom = (Integer)v;
		}
		if (RIGHT.equals(id))
		{
			position.right = (Integer)v;
		}
		if (WIDTH.equals(id))
		{
			position.width = (Integer)v;
		}
		if (HEIGHT.equals(id))
		{
			position.height = (Integer)v;
		}
		return position;
	}

	public static CellEditor createPropertyEditor(Composite parent)
	{
		return new ConvertorObjectCellEditor(parent, cssPositionTextConverter);
	}

	public static ILabelProvider getLabelProvider()
	{
		if (cssPositionLabelProvider == null)
		{
			cssPositionLabelProvider = new LabelProvider()
			{
				@Override
				public String getText(Object element)
				{
					return cssPositionTextConverter.convertToString(element);
				}
			};
		}
		return cssPositionLabelProvider;
	}

	public static class CSSPositionTextConverter implements IObjectTextConverter
	{

		public String isCorrectString(String value)
		{
			if (value != null && value.trim().length() > 0 && convertToObject(value) == null)
			{
				return "Expecting 6 items \"top,left,bottom,right,width,height\"";
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
			if (tok.countTokens() != 6)
			{
				return null;
			}
			int top;
			int left;
			int bottom;
			int right;
			int width;
			int height;
			try
			{
				top = Integer.parseInt(tok.nextToken().trim());
				left = Integer.parseInt(tok.nextToken().trim());
				bottom = Integer.parseInt(tok.nextToken().trim());
				right = Integer.parseInt(tok.nextToken().trim());
				width = Integer.parseInt(tok.nextToken().trim());
				height = Integer.parseInt(tok.nextToken().trim());
			}
			catch (NumberFormatException e)
			{
				return null;
			}
			return new CSSPosition(top, left, bottom, right, width, height);
		}

		public String isCorrectObject(Object value)
		{
			if (value == null || (value instanceof CSSPosition))
			{
				return null;
			}
			return "Object is not " + CSSPosition.class.getName();
		}

		public String convertToString(Object value)
		{
			if (value == null)
			{
				return "";
			}
			return ((CSSPosition)value).top + "," + ((CSSPosition)value).left + "," + ((CSSPosition)value).bottom + "," + ((CSSPosition)value).right + "," +
				((CSSPosition)value).width + "," + ((CSSPosition)value).height;
		}

	}

	public static class CSSPositionLabelProvider extends LabelProvider
	{

		protected static final CSSPositionLabelProvider INSTANCE = new CSSPositionLabelProvider();

		NumberFormat fFormatter = NumberFormat.getInstance();

		@Override
		public String getText(Object element)
		{
			if (element instanceof Number)
			{
				if (((Number)element).intValue() == -1)
				{
					return "not set";
				}
				return fFormatter.format(element);
			}

			return super.getText(element);
		}

	}
}
