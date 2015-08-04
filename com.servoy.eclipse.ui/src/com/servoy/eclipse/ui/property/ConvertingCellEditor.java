/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2015 Servoy BV

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
import org.eclipse.swt.widgets.Composite;

import com.servoy.eclipse.core.util.ReturnValueSnippet;

/**
 * Cell editor useful for dealing with property value conversions at cell editor level. I wraps on top of another cell editor.
 * So a property value passes through the convertor before going to the underlying cell editor. The same for underlying cell editor to property value.
 *
 * @author acostescu
 */
public class ConvertingCellEditor<T, CT> extends ProxyCellEditor
{

	public interface ICellEditorConverter<T, CT>
	{
		boolean allowSetToBaseEditor(T outsideWorldValue);

		CT convertValueToBaseEditor(T outsideWorldValue);

		T convertValueFromBaseEditor(CT baseEditorValue);
	}

	protected ICellEditorConverter<T, CT> valueConverter;

	/**
	 * When using this constructor please make sure to call either ({@link #setBaseCellEditor(CellEditor)} or {@link #setBaseCellEditorCreator(ReturnValueSnippet)})
	 * before {@link #create(Composite)} or {@link #createControl(Composite)} are called.
	 *
	 * @param valueConverter value converter between the baseCellEditor and this one.
	 */
	public ConvertingCellEditor(ICellEditorConverter<T, CT> valueConverter)
	{
		super();
		init(valueConverter);
	}

	/**
	 * @param valueConverter value converter between the baseCellEditor and this one.
	 */
	public ConvertingCellEditor(CellEditor baseCellEditor, ICellEditorConverter<T, CT> valueConverter)
	{
		super(baseCellEditor);
		init(valueConverter);
	}

	/**
	 * In case you must use a cell editor that gets it's parent and calls create in it's own constructor, then we need a getter for the editor instead of a direct reference.
	 * This way the editor is created only when {@link #createControl(Composite)} is called on this instance.
	 */
	public ConvertingCellEditor(ReturnValueSnippet<CellEditor, Composite> baseCellEditorCreator, ICellEditorConverter<T, CT> valueConverter)
	{
		super(baseCellEditorCreator);
		init(valueConverter);
	}

	protected void init(ICellEditorConverter<T, CT> valueConverterO)
	{
		this.valueConverter = valueConverterO;
	}

	@Override
	protected Object doGetValue()
	{
		return valueConverter.convertValueFromBaseEditor((CT)baseCellEditor.getValue());
	}

	@Override
	protected void doSetValue(Object outsideWorldValue)
	{
		if (valueConverter.allowSetToBaseEditor((T)outsideWorldValue)) baseCellEditor.setValue(valueConverter.convertValueToBaseEditor((T)outsideWorldValue));
	}

}
