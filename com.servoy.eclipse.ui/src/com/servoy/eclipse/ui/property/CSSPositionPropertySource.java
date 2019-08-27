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
import org.eclipse.jface.viewers.ICellEditorValidator;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.views.properties.IPropertyDescriptor;

import com.servoy.eclipse.ui.editors.CSSPositionDialog;
import com.servoy.eclipse.ui.editors.TextDialogCellEditor;
import com.servoy.eclipse.ui.property.ConvertorObjectCellEditor.IObjectTextConverter;
import com.servoy.j2db.persistence.CSSPosition;
import com.servoy.j2db.util.Utils;

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
		ICellEditorFactory factory = new ICellEditorFactory()
		{

			@Override
			public CellEditor createPropertyEditor(Composite parent)
			{
				TextDialogCellEditor editor = new TextDialogCellEditor(parent, SWT.NONE, CSSPositionLabelProvider.INSTANCE)
				{

					@Override
					public Object openDialogBox(Control cellEditorWindow)
					{
						CSSPositionDialog dialog = new CSSPositionDialog(getControl().getShell() /* new Shell() */, getValue());
						dialog.open();
						if (dialog.getReturnCode() == Window.CANCEL)
						{
							return CANCELVALUE;
						}
						return dialog.getValue();
					}

				};
				editor.setValidator(new ICellEditorValidator()
				{

					@Override
					public String isValid(Object value)
					{
						if (value != null && !"".equals(value))
						{
							String position = value.toString().trim();
							if (position.startsWith("calc"))
							{
								if (!position.startsWith("calc("))
								{
									return "Calculation call should start with calc(";
								}
								if (position.indexOf("(") > 0 && position.indexOf(")") > 0)
								{
									position = position.substring(position.indexOf("(") + 1, position.lastIndexOf(")"));
									position = position.trim();
									String[] values = position.split(" ");
									if (values.length == 3)
									{
										if (!"-".equals(values[1]) && !"+".equals(values[1]))
										{
											return "Only + and - operations are supported inside calc function.";
										}
										String number = values[0];
										if (number.endsWith("px"))
										{
											number = number.replaceFirst("px", "");
										}
										else if (number.endsWith("%"))
										{
											number = number.replaceFirst("%", "");
										}
										try
										{
											Utils.getAsInteger(number, true);
										}
										catch (Exception ex)
										{
											return "First operand in calc function must be a number in pixels or percentage.";
										}
										number = values[2];
										if (number.endsWith("px"))
										{
											number = number.replaceFirst("px", "");
										}
										else if (number.endsWith("%"))
										{
											number = number.replaceFirst("%", "");
										}
										try
										{
											Utils.getAsInteger(number, true);
										}
										catch (Exception ex)
										{
											return "Second operand in calc function must be a number in pixels or percentage.";
										}
									}
									else
									{
										return "Value must be calc(x +/- y)";
									}
								}
								else
								{
									return "Value must be css calc function.";
								}
								return null;
							}
							else if (position.endsWith("px"))
							{
								position = position.replaceFirst("px", "");
							}
							else if (position.endsWith("%"))
							{
								position = position.replaceFirst("%", "");
							}
							try
							{
								Utils.getAsInteger(position, true);
							}
							catch (Exception ex)
							{
								return "Value must be either a number (in pixels) , a percent or a css calc function.";
							}
						}
						return null;
					}
				});
				return editor;
			}
		};
		// make sure sub-properties are sorted in defined order
		return PropertyController.applySequencePropertyComparator(new IPropertyDescriptor[] { new PropertyController<String, String>(TOP, TOP, null,
			CSSPositionLabelProvider.INSTANCE, factory), new PropertyController<String, String>(RIGHT, RIGHT, null, CSSPositionLabelProvider.INSTANCE,
				factory), new PropertyController<String, String>(BOTTOM, BOTTOM, null, CSSPositionLabelProvider.INSTANCE,
					factory), new PropertyController<String, String>(LEFT, LEFT, null, CSSPositionLabelProvider.INSTANCE,
						factory), new PropertyController<String, String>(WIDTH, WIDTH, null, CSSPositionLabelProvider.INSTANCE,
							factory), new PropertyController<String, String>(HEIGHT, HEIGHT, null, CSSPositionLabelProvider.INSTANCE, factory) });
	}

	@Override
	public Object getPropertyValue(Object id)
	{
		CSSPosition position = getEditableValue();
		if (position == null)
		{
			return "0";
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
		if (LEFT.equals(id) || TOP.equals(id) || BOTTOM.equals(id) || RIGHT.equals(id))
		{
			return "-1";
		}
		if (WIDTH.equals(id))
		{
			return "80";
		}
		if (HEIGHT.equals(id))
		{
			return "20";
		}
		return "0";
	}

	@Override
	protected CSSPosition setComplexPropertyValue(Object id, Object v)
	{
		CSSPosition position = (getEditableValue() == null) ? new CSSPosition("0", "0", "0", "0", "0", "0") : getEditableValue();
		String str = (String)v;
		if (Utils.stringIsEmpty(str))
		{
			str = "-1";
		}
		if (TOP.equals(id))
		{
			position.top = str;
		}
		if (LEFT.equals(id))
		{
			position.left = str;
		}
		if (BOTTOM.equals(id))
		{
			position.bottom = str;
		}
		if (RIGHT.equals(id))
		{
			position.right = str;
		}
		if (WIDTH.equals(id))
		{
			position.width = str;
		}
		if (HEIGHT.equals(id))
		{
			position.height = str;
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
				return "Expecting 6 items \"top,right,bottom,left,width,height\"";
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
			String top;
			String left;
			String bottom;
			String right;
			String width;
			String height;
			top = tok.nextToken().trim();
			right = tok.nextToken().trim();
			bottom = tok.nextToken().trim();
			left = tok.nextToken().trim();
			width = tok.nextToken().trim();
			height = tok.nextToken().trim();
			return new CSSPosition(top, right, bottom, left, width, height);
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
			return ((CSSPosition)value).top + "," + ((CSSPosition)value).right + "," + ((CSSPosition)value).bottom + "," + ((CSSPosition)value).left + "," +
				((CSSPosition)value).width + "," + ((CSSPosition)value).height;
		}

	}

	public static class CSSPositionLabelProvider extends LabelProvider
	{

		protected static final CSSPositionLabelProvider INSTANCE = new CSSPositionLabelProvider();


		@Override
		public String getText(Object element)
		{
			if ("-1".equals(element))
			{
				return "not set";
			}
			return super.getText(element);
		}

	}
}
