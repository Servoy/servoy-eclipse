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
package com.servoy.eclipse.ui.editors.table;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.TableViewer;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.ui.dialogs.MethodDialog.MethodListOptions;
import com.servoy.eclipse.ui.editors.MethodCellEditor;
import com.servoy.eclipse.ui.labelproviders.MethodLabelProvider;
import com.servoy.eclipse.ui.property.MethodPropertyController.MethodValueEditor;
import com.servoy.eclipse.ui.property.MethodWithArguments;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.util.MapEntryValueEditor.IRelayEditorProvider;
import com.servoy.j2db.dataprocessing.IPropertyDescriptor;
import com.servoy.j2db.dataprocessing.IPropertyDescriptorProvider;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.util.ScopesUtils;

public class RelayEditorProvider implements IRelayEditorProvider
{
	private IPropertyDescriptorProvider propertyProvider;

	private ComboBoxCellEditor comboCellEditor;
	private final Map<String, MethodCellEditor> methodCellEditors = new HashMap<String, MethodCellEditor>();

	/**
	 * @see com.servoy.eclipse.ui.util.MapEntryValueEditor.IRelayEditorProvider#getCellEditor(java.lang.Object, org.eclipse.jface.viewers.TableViewer)
	 */
	public CellEditor getCellEditor(String key, TableViewer parent)
	{
		if (propertyProvider != null)
		{
			IPropertyDescriptor propertyDescriptor = propertyProvider.getPropertyDescriptor(key);
			if (propertyDescriptor != null)
			{
				if (propertyDescriptor.getType() == IPropertyDescriptor.GLOBAL_METHOD)
				{
					MethodCellEditor methodCellEditor = methodCellEditors.get(key);
					if (methodCellEditor == null)
					{
						Solution solution = ServoyModelManager.getServoyModelManager().getServoyModel().getFlattenedSolution().getSolution();
						PersistContext persistContext = PersistContext.create(solution);
						// The global method plugins are not tied to a solution, so we cannot filter for private methods here
						MethodLabelProvider methodLabelProvider = new MethodLabelProvider(persistContext, false, false);
						methodCellEditor = new MethodCellEditor(parent.getTable(), methodLabelProvider, new MethodValueEditor(persistContext), persistContext,
							key, false, new MethodListOptions(true, false, false, true, false, null));
						methodCellEditors.put(key, methodCellEditor);
					}
					return methodCellEditor;
				}
				else if (propertyDescriptor.getChoices() != null)
				{
					if (comboCellEditor == null)
					{
						comboCellEditor = new ComboBoxCellEditor(parent.getTable(), propertyDescriptor.getChoices());
					}
					else
					{
						comboCellEditor.setItems(propertyDescriptor.getChoices());
					}
					return comboCellEditor;
				}
			}
		}
		// default just return null
		return null;
	}

	/**
	 * @param conv
	 * @param op
	 */
	public void setPropertyDescriptorProvider(IPropertyDescriptorProvider provider)
	{
		this.propertyProvider = provider;
	}

	/**
	 * @see com.servoy.eclipse.ui.util.MapEntryValueEditor.IRelayEditorProvider#convertGetValue(java.lang.String, java.lang.Object)
	 */
	public Object convertGetValue(String key, Object value)
	{
		Object realValue = value;
		if (propertyProvider != null)
		{
			IPropertyDescriptor propertyDescriptor = propertyProvider.getPropertyDescriptor(key);
			if (propertyDescriptor != null)
			{
				if (propertyDescriptor.getType() == IPropertyDescriptor.GLOBAL_METHOD)
				{
					realValue = value instanceof MethodWithArguments ? value : MethodWithArguments.METHOD_NONE;
					if (value instanceof String)
					{
						ScriptMethod scriptMethod = ServoyModelManager.getServoyModelManager().getServoyModel().getFlattenedSolution().getScriptMethod(null,
							(String)value);
						if (scriptMethod != null)
						{
							realValue = MethodWithArguments.create(scriptMethod, null);
						}
					}
				}
				else
				{
					String[] choices = propertyDescriptor.getChoices();
					if (choices != null)
					{
						realValue = Integer.valueOf(0);
						if (value != null)
						{
							String[] items2 = choices;
							for (int i = 0; i < items2.length; i++)
							{
								if (items2[i].equals(value))
								{
									realValue = Integer.valueOf(i);
									break;
								}
							}
						}
					}
				}
			}
		}
		return realValue;
	}

	/**
	 * @see com.servoy.eclipse.ui.util.MapEntryValueEditor.IRelayEditorProvider#convertSetValue(java.lang.String, java.lang.Object)
	 */
	public Object convertSetValue(String key, Object value)
	{
		Object realValue = value;
		if (propertyProvider != null)
		{
			IPropertyDescriptor propertyDescriptor = propertyProvider.getPropertyDescriptor(key);
			if (propertyDescriptor != null)
			{
				if (propertyDescriptor.getType() == IPropertyDescriptor.GLOBAL_METHOD)
				{
					if (value instanceof MethodWithArguments)
					{
						ScriptMethod scriptMethod = ServoyModelManager.getServoyModelManager().getServoyModel().getFlattenedSolution().getScriptMethod(
							((MethodWithArguments)value).methodUUID);
						if (scriptMethod != null)
						{
							realValue = ScopesUtils.getScopeString(scriptMethod);
						}
						else
						{
							realValue = null;
						}
					}
				}
				else
				{
					String[] choices = propertyDescriptor.getChoices();
					if (choices != null && value instanceof Integer)
					{
						realValue = choices[((Integer)value).intValue()];
					}
				}
			}
		}
		return realValue;
	}

	/**
	 * @see com.servoy.eclipse.ui.util.MapEntryValueEditor.IRelayEditorProvider#getLabel(java.lang.String)
	 */
	public String getLabel(String key)
	{
		if (propertyProvider != null)
		{
			IPropertyDescriptor propertyDescriptor = propertyProvider.getPropertyDescriptor(key);
			if (propertyDescriptor != null)
			{
				return propertyDescriptor.getLabel();
			}
		}
		return key;
	}
}