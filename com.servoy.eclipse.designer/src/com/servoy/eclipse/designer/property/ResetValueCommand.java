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
package com.servoy.eclipse.designer.property;

import org.eclipse.gef.commands.Command;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.IPropertySource2;

import com.servoy.eclipse.ui.property.ComplexPropertySourceWithStandardReset;
import com.servoy.eclipse.ui.views.properties.PropertySheetEntry;

/**
 * Command to reset a value on a IPropertySource target.
 * Copied from gef with minor changes.
 *
 * @author rgansevles
 */

public class ResetValueCommand extends Command
{

	/** the property that has to be reset */
	protected Object propertyName;
	/** the current non-default value of the property */
	protected Object undoValue;
	/** the property source whose property has to be reset */
	protected IPropertySource target;
	private PropertySheetEntry propertySheetEntry;

	/**
	 * Default Constructor: Sets the label for the Command
	 *
	 * @since 3.1
	 */
	public ResetValueCommand()
	{
		super("Restore Default Value");
	}

	/**
	 * Returns <code>true</code> IFF:<br>
	 * 1) the target and property have been specified<br>
	 * 2) the property has a default value<br>
	 * 3) the value set for that property is not the default
	 *
	 * @see org.eclipse.gef.commands.Command#canExecute()
	 */
	@Override
	public boolean canExecute()
	{
		boolean answer = false;
		if (target != null && propertyName != null)
		{
			answer = target.isPropertySet(propertyName);
			if (target instanceof IPropertySource2) answer = answer && (((IPropertySource2)target).isPropertyResettable(propertyName));
		}
		return answer;
	}

	/**
	 * Caches the undo value and invokes redo()
	 *
	 * @see org.eclipse.gef.commands.Command#execute()
	 */
	@Override
	public void execute()
	{
		undoValue = target.getPropertyValue(propertyName);
		if (undoValue instanceof IPropertySource) undoValue = ((IPropertySource)undoValue).getEditableValue();
		redo();
	}

	/**
	 * Sets the IPropertySource.
	 *
	 * @param propSource the IPropertySource whose property has to be reset
	 */
	public void setTarget(IPropertySource propSource)
	{
		target = propSource;
	}

	public void setPropertySheetEntry(PropertySheetEntry propertySheetEntry)
	{
		this.propertySheetEntry = propertySheetEntry;
	}

	/**
	 * Resets the specified property on the specified IPropertySource
	 *
	 * @see org.eclipse.gef.commands.Command#redo()
	 */
	@Override
	public void redo()
	{
		Object restValue;
		if (target instanceof ComplexPropertySourceWithStandardReset< ? >)
		{
			restValue = ((ComplexPropertySourceWithStandardReset< ? >)target).resetComplexPropertyValue(propertyName);
		}
		else
		{
			target.resetPropertyValue(propertyName);
			restValue = target.getPropertyValue(propertyName);
		}
		if (propertySheetEntry != null)
		{
			propertySheetEntry.setValue(restValue);
		}
	}

	/**
	 * Sets the property that is to be reset.
	 *
	 * @param pName the property to be reset
	 */
	public void setPropertyId(Object pName)
	{
		propertyName = pName;
	}

	/**
	 * Restores the non-default value that was reset.
	 *
	 * @see org.eclipse.gef.commands.Command#undo()
	 */
	@Override
	public void undo()
	{
		target.setPropertyValue(propertyName, undoValue);
	}
}
