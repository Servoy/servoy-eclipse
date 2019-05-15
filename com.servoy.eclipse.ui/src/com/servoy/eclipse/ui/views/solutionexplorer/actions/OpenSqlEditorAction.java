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
package com.servoy.eclipse.ui.views.solutionexplorer.actions;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.TableWrapper;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.wizards.ReplaceTableWizard;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.Table;

import net.sourceforge.sqlexplorer.dbproduct.Alias;
import net.sourceforge.sqlexplorer.plugin.SQLExplorerPlugin;
import net.sourceforge.sqlexplorer.plugin.editors.SQLEditor;
import net.sourceforge.sqlexplorer.plugin.editors.SQLEditorInput;

/**
 *
 * @author asisu
 *
 */
public class OpenSqlEditorAction extends Action implements ISelectionChangedListener
{

	private boolean sqlExplorerLoaded = false;
	private SimpleUserNode selectedNode;
	private final String perspectiveId = "net.sourceforge.sqlexplorer.plugin.perspectives.SQLExplorerPluginPerspective";
	public static final String AUTOMATIC_SWITCH_PERSPECTIVE_PROPERTY = "automaticSwitchPerspective";

	/**
	 * Creates a new action for the given solution view.
	 *
	 * @param sev the solution view to use.
	 */
	public OpenSqlEditorAction()
	{
		setImageDescriptor(Activator.loadImageDescriptorFromBundle("sql_editor.png"));
		setText("Open SQL Editor");
		setToolTipText("Open the SQL Editor for this server");
		sqlExplorerLoaded = com.servoy.eclipse.core.Activator.getDefault().isSqlExplorerLoaded();
		if (!sqlExplorerLoaded) setEnabled(false);
	}

	public void selectionChanged(SelectionChangedEvent event)
	{
		if (sqlExplorerLoaded)
		{
			IStructuredSelection sel = (IStructuredSelection)event.getSelection();
			boolean state = (sel.size() == 1);
			if (state)
			{
				SimpleUserNode node = ((SimpleUserNode)sel.getFirstElement());
				state = (node.getType() == UserNodeType.SERVER && node.getRealObject() != null);
				if (!state)
				{
					state = ((node.getType() == UserNodeType.TABLE || node.getType() == UserNodeType.VIEW) && node.getRealObject() != null);
				}
				if (state)
				{
					selectedNode = node;
				}
			}
			setEnabled(state);
		}
	}

	@Override
	public void run()
	{
		if (selectedNode == null) return;
		String serverName = null;
		String tableName = null;
		if (selectedNode.getRealObject() instanceof IServerInternal)
		{
			serverName = ((IServerInternal)selectedNode.getRealObject()).getName();
		}

		if (selectedNode.getRealObject() instanceof Table)
		{
			serverName = ((Table)selectedNode.getRealObject()).getServerName();
			tableName = ((Table)selectedNode.getRealObject()).getName();
		}
		if (selectedNode.getRealObject() instanceof TableWrapper)
		{
			serverName = ((TableWrapper)selectedNode.getRealObject()).getServerName();
			tableName = ((TableWrapper)selectedNode.getRealObject()).getTableName();
		}
		if (serverName != null)
		{
			final IWorkbenchWindow wWindow = SQLExplorerPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow();
			IWorkbenchPage page = wWindow.getActivePage();
			if (!page.getPerspective().getId().equals(perspectiveId))
			{
				IEclipsePreferences store = InstanceScope.INSTANCE.getNode(Activator.getDefault().getBundle().getSymbolicName());
				String option = store.get(AUTOMATIC_SWITCH_PERSPECTIVE_PROPERTY, MessageDialogWithToggle.NEVER);
				if (MessageDialogWithToggle.ALWAYS.equals(option))
				{
					Display.getCurrent().asyncExec(new Runnable()
					{
						public void run()
						{
							try
							{
								SQLExplorerPlugin.getDefault().getWorkbench().showPerspective(perspectiveId, wWindow);
							}
							catch (Exception e)
							{
								ServoyLog.logError(e);
							}
						}
					});
				}
			}
			SQLEditorInput input = new SQLEditorInput("SQL Editor " + serverName + ".sql");
			Alias alias = SQLExplorerPlugin.getDefault().getAliasManager().getAlias(serverName);
			input.setUser(alias.getDefaultUser());
			try
			{
				IEditorPart editorPart = page.openEditor(input, SQLEditor.class.getName());
				if (tableName != null)
				{
					if (editorPart instanceof SQLEditor)
					{
						SQLEditor textEditor = (SQLEditor)editorPart;

						String sql = textEditor.getSQLToBeExecuted();

						if (sql == null || sql.trim().equals(""))
						{
							textEditor.setText("select * from " + tableName);
						}
					}
				}
			}
			catch (PartInitException e)
			{
				ServoyLog.logError(e);
			}
		}
	}

	public static void show()
	{
		ReplaceTableWizard replaceTableWizard = new ReplaceTableWizard();

		IStructuredSelection selection = StructuredSelection.EMPTY;
		replaceTableWizard.init(PlatformUI.getWorkbench(), selection);

		WizardDialog dialog = new WizardDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), replaceTableWizard);
		dialog.create();
		dialog.open();
	}
}
