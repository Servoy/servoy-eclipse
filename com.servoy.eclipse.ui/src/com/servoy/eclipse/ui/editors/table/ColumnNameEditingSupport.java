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

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.widgets.Display;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.util.DocumentValidatorVerifyListener;
import com.servoy.eclipse.ui.util.VerifyingTextCellEditor;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.query.ColumnType;
import com.servoy.j2db.util.docvalidator.IdentDocumentValidator;
import com.servoy.j2db.util.docvalidator.LengthDocumentValidator;
import com.servoy.j2db.util.docvalidator.ValidatingDocument.IDocumentValidator;

public class ColumnNameEditingSupport extends EditingSupport
{
	private final VerifyingTextCellEditor editor;
	private final TableViewer tableViewer;
	private final boolean realName;

	public ColumnNameEditingSupport(TableViewer tv, boolean realName)
	{
		super(tv);
		this.realName = realName;
		tableViewer = tv;
		editor = new VerifyingTextCellEditor(tv.getTable());
		if (realName)
		{
			editor.addVerifyListener(new DocumentValidatorVerifyListener(
				new IDocumentValidator[] { new IdentDocumentValidator(IdentDocumentValidator.TYPE_SQL), new LengthDocumentValidator(
					Column.MAX_SQL_OBJECT_NAME_LENGTH) }));
		}
		else
		{
			editor.addVerifyListener(new DocumentValidatorVerifyListener(new IDocumentValidator[] { new IdentDocumentValidator(
				IdentDocumentValidator.TYPE_SERVOY) }));
		}
	}

	@Override
	protected void setValue(final Object element, final Object value)
	{
		if (element instanceof Column)
		{
			Column pi = (Column)element;
			try
			{
				IValidateName nameValidator = ServoyModelManager.getServoyModelManager().getServoyModel().getNameValidator();
				if (realName)
				{
					pi.updateName(nameValidator, value.toString());

					if (isEmail(value.toString()))
					{
						pi.getColumnInfo().setConfiguredColumnType(ColumnType.getInstance(IColumnTypes.TEXT, 254, 0));
					}
					else if (isUrl(value.toString()))
					{
						pi.getColumnInfo().setConfiguredColumnType(ColumnType.getInstance(IColumnTypes.TEXT, 2048, 0));
					}
				}
				else
				{
					pi.updateDataProviderID(nameValidator, value.toString());
				}
			}
			catch (final Exception e)
			{
				wrongName = value.toString();
				Display.getDefault().asyncExec(new Runnable()
				{
					public void run()
					{
						MessageDialog.openError(UIUtils.getActiveShell(),
							"Error setting " + (realName ? "name" : "dataproviderID") + " on the column", e.getMessage());
						tableViewer.editElement(element, (realName ? ColumnComposite.CI_NAME : ColumnComposite.CI_DATAPROVIDER_ID));
						editor.setValue((realName ? ((Column)element).getName() : ((Column)element).getDataProviderID()));
					}
				});
				ServoyLog.logError(e);
			}
			getViewer().update(element, null);
		}
	}

	@Override
	protected Object getValue(Object element)
	{
		if (element instanceof Column)
		{
			Column pi = (Column)element;
			return realName ? pi.getSQLName() : pi.getDataProviderID();
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
		if (!realName) return true;
		if (element instanceof Column && editor != null)
		{
			return !((Column)element).getExistInDB();
		}
		return false;
	}

	private boolean isEmail(String name)
	{
		return (name != null && name.toLowerCase().contains("email"));
	}

	private boolean isUrl(String name)
	{
		return (name != null && name.toLowerCase().contains("url"));
	}

	@Override
	protected void initializeCellEditorValue(CellEditor cellEditor, ViewerCell cell)
	{
		super.initializeCellEditorValue(cellEditor, cell);
		wrongName = null;
	}

	private String wrongName = null;

	public void checkValidState() throws RepositoryException
	{
		if (wrongName != null)
		{
			throw new RepositoryException("Invalid column name: " + wrongName);
		}
	}
}