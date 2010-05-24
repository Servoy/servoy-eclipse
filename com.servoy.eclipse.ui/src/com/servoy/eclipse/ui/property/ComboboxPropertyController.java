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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import com.servoy.eclipse.ui.labelproviders.ComboBoxLabelProvider;
import com.servoy.eclipse.ui.util.ModifiedComboBoxCellEditor;
import com.servoy.eclipse.ui.views.properties.IMergeablePropertyDescriptor;
import com.servoy.eclipse.ui.views.properties.IMergedPropertyDescriptor;
import com.servoy.j2db.util.IDelegate;


public class ComboboxPropertyController<T> extends PropertyController<T, Integer> implements IMergeablePropertyDescriptor
{
	public static final String[] EMPTY_STRING_ARRAY = new String[0];
	private final IComboboxPropertyModel<T> model;
	private final String unresolved;

	public ComboboxPropertyController(String id, String displayName, IComboboxPropertyModel<T> model, String unresolved)
	{
		super(id, displayName);
		this.model = model;
		this.unresolved = unresolved;
	}

	public IComboboxPropertyModel<T> getModel()
	{
		return model;
	}

	protected String getWarningMessage()
	{
		return null;
	}

	@Override
	public CellEditor createPropertyEditor(Composite parent)
	{
		return new ModifiedComboBoxCellEditor(parent, EMPTY_STRING_ARRAY, SWT.READ_ONLY)
		{
			@Override
			public void activate()
			{
				// set the items at activation, values may have changed
				Object value = doGetValue();
				setItems(model.getDisplayValues());
				doSetValue(value);

				super.activate();
			}

			@Override
			public String getErrorMessage()
			{
				String warningMessage = getWarningMessage();
				if (warningMessage == null || warningMessage.length() == 0)
				{
					return super.getErrorMessage();
				}
				return warningMessage;
			}
		};
	}

	@Override
	protected IPropertyConverter<T, Integer> createConverter()
	{
		return new ComboboxModelConverter<T>(model);
	}

	@Override
	public ILabelProvider getLabelProvider()
	{
		return new ComboBoxLabelProvider(model, unresolved);
	}


	/**
	 * Convert values based on model index.
	 * 
	 * @author rob
	 * 
	 */
	public static class ComboboxModelConverter<T> implements IPropertyConverter<T, Integer>
	{
		private final IComboboxPropertyModel<T> model;

		public ComboboxModelConverter(IComboboxPropertyModel<T> model)
		{
			this.model = model;
		}

		public Integer convertProperty(Object id, T value)
		{
			Object[] real = model.getRealValues();
			for (int i = 0; i < real.length; i++)
			{
				if (real[i] == value || real[i] != null && real[i].equals(value))
				{
					return new Integer(i);
				}
			}
			return new Integer(-1); // not found
		}

		public T convertValue(Object id, Integer value)
		{
			T[] real = model.getRealValues();
			if (value != null)
			{
				int index = value.intValue();
				if (index >= 0 && index < real.length)
				{
					return real[index];
				}
			}
			return null;
		}
	}


	public boolean isMergeableWith(IMergeablePropertyDescriptor pd)
	{
		return getInnermostDelegate(pd) instanceof ComboboxPropertyController;
	}

	/**
	 * Merge another ComboboxPropertyController with this one, the intersection of the models is used.
	 */
	public IMergedPropertyDescriptor createMergedPropertyDescriptor(IMergeablePropertyDescriptor pd)
	{
		MergedComboboxPropertyController<T> merged = new MergedComboboxPropertyController<T>((String)getId(), getDisplayName(), this.model,
			((ComboboxPropertyController<T>)getInnermostDelegate(pd)).model, unresolved);
		merged.setCategory(getCategory());
		merged.setDescription(getDescription());
		merged.setAlwaysIncompatible(getAlwaysIncompatible());
		merged.setFilterFlags(getFilterFlags());
		merged.setHelpContextIds(getHelpContextIds());
		merged.setReadonly(isReadOnly());
		merged.setValidator(getValidator());
		return merged;
	}


	protected static IMergeablePropertyDescriptor getInnermostDelegate(IMergeablePropertyDescriptor pd)
	{
		while (pd instanceof IDelegate)
		{
			Object delegate = ((IDelegate)pd).getDelegate();
			if (delegate instanceof IMergeablePropertyDescriptor)
			{
				pd = (IMergeablePropertyDescriptor)delegate;
			}
			else
			{
				break;
			}
		}
		return pd;
	}


	/**
	 * Merged ComboboxPropertyController.
	 * 
	 * @author rob
	 * 
	 * @param <T>
	 */
	public static class MergedComboboxPropertyController<T> extends ComboboxPropertyController<T> implements IMergedPropertyDescriptor
	{
		public MergedComboboxPropertyController(String id, String displayName, IComboboxPropertyModel<T> model1, IComboboxPropertyModel<T> model2,
			String unresolved)
		{
			super(id, displayName, mergeComboboxPropertyModel(model1, model2), unresolved);
		}

		/**
		 * Merge 2 models, returns the intersection sorted as in the first model.
		 * 
		 * @param <T>
		 * @param model1
		 * @param model2
		 * @return
		 */
		public static <T> IComboboxPropertyModel<T> mergeComboboxPropertyModel(IComboboxPropertyModel<T> model1, IComboboxPropertyModel<T> model2)
		{
			if (model1 == null || model2 == null)
			{
				return null;
			}

			T[] real1 = model1.getRealValues();
			T[] real2 = model2.getRealValues();
			String[] display1 = model1.getDisplayValues();
			String[] display2 = model2.getDisplayValues();

			Map<Object, String> model2Map = new HashMap<Object, String>();
			for (int i = 0; i < real2.length && i < display2.length; i++)
			{
				model2Map.put(real2[i], display2[i]);
			}

			List<T> real = new ArrayList<T>(Math.min(real1.length, real2.length));
			List<String> display = new ArrayList<String>(Math.min(real1.length, real2.length));
			for (int i = 0; i < real1.length && i < display1.length; i++)
			{
				if (model2Map.containsKey(real1[i]))
				{
					String displayValue2 = model2Map.get(real1[i]);
					if (((display1[i] == null && displayValue2 == null) || (display1[i].equals(displayValue2))))
					{
						real.add(real1[i]);
						display.add(display1[i]);
					}
				}
			}

			return new ComboboxPropertyModel<T>(real.toArray((T[])java.lang.reflect.Array.newInstance(real1.getClass().getComponentType(), real.size())),
				display.toArray(new String[display.size()]));
		}

		/**
		 * Convert from index in this combo to index in pd combo.
		 */
		public Object convertToUnmergedValue(IMergeablePropertyDescriptor pd, Object value)
		{
			T convertedValue = getConverter().convertValue(getId(), (Integer)value);
			return ((ComboboxPropertyController<T>)getInnermostDelegate(pd)).getConverter().convertProperty(getId(), convertedValue);
		}

		/**
		 * Convert from index in pd combo to index in this combo.
		 */
		public Object convertToMergedValue(IMergeablePropertyDescriptor pd, Object value)
		{
			T convertedValue = ((ComboboxPropertyController<T>)getInnermostDelegate(pd)).getConverter().convertValue(getId(), (Integer)value);
			return getConverter().convertProperty(getId(), convertedValue);
		}
	}
}
