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

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;

import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.util.IDelegate;
import com.servoy.j2db.util.Utils;

/**
 * when values are changed, do this via the editor that handles the persist. Open the editor when it is not already open.
 *
 * @author rgansevles
 *
 */
public class RetargetToEditorPersistProperties implements IPropertySource, IAdaptable, IDelegate<IPropertySource>
{
	private final IPropertySource persistProperties;

	/**
	 * When <code>true</code>, {@link #updateProperty(boolean, Object, Object)} wraps the change in a GEF {@link Command}
	 * and pushes it onto the editor's {@link CommandStack} so it is undoable (used by direct callers such as the
	 * "Clear property" marker quick-fix - SVY-19810). When <code>false</code>, the caller (the Properties view's
	 * UndoablePropertySheetEntry / OpenEditorUndoablePropertySheetEntry) already wraps set/reset in a proper
	 * Set/ResetValueCommand on the stack, so wrapping again here would double-wrap and corrupt the undo stack.
	 */
	private final boolean wrapInCommand;

	public RetargetToEditorPersistProperties(IPropertySource persistProperties)
	{
		this(persistProperties, true);
	}

	public RetargetToEditorPersistProperties(IPropertySource persistProperties, boolean wrapInCommand)
	{
		this.persistProperties = persistProperties;
		this.wrapInCommand = wrapInCommand;
	}

	@Override
	public IPropertySource getDelegate()
	{
		return persistProperties;
	}

	public Object getEditableValue()
	{
		return persistProperties.getEditableValue();
	}

	public IPropertyDescriptor[] getPropertyDescriptors()
	{
		return persistProperties.getPropertyDescriptors();
	}

	public Object getPropertyValue(Object id)
	{
		return persistProperties.getPropertyValue(id);
	}

	public boolean isPropertySet(Object id)
	{
		return persistProperties.isPropertySet(id);
	}


	public void resetPropertyValue(final Object id)
	{
		Display.getCurrent().syncExec(new Runnable()
		{
			public void run()
			{
				updateProperty(false, id, null);
			}
		});
	}

	public void setPropertyValue(final Object id, final Object value)
	{
		// ignore if the value did not change (Was already set), happens when a form property is changed using props view and form node
		// is selected in the solex tree, setPropertyValue is called with new value while value has already been set.
		Object propertyValue = persistProperties.getPropertyValue(id);
		if (Utils.equalObjects(value, propertyValue))
		{
			return;
		}

		Display.getCurrent().syncExec(new Runnable()
		{
			public void run()
			{
				updateProperty(true, id, value);
			}
		});
	}

	/**
	 * Update the property value via the editor.
	 *
	 * @param set
	 * @param id
	 * @param value
	 */
	protected void updateProperty(final boolean set, final Object id, final Object value)
	{
		// find the editor of this persist and change the value in the editor
		final IEditorPart editor = resolveEditor(); // activate=false otherwise the editor is activated too soon and the save editor button remains grayed out
		if (editor == null)
		{
			return;
		}

		if (!wrapInCommand)
		{
			// Properties view path: the caller (UndoablePropertySheetEntry / OpenEditorUndoablePropertySheetEntry)
			// already wraps set/reset in a proper Command with undo support (SetValueCommand / ResetValueCommand)
			// and pushes it onto the editor's CommandStack. Adding a second Command here would double-wrap
			// the operation, corrupting the undo stack. So apply the change directly.
			applyChange(set, id, value);
			return;
		}

		// Direct caller path (e.g. the "Clear property" marker quick-fix - SVY-19810): nobody wraps this change in a
		// Command, so wrap it here with proper undo()/redo() support and push it onto the editor's CommandStack so the
		// change marks the editor dirty and can be undone/redone.
		Command cmd = new Command(set ? "Set property" : "Reset property")
		{
			private Object oldValue;

			@Override
			public void execute()
			{
				oldValue = persistProperties.getPropertyValue(id);
				if (oldValue instanceof IPropertySource)
				{
					oldValue = ((IPropertySource)oldValue).getEditableValue();
				}
				applyChange(set, id, value);
			}

			@Override
			public void undo()
			{
				if (oldValue != null)
				{
					persistProperties.setPropertyValue(id, oldValue);
				}
				else
				{
					persistProperties.resetPropertyValue(id);
				}
			}

			@Override
			public void redo()
			{
				applyChange(set, id, value);
			}
		};
		CommandStack commandStack = resolveCommandStack(editor);
		if (commandStack != null)
		{
			commandStack.execute(cmd);
		}
		else
		{
			cmd.execute();
		}
	}

	private void applyChange(boolean set, Object id, Object value)
	{
		if (set)
		{
			persistProperties.setPropertyValue(id, value);
		}
		else
		{
			persistProperties.resetPropertyValue(id);
		}
	}

	/**
	 * Opens (without activating) and returns the editor that handles this persist, or <code>null</code> when there is
	 * none. Package-visible seam so tests can supply a fake editor without the live workbench.
	 */
	protected IEditorPart resolveEditor()
	{
		return openPersistEditor(persistProperties, false);
	}

	/**
	 * Returns the editor's GEF {@link CommandStack}, or <code>null</code> when the editor has none. Package-visible seam
	 * so tests can supply a real CommandStack without the live workbench.
	 */
	protected CommandStack resolveCommandStack(IEditorPart editor)
	{
		return editor.getAdapter(CommandStack.class);
	}

	public static IEditorPart openPersistEditor(IPropertySource persistProperties, boolean activate)
	{
		Object editorModel;
		if (persistProperties instanceof HasPersistContext hasPersistContext)
		{
			editorModel = hasPersistContext.getPersistContext().getContext();
		}
		else if (persistProperties instanceof IModelSavePropertySource modelSavePropertySource)
		{
			editorModel = modelSavePropertySource.getSaveModel();
		}
		else
		{
			return null;
		}

		return EditorUtil.openPersistEditor(editorModel, activate);
	}

	public Object getAdapter(Class adapter)
	{
		return persistProperties instanceof IAdaptable ? ((IAdaptable)persistProperties).getAdapter(adapter) : null;
	}

	@Override
	public String toString()
	{
		return persistProperties.toString();
	}
}
