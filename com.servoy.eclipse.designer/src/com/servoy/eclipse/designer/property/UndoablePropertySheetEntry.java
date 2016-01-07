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

import java.util.EventObject;

import org.eclipse.gef.commands.CommandStack;
import org.eclipse.gef.commands.CommandStackListener;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.gef.commands.ForwardUndoCompoundCommand;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;

import com.servoy.eclipse.ui.views.ModifiedPropertySheetEntry;
import com.servoy.eclipse.ui.views.properties.IMergeablePropertyDescriptor;
import com.servoy.eclipse.ui.views.properties.IMergedPropertyDescriptor;
import com.servoy.eclipse.ui.views.properties.PropertySheetEntry;


/**
 * <p>
 * UndoablePropertySheetEntry provides undo support for changes made to IPropertySources by the {@link org.eclipse.ui.views.properties.PropertySheetViewer}.
 * Clients can construct a {@link org.eclipse.ui.views.properties.PropertySheetPage} and use this class as the root entry. All changes made to property sources
 * displayed on that page will be done using the provided command stack.
 * <p>
 * <b>NOTE:</b> If you intend to use an IPropertySourceProvider for a PropertySheetPage whose root entry is an instance of of UndoablePropertySheetEntry, you
 * should set the IPropertySourceProvider on that root entry, rather than the PropertySheetPage.
 */
public final class UndoablePropertySheetEntry extends ModifiedPropertySheetEntry
{
	private CommandStackListener commandStackListener;

	private CommandStack stack;

	private String prevErrorMessage; // keep track of previous message to prevent same message to pop up twice

	private UndoablePropertySheetEntry()
	{
	}

	/**
	 * Constructs the root entry using the given command stack.
	 *
	 * @param stack the command stack
	 * @since 3.1
	 */
	public UndoablePropertySheetEntry(CommandStack stack)
	{
		setCommandStack(stack);
	}

	/**
	 * @see org.eclipse.ui.views.properties.PropertySheetEntry#createChildEntry()
	 */
	@Override
	protected PropertySheetEntry createChildEntry()
	{
		return new UndoablePropertySheetEntry();
	}

	/**
	 * @see org.eclipse.ui.views.properties.IPropertySheetEntry#dispose()
	 */
	@Override
	public void dispose()
	{
		if (stack != null) stack.removeCommandStackListener(commandStackListener);
		super.dispose();
	}

	CommandStack getCommandStack()
	{
		//only the root has, and is listening too, the command stack
		if (getParent() != null) return ((UndoablePropertySheetEntry)getParent()).getCommandStack();
		return stack;
	}

	/**
	 * @see org.eclipse.ui.views.properties.IPropertySheetEntry#resetPropertyValue()
	 */
	@Override
	public void resetPropertyValue()
	{
		if (getParent() == null)
		// root does not have a default value
		{
			return;
		}

		CompoundCommand cc = new CompoundCommand();

		//	Use our parent's values to reset our values.
		Object[] objects = getParent().getValues();
		for (Object element : objects)
		{
			IPropertySource source = getPropertySource(element);
			if (source.isPropertySet(getDescriptor().getId()))
			{
				//source.resetPropertyValue(getDescriptor()getId());
				ResetValueCommand restoreCmd = new ResetValueCommand();
				restoreCmd.setTarget(source);
				restoreCmd.setPropertySheetEntry(this);
				restoreCmd.setPropertyId(getDescriptor().getId());
				cc.add(restoreCmd);
			}
		}
		if (cc.getCommands().size() > 0)
		{
			getCommandStack().execute(cc);
			refreshFromRoot();
		}
	}

	void setCommandStack(CommandStack stack)
	{
		this.stack = stack;
		commandStackListener = new CommandStackListener()
		{
			public void commandStackChanged(EventObject e)
			{
				refreshFromRoot();
			}
		};
		stack.addCommandStackListener(commandStackListener);
	}

	@Override
	public void applyEditorValue()
	{
		Object[] values = getValues();
		if (values != null && values.length > 1 && !editor.isDirty())
		{
			return;
		}

		// Check if editor has a valid value
		if (editor != null && !editor.isValueValid())
		{
			final String errorMessage = editor.getErrorMessage();
			if (!errorMessage.equals(prevErrorMessage))
			{
				prevErrorMessage = errorMessage;
				Display.getDefault().asyncExec(new Runnable()
				{
					public void run()
					{
						MessageDialog.openError(Display.getDefault().getActiveShell(), "Could not set property value", errorMessage);
					}
				});
			}
		}
		else
		{
			prevErrorMessage = null;
		}

		super.applyEditorValue();
	}

	/**
	 * @see PropertySheetEntry#valueChanged(PropertySheetEntry)
	 */
	@Override
	protected void valueChanged(PropertySheetEntry child)
	{
		valueChanged((UndoablePropertySheetEntry)child, new ForwardUndoCompoundCommand());
	}

	void valueChanged(UndoablePropertySheetEntry child, CompoundCommand command)
	{
		CompoundCommand cc = new CompoundCommand();
		command.add(cc);

		for (int i = 0; i < getValues().length; i++)
		{
			IPropertySource target = getPropertySource(getValues()[i]);
			Object value = child.getValues()[0];
			Object entryValue = value;
			IPropertyDescriptor desc = child.getDescriptor();
			if (desc instanceof IMergedPropertyDescriptor && getValues().length > 1)
			{
				for (IPropertyDescriptor desc2 : target.getPropertyDescriptors())
				{
					if (desc.getId().equals(desc2.getId()) && desc2 instanceof IMergeablePropertyDescriptor &&
						((IMergeablePropertyDescriptor)desc).isMergeableWith((IMergeablePropertyDescriptor)desc2))
					{
						entryValue = ((IMergedPropertyDescriptor)desc).convertToUnmergedValue((IMergeablePropertyDescriptor)desc2, value);
						break;
					}
				}
			}
			cc.add(SetValueCommand.createSetvalueCommand(child.getDisplayName(), target, child.getDescriptor().getId(), entryValue));
		}

		// inform our parent
		if (getParent() != null) ((UndoablePropertySheetEntry)getParent()).valueChanged(this, command);
		else
		{
			//I am the root entry
			stack.execute(command);
		}
	}

}
