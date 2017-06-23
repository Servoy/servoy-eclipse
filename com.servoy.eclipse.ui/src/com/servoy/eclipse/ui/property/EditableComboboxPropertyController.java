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

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import com.servoy.eclipse.ui.editors.DialogCellEditor;
import com.servoy.eclipse.ui.editors.DialogCellEditor.ValueEditorCellLayout;
import com.servoy.eclipse.ui.editors.IValueEditor;
import com.servoy.eclipse.ui.util.ModifiedComboBoxCellEditor;

/**
 * An cell editor for a text field that can be edited.
 * <p>
 * A combobox is shown with predefined values.
 *
 * @author rgansevles
 *
 */

public class EditableComboboxPropertyController extends PropertyController<String, String>
{
	private final String[] displayValues;
	private final IValueEditor valueEditor;

	private static final ILabelProvider LABEL_PROVIDER = new LabelProvider()
	{
		@Override
		public String getText(Object element)
		{
			return element == null ? "" : element.toString();
		}
	};

	public EditableComboboxPropertyController(String id, String displayName, String[] displayValues, IValueEditor valueEditor)
	{
		super(id, displayName);
		this.displayValues = displayValues;
		this.valueEditor = valueEditor;
	}

	@Override
	public CellEditor createPropertyEditor(final Composite parent)
	{
		return new ModifiedComboBoxCellEditor(parent, displayValues, SWT.NONE) // not SWT.READ_ONLY
		{
			private CCombo comboBox;
			private Button editorButton;

			@Override
			public void activate()
			{
				if (valueEditor != null)
				{
					editorButton.setEnabled(valueEditor.canEdit(comboBox.getText()));
				}
				super.activate();
			}

			@Override
			protected Control createControl(Composite controlParent)
			{
				Composite composite = null;
				if (valueEditor != null)
				{
					composite = new Composite(parent, SWT.None);
					comboBox = (CCombo)super.createControl(composite);
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
					comboBox = (CCombo)super.createControl(parent);
					composite = comboBox;
				}
				comboBox.addSelectionListener(new SelectionAdapter()
				{
					@Override
					public void widgetSelected(SelectionEvent event)
					{
						// the selection is already updated at this point using the SelectionAdapter created in super.createControl()
						if (valueEditor != null)
						{
							editorButton.setEnabled(valueEditor.canEdit(doGetValue()));
						}
					}
				});
				return composite;
			}

			@Override
			protected void doSetValue(Object value)
			{
				comboBox.setText(LABEL_PROVIDER.getText(value));
			}

			@Override
			protected Object doGetValue()
			{
				return comboBox.getText();
			}
		};
	}

	@Override
	public ILabelProvider getLabelProvider()
	{
		return LABEL_PROVIDER;
	}
}
