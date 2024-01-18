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
package com.servoy.eclipse.ui.wizards;

import java.lang.reflect.InvocationTargetException;
import java.rmi.RemoteException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.e4.ui.css.swt.CSSSWTConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Activator;
import com.servoy.j2db.persistence.DataSourceCollectorVisitor;
import com.servoy.j2db.persistence.IDeveloperRepository;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.ReplaceTableVisitor;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.DataSourceUtils;

public class ReplaceTableWizard extends Wizard implements INewWizard
{
	public static final String ALL_SOLUTION_SERVERS = "-- All solution servers --";
	private static final String ALL_SOLUTION_TABLES = "-- All solution tables --";
	public static final String ALL_SERVERS = "-- All servers --";
	private static final String ALL_TABLES = "-- All tables --";

	private SourcesSelectorWizardPage sourcesSelector;

	public ReplaceTableWizard()
	{
		setWindowTitle("Replace table");
	}

	public void init(IWorkbench workbench, IStructuredSelection selection)
	{
		sourcesSelector = new SourcesSelectorWizardPage("Replace table selection");
	}

	@Override
	public void addPages()
	{
		this.addPage(sourcesSelector);
	}

	@Override
	public void createPageControls(Composite pageContainer)
	{
		pageContainer.getShell().setData(CSSSWTConstants.CSS_ID_KEY, "svydialog");
		super.createPageControls(pageContainer);
	}

	@Override
	public boolean performFinish()
	{
		IRunnableWithProgress solutionSaveRunnable = new IRunnableWithProgress()
		{
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
			{
				String jobName = "Performing the solution saving;";
				monitor.beginTask(jobName, 1);

				String[] result = sourcesSelector.getSelectedItems();

				Solution solution = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject().getEditingSolution();

				ReplaceTableVisitor visitor = new ReplaceTableVisitor(DataSourceUtils.createDBTableDataSource(result[0], result[1]),
					DataSourceUtils.createDBTableDataSource(result[2], result[3]), sourcesSelector.getReplaceCalculationsAndAggregations());
				try
				{
					solution.acceptVisitor(visitor);
					ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject().saveEditingSolutionNodes(new IPersist[] { solution }, true);
					ModelUtils.getEditingFlattenedSolution(solution).flushAllCachedData();
					ServoyModelManager.getServoyModelManager().getServoyModel().getFlattenedSolution().flushAllCachedData();
				}
				catch (RepositoryException e)
				{
					ServoyLog.logError(e);
				}
				monitor.worked(1);
				monitor.done();
			}
		};

		try
		{
			PlatformUI.getWorkbench().getProgressService().run(true, false, solutionSaveRunnable);
		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
		}
		return true;
	}

	public class SourcesSelectorWizardPage extends WizardPage
	{
		private String sourceServer, sourceTable, targetServer, targetTable;
		private boolean replaceCalculationsAndAggregations = false;

		private Set<String> dataSources;

		private Combo sourceServersCombo;
		private Combo sourceTableNamesCombo;
		private Combo targetServersCombo;
		private Combo targetTableNamesCombo;
		private Button replaceCalculationsButton;

		private final IDeveloperRepository repository;

		/**
		 * Creates a Replace Table wizard page.
		 *
		 * @param pageName the name of the page
		 * @param selection the current resource selection
		 */
		public SourcesSelectorWizardPage(String pageName)
		{
			super(pageName);
			setTitle("Select a server and a table to be replaced");
			setDescription("");
			setDialogSettings(Activator.getDefault().getDialogSettings());

			ServoyModelManager.getServoyModelManager().getServoyModel();
			repository = ApplicationServerRegistry.get().getDeveloperRepository();
		}

		public boolean getReplaceCalculationsAndAggregations()
		{
			return replaceCalculationsAndAggregations;
		}

		/**
		 * Retrieve the active solution datasources and also all the servers;
		 */
		private String[] getCurrentServerNames()
		{
			DataSourceCollectorVisitor collector = new DataSourceCollectorVisitor();

			//retrieve the editing solution
			Solution editingSolution = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject().getEditingSolution();

			//visit the solution
			editingSolution.acceptVisitor(collector);

			//retrieve all the solution server tables
			dataSources = collector.getDataSources();
			Set<String> serverNames = DataSourceUtils.getServerNames(dataSources);
			String[] currentServerNames = new String[serverNames.size() + 1];
			int counter = 0;
			currentServerNames[counter++] = ALL_SOLUTION_SERVERS;
			for (String sName : serverNames)
			{
				currentServerNames[counter++] = sName;
			}
			return currentServerNames;
		}

		/**
		 * Retrieve the active solution datasources and also all the servers;
		 */
		private String[] getAllServerNames()
		{
			//retrieve all the servers that are referenced in the repository
			try
			{
				String[] tmp_allServers = repository.getServerNames(true);

				String[] allServers = new String[tmp_allServers.length + 1];
				int counter = 0;
				allServers[counter++] = ALL_SERVERS;
				for (String serverName : tmp_allServers)
				{
					allServers[counter++] = serverName;
				}
				return allServers;
			}
			catch (RemoteException e)
			{
				ServoyLog.logError(e);
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError(e);
			}
			return null;
		}

		/**
		 * When a given server is selected, all the tables are updated in the sourceTablesCombo
		 *
		 * @param serverName
		 */
		private void updateSourceTablesCombo(String serverName)
		{
			List<String> tableNames = serverName == null ? null : DataSourceUtils.getServerTablenames(dataSources, serverName);
			if (tableNames != null)
			{
				Collections.sort(tableNames);
			}
			if (tableNames == null || tableNames.size() == 0)
			{
				sourceTableNamesCombo.setItems(new String[] { ALL_SOLUTION_TABLES });
			}
			else
			{
				sourceTableNamesCombo.setItems(tableNames.toArray(new String[tableNames.size()]));
			}
			sourceTableNamesCombo.select(0);
		}

		/**
		 * When a given server is selected all the tables are updated in the targetTablesCombo
		 *
		 * @param serverName
		 */
		private void updateTargetTablesCombo(String serverName)
		{
			IServerInternal server;
			String[] allTables = null;
			try
			{
				server = (IServerInternal)repository.getServer(serverName);
				if (server != null)
				{
					List<String> tableNames = server.getTableAndViewNames(true, true);
					allTables = new String[tableNames.size()];

					int counter = 0;
					for (String tableName : tableNames)
					{
						allTables[counter++] = tableName;
					}
				}

			}
			catch (RemoteException e)
			{
				ServoyLog.logError(e);
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError(e);
			}

			targetTableNamesCombo.setItems(allTables == null ? new String[] { ALL_TABLES } : allTables);
			targetTableNamesCombo.select(0);
		}

		public boolean validatePage()
		{
			if (sourceServer == null || ALL_SOLUTION_SERVERS.equals(sourceServer)) return false;
			if (sourceTable == null || ALL_SOLUTION_TABLES.equals(sourceTable)) return false;
			if (targetServer == null || ALL_SERVERS.equals(targetServer)) return false;
			if (targetTable == null || ALL_TABLES.equals(targetTable)) return false;
			return true;
		}

		/**
		 *
		 * @return a string[] with the selected combo items
		 */
		public String[] getSelectedItems()
		{
			return new String[] { sourceServer, sourceTable, targetServer, targetTable };
		}

		/**
		 * (non-Javadoc) Method declared on IDialogPage.
		 */
		public void createControl(Composite parent)
		{
			initializeDialogUnits(parent);
			// top level group
			Composite topLevel = new Composite(parent, SWT.NONE);
			topLevel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));

			setControl(topLevel);

			// Source server
			Label sourceServerLabel = new Label(topLevel, SWT.NONE);
			sourceServerLabel.setText("Source server");

			sourceServersCombo = new Combo(topLevel, SWT.DROP_DOWN | SWT.READ_ONLY);
			UIUtils.setDefaultVisibleItemCount(sourceServersCombo);

			sourceServersCombo.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(SelectionEvent event)
				{
					String sourceServerName = sourceServersCombo.getItem(sourceServersCombo.getSelectionIndex());
					updateSourceTablesCombo(sourceServerName);

					sourceServer = sourceServerName;
					sourceTable = sourceTableNamesCombo.getItem(sourceTableNamesCombo.getSelectionIndex());
					setPageComplete(validatePage());
				}
			});


			//Source table
			Label sourceTableLabel = new Label(topLevel, SWT.NONE);
			sourceTableLabel.setText("Source table");

			sourceTableNamesCombo = new Combo(topLevel, SWT.DROP_DOWN | SWT.READ_ONLY);
			UIUtils.setDefaultVisibleItemCount(sourceTableNamesCombo);
			sourceTableNamesCombo.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(SelectionEvent event)
				{
					sourceTable = sourceTableNamesCombo.getItem(sourceTableNamesCombo.getSelectionIndex());
					setPageComplete(validatePage());
				}
			});

			// Target server
			Label targetServerLabel = new Label(topLevel, SWT.NONE);
			targetServerLabel.setText("Target server");

			targetServersCombo = new Combo(topLevel, SWT.DROP_DOWN | SWT.READ_ONLY);
			UIUtils.setDefaultVisibleItemCount(targetServersCombo);
			targetServersCombo.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(SelectionEvent event)
				{
					String targetServerName = targetServersCombo.getItem(targetServersCombo.getSelectionIndex());
					updateTargetTablesCombo(targetServerName);

					targetServer = targetServerName;
					targetTable = targetTableNamesCombo.getItem(targetTableNamesCombo.getSelectionIndex());
					setPageComplete(validatePage());
				}
			});

			//Target table
			Label targetTableLabel = new Label(topLevel, SWT.NONE);
			targetTableLabel.setText("Target table");

			targetTableNamesCombo = new Combo(topLevel, SWT.DROP_DOWN | SWT.READ_ONLY);
			UIUtils.setDefaultVisibleItemCount(targetTableNamesCombo);
			targetTableNamesCombo.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(SelectionEvent event)
				{
					targetTable = targetTableNamesCombo.getItem(targetTableNamesCombo.getSelectionIndex());
					setPageComplete(validatePage());
				}
			});

			replaceCalculationsButton = new Button(topLevel, SWT.CHECK);
			replaceCalculationsButton.setText("Replace table in calculations/aggregations");
			replaceCalculationsButton.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(SelectionEvent e)
				{
					replaceCalculationsAndAggregations = replaceCalculationsButton.getSelection();
				}
			});

			//Define the layout and place the components
			FormLayout formLayout = new FormLayout();
			formLayout.spacing = 5;
			formLayout.marginWidth = formLayout.marginHeight = 20;
			topLevel.setLayout(formLayout);

			// SOURCES
			//label
			FormData formData = new FormData();
			formData.left = new FormAttachment(0, 0);
			formData.top = new FormAttachment(sourceServersCombo, 0, SWT.CENTER);
			sourceServerLabel.setLayoutData(formData);

			//combo
			formData = new FormData();
			formData.left = new FormAttachment(sourceServerLabel, 0);
			formData.top = new FormAttachment(0, 0);
			formData.right = new FormAttachment(100, 0);
			sourceServersCombo.setLayoutData(formData);

			//label
			formData = new FormData();
			formData.left = new FormAttachment(0, 0);
			formData.top = new FormAttachment(sourceTableNamesCombo, 0, SWT.CENTER);
			sourceTableLabel.setLayoutData(formData);

			//combo
			formData = new FormData();
			formData.left = new FormAttachment(sourceServersCombo, 0, SWT.LEFT);
			formData.top = new FormAttachment(sourceServersCombo, 0, SWT.BOTTOM);
			formData.right = new FormAttachment(100, 0);
			sourceTableNamesCombo.setLayoutData(formData);

			//TARGET

			formData = new FormData();
			formData.left = new FormAttachment(0, 0);
			formData.top = new FormAttachment(targetServersCombo, 0, SWT.CENTER);
			targetServerLabel.setLayoutData(formData);

			formData = new FormData();
			formData.left = new FormAttachment(sourceTableNamesCombo, 0, SWT.LEFT);
			formData.top = new FormAttachment(sourceTableNamesCombo, 0, SWT.BOTTOM);
			formData.right = new FormAttachment(100, 0);
			targetServersCombo.setLayoutData(formData);

			formData = new FormData();
			formData.left = new FormAttachment(0, 0);
			formData.top = new FormAttachment(targetTableNamesCombo, 0, SWT.CENTER);
			targetTableLabel.setLayoutData(formData);

			formData = new FormData();
			formData.left = new FormAttachment(targetServersCombo, 0, SWT.LEFT);
			formData.top = new FormAttachment(targetServersCombo, 0, SWT.BOTTOM);
			formData.right = new FormAttachment(100, 0);
			targetTableNamesCombo.setLayoutData(formData);

			formData = new FormData();
			formData.left = new FormAttachment(0, 0);
			formData.top = new FormAttachment(targetTableLabel, 20, SWT.LEFT);
			replaceCalculationsButton.setLayoutData(formData);

			sourceServersCombo.setItems(getCurrentServerNames());
			sourceServersCombo.select(0);
			targetServersCombo.setItems(getAllServerNames());
			targetServersCombo.select(0);

			updateSourceTablesCombo(null);
			updateTargetTablesCombo(null);
		}

		/*
		 * @see DialogPage.setVisible(boolean)
		 */
		@Override
		public void setVisible(boolean visible)
		{
			super.setVisible(visible);
			IDialogSettings settings = getDialogSettings().getSection("replacetablesection");
			if (settings == null)
			{
				settings = getDialogSettings().addNewSection("replacetablesection");
			}
			if (visible)
			{
				setPageComplete(validatePage());
			}
		}
	}
}
