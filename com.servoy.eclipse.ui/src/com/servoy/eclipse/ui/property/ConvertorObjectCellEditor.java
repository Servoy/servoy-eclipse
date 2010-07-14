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

import org.eclipse.swt.widgets.Composite;

/**
 * Cell editor with a converter to validate the typed text and convert to an object.
 * 
 * @author rgansevles
 */

public class ConvertorObjectCellEditor extends ObjectCellEditor
{

	protected IObjectTextConverter convertor;

	public ConvertorObjectCellEditor(Composite parent, IObjectTextConverter converter)
	{
		super(parent);
		this.convertor = converter;
	}

	@Override
	protected Object doGetObject(String value)
	{
		return convertor.convertToObject(value);
	}

	@Override
	protected String doGetString(Object value)
	{
		return convertor.convertToString(value);
	}

	@Override
	protected String isCorrectObject(Object value)
	{
		return convertor.isCorrectObject(value);
	}

	@Override
	protected String isCorrectString(String value)
	{
		return convertor.isCorrectString(value);
	}

	public interface IObjectTextConverter
	{
		Object convertToObject(String value);

		String convertToString(Object value);

		String isCorrectString(String value);

		String isCorrectObject(Object value);
	}
}
