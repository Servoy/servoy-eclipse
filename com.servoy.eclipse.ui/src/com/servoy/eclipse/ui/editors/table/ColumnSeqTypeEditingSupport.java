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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.databinding.observable.AbstractObservable;
import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.ChangeSupport;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.databinding.observable.IObservable;
import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.util.FixedComboBoxCellEditor;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnInfo;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.Table;

public class ColumnSeqTypeEditingSupport extends EditingSupport
{
	private final CellEditor editor;
	private String[] comboSeqTypes;
	private Column column;
	private final IObservable observable;

	public ColumnSeqTypeEditingSupport(TableViewer tv, Table table)
	{
		super(tv);
		int[] types = ColumnInfo.allDefinedSeqTypes;
		comboSeqTypes = null;
		try
		{
			List<String> seqType = new ArrayList<String>();
			IServerInternal server = (IServerInternal)ServoyModel.getServerManager().getServer(table.getServerName());
			for (int element : types)
			{
				if (element == ColumnInfo.SERVOY_SEQUENCE || element == ColumnInfo.NO_SEQUENCE_SELECTED || server.supportsSequenceType(element, null/*
																																					 * TODO: add
																																					 * current
																																					 * selected
																																					 * column
																																					 */))
				{
					seqType.add(ColumnInfo.getSeqDisplayTypeString(element));
				}
			}
			comboSeqTypes = new String[seqType.size()];
			Object[] oType = seqType.toArray();
			for (int i = 0; i < oType.length; i++)
			{
				comboSeqTypes[i] = oType[i].toString();
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
		}
		editor = new FixedComboBoxCellEditor(tv.getTable(), comboSeqTypes, SWT.READ_ONLY);

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
					pi.setFlag(Column.UUID_COLUMN, i == ColumnInfo.UUID_GENERATOR);
					column = pi;
					int dpType = Column.mapToDefaultType(pi.getConfiguredColumnType().getSqlType());
					changeSupport.fireEvent(new ChangeEvent(observable));
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
					else if (i == ColumnInfo.UUID_GENERATOR)
					{
						if (dpType != IColumnTypes.TEXT && dpType != IColumnTypes.MEDIA)
						{
							MessageDialog.openWarning(((TableViewer)this.getViewer()).getTable().getShell(), "Warning",
								"UUID generator sequence is only supported for text and media column types.");
						}
						else if (configuredLength > 0 &&
							((dpType == IColumnTypes.MEDIA && configuredLength < 16) || (dpType == IColumnTypes.TEXT && configuredLength < 36)))
						{
							MessageDialog.openWarning(((TableViewer)this.getViewer()).getTable().getShell(), "Warning",
								"UUID generator column has too small length.");
						}
					}
					break;
				}
			}
			getViewer().update(element, null);
			pi.flagColumnInfoChanged();
		}
	}

	public Column getColumn()
	{
		return column;
	}
}
