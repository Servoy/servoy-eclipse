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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.IPropertySource2;

import com.servoy.eclipse.designer.editor.BaseRestorableCommand;
import com.servoy.eclipse.ui.property.IModelSavePropertySource;
import com.servoy.j2db.util.Pair;

/**
 * Command to set a value on a IPropertySource target.
 * Save state for undo.
 *
 * @author rgansevles
 */

public class SetValueCommand extends Command
{
	private static Comparator<Pair<String, Object>> PAIR_LEFT_KEY_COMPARATOR = new Comparator<Pair<String, Object>>()
	{
		@Override
		public int compare(Pair<String, Object> o1, Pair<String, Object> o2)
		{
			return o1.getLeft().compareTo(o2.getLeft());
		}
	};

	public static final String REQUEST_PROPERTY_PREFIX = "property:";

	private Object propertyValue;
	private final Object propertyId;
	private final IPropertySource target;

	private Object undoValue;
	private boolean resetOnUndo;

	private SetValueCommand(String label, IPropertySource target, Object propertyId, Object propertyValue)
	{
		super(label);
		this.target = target;
		this.propertyId = propertyId;
		this.propertyValue = propertyValue;
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
		boolean wasPropertySet = target.isPropertySet(propertyId);
		undoValue = target.getPropertyValue(propertyId);
		if (undoValue instanceof IPropertySource) undoValue = ((IPropertySource)undoValue).getEditableValue();
		if (propertyValue instanceof IPropertySource) propertyValue = ((IPropertySource)propertyValue).getEditableValue();
		target.setPropertyValue(propertyId, propertyValue);
		if (target instanceof IPropertySource2) resetOnUndo = !wasPropertySet && ((IPropertySource2)target).isPropertyResettable(propertyId);
		else resetOnUndo = !wasPropertySet && target.isPropertySet(propertyId);
		if (resetOnUndo) undoValue = null;
	}

	@Override
	public void redo()
	{
		execute();
	}

	@Override
	public void undo()
	{
		if (resetOnUndo) target.resetPropertyValue(propertyId);
		else target.setPropertyValue(propertyId, undoValue);
	}

	/**
	 * Create a command for setting a value.
	 * Use BaseRestorableCommand if possible (saves state completely before setting).
	 * If not possible (for instance PointPropertySource), use old style save which gets the undo-value first.
	 *
	 * @param propLabel
	 * @param target
	 * @param propertyName
	 * @param propertyValue
	 * @return
	 */
	public static Command createSetvalueCommand(String propLabel, final IPropertySource target, final Object propertyId, final Object propertyValue)
	{
		if (target.getPropertyValue(propertyId) == propertyValue) return null;//we don't want to set the same object

		String label = (propLabel != null && propLabel.length() > 0) ? "Set " + propLabel + " Property" : "";
		if (target instanceof IModelSavePropertySource && BaseRestorableCommand.getRestorer(((IModelSavePropertySource)target).getSaveModel()) != null)
		{
			// save the state before applying the property
			return new BaseRestorableCommand(label)
			{
				@Override
				public void execute()
				{
					setPropertyValue((IModelSavePropertySource)target, propertyId, propertyValue);
				}
			};
		}

		// state cannot be saved, use the old style set-value-command
		return new SetValueCommand(label, target, propertyId, propertyValue);
	}

	public static Command createSetPropertiesCommand(IPropertySource target, Map<Object, Object> extendedData)
	{
		if (target == null || extendedData == null)
		{
			return null;
		}

		List<Pair<String, Object>> entries = new ArrayList<Pair<String, Object>>();
		for (Map.Entry<Object, Object> entry : extendedData.entrySet())
		{
			Object key = entry.getKey();
			if (key instanceof String && ((String)key).startsWith(SetValueCommand.REQUEST_PROPERTY_PREFIX))
			{
				entries.add(new Pair<String, Object>(((String)key).substring(SetValueCommand.REQUEST_PROPERTY_PREFIX.length()), entry.getValue()));
			}
		}

		if (entries.size() == 0)
		{
			return null;
		}

		// sort to make sure subproperties are process after main (border before border.xxx)
		Collections.sort(entries, PAIR_LEFT_KEY_COMPARATOR);

		CompoundCommand command = new CompoundCommand();
		for (Pair<String, Object> entry : entries)
		{
			command.add(createSetvalueCommand("", target, entry.getLeft(), entry.getRight()));
		}
		return command;
	}
}
