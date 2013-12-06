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

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.IPropertySourceProvider;

import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.util.IDelegate;
import com.servoy.j2db.util.Utils;

/**
 * when values are changed, do this via the editor that handles the persist. Open the editor when it is not already open.
 * 
 * @author rgansevles
 * 
 */
public class RetargetToEditorPersistProperties implements IPropertySource, IAdaptable, IDelegate<IModelSavePropertySource>
{
	private final IModelSavePropertySource persistProperties;

	public RetargetToEditorPersistProperties(IModelSavePropertySource persistProperties)
	{
		this.persistProperties = persistProperties;
	}

	@Override
	public IModelSavePropertySource getDelegate()
	{
		return persistProperties;
	}

	public Object getEditableValue()
	{
		return persistProperties.getEditableValue();
	}

	public IPropertyDescriptor[] getPropertyDescriptors()
	{
		return persistProperties.getPropertyDescriptors();
	}

	public Object getPropertyValue(Object id)
	{
		return persistProperties.getPropertyValue(id);
	}

	public boolean isPropertySet(Object id)
	{
		return persistProperties.isPropertySet(id);
	}


	public void resetPropertyValue(final Object id)
	{
		Display.getCurrent().asyncExec(new Runnable()
		{
			public void run()
			{
				updateProperty(false, id, null);
			}
		});
	}

	public void setPropertyValue(final Object id, final Object value)
	{
		// ignore if the value did not change (Was already set), happens when a form property is changed using props view and form node 
		// is selected in the solex tree, setPropertyValue is called with new value while value has already been set.
		if (!Utils.equalObjects(value, persistProperties.getPropertyValue(id)))
		{
			Display.getCurrent().syncExec(new Runnable()
			{
				public void run()
				{
					updateProperty(true, id, value);
				}
			});
		}
	}

	/**
	 * Update the property value via the editor.
	 * 
	 * @param set
	 * @param id
	 * @param value
	 */
	protected void updateProperty(boolean set, Object id, Object value)
	{
		Object model = persistProperties.getSaveModel();

		// find the editor of this persist and change the value in the editor
		IEditorPart editor = EditorUtil.openPersistEditor(model, false); // activate=false here otherwise the editor is activated too soon and the save editor button remains grayed out
		if (editor == null)
		{
			return;
		}

		IPropertySourceProvider propertySourceProvider = (IPropertySourceProvider)editor.getAdapter(IPropertySourceProvider.class);
		if (propertySourceProvider != null)
		{
			IPropertySource propertySource = propertySourceProvider.getPropertySource(model);
			if (propertySource != null)
			{
				if (set)
				{
					propertySource.setPropertyValue(id, value);
				}
				else
				{
					propertySource.resetPropertyValue(id);
				}
			}
		}
	}

	public Object getAdapter(Class adapter)
	{
		return persistProperties instanceof IAdaptable ? ((IAdaptable)persistProperties).getAdapter(adapter) : null;
	}

	@Override
	public String toString()
	{
		return persistProperties.toString();
	}
}
