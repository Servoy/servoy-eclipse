/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2018 Servoy BV

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.window.Window;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.OptionDialog;
import com.servoy.eclipse.model.inmemory.MemServer;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.Procedure;
import com.servoy.j2db.persistence.ProcedureColumn;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.query.ColumnType;

/**
 * @author jcompagner
 *
 */
public class CreateInMemFromSpAction extends Action implements ISelectionChangedListener
{
	private final SolutionExplorerView viewer;

	public CreateInMemFromSpAction(SolutionExplorerView sev)
	{
		this.viewer = sev;
		setText("Create inmem table from procedure");
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event)
	{
		IStructuredSelection sel = (IStructuredSelection)event.getSelection();
		boolean state = (sel.size() == 1);
		if (state)
		{
			SimpleUserNode node = (SimpleUserNode)sel.getFirstElement();
			state = (node.getType() == UserNodeType.PROCEDURE);
		}
		setEnabled(state);
	}

	@Override
	public void run()
	{
		SimpleUserNode node = viewer.getSelectedListNode();
		Procedure proc = (Procedure)node.getRealObject();
		if (proc.getColumns().size() > 0)
		{
			final ServoyProject[] activeModules = ServoyModelManager.getServoyModelManager().getServoyModel().getModulesOfActiveProject();
			List<String> modules = new ArrayList<String>();
			for (ServoyProject project : activeModules)
			{
				modules.add(project.getProject().getName());
			}
			if (modules.size() == 0) return;

			Collections.sort(modules);
			String[] moduleNames = modules.toArray(new String[] { });
			final OptionDialog optionDialog = new OptionDialog(viewer.getSite().getShell(), "Create in mem table(s) for procedure " + proc.getName(), null,
				"Select destination solution for the in mem table", MessageDialog.INFORMATION, new String[] { "OK", "Cancel" }, 0, moduleNames, 0);
			int retval = optionDialog.open();
			String selectedProject = null;
			if (retval == Window.OK)
			{
				selectedProject = moduleNames[optionDialog.getSelectedOption()];
			}
			if (selectedProject != null)
			{
				Map<String, List<ProcedureColumn>> columns = proc.getColumns();
				for (Entry<String, List<ProcedureColumn>> entry : columns.entrySet())
				{
					ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(selectedProject);
					MemServer memServer = servoyProject.getMemServer();

					String name = entry.getKey();
					// if there is only 1 then don't follow the special proc columns divider.
					if (columns.size() == 1) name = proc.getName();
					IValidateName validator = ServoyModelManager.getServoyModelManager().getServoyModel().getNameValidator();

					try
					{
						ITable table = memServer.createNewTable(validator, name);
						List<ProcedureColumn> procColumns = entry.getValue();
						for (ProcedureColumn procColumn : procColumns)
						{
							ColumnType columnType = procColumn.getColumnType();
							table.createNewColumn(validator, procColumn.getName(), columnType.getSqlType(), columnType.getLength(), columnType.getScale());

						}
						EditorUtil.openTableEditor(table);
					}
					catch (RepositoryException e)
					{
						ServoyLog.logError(e);
					}
				}
			}
		}
		else
		{
			// we need to show a dialog so it can execute it?
		}
	}
}
