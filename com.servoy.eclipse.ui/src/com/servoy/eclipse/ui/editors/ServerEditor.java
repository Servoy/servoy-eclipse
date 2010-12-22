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
package com.servoy.eclipse.ui.editors;

import java.util.Arrays;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.databinding.observable.value.AbstractObservableValue;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.grouplayout.GroupLayout;
import org.eclipse.swt.layout.grouplayout.LayoutStyle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.resource.ServerEditorInput;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.util.BindingHelper;
import com.servoy.eclipse.ui.util.DocumentValidatorVerifyListener;
import com.servoy.eclipse.ui.util.ImmutableObjectObservable;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnInfo;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IServerConfigListener;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.IServerManagerInternal;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ServerConfig;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.util.Settings;

public class ServerEditor extends EditorPart
{
	private DataBindingContext m_bindingContext;
	private boolean modified = false;
	private ImmutableObjectObservable<ServerConfig> serverConfigObservable;

	private Button enabledButton;
	private Button logServerButton;
	private Button createLogTableButton;
	private Button skipSysTablesButton;
	private Text validationQueryField;
	private Combo validationTypeField;
	private Text maxPreparedStatementsIdleField;
	private Text maxIdleField;
	private Text maxActiveField;
	private Combo dataModel_cloneFromField;
	private Combo schemaField;
	private Combo catalogField;
	private Combo driverField;
	private Text urlField;
	private Text passwordField;
	private Text userNameField;
	private Text serverNameField;

	private String oldServerName = null;

	private IServerConfigListener logServerListener = null;

	@Override
	public void createPartControl(Composite parent)
	{
		ScrolledComposite myScrolledComposite = new ScrolledComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL);
		myScrolledComposite.setExpandHorizontal(true);
		myScrolledComposite.setExpandVertical(true);

		Composite comp = new Composite(myScrolledComposite, SWT.NONE);
		myScrolledComposite.setContent(comp);

		Label serverNameLabel;
		serverNameLabel = new Label(comp, SWT.RIGHT);
		serverNameLabel.setText("Server name");

		serverNameField = new Text(comp, SWT.BORDER);

		Label userNameLabel;
		userNameLabel = new Label(comp, SWT.RIGHT);
		userNameLabel.setText("User name");

		userNameField = new Text(comp, SWT.BORDER);

		Label passwordLabel;
		passwordLabel = new Label(comp, SWT.RIGHT);
		passwordLabel.setText("Password");

		passwordField = new Text(comp, SWT.BORDER | SWT.PASSWORD);

		Label urlLabel;
		urlLabel = new Label(comp, SWT.RIGHT);
		urlLabel.setText("URL");

		urlField = new Text(comp, SWT.BORDER);

		Label driverLabel;
		driverLabel = new Label(comp, SWT.RIGHT);
		driverLabel.setText("Driver");

		driverField = new Combo(comp, SWT.BORDER);
		UIUtils.setDefaultVisibleItemCount(driverField);

		Label catalogLabel;
		catalogLabel = new Label(comp, SWT.RIGHT);
		catalogLabel.setText("Catalog");

		catalogField = new Combo(comp, SWT.BORDER);
		UIUtils.setDefaultVisibleItemCount(catalogField);

		Label schemaLabel;
		schemaLabel = new Label(comp, SWT.RIGHT);
		schemaLabel.setText("Schema");

		schemaField = new Combo(comp, SWT.BORDER);
		UIUtils.setDefaultVisibleItemCount(schemaField);

		Label maxActiveLabel;
		maxActiveLabel = new Label(comp, SWT.RIGHT);
		maxActiveLabel.setText("Max Connections Active");

		maxActiveField = new Text(comp, SWT.BORDER);

		Label maxIdleLabel;
		maxIdleLabel = new Label(comp, SWT.RIGHT);
		maxIdleLabel.setText("Max Connections Idle");

		maxIdleField = new Text(comp, SWT.BORDER);

		Label maxPreparedStatementsIdleLabel;
		maxPreparedStatementsIdleLabel = new Label(comp, SWT.RIGHT);
		maxPreparedStatementsIdleLabel.setText("Max Prepared Statements Idle");

		maxPreparedStatementsIdleField = new Text(comp, SWT.BORDER);

		Label validationTypeLabel;
		validationTypeLabel = new Label(comp, SWT.RIGHT);
		validationTypeLabel.setText("Validation Type");

		validationTypeField = new Combo(comp, SWT.BORDER | SWT.READ_ONLY);
		UIUtils.setDefaultVisibleItemCount(validationTypeField);

		Label validationQueryLabel;
		validationQueryLabel = new Label(comp, SWT.RIGHT);
		validationQueryLabel.setText("Validation Query");

		validationQueryField = new Text(comp, SWT.BORDER);

		Label dataModel_cloneFromLabel;
		dataModel_cloneFromLabel = new Label(comp, SWT.RIGHT);
		dataModel_cloneFromLabel.setText("Data model clone from");

		dataModel_cloneFromField = new Combo(comp, SWT.BORDER | SWT.READ_ONLY);
		UIUtils.setDefaultVisibleItemCount(dataModel_cloneFromField);

		Label enabledLabel;
		enabledLabel = new Label(comp, SWT.RIGHT);
		enabledLabel.setText("Enabled");

		enabledButton = new Button(comp, SWT.CHECK);

		Label logServerLabel;
		logServerLabel = new Label(comp, SWT.RIGHT);
		logServerLabel.setText("Log Server"); //$NON-NLS-1$

		logServerButton = new Button(comp, SWT.CHECK);
		logServerButton.addListener(SWT.Selection, new Listener()
		{
			public void handleEvent(Event event)
			{
				flagModified();
			}
		});

		ServoyModel.getServerManager().addServerConfigListener(logServerListener = new LogServerListener());

		createLogTableButton = new Button(comp, SWT.PUSH);
		createLogTableButton.setText("Create Log Table"); //$NON-NLS-1$
		logTableButtonEnabling();
		createLogTableButton.addSelectionListener(new SelectionListener()
		{
			public void widgetSelected(SelectionEvent e)
			{
				IServerInternal logServer = (IServerInternal)ServoyModel.getServerManager().getLogServer();
				if (logServer == null)
				{
					MessageDialog.openError(Display.getDefault().getActiveShell(), "Log server not found", "Required server '" + //$NON-NLS-1$ //$NON-NLS-2$
						ServoyModel.getServerManager().getLogServerName() + "' not found or cannot be reached."); //$NON-NLS-1$
					return;
				}
				try
				{
					Table logTable = logServer.getTable("log"); //$NON-NLS-1$
					if (logTable == null || logTable.getColumn("pk_data") == null) //$NON-NLS-1$
					{
						IValidateName validator = ServoyModelManager.getServoyModelManager().getServoyModel().getNameValidator();
						logTable = logServer.createNewTable(validator, "log"); //$NON-NLS-1$
						Column c = logTable.createNewColumn(validator, "log_id", IColumnTypes.INTEGER, 0); //$NON-NLS-1$
						c.setDatabasePK(true);
						c.setSequenceType(ColumnInfo.SERVOY_SEQUENCE);
						logTable.createNewColumn(validator, "server_name", IColumnTypes.TEXT, 50); //$NON-NLS-1$
						logTable.createNewColumn(validator, "table_name", IColumnTypes.TEXT, 50); //$NON-NLS-1$
						logTable.createNewColumn(validator, "column_name", IColumnTypes.TEXT, 50); //$NON-NLS-1$
						logTable.createNewColumn(validator, "log_action", IColumnTypes.INTEGER, 0); //$NON-NLS-1$
						logTable.createNewColumn(validator, "pk_data", IColumnTypes.TEXT, 190); //$NON-NLS-1$
						logTable.createNewColumn(validator, "old_data", IColumnTypes.TEXT, 0); //$NON-NLS-1$
						logTable.createNewColumn(validator, "new_data", IColumnTypes.TEXT, 0); //$NON-NLS-1$
						logTable.createNewColumn(validator, "event_time", IColumnTypes.DATETIME, 0); //$NON-NLS-1$
						logTable.createNewColumn(validator, "user_uid", IColumnTypes.TEXT, 190); //$NON-NLS-1$
						logServer.syncTableObjWithDB(logTable, false, false, null);
						MessageDialog.openInformation(Display.getDefault().getActiveShell(), "Table log created", "Table log successfully created in '" + //$NON-NLS-1$ //$NON-NLS-2$
							ServoyModel.getServerManager().getLogServerName() + "'."); //$NON-NLS-1$
					}
					else
					{
						MessageDialog.openInformation(Display.getDefault().getActiveShell(), "Table already exists", "Log table already exists in '" + //$NON-NLS-1$ //$NON-NLS-2$
							ServoyModel.getServerManager().getLogServerName() + "'."); //$NON-NLS-1$
					}
					createLogTableButton.setEnabled(false);
				}
				catch (RepositoryException re)
				{
					ServoyLog.logError(re);
					MessageDialog.openError(Display.getDefault().getActiveShell(), "Error creating table", "Could not create log table: " + re.getMessage()); //$NON-NLS-1$//$NON-NLS-2$
				}
				catch (Exception err)
				{
					ServoyLog.logError(err);
					MessageDialog.openError(Display.getDefault().getActiveShell(), "Error creating table", //$NON-NLS-1$
						"Unexpected error while creating log table. Check the log for more details."); //$NON-NLS-1$
				}
			}

			public void widgetDefaultSelected(SelectionEvent e)
			{
				//do nothing
			}
		});

		Label skipSysTablesLabel;
		skipSysTablesLabel = new Label(comp, SWT.RIGHT);
		skipSysTablesLabel.setText("Skip System Tables"); //$NON-NLS-1$

		skipSysTablesButton = new Button(comp, SWT.CHECK);

		final GroupLayout groupLayout = new GroupLayout(comp);
		groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
			groupLayout.createSequentialGroup().addContainerGap().add(
				groupLayout.createParallelGroup(GroupLayout.LEADING, false).add(serverNameLabel, GroupLayout.PREFERRED_SIZE, 190, GroupLayout.PREFERRED_SIZE).add(
					validationTypeLabel, GroupLayout.PREFERRED_SIZE, 190, GroupLayout.PREFERRED_SIZE).add(validationQueryLabel, GroupLayout.PREFERRED_SIZE,
					190, GroupLayout.PREFERRED_SIZE).add(enabledLabel, GroupLayout.PREFERRED_SIZE, 190, GroupLayout.PREFERRED_SIZE).add(logServerLabel,
					GroupLayout.PREFERRED_SIZE, 190, GroupLayout.PREFERRED_SIZE).add(maxPreparedStatementsIdleLabel, GroupLayout.PREFERRED_SIZE, 190,
					GroupLayout.PREFERRED_SIZE).add(maxIdleLabel, GroupLayout.PREFERRED_SIZE, 190, GroupLayout.PREFERRED_SIZE).add(maxActiveLabel,
					GroupLayout.PREFERRED_SIZE, 190, GroupLayout.PREFERRED_SIZE).add(schemaLabel, GroupLayout.PREFERRED_SIZE, 190, GroupLayout.PREFERRED_SIZE).add(
					catalogLabel, GroupLayout.PREFERRED_SIZE, 190, GroupLayout.PREFERRED_SIZE).add(driverLabel, GroupLayout.PREFERRED_SIZE, 190,
					GroupLayout.PREFERRED_SIZE).add(urlLabel, GroupLayout.PREFERRED_SIZE, 190, GroupLayout.PREFERRED_SIZE).add(passwordLabel,
					GroupLayout.PREFERRED_SIZE, 190, GroupLayout.PREFERRED_SIZE).add(userNameLabel, GroupLayout.PREFERRED_SIZE, 190, GroupLayout.PREFERRED_SIZE).add(
					dataModel_cloneFromLabel, GroupLayout.PREFERRED_SIZE, 190, GroupLayout.PREFERRED_SIZE).add(skipSysTablesLabel, GroupLayout.PREFERRED_SIZE,
					190, GroupLayout.PREFERRED_SIZE)).addPreferredGap(LayoutStyle.RELATED).add(
				groupLayout.createParallelGroup(GroupLayout.LEADING).add(serverNameField, GroupLayout.PREFERRED_SIZE, 161, Short.MAX_VALUE).add(userNameField,
					GroupLayout.PREFERRED_SIZE, 161, Short.MAX_VALUE).add(passwordField, GroupLayout.PREFERRED_SIZE, 161, Short.MAX_VALUE).add(urlField,
					GroupLayout.PREFERRED_SIZE, 161, Short.MAX_VALUE).add(driverField, GroupLayout.PREFERRED_SIZE, 161, Short.MAX_VALUE).add(catalogField,
					GroupLayout.PREFERRED_SIZE, 161, Short.MAX_VALUE).add(schemaField, GroupLayout.PREFERRED_SIZE, 161, Short.MAX_VALUE).add(maxActiveField,
					GroupLayout.PREFERRED_SIZE, 161, Short.MAX_VALUE).add(maxIdleField, GroupLayout.PREFERRED_SIZE, 161, Short.MAX_VALUE).add(
					maxPreparedStatementsIdleField, GroupLayout.PREFERRED_SIZE, 161, Short.MAX_VALUE).add(validationTypeField, GroupLayout.PREFERRED_SIZE, 161,
					Short.MAX_VALUE).add(validationQueryField, GroupLayout.PREFERRED_SIZE, 161, Short.MAX_VALUE).add(enabledButton, GroupLayout.PREFERRED_SIZE,
					161, Short.MAX_VALUE).add(
					groupLayout.createSequentialGroup().add(logServerButton, GroupLayout.PREFERRED_SIZE, 161, Short.MAX_VALUE).add(createLogTableButton,
						GroupLayout.PREFERRED_SIZE, 161, GroupLayout.PREFERRED_SIZE)).add(dataModel_cloneFromField, GroupLayout.PREFERRED_SIZE, 161,
					Short.MAX_VALUE).add(skipSysTablesButton, GroupLayout.PREFERRED_SIZE, 161, Short.MAX_VALUE)).addContainerGap()));

		groupLayout.setVerticalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
			groupLayout.createSequentialGroup().addContainerGap().add(
				groupLayout.createParallelGroup(GroupLayout.BASELINE).add(serverNameField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
					GroupLayout.PREFERRED_SIZE).add(serverNameLabel)).addPreferredGap(LayoutStyle.RELATED).add(
				groupLayout.createParallelGroup(GroupLayout.BASELINE).add(userNameField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
					GroupLayout.PREFERRED_SIZE).add(userNameLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)).addPreferredGap(
				LayoutStyle.RELATED).add(
				groupLayout.createParallelGroup(GroupLayout.BASELINE).add(passwordField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
					GroupLayout.PREFERRED_SIZE).add(passwordLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)).addPreferredGap(
				LayoutStyle.RELATED).add(
				groupLayout.createParallelGroup(GroupLayout.BASELINE).add(urlField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
					GroupLayout.PREFERRED_SIZE).add(urlLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)).addPreferredGap(
				LayoutStyle.RELATED).add(
				groupLayout.createParallelGroup(GroupLayout.BASELINE).add(driverField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
					GroupLayout.PREFERRED_SIZE).add(driverLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)).addPreferredGap(
				LayoutStyle.RELATED).add(
				groupLayout.createParallelGroup(GroupLayout.BASELINE).add(catalogField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
					GroupLayout.PREFERRED_SIZE).add(catalogLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)).addPreferredGap(
				LayoutStyle.RELATED).add(
				groupLayout.createParallelGroup(GroupLayout.BASELINE).add(schemaField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
					GroupLayout.PREFERRED_SIZE).add(schemaLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)).addPreferredGap(
				LayoutStyle.RELATED).add(
				groupLayout.createParallelGroup(GroupLayout.BASELINE).add(maxActiveField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
					GroupLayout.PREFERRED_SIZE).add(maxActiveLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)).addPreferredGap(
				LayoutStyle.RELATED).add(
				groupLayout.createParallelGroup(GroupLayout.BASELINE).add(maxIdleField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
					GroupLayout.PREFERRED_SIZE).add(maxIdleLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)).add(10,
				10, 10).add(
				groupLayout.createParallelGroup(GroupLayout.BASELINE).add(maxPreparedStatementsIdleField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
					GroupLayout.PREFERRED_SIZE).add(maxPreparedStatementsIdleLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
					GroupLayout.PREFERRED_SIZE)).add(10, 10, 10).add(
				groupLayout.createParallelGroup(GroupLayout.BASELINE).add(validationTypeLabel).add(validationTypeField, GroupLayout.PREFERRED_SIZE,
					GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)).add(10, 10, 10).add(
				groupLayout.createParallelGroup(GroupLayout.BASELINE).add(validationQueryField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
					GroupLayout.PREFERRED_SIZE).add(validationQueryLabel)).add(10, 10, 10).add(
				groupLayout.createParallelGroup(GroupLayout.BASELINE).add(dataModel_cloneFromField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
					GroupLayout.PREFERRED_SIZE).add(dataModel_cloneFromLabel)).add(10, 10, 10).add(
				groupLayout.createParallelGroup(GroupLayout.BASELINE).add(enabledButton, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
					GroupLayout.PREFERRED_SIZE).add(enabledLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)).add(10,
				10, 10).add(
				groupLayout.createParallelGroup(GroupLayout.BASELINE).add(createLogTableButton, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
					GroupLayout.PREFERRED_SIZE).add(logServerButton, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE).add(
					logServerLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)).add(10, 10, 10).add(
				groupLayout.createParallelGroup(GroupLayout.BASELINE).add(skipSysTablesButton, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
					GroupLayout.PREFERRED_SIZE).add(skipSysTablesLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)).addContainerGap(
				GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
		comp.setLayout(groupLayout);
		myScrolledComposite.setMinSize(comp.computeSize(SWT.DEFAULT, SWT.DEFAULT));

		initComboData();
		initDataBindings();
	}

	@Override
	public void dispose()
	{
		super.dispose();
		m_bindingContext.dispose();
		ServoyModel.getServerManager().removeServerConfigListener(logServerListener);
	}

	/**
	 * @see org.eclipse.ui.part.EditorPart#init(org.eclipse.ui.IEditorSite, org.eclipse.ui.IEditorInput)
	 */
	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException
	{
		setSite(site);
		setInput(input);
	}

	/**
	 * @see org.eclipse.ui.part.EditorPart#setInput(org.eclipse.ui.IEditorInput)
	 */
	@Override
	protected void setInput(IEditorInput input)
	{
		super.setInput(input);

		ServerEditorInput serverInput = (ServerEditorInput)input;

		ServerConfig inputConfig = serverInput.getServerConfig();
		ServerConfig serverConfig = inputConfig == null ? ServerConfig.TEMPLATES.get(ServerConfig.EMPTY_TEMPLATE_NAME) : inputConfig;
		oldServerName = inputConfig == null ? null : inputConfig.getServerName();

		serverConfigObservable = new ImmutableObjectObservable(
			serverConfig,
			new Class[] { String.class, String.class, String.class, String.class, String.class, String.class, String.class, int.class, int.class, int.class, int.class, String.class, String.class, boolean.class, boolean.class },
			new String[] { "serverName", "userName", "password", "serverUrl", "driver", "catalog", "schema", "maxActive", "maxIdle", "maxPreparedStatementsIdle", "connectionValidationType", "validationQuery", "dataModelCloneFrom", "enabled", "skipSysTables" });

		serverConfigObservable.setPropertyValue("serverName", serverInput.getName());
		if (serverInput.getIsNew()) flagModified();
		updateTitle();
	}

	protected void updateTitle()
	{
		setPartName(serverConfigObservable.getObject().getServerName());
		setTitleToolTip(serverConfigObservable.getObject().getServerName());
	}

	@Override
	public void doSave(IProgressMonitor monitor)
	{
		try
		{
			ServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
			IServerManagerInternal serverManager = ServoyModel.getServerManager();
			ServerConfig serverConfig = serverConfigObservable.getObject();
			String currentServerName = serverConfig.getServerName();
			boolean log_server = logServerButton.getSelection();

			if (serverConfig.isEnabled())
			{
				serverManager.testServerConfigConnection(serverConfig, 0); //test if we connect
			}

			Settings settings = Settings.getInstance();
			synchronized (settings)
			{
				if (log_server)
				{
					serverManager.setLogServerName(currentServerName);
				}
				else
				{
					if (serverManager.getLogServerName().equals(currentServerName)) serverManager.setLogServerName(""); //$NON-NLS-1$
				}
				settings.save();
			}

			serverManager.saveServerConfig(oldServerName, serverConfig);

			//"refresh" the log table creation button
			logTableButtonEnabling();

			modified = false;
			firePropertyChange(IEditorPart.PROP_DIRTY);
			updateTitle();
			setInput(new ServerEditorInput(serverConfig));
			initDataBindings();
			if (serverConfig.isOracleDriver() && (serverConfig.getSchema() == null || serverConfig.getSchema().trim().length() == 0))
			{
				// if you do not specify the schema in oracle you see thousands of non-useful system tables/views in that server
				MessageDialog.openInformation(
					getSite().getShell(),
					"Oracle server",
					"You should add a 'schema' for Oracle servers = the Oracle user name.\n\nNot specifying a schema will probably result in seing lots of system tables/views in this server, not just user tables/views.");
			}

			// manually trigger a build here as it doesn't modify files on disk
			servoyModel.buildActiveProjectsInJob();
		}
		catch (Exception e)
		{
			ServoyLog.logError("Cannot setup server", e);
			MessageDialog.openError(getSite().getShell(), "Cannot setup server", e.getMessage());
		}
	}

	@Override
	public boolean isDirty()
	{
		return modified;
	}

	private void initComboData()
	{
		serverNameField.addVerifyListener(DocumentValidatorVerifyListener.IDENT_SERVOY_VERIFIER);
		driverField.removeAll();
		ServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
		for (String name : ServoyModel.getServerManager().getKnownDriverClassNames())
		{
			driverField.add(name);
		}

		driverField.add("sun.jdbc.odbc.JdbcOdbcDriver"); //$NON-NLS-1$

		catalogField.removeAll();
		catalogField.add(ServerConfig.NONE);
		catalogField.add(ServerConfig.EMPTY);

		schemaField.removeAll();
		schemaField.add(ServerConfig.NONE);
		schemaField.add(ServerConfig.EMPTY);

		maxActiveField.addVerifyListener(DocumentValidatorVerifyListener.NUMBER_VERIFIER);
		maxIdleField.addVerifyListener(DocumentValidatorVerifyListener.NUMBER_VERIFIER);
		maxPreparedStatementsIdleField.addVerifyListener(DocumentValidatorVerifyListener.NUMBER_VERIFIER);

		validationTypeField.removeAll();
		validationTypeField.add(ServerConfig.getConnectionValidationTypeAsString(ServerConfig.CONNECTION_EXCEPTION_VALIDATION));
		validationTypeField.add(ServerConfig.getConnectionValidationTypeAsString(ServerConfig.CONNECTION_METADATA_VALIDATION));
		validationTypeField.add(ServerConfig.getConnectionValidationTypeAsString(ServerConfig.CONNECTION_QUERY_VALIDATION));

		dataModel_cloneFromField.removeAll();
		dataModel_cloneFromField.add(ServerConfig.NONE);
		for (ServerConfig sc : ServoyModel.getServerManager().getServerConfigs())
		{
			dataModel_cloneFromField.add(sc.getServerName());
		}

	}

	protected void initDataBindings()
	{
		m_bindingContext = BindingHelper.dispose(m_bindingContext);

		IObservableValue getServerNameObserveValue = serverConfigObservable.observePropertyValue("serverName");
		IObservableValue serverNameTextObserveWidget = SWTObservables.observeText(serverNameField, SWT.Modify);
		IObservableValue getUserNameObserveValue = serverConfigObservable.observePropertyValue("userName");
		IObservableValue userNameTextObserveWidget = SWTObservables.observeText(userNameField, SWT.Modify);
		IObservableValue getPasswordObserveValue = serverConfigObservable.observePropertyValue("password");
		IObservableValue passwordTextObserveWidget = SWTObservables.observeText(passwordField, SWT.Modify);
		IObservableValue getDataModel_cloneFromObserveValue = new AbstractObservableValue()
		{
			@Override
			protected Object doGetValue()
			{
				String dataModelCloneFrom = serverConfigObservable.getObject().getDataModelCloneFrom();
				if (Arrays.asList(dataModel_cloneFromField.getItems()).contains(dataModelCloneFrom)) return dataModelCloneFrom;
				else return ServerConfig.NONE;
			}

			public Object getValueType()
			{
				return null;
			}

			@Override
			protected void doSetValue(Object value)
			{
				serverConfigObservable.setPropertyValue("dataModelCloneFrom", value.equals(ServerConfig.NONE) ? null : value);
			}
		};
		IObservableValue dataModel_cloneFromTextObserveWidget = SWTObservables.observeSelection(dataModel_cloneFromField);
		IObservableValue getUrlObserveValue = serverConfigObservable.observePropertyValue("serverUrl");
		IObservableValue urlTextObserveWidget = SWTObservables.observeText(urlField, SWT.Modify);
		IObservableValue getDriverObserveValue = serverConfigObservable.observePropertyValue("driver");
		IObservableValue driverTextObserveWidget = SWTObservables.observeSelection(driverField);
		IObservableValue catalogSelectionObserveWidget = SWTObservables.observeSelection(catalogField);
		IObservableValue getCatalogObserveValue = new AbstractObservableValue()
		{
			public Object getValueType()
			{
				return null;
			}

			@Override
			protected Object doGetValue()
			{
				String catalog = serverConfigObservable.getObject().getCatalog();
				if (catalog == null) catalog = ServerConfig.NONE;
				else if (catalog.length() == 0) catalog = ServerConfig.EMPTY;
				return catalog;
			}

			@Override
			protected void doSetValue(Object value)
			{
				serverConfigObservable.setPropertyValue("catalog", value);
			}
		};
		IObservableValue schemaSelectionObserveWidget = SWTObservables.observeSelection(schemaField);
		IObservableValue getSchemaObserveValue = new AbstractObservableValue()
		{
			public Object getValueType()
			{
				return null;
			}

			@Override
			protected Object doGetValue()
			{
				String schema = serverConfigObservable.getObject().getSchema();
				if (schema == null) schema = ServerConfig.NONE;
				else if (schema.length() == 0) schema = ServerConfig.EMPTY;
				return schema;
			}

			@Override
			protected void doSetValue(Object value)
			{
				serverConfigObservable.setPropertyValue("schema", value);
			}
		};

		//		IObservableValue getMaxActiveObserveValue = PojoObservables.observeValue(serverConfig, "maxActive");
		IObservableValue getMaxActiveObserveValue = serverConfigObservable.observePropertyValue("maxActive");
		IObservableValue maxActiveTextObserveWidget = SWTObservables.observeText(maxActiveField, SWT.Modify);
//		IObservableValue getMaxIdleObserveValue = PojoObservables.observeValue(serverConfig, "maxIdle");
		IObservableValue getMaxIdleObserveValue = serverConfigObservable.observePropertyValue("maxIdle");
		IObservableValue maxIdleTextObserveWidget = SWTObservables.observeText(maxIdleField, SWT.Modify);
		IObservableValue getMaxPreparedStatementsIdleObserveValue = serverConfigObservable.observePropertyValue("maxPreparedStatementsIdle");
		IObservableValue maxPreparedStatementsIdleTextObserveWidget = SWTObservables.observeText(maxPreparedStatementsIdleField, SWT.Modify);
		IObservableValue getValidationTypeObserveValue = new AbstractObservableValue()
		{
			public Object getValueType()
			{
				return null;
			}

			@Override
			protected Object doGetValue()
			{
				return ServerConfig.getConnectionValidationTypeAsString(serverConfigObservable.getObject().getConnectionValidationType());
			}

			@Override
			protected void doSetValue(Object value)
			{
				String validationType = value.toString();
				int type = ServerConfig.VALIDATION_TYPE_DEFAULT;
				if (ServerConfig.getConnectionValidationTypeAsString(ServerConfig.CONNECTION_EXCEPTION_VALIDATION).equals(validationType))
				{
					type = ServerConfig.CONNECTION_EXCEPTION_VALIDATION;
					validationQueryField.setEnabled(false);
				}
				else if (ServerConfig.getConnectionValidationTypeAsString(ServerConfig.CONNECTION_METADATA_VALIDATION).equals(validationType))
				{
					type = ServerConfig.CONNECTION_METADATA_VALIDATION;
					validationQueryField.setEnabled(false);
				}
				if (ServerConfig.getConnectionValidationTypeAsString(ServerConfig.CONNECTION_QUERY_VALIDATION).equals(validationType))
				{
					type = ServerConfig.CONNECTION_QUERY_VALIDATION;
					validationQueryField.setEnabled(true);
				}
				serverConfigObservable.setPropertyValue("connectionValidationType", new Integer(type));
			}
		};
		IObservableValue validationTypeSelectionObserveWidget = SWTObservables.observeSelection(validationTypeField);
		IObservableValue getValidationQueryObserveValue = serverConfigObservable.observePropertyValue("validationQuery");
		IObservableValue validationQueryTextObserveWidget = SWTObservables.observeText(validationQueryField, SWT.Modify);
		IObservableValue getEnabledObserveValue = serverConfigObservable.observePropertyValue("enabled");
		IObservableValue enabledSelectionObserveWidget = SWTObservables.observeSelection(enabledButton);
		IObservableValue getSkipSysTablesObserveValue = serverConfigObservable.observePropertyValue("skipSysTables");
		IObservableValue skipSysTablesSelectionObserveWidget = SWTObservables.observeSelection(skipSysTablesButton);

		m_bindingContext = new DataBindingContext();
		m_bindingContext.bindValue(serverNameTextObserveWidget, getServerNameObserveValue, null, null);
		m_bindingContext.bindValue(userNameTextObserveWidget, getUserNameObserveValue, null, null);
		m_bindingContext.bindValue(passwordTextObserveWidget, getPasswordObserveValue, null, null);
		m_bindingContext.bindValue(urlTextObserveWidget, getUrlObserveValue, null, null);
		m_bindingContext.bindValue(driverTextObserveWidget, getDriverObserveValue, null, null);
		m_bindingContext.bindValue(catalogSelectionObserveWidget, getCatalogObserveValue, null, null);
		m_bindingContext.bindValue(schemaSelectionObserveWidget, getSchemaObserveValue, null, null);
		m_bindingContext.bindValue(maxActiveTextObserveWidget, getMaxActiveObserveValue, null, null);
		m_bindingContext.bindValue(maxIdleTextObserveWidget, getMaxIdleObserveValue, null, null);
		m_bindingContext.bindValue(maxPreparedStatementsIdleTextObserveWidget, getMaxPreparedStatementsIdleObserveValue, null, null);
		m_bindingContext.bindValue(validationTypeSelectionObserveWidget, getValidationTypeObserveValue, null, null);
		m_bindingContext.bindValue(validationQueryTextObserveWidget, getValidationQueryObserveValue, null, null);
		m_bindingContext.bindValue(dataModel_cloneFromTextObserveWidget, getDataModel_cloneFromObserveValue, null, null);
		m_bindingContext.bindValue(enabledSelectionObserveWidget, getEnabledObserveValue, null, null);
		m_bindingContext.bindValue(skipSysTablesSelectionObserveWidget, getSkipSysTablesObserveValue, null, null);

		BindingHelper.addGlobalChangeListener(m_bindingContext, new IChangeListener()
		{
			public void handleChange(ChangeEvent event)
			{
				flagModified();
			}
		});
		validationQueryField.setEnabled(serverConfigObservable.getObject().getConnectionValidationType() == ServerConfig.CONNECTION_QUERY_VALIDATION);

		logServerButton.setSelection(serverConfigObservable.getObject().getServerName().equals(ServoyModel.getServerManager().getLogServerName()));
	}

	public void flagModified()
	{
		modified = true;
		this.getSite().getShell().getDisplay().asyncExec(new Runnable()
		{
			public void run()
			{
				firePropertyChange(IEditorPart.PROP_DIRTY);
			}
		});
	}

	@Override
	public Object getAdapter(Class adapter)
	{
		if (ServerConfig.class.equals(adapter))
		{
			return serverConfigObservable.getObject();
		}
		return super.getAdapter(adapter);
	}

	@Override
	public void setFocus()
	{
		//in case log table is deleted but the current log server remains the same
		logTableButtonEnabling();
	}

	@Override
	public boolean isSaveAsAllowed()
	{
		return false;
	}

	@Override
	public void doSaveAs()
	{
	}

	private void logTableButtonEnabling()
	{
		if (!ServoyModel.getServerManager().logTableExists() &&
			(serverConfigObservable.getObject().getServerName().equals(ServoyModel.getServerManager().getLogServerName())))
		{
			createLogTableButton.setEnabled(true);
			createLogTableButton.setToolTipText("Create a log table for tracking; " //$NON-NLS-1$
				+ "the creation of such a table is possible only if the current database server is the log server " //$NON-NLS-1$
				+ "and if it does not already contain a log table."); //$NON-NLS-1$
		}
		else
		{
			createLogTableButton.setEnabled(false);
			createLogTableButton.setToolTipText("Log table already exists in '" + //$NON-NLS-1$
				ServoyModel.getServerManager().getLogServerName() + "'."); //$NON-NLS-1$
		}
	}

	class LogServerListener implements IServerConfigListener
	{
		public void serverConfigurationChanged(ServerConfig oldServerConfig, ServerConfig newServerConfig)
		{
			if (serverConfigObservable.getObject().getServerName().equals(ServoyModel.getServerManager().getLogServerName()))
			{
				logServerButton.setSelection(true);
			}
			else
			{
				logServerButton.setSelection(false);
			}
		}
	}

}
