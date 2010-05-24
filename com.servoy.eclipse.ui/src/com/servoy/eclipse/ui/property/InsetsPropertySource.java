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

public class InsetsPropertySource extends ComplexPropertySource<java.awt.Insets>
{
	private static final String RIGHT = "right";
	private static final String BOTTOM = "bottom";
	private static final String LEFT = "left";
	private static final String TOP = "top";

	private static IObjectTextConverter insetsTextConverter = new InsetsTextConverter();
	private static ILabelProvider insetsLabelProvider;

	public InsetsPropertySource(ComplexProperty<java.awt.Insets> insets)
	{
		super(insets);
	}

	@Override
	public IPropertyDescriptor[] createPropertyDescriptors()
	{
		// make sure sub-properties are sorted in defined order
		return PropertyController.applySequencePropertyComparator(new IPropertyDescriptor[] { new NumberTypePropertyDescriptor(NumberCellEditor.INTEGER, TOP,
			TOP), new NumberTypePropertyDescriptor(NumberCellEditor.INTEGER, LEFT, LEFT), new NumberTypePropertyDescriptor(NumberCellEditor.INTEGER, BOTTOM,
			BOTTOM), new NumberTypePropertyDescriptor(NumberCellEditor.INTEGER, RIGHT, RIGHT) });
	}

	@Override
	public Object getPropertyValue(Object id)
	{
		java.awt.Insets ins = getEditableValue();
		if (ins == null)
		{
			return new Integer(0);
		}
		if (TOP.equals(id))
		{
			return new Integer(ins.top);
		}
		if (LEFT.equals(id))
		{
			return new Integer(ins.left);
		}
		if (BOTTOM.equals(id))
		{
			return new Integer(ins.bottom);
		}
		if (RIGHT.equals(id))
		{
			return new Integer(ins.right);
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
	protected java.awt.Insets setComplexPropertyValue(Object id, Object v)
	{
		java.awt.Insets ins = (getEditableValue() == null) ? new java.awt.Insets(0, 0, 0, 0) : getEditableValue();
		if (TOP.equals(id))
		{
			ins.top = ((Integer)v).intValue();
		}
		if (LEFT.equals(id))
		{
			ins.left = ((Integer)v).intValue();
		}
		if (BOTTOM.equals(id))
		{
			ins.bottom = ((Integer)v).intValue();
		}
		if (RIGHT.equals(id))
		{
			ins.right = ((Integer)v).intValue();
		}
		return ins;
	}

	public static CellEditor createPropertyEditor(Composite parent)
	{
		return new ConvertorObjectCellEditor(parent, insetsTextConverter);
	}

	public static ILabelProvider getLabelProvider()
	{
		if (insetsLabelProvider == null)
		{
			insetsLabelProvider = new LabelProvider()
			{
				@Override
				public String getText(Object element)
				{
					return insetsTextConverter.convertToString(element);
				}
			};
		}
		return insetsLabelProvider;
	}

	public static class InsetsTextConverter implements IObjectTextConverter
	{

		public String isCorrectString(String value)
		{
			if (value != null && value.trim().length() > 0 && convertToObject(value) == null)
			{
				return "Expecting 2 \"top,left,bottom,right\"";
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
			if (tok.countTokens() != 4)
			{
				return null;
			}
			int top;
			int left;
			int bottom;
			int right;
			try
			{
				top = Integer.parseInt(tok.nextToken().trim());
				left = Integer.parseInt(tok.nextToken().trim());
				bottom = Integer.parseInt(tok.nextToken().trim());
				right = Integer.parseInt(tok.nextToken().trim());
			}
			catch (NumberFormatException e)
			{
				return null;
			}
			return new java.awt.Insets(top, left, bottom, right);
		}

		public String isCorrectObject(Object value)
		{
			if (value == null || (value instanceof java.awt.Insets))
			{
				return null;
			}
			return "Object is not " + java.awt.Insets.class.getName();
		}

		public String convertToString(Object value)
		{
			if (value == null)
			{
				return "";
			}
			return ((java.awt.Insets)value).top + "," + ((java.awt.Insets)value).left + "," + ((java.awt.Insets)value).bottom + "," +
				((java.awt.Insets)value).right;
		}

	}

}
