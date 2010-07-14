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
package com.servoy.eclipse.ui.util;

import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.widgets.Composite;

/**
 * Text cell editor that adds verify listeners to the text field.
 * 
 * @author rgansevles
 * 
 */
public class VerifyingTextCellEditor extends TextCellEditor
{

	/**
	 * Creates a new text string cell editor with no control The cell editor value is the string itself, which is initially the empty string. Initially, the
	 * cell editor has no cell validator.
	 * 
	 */
	public VerifyingTextCellEditor()
	{
	}

	/**
	 * Creates a new text string cell editor parented under the given control. The cell editor value is the string itself, which is initially the empty string.
	 * Initially, the cell editor has no cell validator.
	 * 
	 * @param parent the parent control
	 */
	public VerifyingTextCellEditor(Composite parent)
	{
		super(parent);
	}

	/**
	 * Creates a new text string cell editor parented under the given control. The cell editor value is the string itself, which is initially the empty string.
	 * Initially, the cell editor has no cell validator.
	 * 
	 * @param parent the parent control
	 * @param style the style bits
	 */
	public VerifyingTextCellEditor(Composite parent, int style)
	{
		super(parent, style);
	}


	/**
	 * Adds the listener to the collection of listeners who will be notified when the receiver's text is verified, by sending it one of the messages defined in
	 * the <code>VerifyListener</code> interface.
	 * 
	 * @param listener the listener which should be notified
	 * 
	 * @exception IllegalArgumentException
	 *                <ul>
	 *                <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
	 *                </ul>
	 * @exception SWTException
	 *                <ul>
	 *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
	 *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
	 *                </ul>
	 * 
	 * @see VerifyListener
	 * @see #removeVerifyListener
	 */
	public void addVerifyListener(VerifyListener listener)
	{
		text.addVerifyListener(listener);
	}


	/**
	 * Removes the listener from the collection of listeners who will be notified when the control is verified.
	 * 
	 * @param listener the listener which should no longer be notified
	 * 
	 * @exception IllegalArgumentException
	 *                <ul>
	 *                <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
	 *                </ul>
	 * @exception SWTException
	 *                <ul>
	 *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
	 *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
	 *                </ul>
	 * 
	 * @see VerifyListener
	 * @see #addVerifyListener
	 */
	public void removeVerifyListener(VerifyListener listener)
	{
		text.removeVerifyListener(listener);
	}

}
