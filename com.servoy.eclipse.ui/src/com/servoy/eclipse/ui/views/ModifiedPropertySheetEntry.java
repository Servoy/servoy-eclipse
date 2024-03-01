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
package com.servoy.eclipse.ui.views;

import static com.servoy.j2db.util.ComponentFactoryHelper.createBorderString;
import static java.util.Arrays.stream;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.border.Border;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;

import com.servoy.eclipse.ui.EditorActionsRegistry;
import com.servoy.eclipse.ui.EditorActionsRegistry.EditorComponentActions;
import com.servoy.eclipse.ui.views.properties.IMergeablePropertyDescriptor;
import com.servoy.eclipse.ui.views.properties.IMergedPropertyDescriptor;
import com.servoy.eclipse.ui.views.properties.PropertySheetEntry;
import com.servoy.j2db.dataprocessing.IModificationListener;

/**
 * PropertySheetEntry with additional Servoy features.
 *
 * @author acostescu
 */
public class ModifiedPropertySheetEntry extends PropertySheetEntry implements IAdaptable
{
	private Object[] editValues;
	private boolean hasDefaultValue;
	private IModificationListener ragtestHandler;

	public ModifiedPropertySheetEntry()
	{
		this(false);
	}

	public ModifiedPropertySheetEntry(boolean ragtest)
	{
		if (ragtest)
		{
			ragtestHandler = EditorActionsRegistry.addRagtest(EditorComponentActions.CREATE_CUSTOM_COMPONENT, event -> refreshFromRoot());
		}
	}

	/**
	 * @see org.eclipse.ui.views.properties.PropertySheetEntry#createChildEntry()
	 */
	@Override
	protected PropertySheetEntry createChildEntry()
	{
		return new ModifiedPropertySheetEntry();
	}

	/*
	 * (non-Javadoc) Method declared on IPropertySheetEntry. Also uses label provider for null value.
	 */
	@Override
	public String getValueAsString()
	{
		if (getValues().length == 0)
		{
			return "";
		}
		Object editValue = getEditValue(0);
		ILabelProvider provider = getDescriptor().getLabelProvider();
		if (provider == null)
		{
			return editValue == null ? "" : editValue.toString();
		}
		String text = provider.getText(editValue);
		if (text == null)
		{
			return "";
		}
		return text;
	}

	public IPropertySource[] getPropertySources()
	{
		Object[] values = getValues();
		IPropertySource[] propertySources = new IPropertySource[values.length];
		for (int i = 0; i < values.length; i++)
		{
			propertySources[i] = getPropertySource(values[i]);
		}
		return propertySources;
	}

	public Object getAdapter(Class adapter)
	{
		// allow delegating of comparing to the descriptor
		if (Comparable.class == adapter && getDescriptor() instanceof IAdaptable)
		{
			return ((IAdaptable)getDescriptor()).getAdapter(adapter);
		}
		return null;
	}


	/**
	 * Return the unsorted intersection of all the <code>IPropertyDescriptor</code>s for the objects.
	 *
	 * @return List
	 */
	@Override
	protected List computeMergedPropertyDescriptors()
	{
		if (getValues().length <= 1)
		{
			return super.computeMergedPropertyDescriptors();
		}

		// get all descriptors from each object
		Map[] propertyDescriptorMaps = new Map[getValues().length];
		for (int i = 0; i < getValues().length; i++)
		{
			Object object = getValues()[i];
			IPropertySource source = getPropertySource(object);
			if (source == null)
			{
				// if one of the selected items is not a property source
				// then we show no properties
				return new ArrayList(0);
			}
			// get the property descriptors keyed by id
			propertyDescriptorMaps[i] = computePropertyDescriptorsFor(source);
		}

		// intersect
		Map intersection = propertyDescriptorMaps[0];
		for (int i = 1; i < propertyDescriptorMaps.length; i++)
		{
			// get the current ids
			Object[] ids = intersection.keySet().toArray();
			for (Object id : ids)
			{
				Object object = propertyDescriptorMaps[i].get(id);
				if (object == null ||
					// see if the descriptors (which have the same id) are
					// compatible
					!((IPropertyDescriptor)intersection.get(id)).isCompatibleWith((IPropertyDescriptor)object))
				{
					intersection.remove(id);
				}
			}
		}

		IPropertySource firstSource = getPropertySource(getValues()[0]);
		// sorting is handled in the PropertySheetViewer, return unsorted (in
		// the original order)
		ArrayList result = new ArrayList(intersection.size());
		IPropertyDescriptor[] firstDescs = firstSource.getPropertyDescriptors();
		for (IPropertyDescriptor desc : firstDescs)
		{
			if (intersection.containsKey(desc.getId()))
			{
				IPropertyDescriptor pd = desc;
				for (int i = 1; pd instanceof IMergeablePropertyDescriptor && i < getValues().length; i++)
				{
					IPropertySource nextSource = getPropertySource(getValues()[i]);
					for (IPropertyDescriptor desc2 : nextSource.getPropertyDescriptors())
					{
						if (desc.getId().equals(desc2.getId()))
						{
							if (desc2 instanceof IMergeablePropertyDescriptor &&
								((IMergeablePropertyDescriptor)pd).isMergeableWith((IMergeablePropertyDescriptor)desc2))
							{
								pd = ((IMergeablePropertyDescriptor)pd).createMergedPropertyDescriptor((IMergeablePropertyDescriptor)desc2);
							}
							break;
						}
					}
				}
				if (pd != null)
				{
					result.add(pd);
				}
			}
		}
		return result;
	}

	/**
	 * Update our value objects. We ask our parent for the property values based on our descriptor.
	 */
	@Override
	protected void refreshValues()
	{
		if (!(getDescriptor() instanceof IMergedPropertyDescriptor))
		{
			super.refreshValues();
			return;
		}

		// get our parent's value objects
		Object[] currentSources = getParent().getValues();

		// loop through the objects getting our property value from each
		Object[] newValues = new Object[currentSources.length];
		for (int i = 0; i < currentSources.length; i++)
		{
			IPropertySource source = getParent().getPropertySource(currentSources[i]);
			Object propertyValue = source.getPropertyValue(getDescriptor().getId());
			if (getDescriptor() instanceof IMergedPropertyDescriptor)
			{
				for (IPropertyDescriptor desc : source.getPropertyDescriptors())
				{
					if (getDescriptor().getId().equals(desc.getId()) && desc instanceof IMergeablePropertyDescriptor)
					{
						propertyValue = ((IMergedPropertyDescriptor)getDescriptor()).convertToMergedValue((IMergeablePropertyDescriptor)desc, propertyValue);
						break;
					}
				}
			}
			newValues[i] = propertyValue;
		}

		// set our new values
		setValues(newValues);
	}

	@Override
	public void applyEditorValue()
	{
		if (editor == null)
		{
			return;
		}

		// Check if editor has a valid value
		if (!editor.isValueValid())
		{
			setErrorText(editor.getErrorMessage());
			return;
		}

		setErrorText(null);

		// See if the value changed and if so update
		Object newValue = editor.getValue();
		if (hasDefaultValue || stream(editValues).noneMatch(el -> valueEquals(newValue, el)))
		{
			// Set the editor value
			setValue(newValue);
		}
	}

	@Override
	public void setValuesInternal(Object[] objects, boolean hasDefaultValue)
	{
		super.setValuesInternal(objects, hasDefaultValue);
		if (objects.length == 0)
		{
			editValues = null;
			this.hasDefaultValue = false;
		}
		else
		{
			editValues = new Object[objects.length];
			for (int i = 0; i < objects.length; i++)
			{
				Object newValue = objects[i];
				// see if we should convert the value to an editable value
				IPropertySource source = getPropertySource(newValue);
				if (source != null)
				{
					newValue = source.getEditableValue();
				}
				editValues[i] = newValue;
			}
			this.hasDefaultValue = hasDefaultValue;
		}
	}

	private static boolean valueEquals(Object val1, Object val2)
	{
		if (val1 == val2)
		{
			return true;
		}

		if (val1 == null || val2 == null)
		{
			return false;
		}

		// special cases, borders (like LineBorder) do not implement equals based on fields.
		if (val1 instanceof Border && val2 instanceof Border)
		{
			return createBorderString(val1).equals(createBorderString(val2));
		}

		if (val1 instanceof Object[] && val2 instanceof Object[] && ((Object[])val1).length == ((Object[])val2).length)
		{
			Object[] arr1 = (Object[])val1;
			Object[] arr2 = (Object[])val2;
			for (int i = 0; i < arr1.length; i++)
			{
				if (!valueEquals(arr1[i], arr2[i])) return false;
			}
			return true;
		}

		return val1.equals(val2);
	}


	public CellEditor getCreatedEditor()
	{
		return editor;
	}

	/* just to make it available to classes in this package as well */
	@Override
	protected IPropertyDescriptor getDescriptor()
	{
		return super.getDescriptor();
	}

	@Override
	public void dispose()
	{
		if (ragtestHandler != null)
		{
			EditorActionsRegistry.removeRagtest(EditorComponentActions.CREATE_CUSTOM_COMPONENT, ragtestHandler);
		}

		super.dispose();
	}
}
