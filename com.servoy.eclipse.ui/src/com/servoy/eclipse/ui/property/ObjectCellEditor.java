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

import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.widgets.Composite;

/**
 * This is a cell editor that works on text but expects the values edited and returned to be objects. It provides the necessary interfaces to work with objects and to handle the
 * strings that go to and from the text field.
 */
public abstract class ObjectCellEditor extends TextCellEditor
{
	/**
	 * The set value in the cell editor.
	 */
	private Object setValue;

	public ObjectCellEditor(Composite parent)
	{
		super(parent);
	}

	/**
	 * Return the value of the
	 */
	@Override
	protected Object doGetValue()
	{
		return setValue;
	}

	/**
	 * Subclassed need to implement returning the object that the string represents. This is called when editing and a string from the text editor is sent in and we need to send
	 * the object that it represents up to the validators. The string to convert will be passed in.
	 */
	protected abstract Object doGetObject(String value);

	/**
	 * This returns the string that is in the editor. Implementers can use this to get the string to create the object.
	 */
	protected final String doGetEditorString()
	{
		return (String)super.doGetValue();
	}

	/**
	 * This override to doSetValue allows nulls to be entered. It will pass the value entered to doGetString() to return the string to send on up to the text editor.
	 * 
	 * doSetObject will be called to allow the implementers to save it if they wish.
	 */
	@Override
	protected void doSetValue(Object value)
	{
		setValue = value;
		doSetObject(value); // Let implementers do something with it.
		String v = doGetString(value);
		doSetEditorString(v != null ? v : ""); //$NON-NLS-1$
	}

	/**
	 * This is called when a doSetValue has been called.
	 * 
	 * This is not abstract, but a default implementation of doSetObject. It does nothing. Implementers may do something else with it, such as store for easier retrieval later.
	 */
	protected void doSetObject(@SuppressWarnings("unused")
	Object value)
	{
	}

	/**
	 * The object is being passed in, return the string to be used in the editor.
	 * 
	 * It should return null if the value can't be converted to a string. The errormsg will have already been set in this case.
	 */
	protected abstract String doGetString(Object value);

	/**
	 * This is private because one should not set the string outside of doSetValue. It that is done, then there would be a value set that doesn't represent the currently set value.
	 */
	private void doSetEditorString(String value)
	{
		super.doSetValue(value);
	}

	/**
	 * Return the currently set value.
	 */
	protected final Object getSetValue()
	{
		return setValue;
	}

	/**
	 * This handles isCorrect. It unfortunately is called both with the string from the text editor and the value when originally set. Because of this we need to test and if a
	 * string send to isCorrectString(), else we send to isCorrectObject(). If these return null or empty string, we will then let the super handle it so that the verifiers can be
	 * used. If they return a string, then we will set the error message, and return false.
	 */
	@Override
	protected boolean isCorrect(Object value)
	{
		String errMsg = null;
		if (value instanceof String)
		{
			errMsg = isCorrectString((String)value);
			if (errMsg == null || errMsg.length() == 0) value = doGetObject((String)value); // Convert to an object so validators can test it.
		}
		else errMsg = isCorrectObject(value);

		if (errMsg == null || errMsg.length() == 0) return super.isCorrect(value);

		// We had an error, so set error message and return false.
		setErrorMessage(errMsg);
		return false;
	}

	/**
	 * Implement this method to verify if the string represents a good object. This is usually from the text editor itself. Though it could be coming directly in from the setting,
	 * but we don't know this.
	 */
	protected abstract String isCorrectString(String value);

	/**
	 * Implement this method to verify if this is a good object. This would be coming from a direct setting.
	 */
	protected abstract String isCorrectObject(Object value);


	@Override
	protected void valueChanged(boolean oldValidState, boolean newValidState)
	{
		super.valueChanged(oldValidState, newValidState);
		if (newValidState) setValue = doGetObject(doGetEditorString()); // Save the object for later retrieval.
	}
}
