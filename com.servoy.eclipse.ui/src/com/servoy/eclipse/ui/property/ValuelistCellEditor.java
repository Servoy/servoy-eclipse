/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2023 Servoy BV

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

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.swt.widgets.Composite;

import com.servoy.eclipse.ui.editors.IValueEditor;
import com.servoy.eclipse.ui.editors.ListSelectCellEditor;
import com.servoy.eclipse.ui.labelproviders.ValuelistLabelProvider;

/**
 * @author jcompagner
 *
 */
public class ValuelistCellEditor<P> extends ListSelectCellEditor
{
	private final Object defaultValue;

	/**
	 * @param parent
	 * @param string
	 * @param valuelistContentProvider
	 * @param labelProvider
	 * @param valueListValueEditor
	 * @param readOnly
	 * @param valuelistListOptions
	 * @param none
	 * @param listSelectControlFactory
	 * @param string2
	 */
	public ValuelistCellEditor(Composite parent, String title, ITreeContentProvider contentProvider, ILabelProvider labelProvider,
		IValueEditor< ? > valueEditor, boolean readOnly, Object input, int treeStyle, ListSelectControlFactory controlFactory, String name,
		Object defaultValue)
	{
		super(parent, title, contentProvider, labelProvider, valueEditor, readOnly, input, treeStyle, controlFactory, name);
		this.defaultValue = defaultValue;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.jface.viewers.DialogCellEditor#doGetValue()
	 */
	@Override
	protected Object doGetValue()
	{
		Object value = super.doGetValue();
		// if this value is the none value return the defaultValue that this property should have.
		if (ValuelistLabelProvider.VALUELIST_NONE_STRING.equals(value))
		{
			return defaultValue;
		}
		else
		{
			return value;
		}
	}
}
