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

import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.OptionDialog;
import com.servoy.eclipse.model.inmemory.MemServer;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.DataModelManager;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.Procedure;
import com.servoy.j2db.persistence.ProcedureColumn;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.query.ColumnType;
import com.servoy.j2db.util.serialize.JSONSerializerWrapper;

/**
 * @author jcompagner
 *
 */
public class CreateInMemFromSpAction extends Action implements ISelectionChangedListener
{
	/**
	 * @author jcomp
	 *
	 */
	private final class OptionWithTextFields extends OptionDialog
	{
		private final String[] inputValues;
		private final Map<String, Text> fields = new HashMap<>();
		private final Map<String, String> values = new HashMap<>();

		/**
		 * @param parentShell
		 * @param dialogTitle
		 * @param dialogTitleImage
		 * @param dialogMessage
		 * @param dialogImageType
		 * @param dialogButtonLabels
		 * @param defaultIndex
		 * @param options
		 * @param defaultOptionsIndex
		 */
		private OptionWithTextFields(Shell parentShell, String dialogTitle, Image dialogTitleImage, String dialogMessage, int dialogImageType,
			String[] dialogButtonLabels, int defaultIndex, String[] options, int defaultOptionsIndex, String[] inputValues)
		{
			super(parentShell, dialogTitle, dialogTitleImage, dialogMessage, dialogImageType, dialogButtonLabels, defaultIndex, options, defaultOptionsIndex);
			this.inputValues = inputValues;
		}

		@Override
		protected Control createCustomArea(Composite parent)
		{
			super.createCustomArea(parent);
			Composite comp = new Composite(parent, SWT.NONE);
			GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
			comp.setLayoutData(gridData);
			comp.setLayout(new GridLayout(2, false));
			for (int index = 0; index < inputValues.length; index++)
			{
				Label lbl = new Label(comp, SWT.NONE);
				if (inputValues.length == 1)
				{
					lbl.setText("Tablename: ");
				}
				else
				{
					lbl.setText("Tablename " + (index + 1) + ":");
				}
				Text text = new Text(comp, SWT.SINGLE | SWT.BORDER);
				gridData = new GridData(GridData.FILL_HORIZONTAL);
				text.setLayoutData(gridData);
				String label = inputValues[index];
				text.setText(label);
				fields.put(label, text);
			}
			return comp;
		}

		public String getInputText(String inputValue)
		{
			return values.get(inputValue);
		}

		@Override
		public boolean close()
		{
			for (Entry<String, Text> entry : fields.entrySet())
			{
				values.put(entry.getKey(), entry.getValue().getText());
			}
			return super.close();
		}
	}

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

			Map<String, List<ProcedureColumn>> columns = proc.getColumns();
			String[] labels = null;
			if (columns.size() == 1)
			{
				labels = new String[] { proc.getName() };
			}
			else
			{
				labels = columns.keySet().toArray(new String[] { });
			}

			final OptionWithTextFields optionDialog = new OptionWithTextFields(viewer.getSite().getShell(),
				"Create in mem table(s) for procedure " + proc.getName(), null, "Select destination solution for the in mem table", MessageDialog.INFORMATION,
				new String[] { "OK", "Cancel" }, 0, moduleNames, 0, labels);
			int retval = optionDialog.open();
			String selectedProject = null;
			if (retval == Window.OK)
			{
				selectedProject = moduleNames[optionDialog.getSelectedOption()];
			}
			if (selectedProject != null)
			{
				for (Entry<String, List<ProcedureColumn>> entry : columns.entrySet())
				{
					ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(selectedProject);
					MemServer memServer = servoyProject.getMemServer();

					String name = entry.getKey();
					// if there is only 1 then don't follow the special proc columns divider.
					if (columns.size() == 1) name = optionDialog.getInputText(proc.getName());
					else name = optionDialog.getInputText(name);

					IValidateName validator = ServoyModelManager.getServoyModelManager().getServoyModel().getNameValidator();

					try
					{
						if (memServer.hasTable(name))
						{
							// TODO ask the question what should happen, cancel, overwrite, merge
						}
						ITable table = memServer.createNewTable(validator, name);
						List<ProcedureColumn> procColumns = entry.getValue();
						for (ProcedureColumn procColumn : procColumns)
						{
							// for now merge new columns in.
							if (table.getColumn(procColumn.getName()) == null)
							{
								String converterName = null;
								ColumnType columnType = procColumn.getColumnType();
								if (columnType.getSqlType() == Types.ARRAY)
								{
									// we have no direct support for array, use TEXT with stringserializer
									columnType = ColumnType.getColumnType(IColumnTypes.TEXT);
									converterName = JSONSerializerWrapper.STRING_SERIALIZER_NAME;
								}
								Column column = table.createNewColumn(validator, procColumn.getName(), columnType);
								if (converterName != null)
								{
									DataModelManager dmm = ServoyModelManager.getServoyModelManager().getServoyModel().getDataModelManager();
									if (dmm != null)
									{
										try
										{
											dmm.createNewColumnInfo(column, false);
											column.getColumnInfo().setConverterName(converterName);
										}
										catch (RepositoryException e)
										{
											ServoyLog.logWarning("Cannot create new column info in table editor", e);
										}
									}
								}
							}
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
