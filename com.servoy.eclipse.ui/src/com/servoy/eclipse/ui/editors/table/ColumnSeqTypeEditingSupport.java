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

import org.eclipse.core.databinding.observable.ChangeSupport;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Control;

import com.servoy.base.persistence.IBaseColumn;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.ui.util.FixedComboBoxCellEditor;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnInfo;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.query.ColumnType;

public class ColumnSeqTypeEditingSupport extends EditingSupport
{

	public class ColumnSeqTypeEditingObservable extends ChangeSupportObservable
	{
		public ColumnSeqTypeEditingObservable(ChangeSupport changeSupport)
		{
			super(changeSupport);
		}

		public ColumnSeqTypeEditingSupport getEditingSupport()
		{
			return ColumnSeqTypeEditingSupport.this;
		}
	}

	private final CellEditor editor;
	private final String[] comboSeqTypes;
	private Column column;
	private final ColumnSeqTypeEditingObservable observable;

	public ColumnSeqTypeEditingSupport(TableViewer tv, ITable table)
	{
		super(tv);
		comboSeqTypes = ColumnComposite.getSeqDisplayTypeStrings(table);
		editor = new FixedComboBoxCellEditor(tv.getTable(), comboSeqTypes, SWT.READ_ONLY);
		Control control = editor.getControl();
		CCombo c = (CCombo)control;
		c.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				editor.deactivate();
			}
		});
		observable = new ColumnSeqTypeEditingObservable(new SimpleChangeSupport());
	}

	@Override
	protected boolean canEdit(Object element)
	{
		// only if we have active solution
		return ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject() != null;
	}

	@Override
	protected CellEditor getCellEditor(Object element)
	{
		return editor;
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
	protected Object getValue(Object element)
	{
		if (element instanceof Column)
		{
			Column pi = (Column)element;
			String seqType = ColumnInfo.getSeqDisplayTypeString(pi.getSequenceType());
			for (int i = 0; i < comboSeqTypes.length; i++)
			{
				if (seqType.equals(comboSeqTypes[i])) return new Integer(i);
			}

		}
		return null;
	}

	@Override
	protected void setValue(Object element, Object value)
	{
		if (element instanceof Column)
		{
			Column pi = (Column)element;
			int previousSeqType = pi.getSequenceType();
			int index = Integer.parseInt(value.toString());
			String newSeqType = comboSeqTypes[index];
			int[] types = ColumnInfo.allDefinedSeqTypes;
			int configuredLength = pi.getConfiguredColumnType().getLength();
			for (int i : types)
			{
				if (ColumnInfo.getSeqDisplayTypeString(i).equals(newSeqType))
				{
					if (i == previousSeqType) return;
					pi.setSequenceType(i);
					pi.setFlag(IBaseColumn.UUID_COLUMN, i == ColumnInfo.UUID_GENERATOR);
					column = pi;
					int dpType = Column.mapToDefaultType(pi.getConfiguredColumnType().getSqlType());
					observable.fireChangeEvent();
					if (i == ColumnInfo.DATABASE_IDENTITY && i != previousSeqType && pi.getTable().getExistInDB())
					{
						MessageDialog.openWarning(((TableViewer)this.getViewer()).getTable().getShell(), "Warning",
							"Servoy won't alter the table for you, so you must be sure that it is a identity/auto-increment column!");
					}
					else if (i == ColumnInfo.SERVOY_SEQUENCE && dpType != IColumnTypes.INTEGER && dpType != IColumnTypes.NUMBER)
					{
						MessageDialog.openWarning(((TableViewer)this.getViewer()).getTable().getShell(), "Warning",
							"Servoy sequence is only supported for numeric column types.");
					}
					else if (column.getSequenceType() == ColumnInfo.UUID_GENERATOR)
					{
						if (!(dpType == IColumnTypes.MEDIA || dpType == IColumnTypes.TEXT) && (column.getLength() == 0 || column.getLength() == 50) &&
							!column.getExistInDB())
						{
							column.getColumnInfo().setConfiguredColumnType(ColumnType.getInstance(IColumnTypes.TEXT, 36, 0));
						}
					}
					break;
				}
			}
			pi.flagColumnInfoChanged();
			getViewer().update(element, null);
		}
	}

	public Column getColumn()
	{
		return column;
	}
}
