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
package com.servoy.eclipse.ui.editors.table;

import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Display;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.util.DocumentValidatorVerifyListener;
import com.servoy.eclipse.ui.util.VerifyingTextCellEditor;
import com.servoy.j2db.persistence.AggregateVariable;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.RepositoryException;

public class AggregationNameEditingSupport extends EditingSupport
{
	private final VerifyingTextCellEditor editor;
	private final ChangeSupportObservable observable;

	public AggregationNameEditingSupport(TreeViewer tv)
	{
		super(tv);
		editor = new VerifyingTextCellEditor(tv.getTree());
		editor.addVerifyListener(DocumentValidatorVerifyListener.IDENT_SERVOY_VERIFIER);
		observable = new ChangeSupportObservable(new SimpleChangeSupport());
	}

	public void addChangeListener(IChangeListener listener)
	{
		observable.addChangeListener(listener);
	}

	public void removeChangeListener(IChangeListener listener)
	{
		observable.removeChangeListener(listener);
	}

	@Override
	protected void setValue(Object element, Object value)
	{
		if (element instanceof AggregateVariable)
		{
			AggregateVariable aggregationVariable = (AggregateVariable)element;
			try
			{
				if (value != null && !value.equals(aggregationVariable.getName()))
				{
					IValidateName nameValidator = ServoyModelManager.getServoyModelManager().getServoyModel().getNameValidator();
					aggregationVariable.updateName(nameValidator, value.toString());
					observable.fireChangeEvent();
				}
			}
			catch (final RepositoryException e)
			{
				Display.getDefault().asyncExec(new Runnable()
				{
					public void run()
					{
						MessageDialog.openError(UIUtils.getActiveShell(), "Error", "Save failed: " + e.getMessage());
					}
				});
			}
			catch (Exception e)
			{
				ServoyLog.logError(e);
			}
			getViewer().update(element, null);
		}
	}

	@Override
	protected Object getValue(Object element)
	{
		if (element instanceof AggregateVariable)
		{
			AggregateVariable aggregationVariable = (AggregateVariable)element;
			return aggregationVariable.getName();
		}
		return null;
	}

	@Override
	protected CellEditor getCellEditor(Object element)
	{
		return editor;
	}

	@Override
	protected boolean canEdit(Object element)
	{
		if (element instanceof AggregateVariable) return true;
		else return false;
	}
}
