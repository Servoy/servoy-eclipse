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

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import com.servoy.eclipse.model.util.TableWrapper;
import com.servoy.eclipse.ui.dialogs.I18NServerTableDialog;
import com.servoy.eclipse.ui.dialogs.TableContentProvider;
import com.servoy.eclipse.ui.dialogs.TableContentProvider.TableListOptions;
import com.servoy.eclipse.ui.dialogs.TreeSelectDialog;
import com.servoy.eclipse.ui.editors.ListSelectCellEditor;
import com.servoy.eclipse.ui.labelproviders.DatasourceLabelProvider;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.util.DataSourceUtils;

/**
 * Property controller for dataSources
 * 
 * @author rgansevles
 */

public class DatasourceController extends PropertyController<String, TableWrapper>
{
	private final Object input;
	private final String title;

	public DatasourceController(String id, String displayName, String title, boolean readOnly, Object input, ILabelProvider labelProvider)
	{
		super(id, displayName, null, labelProvider, null);
		this.title = title;
		this.input = input;
		setReadonly(readOnly);
		setSupportsReadonly(true);
	}

	@Override
	protected IPropertyConverter<String, TableWrapper> createConverter()
	{
		return new IPropertyConverter<String, TableWrapper>()
		{
			public String convertValue(Object id, TableWrapper tw)
			{
				return tw == null ? null : DataSourceUtils.createDBTableDataSource(tw.getServerName(), tw.getTableName());
			}

			public TableWrapper convertProperty(Object id, String dataSource)
			{
				String[] servernameTablename = DataSourceUtils.getDBServernameTablename(dataSource);
				if (servernameTablename == null)
				{
					return null;
				}

				return new TableWrapper(servernameTablename[0], servernameTablename[1]);
			}
		};
	}

	@Override
	public CellEditor createPropertyEditor(Composite parent)
	{
		final Shell shell = parent.getShell();
		ListSelectCellEditor.ListSelectControlFactory i18nControlFactory = null;
		if (input instanceof TableListOptions && ((TableListOptions)input).type == TableListOptions.TableListType.I18N)
		{
			i18nControlFactory = new ListSelectCellEditor.ListSelectControlFactory()
			{
				private TreeSelectDialog dialog = null;

				public Control createControl(Composite composite)
				{
					Button createButton = new Button(composite, SWT.PUSH);
					createButton.setText("Create new I18N table"); //$NON-NLS-1$
					createButton.addListener(SWT.Selection, new Listener()
					{
						public void handleEvent(Event event)
						{
							String serverName = null;
							TreeSelection ts = (TreeSelection)dialog.getTreeViewer().getViewer().getSelection();
							if (!ts.isEmpty())
							{
								Object selection = ts.getFirstElement();
								if (selection instanceof TableWrapper)
								{
									serverName = ((TableWrapper)selection).getServerName();
								}
							}
							I18NServerTableDialog dlg = new I18NServerTableDialog(shell, serverName, "");
							dlg.open();

							if (dlg.getReturnCode() == Window.OK)
							{
								serverName = dlg.getSelectedServerName();
								String selectedTableName = dlg.getSelectedTableName();
								Table newTable = I18NServerTableDialog.createDefaultMessagesTable(serverName, selectedTableName, shell);

								dialog.refreshTree();
								dialog.getTreeViewer().setSelection(new StructuredSelection(new TableWrapper(newTable.getServerName(), newTable.getName())));
							}
						}
					});
					return createButton;
				}

				public void setTreeSelectDialog(TreeSelectDialog dialog)
				{
					this.dialog = dialog;
				}
			};
		}
		return new ListSelectCellEditor(parent, title, new TableContentProvider(), DatasourceLabelProvider.INSTANCE_IMAGE_NAMEONLY, TableValueEditor.INSTANCE,
			isReadOnly(), input, SWT.NONE, i18nControlFactory, "serverTableDialog");
	}
}
