/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2016 Servoy BV

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

package com.servoy.eclipse.ui.views.solutionexplorer.actions;

import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.inmemory.AbstractMemServer;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.DataModelManager;
import com.servoy.eclipse.model.util.IDataSourceWrapper;
import com.servoy.eclipse.model.util.InMemServerWrapper;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.ViewFoundsetServerWrapper;
import com.servoy.eclipse.ui.editors.TableEditor;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IPersistVisitor;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.docvalidator.IdentDocumentValidator;

/**
 * @author emera
 */
public class RenameInMemTableAction extends AbstractInMemTableAction
{
	private HashMap<String, String> names;
	private final IWorkbenchPage page;
	private final UserNodeType type;

	public RenameInMemTableAction(Shell shell, IWorkbenchPage page, UserNodeType type)
	{
		super(shell, "rename", "Renaming");
		String text = type == UserNodeType.INMEMORY_DATASOURCE ? "Rename in memory datasource" : "Rename view foundset";
		setText(text);
		setToolTipText(text);
		this.page = page;
		this.type = type;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.eclipse.ui.views.solutionexplorer.actions.AbstractInMemTableAction#doAction(com.servoy.j2db.persistence.IServer,
	 * com.servoy.j2db.persistence.ITable)
	 */
	@Override
	protected void doAction(IServer server, ITable table) throws SQLException, RepositoryException
	{
		((AbstractMemServer< ? >)server).renameTable(table, names.get(table.getName()));
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.eclipse.ui.views.solutionexplorer.actions.AbstractInMemTableAction#doAction(com.servoy.j2db.persistence.IServer,
	 * com.servoy.j2db.persistence.ITable, java.util.ArrayList)
	 */
	@Override
	protected void doAction(IServer server, ITable table, ArrayList<String> userSelection) throws RepositoryException
	{
		try
		{
			((AbstractMemServer< ? >)server).renameTable(table, names.get(table.getName()), userSelection);
		}
		catch (SQLException e)
		{
			ServoyLog.logError(e);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.eclipse.ui.views.solutionexplorer.actions.AbstractInMemTableAction#run()
	 */
	@Override
	public void run()
	{
		names = new HashMap<String, String>();
		for (IDataSourceWrapper selectedTable : selection.keySet())
		{
			if (!checkAndAskUnsaved(selectedTable)) continue;

			InputDialog nameDialog = new InputDialog(UIUtils.getActiveShell(), getText(), "Supply a new datasource name",
				selectedTable.getTableName(), new IInputValidator()
				{
					public String isValid(String newText)
					{
						if (type == UserNodeType.INMEMORY_DATASOURCE && new InMemServerWrapper().getTableNames().contains(newText) ||
							type == UserNodeType.VIEW_FOUNDSET && new ViewFoundsetServerWrapper().getTableNames().contains(newText))
						{
							return "Name already used";
						}

						boolean valid = (IdentDocumentValidator.isSQLIdentifier(newText) &&
							(!(newText.toUpperCase()).startsWith(DataModelManager.TEMP_UPPERCASE_PREFIX)) &&
							(!(newText.toUpperCase()).startsWith(IServer.SERVOY_UPPERCASE_PREFIX)));
						return valid ? null : (newText.length() == 0 ? "" : "Invalid datasource name");
					}
				});
			int res = nameDialog.open();
			if (res == Window.OK)
			{
				names.put(selectedTable.getTableName(), nameDialog.getValue());
			}
		}
		if (names.size() > 0)
		{
			super.run();
		}
	}

	private boolean checkAndAskUnsaved(IDataSourceWrapper dsWrapper)
	{
		IEditorPart[] dirtyEditors = page.getDirtyEditors();
		for (IEditorPart dirtyEditor : dirtyEditors)
		{
			if (dirtyEditor instanceof TableEditor)
			{
				final TableEditor editor = (TableEditor)dirtyEditor;
				String ds = editor.getTable().getDataSource();
				if (ds.equals(dsWrapper.getDataSource()))
				{
					boolean rename = MessageDialog.openConfirm(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), getText(),
						"Table '" + editor.getTable().getName() + "' has unsaved changes. Do you want to rename?");
					if (rename)
					{
						Display.getDefault().syncExec(new Runnable()
						{
							public void run()
							{
								editor.doSave(null);
							}
						});
						break;
					}
					else
					{
						return false;
					}
				}
			}
		}
		return true;
	}

	@Override
	protected void updateReferencesIfNeeded(ServoyProject project)
	{
		try
		{
			project.getProject().refreshLocal(IResource.DEPTH_INFINITE, null);
			final Set<IPersist> persists = new HashSet<>();
			project.getEditingSolution().acceptVisitor(new IPersistVisitor()
			{
				public Object visit(IPersist o)
				{
					String datasourceScheme = type == UserNodeType.INMEMORY_DATASOURCE ? DataSourceUtils.INMEM_DATASOURCE_SCHEME_COLON
						: DataSourceUtils.VIEW_DATASOURCE_SCHEME_COLON;
					if (o instanceof Form)
					{
						Form f = (Form)o;
						String tableName = DataSourceUtils.getDataSourceTableName(f.getDataSource());
						if (f.getDataSource() != null && f.getDataSource().startsWith(datasourceScheme) && names.containsKey(tableName))
						{
							f.setDataSource(datasourceScheme + names.get(tableName));
							persists.add(f);
						}
					}
					else if (o instanceof Relation)
					{
						Relation r = (Relation)o;
						if (r.getPrimaryDataSource().startsWith(datasourceScheme) && names.containsKey(r.getPrimaryTableName()))
						{
							r.setPrimaryDataSource(datasourceScheme + names.get(r.getPrimaryTableName()));
							persists.add(r);
						}
						if (r.getForeignDataSource().startsWith(datasourceScheme) && names.containsKey(r.getForeignTableName()))
						{
							r.setForeignDataSource(datasourceScheme + names.get(r.getForeignTableName()));
							persists.add(r);
						}
					}
					else if (o instanceof ValueList)
					{
						ValueList vl = (ValueList)o;
						if (vl.getDataSource() != null && vl.getDataSource().startsWith(datasourceScheme) && names.containsKey(vl.getTableName()))
						{
							vl.setDataSource(datasourceScheme + names.get(vl.getTableName()));
							persists.add(vl);
						}
					}
					return IPersistVisitor.CONTINUE_TRAVERSAL;
				}
			});
			project.saveEditingSolutionNodes(persists.toArray(new IPersist[persists.size()]), false, false);
		}
		catch (Exception e)
		{
			ServoyLog.logError("Could not update mem table references", e);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.eclipse.ui.views.solutionexplorer.actions.AbstractInMemTableAction#confirm()
	 */
	@Override
	protected boolean confirm()
	{
		return true;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.eclipse.ui.views.solutionexplorer.actions.AbstractInMemTableAction#shouldCompleteActionIfUnsaved(java.lang.String)
	 */
	@Override
	protected boolean shouldCompleteActionIfUnsaved(String tableName)
	{
		return names.containsKey(tableName);
	}

	@Override
	protected void refreshEditor(final IServer server, final ITable table)
	{
		try
		{
			final ITable newTable = server.getTable(names.get(table.getName()));
			if (newTable == null)
			{
				ServoyLog.logInfo("Table '" + names.get(table.getName()) + "' not found, cannot open it in editor.");
			}
			if (Display.getCurrent() != null)
			{
				EditorUtil.closeEditor(table);
				EditorUtil.openTableEditor(newTable);
			}
			else
			{
				Display.getDefault().asyncExec(new Runnable()
				{
					public void run()
					{
						EditorUtil.closeEditor(table);
						EditorUtil.openTableEditor(newTable);
					}
				});
			}
		}
		catch (RepositoryException | RemoteException e)
		{
			ServoyLog.logError("Could not open mem table '" + names.get(table.getName()) + "' in editor", e);
		}
	}
}
