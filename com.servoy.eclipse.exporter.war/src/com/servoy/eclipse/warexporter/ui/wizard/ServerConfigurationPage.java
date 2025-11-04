/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2011 Servoy BV

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

package com.servoy.eclipse.warexporter.ui.wizard;

import java.util.HashMap;
import java.util.SortedSet;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.typed.PojoProperties;
import org.eclipse.core.databinding.observable.value.AbstractObservableValue;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.jface.databinding.swt.typed.WidgetProperties;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.grouplayout.GroupLayout;
import org.eclipse.swt.layout.grouplayout.LayoutStyle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.model.war.exporter.ServerConfiguration;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.ServerConfig;

/**
 * @author jcompagner
 *
 */
public class ServerConfigurationPage extends WizardPage implements IRestoreDefaultPage
{
	private DataBindingContext m_bindingContext;

	private final HashMap<String, IWizardPage> serverConfigurationPages;
	private final SortedSet<String> selectedServerNames;
	private final ServerConfiguration config;
	private Text statementsIdle;
	private Text maxIdle;
	private Text maxActive;
	private Text schema;
	private Text catalog;
	private Text username;
	private Text driver;
	private Text url;
	private Text validationQuery;
	private Combo clone;
	private Text password;
	private Button skip;
	private Button procedures;
	private Combo validationType;

	private final Wizard wizard;

	/**
	 * @param pageName
	 * @param selectedServerNames
	 * @param serverConfigurationPages
	 */
	public ServerConfigurationPage(String pageName, ServerConfiguration serverConfig, SortedSet<String> selectedServerNames,
		HashMap<String, IWizardPage> serverConfigurationPages, Wizard wizard)
	{
		super(pageName);
		this.selectedServerNames = selectedServerNames;
		this.serverConfigurationPages = serverConfigurationPages;
		this.config = serverConfig;
		this.wizard = wizard;

		// if repository_server, mark configuration required
		boolean isRepositoryServer = serverConfig.getName().equals(IServer.REPOSITORY_SERVER);
		setTitle("Database server configuration for: " + serverConfig.getName() + (isRepositoryServer ? " (required)" : ""));
		setDescription("Specify the configuration for the database server '" + serverConfig.getName() + "' as it is used on the application server" +
			(isRepositoryServer ? ". This server configuration is required" : ""));
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent)
	{
		Composite container = new Composite(parent, SWT.NULL);
		setControl(container);

		Label urlLabel = label(container, "URL");
		Label driverlabel = label(container, "Driver");
		Label usernameLabel = label(container, "Username");
		Label catalogLabel = label(container, "Catalog");
		Label schemaLabel = label(container, "Schema");
		Label maximumConnectionsActiveLabel = label(container, "Maximum connections active");
		Label maximumConnectionsIdleLabel = label(container, "Maximum connections idle");
		Label maximumPreparedStatementsIdleLabel = label(container, "Maximum prepared statements idle");
		Label connectionValidationTypeLabel = label(container, "Connection validation type");

		statementsIdle = new Text(container, SWT.BORDER);

		maxIdle = new Text(container, SWT.BORDER);

		maxActive = new Text(container, SWT.BORDER);

		schema = new Text(container, SWT.BORDER);
		schema.setText("");

		catalog = new Text(container, SWT.BORDER);
		catalog.setText("");

		username = new Text(container, SWT.BORDER);
		username.setText("");

		driver = new Text(container, SWT.BORDER);
		driver.setText("");

		url = new Text(container, SWT.BORDER);
		url.setText("");

		Label validationQueryLabel = label(container, "Validation Query");
		Label datamodelCloedFromLabel = label(container, "Data model cloned from");
		Label skipSystemtableslabel = label(container, "Skip system tables");
		Label proceduresLabel = label(container, "Use procedures");

		validationQuery = new Text(container, SWT.BORDER);

		clone = new Combo(container, SWT.NONE);
		clone.setItems(selectedServerNames.toArray(new String[selectedServerNames.size()]));

		skip = new Button(container, SWT.CHECK);

		procedures = new Button(container, SWT.CHECK);

		ComboViewer comboViewer = new ComboViewer(container, SWT.NONE);
		comboViewer.setContentProvider(ArrayContentProvider.getInstance());
		comboViewer.setLabelProvider(new LabelProvider()
		{
			/*
			 * (non-Javadoc)
			 *
			 * @see org.eclipse.jface.viewers.LabelProvider#getText(java.lang.Object)
			 */
			@Override
			public String getText(Object element)
			{
				return ServerConfig.getConnectionValidationTypeAsString(((Integer)element).intValue());
			}
		});
		comboViewer.setInput(new Integer[] { Integer.valueOf(ServerConfig.CONNECTION_EXCEPTION_VALIDATION), Integer.valueOf(
			ServerConfig.CONNECTION_METADATA_VALIDATION), Integer.valueOf(
				ServerConfig.CONNECTION_QUERY_VALIDATION), Integer.valueOf(ServerConfig.CONNECTION_DRIVER_VALIDATION) });

		validationType = comboViewer.getCombo();
		validationType.select(config.getConnectionValidationType());

		Label lblNewLabel_12 = label(container, "Password");
		password = new Text(container, SWT.BORDER | SWT.PASSWORD);

		GroupLayout gl_container = new GroupLayout(container);
		gl_container.setHorizontalGroup(gl_container.createParallelGroup(GroupLayout.LEADING).add(gl_container.createSequentialGroup().addContainerGap().add(
			gl_container.createParallelGroup(GroupLayout.LEADING).add(maximumPreparedStatementsIdleLabel).add(maximumConnectionsIdleLabel)
				.add(maximumConnectionsActiveLabel)
				.add(schemaLabel).add(
					catalogLabel)
				.add(usernameLabel).add(driverlabel).add(urlLabel).add(connectionValidationTypeLabel).add(validationQueryLabel)
				.add(datamodelCloedFromLabel).add(
					skipSystemtableslabel)
				.add(proceduresLabel).add(18).add(lblNewLabel_12))
			.add(18).add(
				gl_container.createParallelGroup(GroupLayout.LEADING).add(procedures).add(skip).add(statementsIdle, GroupLayout.DEFAULT_SIZE, 347,
					Short.MAX_VALUE).add(url, GroupLayout.DEFAULT_SIZE, 347, Short.MAX_VALUE).add(driver, GroupLayout.DEFAULT_SIZE, 347,
						Short.MAX_VALUE)
					.add(username, GroupLayout.DEFAULT_SIZE, 347, Short.MAX_VALUE).add(catalog, GroupLayout.DEFAULT_SIZE, 347,
						Short.MAX_VALUE)
					.add(schema, GroupLayout.DEFAULT_SIZE, 347, Short.MAX_VALUE).add(maxActive, GroupLayout.DEFAULT_SIZE, 347,
						Short.MAX_VALUE)
					.add(maxIdle, GroupLayout.DEFAULT_SIZE, 347, Short.MAX_VALUE).add(validationQuery,
						GroupLayout.DEFAULT_SIZE, 347, Short.MAX_VALUE)
					.add(clone, GroupLayout.DEFAULT_SIZE, 347, Short.MAX_VALUE).add(
						validationType, GroupLayout.DEFAULT_SIZE, 347, Short.MAX_VALUE)
					.add(password, GroupLayout.DEFAULT_SIZE, 347,
						Short.MAX_VALUE))
			.addContainerGap()));
		gl_container.setVerticalGroup(gl_container.createParallelGroup(GroupLayout.LEADING).add(
			gl_container.createSequentialGroup().addContainerGap().add(gl_container.createParallelGroup(GroupLayout.BASELINE).add(urlLabel).add(url,
				GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)).addPreferredGap(LayoutStyle.RELATED).add(
					gl_container.createParallelGroup(GroupLayout.BASELINE).add(driverlabel).add(driver, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
						GroupLayout.PREFERRED_SIZE))
				.addPreferredGap(LayoutStyle.RELATED).add(
					gl_container.createParallelGroup(GroupLayout.BASELINE).add(usernameLabel).add(username, GroupLayout.PREFERRED_SIZE,
						GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
				.addPreferredGap(LayoutStyle.RELATED).add(
					gl_container.createParallelGroup(GroupLayout.BASELINE).add(lblNewLabel_12).add(password, GroupLayout.PREFERRED_SIZE,
						GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
				.addPreferredGap(LayoutStyle.RELATED).add(
					gl_container.createParallelGroup(GroupLayout.BASELINE).add(catalogLabel).add(catalog, GroupLayout.PREFERRED_SIZE,
						GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
				.addPreferredGap(LayoutStyle.RELATED).add(
					gl_container.createParallelGroup(GroupLayout.BASELINE).add(schemaLabel).add(
						schema, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
						GroupLayout.PREFERRED_SIZE))
				.addPreferredGap(LayoutStyle.RELATED).add(
					gl_container.createParallelGroup(GroupLayout.BASELINE).add(maximumConnectionsActiveLabel).add(maxActive,
						GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
						GroupLayout.PREFERRED_SIZE))
				.addPreferredGap(LayoutStyle.RELATED).add(
					gl_container.createParallelGroup(GroupLayout.BASELINE).add(maximumConnectionsIdleLabel).add(maxIdle,
						GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
						GroupLayout.PREFERRED_SIZE))
				.addPreferredGap(LayoutStyle.RELATED).add(
					gl_container.createParallelGroup(GroupLayout.BASELINE).add(maximumPreparedStatementsIdleLabel).add(
						statementsIdle, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
						GroupLayout.PREFERRED_SIZE))
				.addPreferredGap(LayoutStyle.RELATED).add(
					gl_container.createParallelGroup(GroupLayout.BASELINE).add(
						connectionValidationTypeLabel).add(validationType, GroupLayout.PREFERRED_SIZE,
							GroupLayout.DEFAULT_SIZE,
							GroupLayout.PREFERRED_SIZE))
				.addPreferredGap(
					LayoutStyle.RELATED)
				.add(
					gl_container.createParallelGroup(GroupLayout.BASELINE).add(
						validationQueryLabel).add(validationQuery,
							GroupLayout.PREFERRED_SIZE,
							GroupLayout.DEFAULT_SIZE,
							GroupLayout.PREFERRED_SIZE))
				.addPreferredGap(
					LayoutStyle.RELATED)
				.add(
					gl_container.createParallelGroup(
						GroupLayout.BASELINE).add(
							datamodelCloedFromLabel)
						.add(clone,
							GroupLayout.PREFERRED_SIZE,
							GroupLayout.DEFAULT_SIZE,
							GroupLayout.PREFERRED_SIZE))
				.addPreferredGap(
					LayoutStyle.RELATED)
				.add(
					gl_container.createParallelGroup(
						GroupLayout.BASELINE).add(
							skipSystemtableslabel)
						.add(
							skip))
				.addPreferredGap(
					LayoutStyle.RELATED)
				.add(
					gl_container.createParallelGroup(
						GroupLayout.BASELINE).add(
							proceduresLabel)
						.add(
							procedures))
				.addContainerGap(
					GroupLayout.DEFAULT_SIZE,
					Short.MAX_VALUE)));
		container.setLayout(gl_container);
		container.setTabList(
			new Control[] { url, driver, username, password, catalog, schema, maxActive, maxIdle, statementsIdle, validationType, validationQuery, clone, skip, procedures });
		m_bindingContext = initDataBindings();

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.jface.wizard.WizardPage#getNextPage()
	 */
	@Override
	public IWizardPage getNextPage()
	{
		boolean next = false;
		//check for the required repository_server and add it if not present in the selected servers list
		for (String serverName : selectedServerNames)
		{
			if (next)
			{
				return serverConfigurationPages.get(serverName);
			}
			if (config.getName().equals(serverName))
			{
				next = true;
			}
		}
		return null;
	}


	protected DataBindingContext initDataBindings()
	{
		DataBindingContext bindingContext = new DataBindingContext();
		//
		IObservableValue urlObserveTextObserveWidget = WidgetProperties.text(SWT.Modify).observe(url);
		IObservableValue configServerUrlObserveValue = PojoProperties.value(ServerConfiguration.class, "serverUrl").observe(config);
		bindingContext.bindValue(urlObserveTextObserveWidget, configServerUrlObserveValue, null, null);
		//
		IObservableValue catalogObserveTextObserveWidget = WidgetProperties.text(SWT.Modify).observe(catalog);
		IObservableValue configCatalogObserveValue = PojoProperties.value(ServerConfiguration.class, "catalog").observe(config);
		bindingContext.bindValue(catalogObserveTextObserveWidget, configCatalogObserveValue, null, null);
		//
		IObservableValue cloneObserveSelectionObserveWidget = WidgetProperties.widgetSelection().observe(clone);
		IObservableValue configDataModelCloneFromObserveValue = PojoProperties.value(ServerConfiguration.class, "dataModelCloneFrom").observe(config);
		bindingContext.bindValue(cloneObserveSelectionObserveWidget, configDataModelCloneFromObserveValue, null, null);
		//
		IObservableValue driverObserveTextObserveWidget = WidgetProperties.text(SWT.Modify).observe(driver);
		IObservableValue configDriverObserveValue = PojoProperties.value(ServerConfiguration.class, "driver").observe(config);
		bindingContext.bindValue(driverObserveTextObserveWidget, configDriverObserveValue, null, null);
		//
		IObservableValue validationQueryObserveTextObserveWidget = WidgetProperties.text(SWT.Modify).observe(validationQuery);
		IObservableValue configValidationQueryObserveValue = PojoProperties.value(ServerConfiguration.class, "validationQuery").observe(config);
		bindingContext.bindValue(validationQueryObserveTextObserveWidget, configValidationQueryObserveValue, null, null);
		//
		IObservableValue usernameObserveTextObserveWidget = WidgetProperties.text(SWT.Modify).observe(username);
		IObservableValue configUserNameObserveValue = PojoProperties.value(ServerConfiguration.class, "userName").observe(config);
		bindingContext.bindValue(usernameObserveTextObserveWidget, configUserNameObserveValue, null, null);
		//
		IObservableValue schemaObserveTextObserveWidget = WidgetProperties.text(SWT.Modify).observe(schema);
		IObservableValue configSchemaObserveValue = PojoProperties.value(ServerConfiguration.class, "schema").observe(config);
		bindingContext.bindValue(schemaObserveTextObserveWidget, configSchemaObserveValue, null, null);
		//
		IObservableValue maxActiveObserveTextObserveWidget = WidgetProperties.text(SWT.Modify).observe(maxActive);
		IObservableValue configMaxActiveObserveValue = PojoProperties.value(ServerConfiguration.class, "maxActive").observe(config);
		bindingContext.bindValue(maxActiveObserveTextObserveWidget, configMaxActiveObserveValue, null, null);
		//
		IObservableValue maxIdleObserveTextObserveWidget = WidgetProperties.text(SWT.Modify).observe(maxIdle);
		IObservableValue configMaxIdleObserveValue = PojoProperties.value(ServerConfiguration.class, "maxIdle").observe(config);
		bindingContext.bindValue(maxIdleObserveTextObserveWidget, configMaxIdleObserveValue, null, null);
		//
		IObservableValue statementsIdleObserveTextObserveWidget = WidgetProperties.text(SWT.Modify).observe(statementsIdle);
		IObservableValue configMaxPreparedStatementsIdleObserveValue = PojoProperties.value(ServerConfiguration.class, "maxPreparedStatementsIdle")
			.observe(config);
		bindingContext.bindValue(statementsIdleObserveTextObserveWidget, configMaxPreparedStatementsIdleObserveValue, null, null);
		//
		IObservableValue skipObserveSelectionObserveWidget = WidgetProperties.widgetSelection().observe(skip);
		IObservableValue configSkipSysTablesObserveValue = PojoProperties.value(ServerConfiguration.class, "skipSysTables")
			.observe(config);
		bindingContext.bindValue(skipObserveSelectionObserveWidget, configSkipSysTablesObserveValue, null, null);
		//
		IObservableValue proceduresObserveSelectionObserveWidget = WidgetProperties.widgetSelection().observe(procedures);
		IObservableValue configproceduresObserveValue = PojoProperties.value(ServerConfiguration.class, "queryProcedures")
			.observe(config);
		bindingContext.bindValue(proceduresObserveSelectionObserveWidget, configproceduresObserveValue, null, null);
		//
		IObservableValue passwordObserveTextObserveWidget = WidgetProperties.text(SWT.Modify).observe(password);
		IObservableValue configPasswordObserveValue = PojoProperties.value(ServerConfiguration.class, "password")
			.observe(config);
		bindingContext.bindValue(passwordObserveTextObserveWidget, configPasswordObserveValue, null, null);
		//
		IObservableValue getValidationTypeObserveValue = new AbstractObservableValue()
		{
			public Object getValueType()
			{
				return null;
			}

			@Override
			protected Object doGetValue()
			{
				return ServerConfig.getConnectionValidationTypeAsString(config.getConnectionValidationType());
			}

			@Override
			protected void doSetValue(Object value)
			{
				String valType = value.toString();
				int type = ServerConfig.VALIDATION_TYPE_DEFAULT;
				if (ServerConfig.getConnectionValidationTypeAsString(ServerConfig.CONNECTION_EXCEPTION_VALIDATION).equals(valType))
				{
					type = ServerConfig.CONNECTION_EXCEPTION_VALIDATION;
					validationQuery.setEnabled(false);
				}
				else if (ServerConfig.getConnectionValidationTypeAsString(ServerConfig.CONNECTION_METADATA_VALIDATION).equals(valType))
				{
					type = ServerConfig.CONNECTION_METADATA_VALIDATION;
					validationQuery.setEnabled(false);
				}
				else if (ServerConfig.getConnectionValidationTypeAsString(ServerConfig.CONNECTION_QUERY_VALIDATION).equals(valType))
				{
					type = ServerConfig.CONNECTION_QUERY_VALIDATION;
					validationQuery.setEnabled(true);
				}
				else if (ServerConfig.getConnectionValidationTypeAsString(ServerConfig.CONNECTION_DRIVER_VALIDATION).equals(valType))
				{
					type = ServerConfig.CONNECTION_DRIVER_VALIDATION;
					validationQuery.setEnabled(false);
				}
				config.setConnectionValidationType(type);
			}
		};
		IObservableValue validationTypeSelectionObserveWidget = WidgetProperties.widgetSelection().observe(validationType);
		bindingContext.bindValue(validationTypeSelectionObserveWidget, getValidationTypeObserveValue, null, null);
		return bindingContext;
	}

	@Override
	public void restoreDefaults()
	{
		url.setText("jdbc:postgresql://localhost:5432/" + config.getName());
		driver.setText("org.postgresql.Driver");
		username.setText("DBA");
		password.setText("");
		catalog.setText("<none>");
		schema.setText("<none>");
		maxActive.setText(String.valueOf(ServerConfig.MAX_ACTIVE_DEFAULT));
		maxIdle.setText(String.valueOf(ServerConfig.MAX_IDLE_DEFAULT));
		statementsIdle.setText(String.valueOf(ServerConfig.MAX_PREPSTATEMENT_IDLE_DEFAULT));
		validationType.select(ServerConfig.VALIDATION_TYPE_DEFAULT);
		validationQuery.setText("");
		clone.setItems(selectedServerNames.toArray(new String[selectedServerNames.size()]));//can't set to empty value otherwise
		skip.setSelection(false);
		procedures.setSelection(false);
	}

	@Override
	public void performHelp()
	{
		PlatformUI.getWorkbench().getHelpSystem().displayHelp("com.servoy.eclipse.exporter.war.export_war_db_server_config");
	}

	private static Label label(Composite parent, String text)
	{
		Label label = new Label(parent, SWT.NONE);
		label.setText(text);
		return label;
	}
}
