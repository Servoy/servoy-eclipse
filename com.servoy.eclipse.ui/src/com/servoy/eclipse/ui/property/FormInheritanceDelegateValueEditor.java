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

import org.eclipse.ui.internal.views.ViewsPlugin;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.IPropertySourceProvider;

import com.servoy.eclipse.ui.editors.IValueEditor;
import com.servoy.j2db.persistence.Form;

/**
 * Value editor delegate that finds the value to be edited following form inheritance.
 * 
 * @author rgansevles
 * 
 * @param <T>
 */
public class FormInheritanceDelegateValueEditor<T> implements IValueEditor<T>
{
	private final IValueEditor<T> valueEditor;
	private final Form form;
	private final Object propertyId;

	public FormInheritanceDelegateValueEditor(Form form, IValueEditor<T> valueEditor, Object propertyId)
	{
		this.form = form;
		this.valueEditor = valueEditor;
		this.propertyId = propertyId;
	}

	public void openEditor(T value)
	{
		valueEditor.openEditor(getInheritedValue(value));
	}

	public boolean canEdit(T value)
	{
		return valueEditor.canEdit(getInheritedValue(value));
	}

	protected T getInheritedValue(T value)
	{
		if (form != null & form.getExtendsFormID() > 0)
		{
			Object object = new PersistPropertySource(form, null, true).getInheritedPropertyValue(value, propertyId);
			if (object == null)
			{
				return null;
			}
			// must return the editable value
			IPropertySourceProvider provider = (IPropertySourceProvider)ViewsPlugin.getAdapter(object, IPropertySourceProvider.class, false);
			IPropertySource propertySource;
			if (provider != null)
			{
				propertySource = provider.getPropertySource(object);
			}
			else
			{
				propertySource = (IPropertySource)ViewsPlugin.getAdapter(object, IPropertySource.class, false);
			}
			if (propertySource != null)
			{
				return (T)propertySource.getEditableValue();
			}
			return (T)object;
		}
		return value;
	}
}
