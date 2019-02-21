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

import java.beans.PropertyEditor;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.widgets.Composite;


/**
 * Property controller for bean property editor that supports getTags().
 *
 * @author rgansevles
 *
 */
public class BeanTagsPropertyController extends PropertyController<Object, Integer>
{
	private final PropertyEditor propertyEditor;
	private final ComboboxPropertyController<String> comboboxPropertyController;

	public BeanTagsPropertyController(Object id, String displayName, PropertyEditor propertyEditor, String unresolved)
	{
		super(id, displayName);
		this.propertyEditor = propertyEditor;
		comboboxPropertyController = new ComboboxPropertyController<String>(id, displayName, new ComboboxPropertyModel<String>(propertyEditor.getTags()),
			unresolved);
	}

	@Override
	public CellEditor createPropertyEditor(Composite parent)
	{
		return comboboxPropertyController.createPropertyEditor(parent);
	}

	@Override
	protected IPropertyConverter<Object, Integer> createConverter()
	{
		return new ChainedPropertyConverter<Object, String, Integer>(new BeanAsTextPropertyConverter(propertyEditor),
			comboboxPropertyController.getConverter());
	}

	@Override
	public ILabelProvider getLabelProvider()
	{
		return comboboxPropertyController.getLabelProvider();
	}

}
