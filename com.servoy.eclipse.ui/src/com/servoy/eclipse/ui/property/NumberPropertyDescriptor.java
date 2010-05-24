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

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ICellEditorValidator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.properties.PropertyDescriptor;

/**
 * A Generic property descriptor for numbers. It will use the NumberCellEditor and Label provider. These editors and providers display and format the numbers in
 * locale format.
 */
public class NumberPropertyDescriptor extends PropertyDescriptor
{
	public NumberPropertyDescriptor(Object propertyID, String propertyDisplayname)
	{
		super(propertyID, propertyDisplayname);

		setLabelProvider(NumberLabelProvider.INSTANCE); // The default provider, this can be overridden by just setting in a different value after creating descriptor.
	}

	@Override
	public CellEditor createPropertyEditor(Composite parent)
	{
		CellEditor editor = new NumberCellEditor(parent);
		ICellEditorValidator v = getValidator();
		if (v != null) editor.setValidator(v);
		return editor;
	}

	/**
	 * This will format numbers in locale dependent format. (e.g. 3,000 instead of 3000)
	 */
	public static class NumberLabelProvider extends LabelProvider
	{

		protected static final NumberLabelProvider INSTANCE = new NumberLabelProvider(); // Need only one, they are not descriptor specific.

		NumberFormat fFormatter = NumberFormat.getInstance();

		@Override
		public String getText(Object element)
		{
			if (element instanceof Number) return fFormatter.format(element);

			return super.getText(element);
		}

	}
}
