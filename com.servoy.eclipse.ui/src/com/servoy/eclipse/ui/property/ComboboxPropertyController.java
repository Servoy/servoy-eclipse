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
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.team.internal.ui.Utils;

import com.servoy.eclipse.ui.editors.DialogCellEditor;
import com.servoy.eclipse.ui.editors.DialogCellEditor.ValueEditorCellLayout;
import com.servoy.eclipse.ui.editors.IValueEditor;
import com.servoy.eclipse.ui.labelproviders.ComboBoxLabelProvider;
import com.servoy.eclipse.ui.util.ModifiedComboBoxCellEditor;
import com.servoy.eclipse.ui.views.properties.IMergeablePropertyDescriptor;
import com.servoy.eclipse.ui.views.properties.IMergedPropertyDescriptor;
import com.servoy.j2db.util.IDelegate;

/**
 * Property controller for properties with a fixed list of values to be shown in a combo box.
 *
 * @author rgansevles
 */

public class ComboboxPropertyController<T> extends PropertyController<T, Integer> implements IMergeablePropertyDescriptor
{
	public static final String[] EMPTY_STRING_ARRAY = new String[0];
	private final IComboboxPropertyModel<T> model;
	private final String unresolved;
	private final IValueEditor valueEditor;

	public ComboboxPropertyController(Object id, String displayName, IComboboxPropertyModel<T> model, String unresolved)
	{
		this(id, displayName, model, unresolved, null);
	}

	public ComboboxPropertyController(Object id, String displayName, IComboboxPropertyModel<T> model, String unresolved, IValueEditor valueEditor)
	{
		super(id, displayName);
		this.model = model;
		this.unresolved = unresolved;
		this.valueEditor = valueEditor;
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
		ModifiedComboBoxCellEditor editor = new ModifiedComboBoxCellEditor(parent, EMPTY_STRING_ARRAY, SWT.READ_ONLY, model.getDefaultValueIndex() == 0)
		{
			private Button editorButton;

			@Override
			public void activate()
			{
				// set the items at activation, values may have changed
				Object value = doGetValue();
				setItems(model.getDisplayValues());
				doSetValue(value);
				if (valueEditor != null)
				{
					editorButton.setEnabled(valueEditor.canEdit(value));
				}

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

			@Override
			protected Control createControl(Composite parent)
			{
				Composite composite = null;
				CCombo combo = null;
				if (valueEditor != null)
				{
					composite = new Composite(parent, SWT.None);
					combo = (CCombo)super.createControl(composite);
					editorButton = new Button(composite, SWT.FLAT);
					editorButton.setImage(DialogCellEditor.OPEN_IMAGE);
					editorButton.addMouseListener(new MouseAdapter()
					{
						@Override
						public void mouseDown(org.eclipse.swt.events.MouseEvent e)
						{
							valueEditor.openEditor(doGetValue());
						}
					});
					ValueEditorCellLayout layout = new ValueEditorCellLayout();
					layout.setValueEditor(valueEditor);
					composite.setLayout(layout);
				}
				else
				{
					combo = (CCombo)super.createControl(parent);
					composite = combo;
				}
				combo.addSelectionListener(new SelectionAdapter()
				{
					@Override
					public void widgetSelected(SelectionEvent event)
					{
						// the selection is already updated at this point using the SelectionAdapter created in super.createControl()
						if (valueEditor != null)
						{
							editorButton.setEnabled(valueEditor.canEdit(doGetValue()));
						}
						fireApplyEditorValue();
					}
				});
				return composite;
			}
		};
		return editor;
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
	 * @author rgansevles
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
			// go backwards, in case value is in default value as well, show the real value instead of DEFAULT
			for (int i = real.length - 1; i >= 0; i--)
			{
				if (real[i] == value || real[i] != null && i != model.getDefaultValueIndex() && real[i].equals(value))
				{
					return Integer.valueOf(i);
				}
			}

			return Integer.valueOf(
				value == null || (model.getDefaultValueIndex() != -1 && value.equals(real[model.getDefaultValueIndex()])) ? model.getDefaultValueIndex() : -1);
		}

		public T convertValue(Object id, Integer value)
		{
			T[] real = model.getRealValues();
			if (value != null)
			{
				int index = value.intValue();
				if (index >= 0 && index < real.length && index != model.getDefaultValueIndex())
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
	 * @author rgansevles
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

			ComboboxPropertyModel model = new ComboboxPropertyModel<T>(
				real.toArray((T[])java.lang.reflect.Array.newInstance(real1.getClass().getComponentType(), real.size())),
				display.toArray(new String[display.size()]));
			if (model1.getDefaultValueIndex() >= 0 && model2.getDefaultValueIndex() >= 0 &&
				Utils.equalObject(real1[model1.getDefaultValueIndex()], real2[model2.getDefaultValueIndex()]) &&
				Utils.equalObject(display1[model1.getDefaultValueIndex()], display2[model2.getDefaultValueIndex()]))
			{
				model.setDefaultValueIndex(real.indexOf(real1[model1.getDefaultValueIndex()]));
			}
			return model;
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

	@Override
	public String toString()
	{
		return "ComboboxPropertyController [" + getDisplayName() + "]";
	}
}