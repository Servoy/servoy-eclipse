package com.servoy.eclipse.ui.property;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

/**
 * CellEditorFactory that creates a dummy cell editor, this enables restore-default in the properties view menu.
 * 
 * @author rgansevles
 *
 */
public class DummyCellEditorFactory implements ICellEditorFactory
{
	private final ILabelProvider labelProvider;

	public DummyCellEditorFactory(ILabelProvider labelProvider)
	{
		this.labelProvider = labelProvider;
	}

	public CellEditor createPropertyEditor(Composite parent)
	{
		return new CellEditor(parent, SWT.NONE)
		{
			private Object value;

			@Override
			protected Control createControl(Composite parent)
			{
				return new Label(parent, SWT.NONE);
			}

			@Override
			protected Object doGetValue()
			{
				return value;
			}

			@Override
			protected void doSetFocus()
			{
			}

			@Override
			protected void doSetValue(Object newValue)
			{
				this.value = newValue;
				if (getControl() != null && !getControl().isDisposed())
				{
					((Label)getControl()).setText(labelProvider.getText(newValue));
				}
			}
		};
	}
}