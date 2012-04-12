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
package com.servoy.eclipse.ui.editors;


import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import com.servoy.eclipse.ui.dialogs.MethodDialog;
import com.servoy.eclipse.ui.dialogs.MethodDialog.MethodListOptions;
import com.servoy.eclipse.ui.dialogs.MethodDialog.MethodTreeContentProvider;
import com.servoy.eclipse.ui.dialogs.Scope;
import com.servoy.eclipse.ui.property.MethodWithArguments;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.util.IControlFactory;

/**
 * A cell editor that manages a method field.
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 */
public class MethodCellEditor extends DialogCellEditor
{
	private final PersistContext persistContext;
	private final ILabelProvider labelProvider;
	private final Object id;
	private final IValueEditor valueEditor;
	private final MethodListOptions options;

	/**
	 * Creates a new method cell editor parented under the given control.
	 * 
	 * @param parent the parent control
	 */
	public MethodCellEditor(Composite parent, ILabelProvider labelProvider, IValueEditor<MethodWithArguments> valueEditor, PersistContext persistContext,
		Object id, boolean readOnly, MethodListOptions options)
	{
		super(parent, labelProvider, valueEditor, readOnly, SWT.NONE);
		this.persistContext = persistContext;
		this.labelProvider = labelProvider;
		this.id = id;
		this.options = options;
		this.valueEditor = valueEditor;
	}

	@Override
	public MethodWithArguments openDialogBox(Control cellEditorWindow)
	{
		final MethodDialog dialog = new MethodDialog(cellEditorWindow.getShell(), labelProvider, new MethodTreeContentProvider(persistContext), getSelection(),
			options, SWT.NONE, "Select Method", this.valueEditor);
		dialog.setOptionsAreaFactory(new IControlFactory()
		{
			public Control createControl(Composite composite)
			{
				final AddMethodButtonsComposite buttons = new AddMethodButtonsComposite(composite, SWT.NONE);
				buttons.setContext(persistContext, id.toString());
				buttons.setDialog(dialog);
				setSelectedScope(buttons, (IStructuredSelection)dialog.getTreeViewer().getViewer().getSelection());
				dialog.getTreeViewer().addSelectionChangedListener(new ISelectionChangedListener()
				{
					public void selectionChanged(SelectionChangedEvent event)
					{
						setSelectedScope(buttons, (IStructuredSelection)event.getSelection());
					}
				});
				return buttons;
			}

			/**
			 * @param buttons
			 * @param selection
			 */
			private void setSelectedScope(final AddMethodButtonsComposite buttons, IStructuredSelection selection)
			{
				buttons.setSelectedScope(null);
				if (selection != null && !selection.isEmpty() && selection.getFirstElement() instanceof Scope)
				{
					buttons.setSelectedScope((Scope)selection.getFirstElement());
				}
				else if (selection instanceof ITreeSelection)
				{
					TreePath[] paths = ((ITreeSelection)selection).getPaths();
					if (paths != null && paths.length == 1)
					{
						for (int i = paths[0].getSegmentCount(); i-- > 0;)
						{
							if (paths[0].getSegment(i) instanceof Scope)
							{
								buttons.setSelectedScope((Scope)paths[0].getSegment(i));
								break;
							}
						}
					}
				}
			}
		});
		dialog.open();

		if (dialog.getReturnCode() == Window.CANCEL)
		{
			return null;
		}
		return (MethodWithArguments)((IStructuredSelection)dialog.getSelection()).getFirstElement(); // single select
	}
}
