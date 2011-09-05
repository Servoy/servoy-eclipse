/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2011 Servoy BV

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
package com.servoy.eclipse.ui.editors.table;

import org.eclipse.core.databinding.observable.AbstractObservable;
import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.ChangeSupport;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.databinding.observable.IObservable;
import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Text;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.util.DocumentValidatorVerifyListener;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.util.docvalidator.IdentDocumentValidator;
import com.servoy.j2db.util.docvalidator.ValidatingDocument.IDocumentValidator;

/**
 * Edit method name in table editor.
 * 
 * @author rgansevles
 *
 */
public class MethodNameEditingSupport extends EditingSupport
{
	private final TextCellEditor editor;
	private final IObservable observable;

	public MethodNameEditingSupport(TreeViewer viewer)
	{
		super(viewer);
		editor = new TextCellEditor(viewer.getTree(), SWT.NONE);
		((Text)editor.getControl()).addVerifyListener(new DocumentValidatorVerifyListener(new IDocumentValidator[] { new IdentDocumentValidator(
			IdentDocumentValidator.TYPE_SQL) }));
		changeSupport = new ChangeSupport(Realm.getDefault())
		{
			@Override
			protected void lastListenerRemoved()
			{
			}

			@Override
			protected void firstListenerAdded()
			{
			}
		};
		observable = new AbstractObservable(Realm.getDefault())
		{
			@Override
			public void addChangeListener(IChangeListener listener)
			{
				changeSupport.addChangeListener(listener);
			}

			@Override
			public void removeChangeListener(IChangeListener listener)
			{
				changeSupport.removeChangeListener(listener);
			}

			public boolean isStale()
			{
				return false;
			}
		};
	}

	private final ChangeSupport changeSupport;

	public void addChangeListener(IChangeListener listener)
	{
		observable.addChangeListener(listener);
	}

	public void removeChangeListener(IChangeListener listener)
	{
		observable.removeChangeListener(listener);
	}

	@Override
	protected boolean canEdit(Object element)
	{
		return !(element instanceof Solution);
	}

	@Override
	protected CellEditor getCellEditor(Object element)
	{
		return editor;
	}

	@Override
	protected Object getValue(Object element)
	{
		if (element instanceof ScriptMethod)
		{
			return ((ScriptMethod)element).getName();
		}
		return "";
	}

	@Override
	protected void setValue(Object element, Object value)
	{
		if (element instanceof ScriptMethod)
		{
			IValidateName nameValidator = ServoyModelManager.getServoyModelManager().getServoyModel().getNameValidator();
			ScriptMethod method = (ScriptMethod)element;
			try
			{
				Text text = (Text)editor.getControl();
				if (!method.getName().equals(text.getText()))
				{
					method.updateName(nameValidator, text.getText());
					changeSupport.fireEvent(new ChangeEvent(observable));
				}
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError(e);
				MessageDialog.openError(this.getViewer().getControl().getShell(), "Error", "Error while saving: " + e.getMessage());
			}

			getViewer().update(element, null);
		}
	}
}
