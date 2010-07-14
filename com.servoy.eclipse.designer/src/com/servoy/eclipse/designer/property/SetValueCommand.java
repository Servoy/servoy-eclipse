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

/**
 * Command to set a value on a IPropertySource target.
 * Copied from gef with minor changes.
 * 
 * @author rgansevles
 */


public class SetValueCommand extends Command
{

	protected Object propertyValue;
	protected Object propertyName;
	protected Object undoValue;
	protected boolean resetOnUndo;
	protected IPropertySource target;

	public SetValueCommand()
	{
		super(""); //$NON-NLS-1$
	}

	public SetValueCommand(String propLabel)
	{
		// hard-coded for optimization, this gets called very often if many form elements are moved at the same time in form designer.
		//	super(MessageFormat.format(GEFMessages.SetPropertyValueCommand_Label, new Object[] { propLabel }).trim());
		super("Set " + propLabel + " Property");

	}

	@Override
	public boolean canExecute()
	{
		return true;
	}

	@Override
	public void execute()
	{
/*
 * Fix for Bug# 54250 IPropertySource.isPropertySet(String) returns false both when there is no default value, and when there is a default value and the
 * property is set to that value. To correctly determine if a reset should be done during undo, we compare the return value of isPropertySet(String) before and
 * after setPropertyValue(...) is invoked. If they are different (it must have been false before and true after -- it cannot be the other way around), then that
 * means we need to reset.
 */
		boolean wasPropertySet = getTarget().isPropertySet(propertyName);
		undoValue = getTarget().getPropertyValue(propertyName);
		if (undoValue instanceof IPropertySource) undoValue = ((IPropertySource)undoValue).getEditableValue();
		if (propertyValue instanceof IPropertySource) propertyValue = ((IPropertySource)propertyValue).getEditableValue();
		getTarget().setPropertyValue(propertyName, propertyValue);
		if (getTarget() instanceof IPropertySource2) resetOnUndo = !wasPropertySet && ((IPropertySource2)getTarget()).isPropertyResettable(propertyName);
		else resetOnUndo = !wasPropertySet && getTarget().isPropertySet(propertyName);
		if (resetOnUndo) undoValue = null;
	}

	public IPropertySource getTarget()
	{
		return target;
	}

	public void setTarget(IPropertySource aTarget)
	{
		target = aTarget;
	}

	@Override
	public void redo()
	{
		execute();
	}

	public void setPropertyId(Object pName)
	{
		propertyName = pName;
	}

	public void setPropertyValue(Object val)
	{
		propertyValue = val;
	}

	@Override
	public void undo()
	{
		if (resetOnUndo) getTarget().resetPropertyValue(propertyName);
		else getTarget().setPropertyValue(propertyName, undoValue);
	}
}
