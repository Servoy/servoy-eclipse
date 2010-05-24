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

import java.util.StringTokenizer;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.properties.IPropertyDescriptor;

import com.servoy.eclipse.ui.property.ConvertorObjectCellEditor.IObjectTextConverter;

public class PointPropertySource extends ComplexPropertySource<java.awt.Point>
{
	private static final String Y = "y";
	private static final String X = "x";

	private static IObjectTextConverter pointTextConverter = new PointTextConverter();
	private static ILabelProvider pointLabelProvider;

	public PointPropertySource(ComplexProperty<java.awt.Point> point)
	{
		super(point);
	}

	@Override
	public IPropertyDescriptor[] createPropertyDescriptors()
	{
		return new IPropertyDescriptor[] { new NumberTypePropertyDescriptor(NumberCellEditor.DOUBLE, X, X), new NumberTypePropertyDescriptor(
			NumberCellEditor.DOUBLE, Y, Y) };
	}

	@Override
	public Object getPropertyValue(Object id)
	{
		java.awt.Point pnt = getEditableValue();
		if (pnt == null)
		{
			return new Double(0);
		}
		if (X.equals(id))
		{
			return new Double(pnt.getX());
		}
		if (Y.equals(id))
		{
			return new Double(pnt.getY());
		}
		return null;
	}

	@Override
	public boolean isPropertySet(Object id)
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void resetPropertyValue(Object id)
	{
		// TODO Auto-generated method stub

	}

	@Override
	protected java.awt.Point setComplexPropertyValue(Object id, Object v)
	{
		java.awt.Point pnt = (getEditableValue() == null) ? new java.awt.Point(0, 0) : getEditableValue();
		if (X.equals(id))
		{
			pnt.setLocation(((Double)v).doubleValue(), pnt.getY());
		}
		if (Y.equals(id))
		{
			pnt.setLocation(pnt.getX(), ((Double)v).doubleValue());
		}
		return pnt;
	}

	public static CellEditor createPropertyEditor(Composite parent)
	{
		return new ConvertorObjectCellEditor(parent, pointTextConverter);
	}

	public static ILabelProvider getLabelProvider()
	{
		if (pointLabelProvider == null)
		{
			pointLabelProvider = new LabelProvider()
			{
				@Override
				public String getText(Object element)
				{
					return pointTextConverter.convertToString(element);
				}
			};
		}
		return pointLabelProvider;
	}

	public static class PointTextConverter implements IObjectTextConverter
	{

		public String isCorrectString(String value)
		{
			if (value != null && value.trim().length() > 0 && convertToObject(value) == null)
			{
				return "Expecting 2 \"x,y\"";
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
			int x;
			int y;
			try
			{
				x = Integer.parseInt(tok.nextToken().trim());
				y = Integer.parseInt(tok.nextToken().trim());
			}
			catch (NumberFormatException e)
			{
				return null;
			}
			return new java.awt.Point(x, y);
		}

		public String isCorrectObject(Object value)
		{
			if (value == null || (value instanceof java.awt.Point))
			{
				return null;
			}
			return "Object is not " + java.awt.Point.class.getName();
		}

		public String convertToString(Object value)
		{
			if (value == null)
			{
				return "";
			}
			return (int)((java.awt.Point)value).getX() + "," + (int)((java.awt.Point)value).getY();
		}

	}

}
