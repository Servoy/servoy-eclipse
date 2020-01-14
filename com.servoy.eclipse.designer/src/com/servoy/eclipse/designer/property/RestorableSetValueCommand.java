/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2020 Servoy BV

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

import com.servoy.eclipse.designer.editor.BaseRestorableCommand;
import com.servoy.eclipse.designer.editor.IPreExecuteCommand;
import com.servoy.eclipse.ui.property.IModelSavePropertySource;

/**
 * @author rgansevles
 *
 */
public class RestorableSetValueCommand extends BaseRestorableCommand implements IPreExecuteCommand
{
	private final IModelSavePropertySource target;
	private final Object propertyId;
	private final Object propertyValue;

	/**
	 * @param label
	 * @param target
	 * @param propertyId
	 * @param propertyValue
	 */
	RestorableSetValueCommand(String label, IModelSavePropertySource target, Object propertyId, Object propertyValue)
	{
		super(label);
		this.target = target;
		this.propertyId = propertyId;
		this.propertyValue = propertyValue;
	}

	@Override
	public void preExecute()
	{
		Object modelBefore = target.getSaveModel();
		Object stateBefore = getState(modelBefore);
		save(modelBefore, stateBefore);
	}

	@Override
	public void execute()
	{
		// do not save model , preExecute should do this
		target.setPropertyValue(propertyId, propertyValue);
	}
}