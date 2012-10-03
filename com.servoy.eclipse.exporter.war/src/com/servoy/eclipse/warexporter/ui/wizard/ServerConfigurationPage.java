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
import org.eclipse.core.databinding.beans.PojoObservables;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wb.swt.layout.grouplayout.GroupLayout;
import org.eclipse.wb.swt.layout.grouplayout.LayoutStyle;

import com.servoy.j2db.persistence.ServerConfig;

/**
 * @author jcompagner
 *
 */
public class ServerConfigurationPage extends WizardPage
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

	/**
	 * @param pageName
	 * @param selectedServerNames 
	 * @param serverConfigurationPages 
	 */
	@SuppressWarnings("nls")
	protected ServerConfigurationPage(String pageName, ServerConfiguration serverConfig, SortedSet<String> selectedServerNames,
		HashMap<String, IWizardPage> serverConfigurationPages)
	{
		super(pageName);
		this.selectedServerNames = selectedServerNames;
		this.serverConfigurationPages = serverConfigurationPages;
		this.config = serverConfig;

		setTitle("Database server configuration for: " + serverConfig.getName());
		setDescription("Specify the configuration for the database server '" + serverConfig.getName() + "' as it is used on the application server");
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

		Label lblNewLabel = new Label(container, SWT.NONE);
		lblNewLabel.setText("URL");

		Label lblNewLabel_1 = new Label(container, SWT.NONE);
		lblNewLabel_1.setText("Driver");

		Label lblNewLabel_2 = new Label(container, SWT.NONE);
		lblNewLabel_2.setText("Username");

		Label lblNewLabel_3 = new Label(container, SWT.NONE);
		lblNewLabel_3.setText("Catalog");

		Label lblNewLabel_4 = new Label(container, SWT.NONE);
		lblNewLabel_4.setText("Schema");

		Label lblNewLabel_5 = new Label(container, SWT.NONE);
		lblNewLabel_5.setText("Maximum connections active");

		Label lblNewLabel_6 = new Label(container, SWT.NONE);
		lblNewLabel_6.setText("Maximum connections idle");

		Label lblNewLabel_7 = new Label(container, SWT.NONE);
		lblNewLabel_7.setText("Maximum prepared statements idle");

		Label lblNewLabel_8 = new Label(container, SWT.NONE);
		lblNewLabel_8.setText("Connection validation type");

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

		Label lblNewLabel_9 = new Label(container, SWT.NONE);
		lblNewLabel_9.setText("Validation Query");

		Label lblNewLabel_10 = new Label(container, SWT.NONE);
		lblNewLabel_10.setText("Data model cloned from");

		Label lblNewLabel_11 = new Label(container, SWT.NONE);
		lblNewLabel_11.setText("Skip system tables");

		validationQuery = new Text(container, SWT.BORDER);

		clone = new Combo(container, SWT.NONE);
		clone.setItems(selectedServerNames.toArray(new String[selectedServerNames.size()]));

		skip = new Button(container, SWT.CHECK);

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
		comboViewer.setInput(new Integer[] { Integer.valueOf(ServerConfig.CONNECTION_EXCEPTION_VALIDATION), Integer.valueOf(ServerConfig.CONNECTION_METADATA_VALIDATION), Integer.valueOf(ServerConfig.CONNECTION_QUERY_VALIDATION) });

		Combo validationType = comboViewer.getCombo();
		validationType.select(config.getConnectionValidationType());

		Label lblNewLabel_12 = new Label(container, SWT.NONE);
		lblNewLabel_12.setText("Password");

		password = new Text(container, SWT.BORDER);
		GroupLayout gl_container = new GroupLayout(container);
		gl_container.setHorizontalGroup(gl_container.createParallelGroup(GroupLayout.LEADING).add(
			gl_container.createSequentialGroup().addContainerGap().add(
				gl_container.createParallelGroup(GroupLayout.LEADING).add(lblNewLabel_7).add(lblNewLabel_6).add(lblNewLabel_5).add(lblNewLabel_4).add(
					lblNewLabel_3).add(lblNewLabel_2).add(lblNewLabel_1).add(lblNewLabel).add(lblNewLabel_8).add(lblNewLabel_9).add(lblNewLabel_10).add(
					lblNewLabel_11).add(lblNewLabel_12)).add(18).add(
				gl_container.createParallelGroup(GroupLayout.LEADING).add(skip).add(statementsIdle, GroupLayout.DEFAULT_SIZE, 347, Short.MAX_VALUE).add(url,
					GroupLayout.DEFAULT_SIZE, 347, Short.MAX_VALUE).add(driver, GroupLayout.DEFAULT_SIZE, 347, Short.MAX_VALUE).add(username,
					GroupLayout.DEFAULT_SIZE, 347, Short.MAX_VALUE).add(catalog, GroupLayout.DEFAULT_SIZE, 347, Short.MAX_VALUE).add(schema,
					GroupLayout.DEFAULT_SIZE, 347, Short.MAX_VALUE).add(maxActive, GroupLayout.DEFAULT_SIZE, 347, Short.MAX_VALUE).add(maxIdle,
					GroupLayout.DEFAULT_SIZE, 347, Short.MAX_VALUE).add(validationQuery, GroupLayout.DEFAULT_SIZE, 347, Short.MAX_VALUE).add(clone,
					GroupLayout.DEFAULT_SIZE, 347, Short.MAX_VALUE).add(validationType, GroupLayout.DEFAULT_SIZE, 347, Short.MAX_VALUE).add(password,
					GroupLayout.DEFAULT_SIZE, 347, Short.MAX_VALUE)).addContainerGap()));
		gl_container.setVerticalGroup(gl_container.createParallelGroup(GroupLayout.LEADING).add(
			gl_container.createSequentialGroup().addContainerGap().add(
				gl_container.createParallelGroup(GroupLayout.BASELINE).add(lblNewLabel).add(url, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
					GroupLayout.PREFERRED_SIZE)).addPreferredGap(LayoutStyle.RELATED).add(
				gl_container.createParallelGroup(GroupLayout.BASELINE).add(lblNewLabel_1).add(driver, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
					GroupLayout.PREFERRED_SIZE)).addPreferredGap(LayoutStyle.RELATED).add(
				gl_container.createParallelGroup(GroupLayout.BASELINE).add(lblNewLabel_2).add(username, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
					GroupLayout.PREFERRED_SIZE)).addPreferredGap(LayoutStyle.RELATED).add(
				gl_container.createParallelGroup(GroupLayout.BASELINE).add(lblNewLabel_12).add(password, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
					GroupLayout.PREFERRED_SIZE)).addPreferredGap(LayoutStyle.RELATED).add(
				gl_container.createParallelGroup(GroupLayout.BASELINE).add(lblNewLabel_3).add(catalog, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
					GroupLayout.PREFERRED_SIZE)).addPreferredGap(LayoutStyle.RELATED).add(
				gl_container.createParallelGroup(GroupLayout.BASELINE).add(lblNewLabel_4).add(schema, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
					GroupLayout.PREFERRED_SIZE)).addPreferredGap(LayoutStyle.RELATED).add(
				gl_container.createParallelGroup(GroupLayout.BASELINE).add(lblNewLabel_5).add(maxActive, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
					GroupLayout.PREFERRED_SIZE)).addPreferredGap(LayoutStyle.RELATED).add(
				gl_container.createParallelGroup(GroupLayout.BASELINE).add(lblNewLabel_6).add(maxIdle, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
					GroupLayout.PREFERRED_SIZE)).addPreferredGap(LayoutStyle.RELATED).add(
				gl_container.createParallelGroup(GroupLayout.BASELINE).add(lblNewLabel_7).add(statementsIdle, GroupLayout.PREFERRED_SIZE,
					GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)).addPreferredGap(LayoutStyle.RELATED).add(
				gl_container.createParallelGroup(GroupLayout.BASELINE).add(lblNewLabel_8).add(validationType, GroupLayout.PREFERRED_SIZE,
					GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)).addPreferredGap(LayoutStyle.RELATED).add(
				gl_container.createParallelGroup(GroupLayout.BASELINE).add(lblNewLabel_9).add(validationQuery, GroupLayout.PREFERRED_SIZE,
					GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)).addPreferredGap(LayoutStyle.RELATED).add(
				gl_container.createParallelGroup(GroupLayout.BASELINE).add(lblNewLabel_10).add(clone, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
					GroupLayout.PREFERRED_SIZE)).addPreferredGap(LayoutStyle.RELATED).add(
				gl_container.createParallelGroup(GroupLayout.BASELINE).add(lblNewLabel_11).add(skip)).addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
		container.setLayout(gl_container);
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
		for (String selectedServerName : selectedServerNames)
		{
			if (next)
			{
				return serverConfigurationPages.get(selectedServerName);
			}
			if (config.getName().equals(selectedServerName))
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
		IObservableValue urlObserveTextObserveWidget = SWTObservables.observeText(url, SWT.Modify);
		IObservableValue configServerUrlObserveValue = PojoObservables.observeValue(config, "serverUrl");
		bindingContext.bindValue(urlObserveTextObserveWidget, configServerUrlObserveValue, null, null);
		//
		IObservableValue catalogObserveTextObserveWidget = SWTObservables.observeText(catalog, SWT.Modify);
		IObservableValue configCatalogObserveValue = PojoObservables.observeValue(config, "catalog");
		bindingContext.bindValue(catalogObserveTextObserveWidget, configCatalogObserveValue, null, null);
		//
		IObservableValue cloneObserveSelectionObserveWidget = SWTObservables.observeSelection(clone);
		IObservableValue configDataModelCloneFromObserveValue = PojoObservables.observeValue(config, "dataModelCloneFrom");
		bindingContext.bindValue(cloneObserveSelectionObserveWidget, configDataModelCloneFromObserveValue, null, null);
		//
		IObservableValue driverObserveTextObserveWidget = SWTObservables.observeText(driver, SWT.Modify);
		IObservableValue configDriverObserveValue = PojoObservables.observeValue(config, "driver");
		bindingContext.bindValue(driverObserveTextObserveWidget, configDriverObserveValue, null, null);
		//
		IObservableValue validationQueryObserveTextObserveWidget = SWTObservables.observeText(validationQuery, SWT.Modify);
		IObservableValue configValidationQueryObserveValue = PojoObservables.observeValue(config, "validationQuery");
		bindingContext.bindValue(validationQueryObserveTextObserveWidget, configValidationQueryObserveValue, null, null);
		//
		IObservableValue usernameObserveTextObserveWidget = SWTObservables.observeText(username, SWT.Modify);
		IObservableValue configUserNameObserveValue = PojoObservables.observeValue(config, "userName");
		bindingContext.bindValue(usernameObserveTextObserveWidget, configUserNameObserveValue, null, null);
		//
		IObservableValue schemaObserveTextObserveWidget = SWTObservables.observeText(schema, SWT.Modify);
		IObservableValue configSchemaObserveValue = PojoObservables.observeValue(config, "schema");
		bindingContext.bindValue(schemaObserveTextObserveWidget, configSchemaObserveValue, null, null);
		//
		IObservableValue maxActiveObserveTextObserveWidget = SWTObservables.observeText(maxActive, SWT.Modify);
		IObservableValue configMaxActiveObserveValue = PojoObservables.observeValue(config, "maxActive");
		bindingContext.bindValue(maxActiveObserveTextObserveWidget, configMaxActiveObserveValue, null, null);
		//
		IObservableValue maxIdleObserveTextObserveWidget = SWTObservables.observeText(maxIdle, SWT.Modify);
		IObservableValue configMaxIdleObserveValue = PojoObservables.observeValue(config, "maxIdle");
		bindingContext.bindValue(maxIdleObserveTextObserveWidget, configMaxIdleObserveValue, null, null);
		//
		IObservableValue statementsIdleObserveTextObserveWidget = SWTObservables.observeText(statementsIdle, SWT.Modify);
		IObservableValue configMaxPreparedStatementsIdleObserveValue = PojoObservables.observeValue(config, "maxPreparedStatementsIdle");
		bindingContext.bindValue(statementsIdleObserveTextObserveWidget, configMaxPreparedStatementsIdleObserveValue, null, null);
		//
		IObservableValue skipObserveSelectionObserveWidget = SWTObservables.observeSelection(skip);
		IObservableValue configSkipSysTablesObserveValue = PojoObservables.observeValue(config, "skipSysTables");
		bindingContext.bindValue(skipObserveSelectionObserveWidget, configSkipSysTablesObserveValue, null, null);
		//
		IObservableValue passwordObserveTextObserveWidget = SWTObservables.observeText(password, SWT.Modify);
		IObservableValue configPasswordObserveValue = PojoObservables.observeValue(config, "password");
		bindingContext.bindValue(passwordObserveTextObserveWidget, configPasswordObserveValue, null, null);
		//
		return bindingContext;
	}
}
