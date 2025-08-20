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


import java.util.List;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.ui.dialogs.MethodDialog;
import com.servoy.eclipse.ui.dialogs.MethodDialog.MethodListOptions;
import com.servoy.eclipse.ui.dialogs.MethodDialog.MethodTreeContentProvider;
import com.servoy.eclipse.ui.labelproviders.MethodLabelProvider;
import com.servoy.eclipse.ui.property.MethodWithArguments;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.util.IControlFactory;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.ScriptMethod;

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
		final MethodDialog dialog = new MethodDialog(cellEditorWindow.getShell(), new MethodLabelProvider(persistContext, false, !options.includeDefault, true),
			new MethodTreeContentProvider(persistContext), getSelection(), options, SWT.NONE, "Select Method", this.valueEditor);
		dialog.setOptionsAreaFactory(new IControlFactory()
		{
			public Control createControl(Composite composite)
			{
				final AddMethodButtonsComposite buttons = new AddMethodButtonsComposite(composite, SWT.NONE);
				String methodName = id.toString();
				IScriptProvider scriptProvider = !getSelection().isEmpty() && getSelection().getFirstElement() instanceof MethodWithArguments
					? MethodLabelProvider.getScriptProvider((MethodWithArguments)getSelection().getFirstElement(), persistContext) : null;
				if (scriptProvider != null && persistContext.getContext() instanceof Form)
				{
					Form form = (Form)persistContext.getContext();
					if (form.getExtendsID() != null)
					{
						List<Form> formHierarchy = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject().getEditingFlattenedSolution()
							.getFormHierarchy(
								form);
						for (Form f : formHierarchy)
						{
							if (f != form)
							{
								ScriptMethod superMethod = f.getScriptMethod(scriptProvider.getName());
								if (superMethod != null)
								{
									methodName = scriptProvider.getName();
									break;
								}
							}
						}
					}
				}
				buttons.setContext(persistContext, methodName);
				buttons.setDialog(dialog);
				buttons.searchSelectedScope((IStructuredSelection)dialog.getTreeViewer().getViewer().getSelection());
				dialog.getTreeViewer().addSelectionChangedListener(new ISelectionChangedListener()
				{
					public void selectionChanged(SelectionChangedEvent event)
					{
						buttons.searchSelectedScope((IStructuredSelection)event.getSelection());
					}
				});
				return buttons;
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
