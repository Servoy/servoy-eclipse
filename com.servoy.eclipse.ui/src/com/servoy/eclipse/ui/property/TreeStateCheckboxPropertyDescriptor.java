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

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ICellEditorListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import com.servoy.eclipse.ui.Activator;

/**
 * Property controller for boolean properties.
 *
 * @author rgansevles
 *
 */

public class TreeStateCheckboxPropertyDescriptor extends PropertyDescriptorWithTooltip
{
	static final CheckboxLabelProvider LABEL_PROVIDER = new CheckboxLabelProvider();

	public TreeStateCheckboxPropertyDescriptor(Object id, String displayName)
	{
		super(id, displayName);
	}

	@Override
	public CellEditor createPropertyEditor(Composite parent)
	{
		return new CheckboxCellEditor(parent)
		{
			@Override
			public void activate()
			{
				markDirty();
				super.activate();
			}
		};
	}

	@Override
	public ILabelProvider getLabelProvider()
	{
		return LABEL_PROVIDER;
	}


	/**
	 * Boolean type property editor. It will default to boolean unless the initData string is "class". In that case it will be for java.lang.Boolean.
	 */
	public static class CheckboxCellEditor extends CellEditor
	{

		/**
		 * The checkbox value.
		 */
		/* package */
		Boolean value = null;

		/**
		 * Default CheckboxCellEditor style
		 */
		private static final int defaultStyle = SWT.NONE;

		/**
		 * Creates a new checkbox cell editor parented under the given control. The cell editor value is a boolean value, which is initially <code>false</code>.
		 * Initially, the cell editor has no cell validator.
		 *
		 * @param parent the parent control
		 */
		public CheckboxCellEditor(Composite parent)
		{
			this(parent, defaultStyle);
		}

		/**
		 * Creates a new checkbox cell editor parented under the given control. The cell editor value is a boolean value, which is initially <code>false</code>.
		 * Initially, the cell editor has no cell validator.
		 *
		 * @param parent the parent control
		 * @param style the style bits
		 */
		public CheckboxCellEditor(Composite parent, int style)
		{
			super(parent, style);
		}

		@Override
		protected Control createControl(final Composite parent)
		{
			CLabel label = new CLabel(parent, SWT.NONE);
			label.setMargins(5, 0, 0, 0);
			label.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));
			label.addMouseListener(new MouseAdapter()
			{
				@Override
				public void mouseUp(MouseEvent e)
				{
					toggle();
				}
			});
			label.setText(LABEL_PROVIDER.getText(value));

			CheckboxCellEditor.this.addListener(new ICellEditorListener()
			{
				@Override
				public void applyEditorValue()
				{
					label.setText(LABEL_PROVIDER.getText(value));
				}

				@Override
				public void editorValueChanged(boolean oldValidState, boolean newValidState)
				{
				}

				@Override
				public void cancelEditor()
				{
				}
			});

			return label;
		}

		/**
		 * The object is being passed in, return the index to be used in the editor.
		 *
		 * It should return sNoSelection if the value can't be converted to a index. The errormsg will have already been set in this case.
		 */

		@Override
		public void activate()
		{
			toggle();
		}

		protected void toggle()
		{
			if (value == null) value = Boolean.TRUE;
			else if (value == Boolean.TRUE) value = Boolean.FALSE;
			else value = null;
			markDirty();
			fireApplyEditorValue();
		}


		/**
		 * The <code>CheckboxCellEditor</code> implementation of this <code>CellEditor</code> framework method returns the checkbox setting wrapped as a
		 * <code>Boolean</code>.
		 *
		 * @return the Boolean checkbox value
		 */
		@Override
		protected Object doGetValue()
		{
			return value;
		}

		/*
		 * (non-Javadoc) Method declared on CellEditor.
		 */
		@Override
		protected void doSetFocus()
		{
			// Ignore
		}

		/**
		 * The <code>CheckboxCellEditor</code> implementation of this <code>CellEditor</code> framework method accepts a value wrapped as a
		 * <code>Boolean</code> .
		 *
		 * @param val a Boolean value
		 */
		@Override
		protected void doSetValue(Object val)
		{
			this.value = val instanceof Boolean ? (Boolean)val : null;
		}
	}


	/**
	 * Label provider for checkboxes.
	 *
	 * @author rgansevles
	 *
	 */
	public static class CheckboxLabelProvider extends BaseLabelProvider implements ILabelProvider
	{
		public static final Image NULL_IMAGE = Activator.getDefault().loadImageFromBundle("check_null.png");
		public static final Image TRUE_IMAGE = Activator.getDefault().loadImageFromBundle("check_on.png");
		public static final Image FALSE_IMAGE = Activator.getDefault().loadImageFromBundle("check_off.png");

		public Image getImage(Object element)
		{
			if (element == null) return NULL_IMAGE;
			return Boolean.TRUE.equals(element) ? TRUE_IMAGE : FALSE_IMAGE;
		}

		public String getText(Object element)
		{
			if (element == null) return " unset [default]";
			return Boolean.TRUE.equals(element) ? " true" : " false";
		}
	}
}
