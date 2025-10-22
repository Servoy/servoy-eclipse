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

import static com.servoy.j2db.persistence.ServerConfig.CONNECTION_DRIVER_VALIDATION;
import static com.servoy.j2db.persistence.ServerConfig.CONNECTION_EXCEPTION_VALIDATION;
import static com.servoy.j2db.persistence.ServerConfig.CONNECTION_METADATA_VALIDATION;
import static com.servoy.j2db.persistence.ServerConfig.CONNECTION_QUERY_VALIDATION;
import static com.servoy.j2db.persistence.ServerConfig.getConnectionValidationTypeAsString;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.databinding.observable.value.AbstractObservableValue;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.e4.ui.css.swt.CSSSWTConstants;
import org.eclipse.help.IContextProvider;
import org.eclipse.jface.databinding.swt.typed.WidgetProperties;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.forms.HyperlinkSettings;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.IShowInSource;
import org.eclipse.ui.part.ShowInContext;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.resource.ServerEditorInput;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.repository.DataModelManager;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.ViewPartHelpContextProvider;
import com.servoy.eclipse.ui.util.BindingHelper;
import com.servoy.eclipse.ui.util.DocumentValidatorVerifyListener;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.eclipse.ui.util.ImmutableObjectObservable;
import com.servoy.eclipse.ui.util.WrappingControl;
import com.servoy.eclipse.ui.util.XLControl;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.IServerConfigListener;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.IServerManagerInternal;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ServerConfig;
import com.servoy.j2db.persistence.ServerSettings;
import com.servoy.j2db.persistence.SortingNullprecedence;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.serverconfigtemplates.ServerTemplateDefinition;
import com.servoy.j2db.util.Settings;

public class ServerEditor extends EditorPart implements IShowInSource
{
	public ServerEditor()
	{
	}

	private DataBindingContext m_bindingContext;
	private boolean modified = false;
	private ImmutableObjectObservable<ServerConfig> serverConfigObservable;
	private ImmutableObjectObservable<ServerSettings> serverSettingsObservable;

	private Button enabledButton;
	private Button logServerButton;
	private Button proceduresButton;
	private Button sortIgnoreCaseButton;
	private Combo sortNullprecedenceField;
	private Button clientOnlyConnectionsButton;
	private Button prefixTablesButton;
	private Button createLogTableButton;
	private Button createClientstatsTableButton;
	private Button skipSysTablesButton;
	private Button saveButton;
	private Button testConnectionButton;
	private Text validationQueryField;
	private Combo validationTypeField;
	private Text maxPreparedStatementsIdleField;
	private Text maxIdleField;
	private Text logTableName;
	private Text maxActiveField;
	private Text idleTimoutField;
	private Combo dataModel_cloneFromField;
	private Combo schemaField;
	private Combo catalogField;
	private Combo driverField;
	private Text urlField;
	private Text passwordField;
	private Text userNameField;
	private Text serverNameField;

	private ServerTemplateDefinition serverTemplateDefinition;
	private final ArrayList<Label> urlPropertiesLabels = new ArrayList<Label>();
	private final ArrayList<Text> urlPropertiesFields = new ArrayList<Text>();

	private Label noDriverWarning;
	private FormText noDriverMessage;
	private Button addDriverButton;

	private String oldServerName = null;

	private IServerConfigListener logServerListener = null;

	private Composite mainComposite;
	private ScrolledComposite myScrolledComposite;
	private Composite advancedSettingsComposite;

	private Composite parentControl;

	@Override
	public void createPartControl(final Composite parent)
	{
		this.parentControl = parent;
		parentControl.setData(CSSSWTConstants.CSS_ID_KEY, "svyeditor");

		GridData tmpGD;

		Display display = getDisplay(parent);

		myScrolledComposite = new ScrolledComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL);
		myScrolledComposite.setShowFocusedControl(true);
		myScrolledComposite.setExpandHorizontal(true);
		myScrolledComposite.setExpandVertical(true);

		Composite bottomComposite = new Composite(parent, SWT.NONE);

		mainComposite = new Composite(myScrolledComposite, SWT.NONE);
		myScrolledComposite.setContent(mainComposite);

		String toolTip = "The (friendly) name that this DB server/connection will have in Servoy Developer.\nIt doesn't have to match the real database name (the one used by DB Server).";
		Label serverNameLabel = label(mainComposite, "Server name", toolTip);
		serverNameField = text(mainComposite, toolTip);

		ModifyListener ml = null;
		if (serverTemplateDefinition != null)
		{
			String[] urlKeys = serverTemplateDefinition.getUrlKeys();
			String[] urlKeyDescriptions = serverTemplateDefinition.getUrlKeyDescriptions();
			if (urlKeys != null)
			{
				ml = new ModifyListener()
				{
					@Override
					public void modifyText(ModifyEvent e)
					{
						String[] values = new String[urlPropertiesFields.size()];
						for (int z = 0; z < urlPropertiesFields.size(); z++)
						{
							values[z] = urlPropertiesFields.get(z).getText();
						}
						String newUrl = serverTemplateDefinition.getUrlForValues(values, serverConfigObservable.getObject().getServerUrl());
						if (newUrl != null && !newUrl.equals(urlField.getText()))
						{
							urlField.setText(newUrl);
						}
					}
				};
				String[] urlValues = serverTemplateDefinition.getUrlValues(serverConfigObservable.getObject().getServerUrl());

				for (int z = 0; z < urlKeys.length; z++)
				{
					toolTip = (urlKeyDescriptions != null && urlKeyDescriptions.length > z ? urlKeyDescriptions[z] : null);

					Label templateLabel = label(mainComposite, urlKeys[z], toolTip);
					urlPropertiesLabels.add(templateLabel);

					Text templateField = text(mainComposite, toolTip);
					if (urlValues != null && z < urlValues.length)
					{
						templateField.setText(urlValues[z]);
					}
					templateField.addModifyListener(ml);

					urlPropertiesFields.add(templateField);
				}
			}
		}

		toolTip = "User name for connecting to the database.";
		Label userNameLabel = label(mainComposite, "User name", toolTip);
		userNameField = text(mainComposite, toolTip);

		toolTip = "Password to use when connecting to the database.";
		Label passwordLabel = label(mainComposite, "Password", toolTip);
		passwordField = new Text(mainComposite, SWT.BORDER | SWT.PASSWORD);
		passwordField.setToolTipText(toolTip);

		saveButton = new Button(mainComposite, SWT.PUSH);
		saveButton.setText("Save");
		saveButton.setToolTipText(
			"You can also use CTRL+S (CMD+S) or main developer save button.\nThe connection will also be tested if 'enabled' is checked in advanced section.");
		saveButton.addSelectionListener(new SelectionAdapter()
		{

			@Override
			public void widgetSelected(SelectionEvent e)
			{
				doSave(null);
			}
		});

		testConnectionButton = new Button(mainComposite, SWT.PUSH);
		testConnectionButton.setToolTipText("Checks to see if a connection can be established to the server using this configuration.");
		testConnectionButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				checkConnection();
			}
		});

		IHyperlinkListener hyperLinkNativeOpenHandler = new IHyperlinkListener()
		{
			@Override
			public void linkExited(HyperlinkEvent e)
			{
				// nothing to do here
			}

			@Override
			public void linkEntered(HyperlinkEvent e)
			{
				// nothing to do here
			}

			@Override
			public void linkActivated(HyperlinkEvent e)
			{
				org.eclipse.swt.program.Program.launch((String)e.getHref());
			}
		};
		HyperlinkSettings hyperlinkSettings = new HyperlinkSettings(display);
		hyperlinkSettings.setHyperlinkUnderlineMode(HyperlinkSettings.UNDERLINE_HOVER);

		WrappingControl<Label> noDriverWarningWrapper = null;
		WrappingControl<FormText> noDriverMessageWrapper = null;

		if (!isExistingDriver(((ServerEditorInput)getEditorInput()).getServerConfig().getDriver()))
		{
			noDriverWarningWrapper = new WrappingControl<Label>(mainComposite, SWT.NONE);
			noDriverWarningWrapper.wrapControl(new Label(noDriverWarningWrapper, SWT.WRAP));
			noDriverWarning = noDriverWarningWrapper.getWrappedControl();
			noDriverWarning.setText("JDBC Driver is not installed for this database type.");
			noDriverWarning.setForeground(display.getSystemColor(SWT.COLOR_RED));

			// form text can handle links correctly
			noDriverMessageWrapper = new WrappingControl<FormText>(mainComposite, SWT.NONE);
			noDriverMessageWrapper.wrapControl(new FormText(noDriverMessageWrapper, SWT.NONE));
			noDriverMessage = noDriverMessageWrapper.getWrappedControl();
			StringBuffer msg = new StringBuffer("<form><p>Please download a driver for this type of database (\"");
			msg.append(((ServerEditorInput)getEditorInput()).getServerConfig().getDriver()).append(
				"\") and use the \"Install (downloaded) driver\" button bellow to install it. You can also install the driver manually by copying it to Servoy Application Server's \"drivers\" directory and then restarting Servoy Developer.</p>");

			if (serverTemplateDefinition != null && serverTemplateDefinition.getDriverDownloadURL() != null)
			{
				msg.append("\n<p>You can download a driver from: ").append(serverTemplateDefinition.getDriverDownloadURL()).append("</p>");
			}
			msg.append("</form>");

			noDriverMessage.setHyperlinkSettings(hyperlinkSettings);

			noDriverMessage.setText(msg.toString(), true, true);
			noDriverMessage.addHyperlinkListener(hyperLinkNativeOpenHandler);

			addDriverButton = new Button(mainComposite, SWT.PUSH);
			addDriverButton.setText("Install (downloaded) driver");
			addDriverButton.setToolTipText(
				"Once you have the needed JDBC Driver (.jar) you can install it into Servoy Developer. It will be copied to the drivers directory and loaded for usage.");
			addDriverButton.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(SelectionEvent e)
				{
					FileDialog fileOpenDlg = new FileDialog(getDisplay(parent).getActiveShell(), SWT.OPEN | SWT.MULTI);
					fileOpenDlg.setText("Select database driver files to import");
					if (fileOpenDlg.open() != null)
					{
						File driversDir = new File(ApplicationServerRegistry.get().getServoyApplicationServerDirectory(), "drivers");
						try
						{
							for (String f : fileOpenDlg.getFileNames())
							{
								FileUtils.copyFileToDirectory(new File(fileOpenDlg.getFilterPath(), f), driversDir);
							}

							ApplicationServerRegistry.get().getServerManager().loadInstalledDrivers();
							ServerEditor.this.getEditorSite().getPage().closeEditor(ServerEditor.this, false);
							EditorUtil.openServerEditor(serverConfigObservable.getObject(), serverSettingsObservable.getObject(), true);
						}
						catch (IOException ex)
						{
							MessageDialog.openError(getDisplay(parent).getActiveShell(), "Install (downloaded) driver",
								"Error during copy of database driver files to Servoy");
							ServoyLog.logError(ex);
						}
					}
				}
			});
		}

		Composite advancedSettingsCollapserComposite = new Composite(mainComposite, SWT.NONE);

		ExpandableComposite excomposite = new ExpandableComposite(advancedSettingsCollapserComposite, SWT.NONE,
			ExpandableComposite.TWISTIE | ExpandableComposite.CLIENT_INDENT);
		excomposite.setText("Advanced server settings");
		excomposite.setExpanded(false);
		excomposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		excomposite.setData(CSSSWTConstants.CSS_ID_KEY, "svyeditor");
		excomposite.addExpansionListener(new ExpansionAdapter()
		{
			@Override
			public void expansionStateChanged(ExpansionEvent e)
			{
				relayout();
			}
		});

		advancedSettingsComposite = new Composite(excomposite, SWT.NONE);
		advancedSettingsComposite.setData(CSSSWTConstants.CSS_ID_KEY, "svyeditor");
		ImageHyperlink image = new ImageHyperlink(excomposite, SWT.None);
		image.setImage(Activator.getDefault().loadImageFromBundle("advanced_serverproperties.png"));
		excomposite.setClient(advancedSettingsComposite);
		excomposite.setTextClient(image);
		Label urlLabel = label(advancedSettingsComposite, "URL", ServerTemplateDefinition.JDBC_URL_DESCRIPTION);
		urlField = text(advancedSettingsComposite, ServerTemplateDefinition.JDBC_URL_DESCRIPTION);

		final ModifyListener finalML = ml;
		urlField.addModifyListener(new ModifyListener()
		{
			public void modifyText(ModifyEvent e)
			{
				if (serverTemplateDefinition != null)
				{
					String[] urlValues = serverTemplateDefinition.getUrlValues(urlField.getText());
					if (urlValues != null && urlValues.length == urlPropertiesFields.size())
					{
						for (int i = 0; i < urlPropertiesFields.size(); i++)
						{
							if (!urlPropertiesFields.get(i).getText().equals(urlValues[i]))
							{
								if (finalML != null) urlPropertiesFields.get(i).removeModifyListener(finalML);
								urlPropertiesFields.get(i).setText(urlValues[i]);
								if (finalML != null) urlPropertiesFields.get(i).addModifyListener(finalML);
							}

						}
					}
				}
			}
		});

		toolTip = "JDBC driver to use. Each DB type has a different driver. For some DB types there are multiple drivers available.\nThis is the name of a driver class that is located in the driver directory's jar files.";
		Label driverLabel = label(advancedSettingsComposite, "Driver", toolTip);
		driverField = dropdown(advancedSettingsComposite, toolTip);
		driverField.addModifyListener(
			e -> driverField.setForeground(getDisplay(parent).getSystemColor(isExistingDriver(driverField.getText()) ? SWT.COLOR_BLACK : SWT.COLOR_RED)));
		driverField.setForeground(
			display.getSystemColor(isExistingDriver(((ServerEditorInput)getEditorInput()).getServerConfig().getDriver()) ? SWT.COLOR_BLACK : SWT.COLOR_RED));

		toolTip = "The specific catalog to connect to; not all databases support this option.";
		Label catalogLabel = label(advancedSettingsComposite, "Catalog", toolTip);
		catalogField = dropdown(advancedSettingsComposite, toolTip, ServerConfig.NONE, ServerConfig.EMPTY);

		toolTip = "The specific schema to connect to; not all databases support this option.";
		Label schemaLabel = label(advancedSettingsComposite, "Schema", toolTip);
		schemaField = dropdown(advancedSettingsComposite, toolTip, ServerConfig.NONE, ServerConfig.EMPTY);

		toolTip = "Determines the maximum number of connections that will be made to the database simultaneously.";
		Label maxActiveLabel = label(advancedSettingsComposite, "Max Active Connections", toolTip);
		maxActiveField = text(advancedSettingsComposite, toolTip);

		toolTip = "Determines the maximum number of unused connections that are in the pool.";
		Label maxIdleLabel = label(advancedSettingsComposite, "Max Idle Connections", toolTip);
		maxIdleField = text(advancedSettingsComposite, toolTip);

		toolTip = "Idle connections from the connection pool will be disposed of if they are not used for this given amount of time (minutes).";
		Label idleTimoutLabel = label(advancedSettingsComposite, "Connection Idle Timeout", toolTip);
		idleTimoutField = text(advancedSettingsComposite, toolTip);

		toolTip = "All Servoy generated SQL statements are in the form of Prepared statements, to increase the performance of statement execution.\nThis setting determines how many prepared statements are kept in cache.";
		Label maxPreparedStatementsIdleLabel = label(advancedSettingsComposite, "Max Idle Prepared Statements", toolTip);
		maxPreparedStatementsIdleField = text(advancedSettingsComposite, toolTip);

		toolTip = "Specifies a way to determine if a DB idle connection leased from the connection pool is still valid or not.\n\n" + "\"" +
			getConnectionValidationTypeAsString(CONNECTION_EXCEPTION_VALIDATION) +
			"\" - will consider a connection invalid if it's getException() returns non-null.\n" + "\"" +
			getConnectionValidationTypeAsString(CONNECTION_METADATA_VALIDATION) +
			"\" - will consider a connection invalid if fetching database metadata fails.\n" + "\"" +
			getConnectionValidationTypeAsString(CONNECTION_QUERY_VALIDATION) +
			"\" - will consider a connection valid as long as it is able to run the given query scucessfully.\n" + "\"" +
			getConnectionValidationTypeAsString(CONNECTION_DRIVER_VALIDATION) +
			"\" - use the connection validation mechanism of the JDBC driver.";

		Label validationTypeLabel = label(advancedSettingsComposite, "Connection Validation Type", toolTip);
		validationTypeField = dropdown(advancedSettingsComposite, SWT.READ_ONLY, toolTip,
			getConnectionValidationTypeAsString(CONNECTION_EXCEPTION_VALIDATION),
			getConnectionValidationTypeAsString(CONNECTION_METADATA_VALIDATION),
			getConnectionValidationTypeAsString(CONNECTION_QUERY_VALIDATION),
			getConnectionValidationTypeAsString(CONNECTION_DRIVER_VALIDATION));

		toolTip = "If validation type is set to \"" + getConnectionValidationTypeAsString(CONNECTION_QUERY_VALIDATION) +
			"\", then this is the query that must run successfully in order for the connection to be considered valid.";
		Label validationQueryLabel = label(advancedSettingsComposite, "Connection Validation Query", toolTip);
		validationQueryField = text(advancedSettingsComposite, toolTip);

		toolTip = "This setting allows marking a Database Server as a clone of another Database Server.\nWhen marked as such, if a Solution is imported on the Servoy Application Server, any updates to the datamodel of the master Database Server are also applied to the Database Servers that are marked as a clone of the master Database Server.";
		Label dataModel_cloneFromLabel = label(advancedSettingsComposite, "Data model clone from", toolTip);
		dataModel_cloneFromField = dropdown(advancedSettingsComposite, SWT.READ_ONLY, toolTip);

		Label enabledLabel = label(advancedSettingsComposite, "Enabled", "Enabled");
		enabledButton = new Button(advancedSettingsComposite, SWT.CHECK);
		enabledButton.addSelectionListener(new SelectionListener()
		{

			@Override
			public void widgetSelected(SelectionEvent e)
			{
				widgetDefaultSelected(e);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e)
			{
				updateTestConnectionButton();
				relayoutAsync(getDisplay(parent));
			}

		});

		toolTip = "Specifies whether or not System Tables and Views from the database are to be exposed in Servoy.";
		Label skipSysTablesLabel = label(advancedSettingsComposite, "Skip System Tables", toolTip);
		skipSysTablesButton = checkbox(advancedSettingsComposite, toolTip);

		toolTip = "When tables are defined in multiple schemas, with this option set to true, Servoy will prefix the table in the sql when needed.";
		label(advancedSettingsComposite, "Prefix Tables", toolTip);
		prefixTablesButton = checkbox(advancedSettingsComposite, toolTip);

		toolTip = "Servoy has functionality that allows to automatically track all insert/updates/deletes on tables.\nThis functionality can be enabled through the Security layer inside the Solution.\nThis functionality relies on one of the enabled Database Servers configured on the Servoy Application Server being marked at 'Log server'.";
		Label logServerLabel = label(advancedSettingsComposite, "Log Server", toolTip);
		logServerButton = checkbox(advancedSettingsComposite, toolTip);

		toolTip = "Specifies a way to determine if a DB idle connection leased from the connection pool is still valid or not.\n\n" + "\"" +
			getConnectionValidationTypeAsString(CONNECTION_EXCEPTION_VALIDATION) +
			"\" - will consider a connection invalid if it's getException() returns non-null.\n" + "\"" +
			getConnectionValidationTypeAsString(CONNECTION_METADATA_VALIDATION) +
			"\" - will consider a connection invalid if fetching database metadata fails.\n" + "\"" +
			getConnectionValidationTypeAsString(CONNECTION_QUERY_VALIDATION) +
			"\" - will consider a connection valid as long as it is able to run the given query scucessfully.\n" + "\"" +
			getConnectionValidationTypeAsString(CONNECTION_DRIVER_VALIDATION) +
			"\" - use the connection validation mechanism of the JDBC driver.";

		ApplicationServerRegistry.get().getServerManager().addServerConfigListener(logServerListener = new EnableServerListener());

		Composite buttonsComposite = new Composite(advancedSettingsComposite, SWT.NONE);

		toolTip = "Log table name, default is \"log\"";
		Label logTableLabel = label(buttonsComposite, "Log Table Name", toolTip);
		logTableName = text(buttonsComposite, toolTip);

		createLogTableButton = new Button(buttonsComposite, SWT.PUSH);
		createLogTableButton.setText("Create Log Table");
		createLogTableButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				IServerInternal logServer = (IServerInternal)ApplicationServerRegistry.get().getServerManager().getLogServer();
				if (logServer == null)
				{
					MessageDialog.openError(getDisplay(parent).getActiveShell(), "Log server not found",
						"Required server '" + ApplicationServerRegistry.get().getServerManager().getLogServerName() + "' not found or cannot be reached.");
					return;
				}

				try
				{
					ApplicationServerRegistry.get().getServerManager().setLogTableName(logTableName.getText());
					Table logTable = logServer.getLogTable();
					if (logTable == null)
					{
						logTable = logServer.createLogTable();
						MessageDialog.openInformation(getDisplay(parent).getActiveShell(), "Table log created",
							"Table log successfully created in '" + ApplicationServerRegistry.get().getServerManager().getLogServerName() + "'.");
					}
					else
					{
						MessageDialog.openInformation(getDisplay(parent).getActiveShell(), "Table already exists",
							"Log table already exists in '" + ApplicationServerRegistry.get().getServerManager().getLogServerName() + "'.");
					}
					createLogTableButton.setEnabled(logTable == null);
				}
				catch (RepositoryException re)
				{
					ServoyLog.logError(re);
					MessageDialog.openError(getDisplay(parent).getActiveShell(), "Error creating table", "Could not create log table: " + re.getMessage());
				}
				catch (Exception err)
				{
					ServoyLog.logError(err);
					MessageDialog.openError(getDisplay(parent).getActiveShell(), "Error creating table",
						"Unexpected error while creating log table. Check the log for more details.");
				}
			}
		});

		createClientstatsTableButton = new Button(buttonsComposite, SWT.PUSH);
		createClientstatsTableButton.setText("Create Client Statistics Table");
		createClientstatsTableButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				IServerInternal logServer = (IServerInternal)ApplicationServerRegistry.get().getServerManager().getLogServer();
				if (logServer == null)
				{
					MessageDialog.openError(getDisplay(parent).getActiveShell(), "Log server not found",
						"Required server '" + ApplicationServerRegistry.get().getServerManager().getLogServerName() + "' not found or cannot be reached.");
					return;
				}

				try
				{
					Table statsTable = logServer.getClientStatsTable();
					if (statsTable == null)
					{
						statsTable = logServer.createClientStatsTable();
						MessageDialog.openInformation(getDisplay(parent).getActiveShell(), "Table client_stats created",
							"Table client_stats successfully created in '" + ApplicationServerRegistry.get().getServerManager().getLogServerName() + "'.");
					}
					else
					{
						MessageDialog.openInformation(getDisplay(parent).getActiveShell(), "Table already exists",
							"Client statistics table already exists in '" + ApplicationServerRegistry.get().getServerManager().getLogServerName() + "'.");
					}
					createClientstatsTableButton.setEnabled(statsTable != null);
				}
				catch (RepositoryException re)
				{
					ServoyLog.logError(re);
					MessageDialog.openError(getDisplay(parent).getActiveShell(), "Error creating table",
						"Could not create client statistics table: " + re.getMessage());
				}
				catch (Exception err)
				{
					ServoyLog.logError(err);
					MessageDialog.openError(getDisplay(parent).getActiveShell(), "Error creating table",
						"Unexpected error while creating client statistics table. Check the log for more details.");
				}
			}
		});


		toolTip = "Enabling this will enable querying for stored procedures on the database and exposing that under datasources.sp.servername";
		label(advancedSettingsComposite, "Enable procedures", toolTip);
		proceduresButton = checkbox(advancedSettingsComposite, toolTip);

		toolTip = "Enabling this will set this server to only have client defined connections (datasources.db.server.defineDatasource()). This tries to postpone also the loading of the tables (only do that with a client connection)";
		label(advancedSettingsComposite, "Client Only Connections", toolTip);
		clientOnlyConnectionsButton = checkbox(advancedSettingsComposite, toolTip);

		toolTip = "Options for ignoring case when sorting, this can be overridden at column level (in the table editor)";
		label(advancedSettingsComposite, "Sort ignoring case", toolTip);
		sortIgnoreCaseButton = checkbox(advancedSettingsComposite, toolTip);

		toolTip = "Options for setting sorting of null values, this can be overridden at column level (in the table editor)";
		label(advancedSettingsComposite, "Sorting null-precedence", toolTip);
		sortNullprecedenceField = dropdown(advancedSettingsComposite, SWT.READ_ONLY, toolTip,
			SortingNullprecedence.databaseDefault.display(),
			SortingNullprecedence.ascNullsFirst.display(),
			SortingNullprecedence.ascNullsLast.display());

		XLControl<FormText> wikiLinkWrapper = new XLControl<FormText>(bottomComposite, SWT.NONE);
		wikiLinkWrapper.wrapControl(new FormText(wikiLinkWrapper, SWT.NONE));
		wikiLinkWrapper.setExtraWidth(10); // workaround for a SWT-native bug on Mac where the FormText reports less preferred width then it actually needs and then when painting it wraps
		FormText wikiLink = wikiLinkWrapper.getWrappedControl();
		wikiLink.setHyperlinkSettings(hyperlinkSettings);
		wikiLink.setText("<form><p>See <a href='https://wiki.servoy.com/display/DOCS/Database+Connections'>wiki page</a> for more information...</p></form>",
			true, true);
		wikiLink.addHyperlinkListener(hyperLinkNativeOpenHandler);

		enableButtons();

		// now do the main composite layout
		GridLayout gridLayout = new GridLayout(1, false);
		gridLayout.marginWidth = gridLayout.marginHeight = 10;
		gridLayout.verticalSpacing = 6;
		gridLayout.horizontalSpacing = 5;
		parent.setLayout(gridLayout);

		myScrolledComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));


		bottomComposite.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, false, false));

		gridLayout = new GridLayout(4, false);
		gridLayout.marginWidth = gridLayout.marginHeight = 10;
		gridLayout.verticalSpacing = 6;
		gridLayout.horizontalSpacing = 10;
		mainComposite.setLayout(gridLayout);

		// simple part of editor layout setup follows
		serverNameLabel.setLayoutData(oneColumn());
		serverNameField.setLayoutData(threeColumns());

		for (int i = 0; i < urlPropertiesLabels.size(); i += 2)
		{
			urlPropertiesLabels.get(i).setLayoutData(oneColumn());
			urlPropertiesFields.get(i).setLayoutData(oneColumnFill());
			if (i + 1 < urlPropertiesLabels.size())
			{
				urlPropertiesLabels.get(i + 1).setLayoutData(oneColumn());
				urlPropertiesFields.get(i + 1).setLayoutData(oneColumnFill());
			}
			else
			{
				urlPropertiesFields.get(i).setLayoutData(threeColumns());
			}
		}

		userNameLabel.setLayoutData(oneColumn());
		userNameField.setLayoutData(oneColumnFill());

		passwordLabel.setLayoutData(oneColumn());
		passwordField.setLayoutData(oneColumnFill());

		tmpGD = new GridData(SWT.FILL, SWT.CENTER, false, false);
		tmpGD.verticalIndent = 15;
		saveButton.setLayoutData(tmpGD);
		tmpGD = new GridData(SWT.BEGINNING, SWT.CENTER, true, false, 3, 1);
		tmpGD.verticalIndent = 15;
		testConnectionButton.setLayoutData(tmpGD);

		// layout missing driver if necessary
		if (noDriverMessage != null)
		{
			tmpGD = fourColumns();
			tmpGD.verticalIndent = 20;
			noDriverWarningWrapper.setLayoutData(tmpGD);
			noDriverMessageWrapper.setLayoutData(fourColumns());
			addDriverButton.setLayoutData(fourColumns());
		}

		// layout advanced settings

		gridLayout = new GridLayout(1, false);
		gridLayout.marginWidth = gridLayout.marginHeight = 0;
		gridLayout.verticalSpacing = 0;
		gridLayout.horizontalSpacing = 0;
		advancedSettingsCollapserComposite.setLayout(gridLayout);

		advancedSettingsCollapserComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 4, 1));

		gridLayout = new GridLayout(4, false);
		gridLayout.marginRight = 0;
		gridLayout.marginLeft = 20;
		gridLayout.marginTop = 10;
		gridLayout.marginBottom = 5;
		gridLayout.verticalSpacing = 6;
		gridLayout.horizontalSpacing = 10;
		advancedSettingsComposite.setLayout(gridLayout);

		urlLabel.setLayoutData(oneColumn());
		urlField.setLayoutData(threeColumns());

		driverLabel.setLayoutData(oneColumn());
		driverField.setLayoutData(threeColumns());

		catalogLabel.setLayoutData(oneColumn());
		catalogField.setLayoutData(oneColumnFill());

		schemaLabel.setLayoutData(oneColumn());
		schemaField.setLayoutData(oneColumnFill());

		maxActiveLabel.setLayoutData(oneColumn());
		maxActiveField.setLayoutData(oneColumnFill());

		maxIdleLabel.setLayoutData(oneColumn());
		maxIdleField.setLayoutData(oneColumnFill());

		idleTimoutLabel.setLayoutData(oneColumn());
		idleTimoutField.setLayoutData(oneColumnFill());

		maxPreparedStatementsIdleLabel.setLayoutData(oneColumn());
		maxPreparedStatementsIdleField.setLayoutData(oneColumnFill());

		tmpGD = oneColumn();
		tmpGD.verticalIndent = 10;
		validationTypeLabel.setLayoutData(tmpGD);

		tmpGD = oneColumnFill();
		tmpGD.verticalIndent = 10;
		validationTypeField.setLayoutData(tmpGD);

		tmpGD = oneColumn();
		tmpGD.verticalIndent = 10;
		validationQueryLabel.setLayoutData(tmpGD);

		tmpGD = oneColumnFill();
		tmpGD.verticalIndent = 10;
		validationQueryField.setLayoutData(tmpGD);

		dataModel_cloneFromLabel.setLayoutData(oneColumn());
		dataModel_cloneFromField.setLayoutData(threeColumns());

		enabledLabel.setLayoutData(oneColumn());
		enabledButton.setLayoutData(oneColumn());

		skipSysTablesLabel.setLayoutData(oneColumn());
		skipSysTablesButton.setLayoutData(oneColumn());

		logServerLabel.setLayoutData(oneColumn());
		logServerButton.setLayoutData(oneColumn());

		buttonsComposite.setLayoutData(twoColumns());

		gridLayout = new GridLayout(4, false);
		gridLayout.marginWidth = gridLayout.marginHeight = 0;
		gridLayout.horizontalSpacing = 10;
		buttonsComposite.setLayout(gridLayout);

		logTableLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		logTableName.setLayoutData(oneColumnFill());
		createLogTableButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		createClientstatsTableButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

		gridLayout = new GridLayout(1, false);
		gridLayout.marginWidth = gridLayout.marginHeight = 0;
		gridLayout.horizontalSpacing = 5;
		bottomComposite.setLayout(gridLayout);

		wikiLinkWrapper.setLayoutData(new GridData(SWT.END, SWT.CENTER, true, false));

		initComboData();
		initDataBindings();
		updateSaveButton();

		logTableName.addKeyListener(new KeyAdapter()
		{
			@Override
			public void keyPressed(KeyEvent e)
			{
				flagModified();
			}
		});
		myScrolledComposite.setMinSize(mainComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT));

	}

	private static Label label(Composite parent, String text, String toolTip)
	{
		Label label = new Label(parent, SWT.LEFT);
		label.setText(text);
		label.setToolTipText(toolTip);
		return label;
	}

	private static Text text(Composite parent, String toolTip)
	{
		Text text = new Text(parent, SWT.BORDER);
		text.setToolTipText(toolTip);
		return text;
	}

	private Button checkbox(Composite parent, String toolTip)
	{
		Button checkbox = new Button(parent, SWT.CHECK);
		checkbox.setToolTipText(toolTip);
		checkbox.addListener(SWT.Selection, event -> flagModified());
		return checkbox;
	}

	private static Combo dropdown(Composite parent, String toolTip, String... items)
	{
		return dropdown(parent, SWT.NONE, toolTip, items);
	}

	private static Combo dropdown(Composite parent, int style, String toolTip, String... items)
	{
		Combo combo = new Combo(parent, SWT.BORDER | style);
		combo.setToolTipText(toolTip);
		combo.setVisibleItemCount(UIUtils.COMBO_VISIBLE_ITEM_COUNT);
		stream(items).forEach(combo::add);
		return combo;
	}

	/**
	 * @param parent can be null.
	 */
	protected Display getDisplay(final Composite parent)
	{
		Display display = parent != null ? parent.getDisplay() : null;
		if (display == null)
		{
			if (display == null)
			{
				display = getSite().getShell().getDisplay();
				if (display == null)
				{
					display = Display.getCurrent();
					if (display == null) display = Display.getDefault();
				}
			}
		}
		return display;
	}

	protected void relayoutAsync(Display display)
	{
		runLaterInDisplayThread(display, 0, new Runnable()
		{
			public void run()
			{
				relayout();
			}
		});
	}

	protected void runLaterInDisplayThread(Display display, int afterMillis, Runnable r)
	{
		if (afterMillis <= 0) display.asyncExec(r);
		else display.timerExec(afterMillis, r);
	}

	protected void relayout()
	{
		myScrolledComposite.setMinSize(mainComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		parentControl.layout(true, true);
	}

	private static GridData oneColumnFill()
	{
		return width(new GridData(SWT.FILL, SWT.CENTER, true, false));
	}

	private static GridData oneColumn()
	{
		return new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
	}

	private static GridData twoColumns()
	{
		return width(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
	}

	private static GridData threeColumns()
	{
		return width(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
	}

	private static GridData fourColumns()
	{
		return new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1);
	}

	private static GridData width(GridData gd)
	{
		gd.minimumWidth = 100;
		return gd;
	}


	@Override
	public void dispose()
	{
		super.dispose();
		m_bindingContext.dispose();
		ApplicationServerRegistry.get().getServerManager().removeServerConfigListener(logServerListener);
	}

	/**
	 * @see org.eclipse.ui.part.EditorPart#init(org.eclipse.ui.IEditorSite, org.eclipse.ui.IEditorInput)
	 */
	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException
	{
		setSite(site);
		setInput(convertInput(input));
	}

	/**
	 * @see org.eclipse.ui.part.EditorPart#setInput(org.eclipse.ui.IEditorInput)
	 */
	@Override
	protected void setInput(IEditorInput input)
	{
		super.setInput(input);

		ServerEditorInput serverInput = (ServerEditorInput)input;

		ServerConfig serverConfig = serverInput.getServerConfig();
		ServerSettings serverSettings = serverInput.getServerSettings();
		oldServerName = serverConfig.getServerName();

		for (ServerTemplateDefinition templateDefinition : ServerConfig.TEMPLATES.values())
		{
			if (templateDefinition.getTemplate().getDriver().equals(serverConfig.getDriver()))
			{
				serverTemplateDefinition = templateDefinition;
				break;
			}
		}

		serverConfigObservable = new ImmutableObjectObservable<ServerConfig>(serverConfig, new Class[] { //
			String.class, String.class, String.class, String.class, Map.class, //
			String.class, String.class, String.class, int.class, int.class, //
			int.class, int.class, String.class, String.class, boolean.class, //
			boolean.class, boolean.class, boolean.class, int.class, Integer.class, //
			String.class, List.class, boolean.class, String.class //
		}, new String[] { //
			"serverName", "userName", "password", "serverUrl", "connectionProperties", //
			"driver", "catalog", "schema", "maxActive", "maxIdle", //
			"maxPreparedStatementsIdle", "connectionValidationType", "validationQuery", "dataModelCloneFrom", "enabled", //
			"skipSysTables", "prefixTables", "queryProceduresNOTUSED", "idleTimeout", "selectINValueCountLimit", //
			"dialectClass", "quoteList", "clientOnlyConnectionsNOTUSED", "initializationString" },
			new String[] { "queryProceduresNOTUSED", "clientOnlyConnectionsNOTUSED" });

		serverSettingsObservable = new ImmutableObjectObservable<ServerSettings>(serverSettings,
			new Class[] { boolean.class, SortingNullprecedence.class, Boolean.class, Boolean.class
			}, new String[] { "sortIgnorecase", "sortingNullprecedence", "queryProcedures", "clientOnlyConnections" });

		if (serverInput.getIsNew())
		{
			flagModified();
		}

		updateTitle();
	}

	private IEditorInput convertInput(IEditorInput input)
	{
		if (input instanceof FileEditorInput fileEditorInput)
		{
			int dbiExtensionIndex = fileEditorInput.getFile().getName().lastIndexOf(DataModelManager.COLUMN_INFO_FILE_EXTENSION_WITH_DOT);
			if (dbiExtensionIndex != -1)
			{
				// this is a column info file; get the server config for the server name part of the file name
				String serverName = fileEditorInput.getFile().getName().substring(0, dbiExtensionIndex);
				IServerManagerInternal serverManager = ApplicationServerRegistry.get().getServerManager();
				ServerConfig serverConfig = serverManager.getServerConfig(serverName);
				if (serverConfig == null)
				{
					return input; // cannot convert
				}
				return new ServerEditorInput(serverConfig, serverManager.getServerSettings(serverName));
			}
		}
		return input;
	}

	protected void updateTitle()
	{
		setPartName(serverConfigObservable.getObject().getServerName());
		setTitleToolTip(serverConfigObservable.getObject().getServerName());
	}

	protected void checkConnection()
	{
		ServerConfig serverConfig = serverConfigObservable.getObject();

		if (serverConfig.isEnabled())
		{
			try
			{
				IServerManagerInternal serverManager = ApplicationServerRegistry.get().getServerManager();
				serverManager.testServerConfigConnection(serverConfig, 0); //test if we connect
				MessageDialog.openInformation(getSite().getShell(), "Connection successful", "Test connection to DB server was established successfully.");
			}
			catch (Exception e)
			{
				MessageDialog.openError(getSite().getShell(), "Connection to DB server could not be established using this configuration", e.getMessage());
			}
		} // else should never happen as the test connection button should be disabled in that case
	}

	@Override
	public void doSave(IProgressMonitor monitor)
	{
		try
		{
			IServerManagerInternal serverManager = ApplicationServerRegistry.get().getServerManager();
			ServerConfig serverConfig = serverConfigObservable.getObject();
			ServerSettings serverSettings = serverSettingsObservable.getObject();
			String currentServerName = serverConfig.getServerName();
			String dataModelCloneFrom = null;
			String oldURL = null;
			ServerConfig oldConfig = serverManager.getServerConfig(oldServerName);
			if (oldConfig != null)
			{
				dataModelCloneFrom = oldConfig.getDataModelCloneFrom();
				oldURL = oldConfig.getServerUrl();
			}
			boolean log_server = logServerButton.getSelection();

			if (serverConfig.isOracleDriver() && (serverConfig.getSchema() == null || serverConfig.getSchema().trim().length() == 0))
			{
				// if you do not specify the schema in oracle you see thousands of non-useful system tables/views in that server
				if (MessageDialog.openConfirm(getSite().getShell(), "Fill Oracle schema",
					"Schema should be filled for Oracle servers, mostly the same as the user name. Should we fill it in? \n\nNot specifying a schema will probably result in seing lots of system tables/views in this server, not just user tables/views."))
				{
					//serverConfigObservable.setPropertyValue("schema", serverConfig.getUserName());
					schemaField.setText(serverConfig.getUserName());
				}
			}

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
					serverManager.setLogTableName(logTableName.getText());
				}
				else
				{
					if (serverManager.getLogServerName().equals(currentServerName)) serverManager.setLogServerName("");
				}
				settings.save();
			}

			serverManager.saveServerConfig(oldServerName, serverConfig);
			serverManager.saveServerSettings(serverConfig.getServerName(), serverSettings);

			//"refresh" the log table creation button
			enableButtons();

			modified = false;
			firePropertyChange(IEditorPart.PROP_DIRTY);
			updateTitle();
			setInput(new ServerEditorInput(serverConfig, serverSettings));
			initDataBindings();
			if (serverConfig.getDataModelCloneFrom() != null && !Utils.equalObject(dataModelCloneFrom, serverConfig.getDataModelCloneFrom()))
			{
				DataModelManager dataModelManager = ServoyModelManager.getServoyModelManager().getServoyModel().getDataModelManager();
				if (dataModelManager != null &&
					MessageDialog.openQuestion(getSite().getShell(), "Copy files", "Server '" + currentServerName + "' was marked as clone of '" +
						serverConfig.getDataModelCloneFrom() + "'. Do you want to copy(overwrite) all table related files from parent server?"))
				{
					IFolder sourceFolder = dataModelManager.getServerInformationFolder(serverConfig.getDataModelCloneFrom());
					IFolder cloneFolder = dataModelManager.getServerInformationFolder(currentServerName);
					if (cloneFolder.exists())
					{
						cloneFolder.delete(true, null);
					}
					if (sourceFolder.exists())
					{
						sourceFolder.copy(cloneFolder.getFullPath(), true, null);
					}
				}
			}
			if (oldURL != null && !oldURL.equals(serverConfig.getServerUrl()))
			{
				if (MessageDialog.openQuestion(getSite().getShell(), "Database Server URL changed",
					"It is strongly recommended to restart your Servoy Developer. Would you like to restart now?"))
				{
					PlatformUI.getWorkbench().restart();
				}
				else
				{
					IServer server = serverManager.getServer(currentServerName);
					if (server instanceof IServerInternal && server.isValid())
					{
						((IServerInternal)server).reloadTables();
					}
				}
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError("Cannot save server", e);
			MessageDialog.openError(getSite().getShell(), "Cannot save server", e.getMessage());
		}

		// show in solution explorer view so that new users notice that something has happened
		try
		{
			IWorkbench workbench = PlatformUI.getWorkbench();
			ICommandService commandService = workbench.getService(ICommandService.class);
			IHandlerService handlerService = workbench.getService(IHandlerService.class);
			Command showInCommand = commandService.getCommand(IWorkbenchCommandConstants.NAVIGATE_SHOW_IN);
			handlerService.executeCommand(ParameterizedCommand.generateCommand(showInCommand,
				Collections.singletonMap(IWorkbenchCommandConstants.NAVIGATE_SHOW_IN_PARM_TARGET, SolutionExplorerView.PART_ID)), null);
		}
		catch (Exception e)
		{
			ServoyLog.logError("Cannot expand server node in SolEx after save", e);
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
		stream(ApplicationServerRegistry.get().getServerManager().getKnownDriverClassNames()).forEach(driverField::add);
		driverField.add("sun.jdbc.odbc.JdbcOdbcDriver");

		maxActiveField.addVerifyListener(DocumentValidatorVerifyListener.NUMBER_VERIFIER);
		maxIdleField.addVerifyListener(DocumentValidatorVerifyListener.NUMBER_VERIFIER);
		idleTimoutField.addVerifyListener(DocumentValidatorVerifyListener.NUMBER_VERIFIER);
		maxPreparedStatementsIdleField.addVerifyListener(DocumentValidatorVerifyListener.NUMBER_VERIFIER);

		dataModel_cloneFromField.removeAll();
		dataModel_cloneFromField.add(ServerConfig.NONE);
		String serverName = serverConfigObservable.getObject().getServerName();
		String cloneServerName;
		for (ServerConfig sc : ApplicationServerRegistry.get().getServerManager().getServerConfigs())
		{
			cloneServerName = sc.getServerName();
			if (cloneServerName != null && !cloneServerName.equals(serverName)) dataModel_cloneFromField.add(cloneServerName);
		}
	}

	protected void initDataBindings()
	{
		m_bindingContext = BindingHelper.dispose(m_bindingContext);

		IObservableValue getServerNameObserveValue = serverConfigObservable.observePropertyValue("serverName");
		IObservableValue serverNameTextObserveWidget = WidgetProperties.text(SWT.Modify).observe(serverNameField);
		IObservableValue getUserNameObserveValue = serverConfigObservable.observePropertyValue("userName");
		IObservableValue userNameTextObserveWidget = WidgetProperties.text(SWT.Modify).observe(userNameField);
		IObservableValue getPasswordObserveValue = serverConfigObservable.observePropertyValue("password");
		IObservableValue passwordTextObserveWidget = WidgetProperties.text(SWT.Modify).observe(passwordField);
		IObservableValue getDataModel_cloneFromObserveValue = new AbstractObservableValue()
		{
			@Override
			protected Object doGetValue()
			{
				String dataModelCloneFrom = serverConfigObservable.getObject().getDataModelCloneFrom();
				if (asList(dataModel_cloneFromField.getItems()).contains(dataModelCloneFrom)) return dataModelCloneFrom;
				return ServerConfig.NONE;
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
		IObservableValue dataModel_cloneFromTextObserveWidget = WidgetProperties.widgetSelection().observe(dataModel_cloneFromField);
		IObservableValue getUrlObserveValue = serverConfigObservable.observePropertyValue("serverUrl");
		IObservableValue urlTextObserveWidget = WidgetProperties.text(SWT.Modify).observe(urlField);
		IObservableValue getDriverObserveValue = serverConfigObservable.observePropertyValue("driver");
		IObservableValue driverTextObserveWidget = WidgetProperties.widgetSelection().observe(driverField);
		IObservableValue catalogSelectionObserveWidget = WidgetProperties.widgetSelection().observe(catalogField);
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
		IObservableValue schemaSelectionObserveWidget = WidgetProperties.widgetSelection().observe(schemaField);
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

		IObservableValue getMaxActiveObserveValue = serverConfigObservable.observePropertyValue("maxActive");
		IObservableValue maxActiveTextObserveWidget = WidgetProperties.text(SWT.Modify).observe(maxActiveField);
		IObservableValue getMaxIdleObserveValue = serverConfigObservable.observePropertyValue("maxIdle");
		IObservableValue maxIdleTextObserveWidget = WidgetProperties.text(SWT.Modify).observe(maxIdleField);
		IObservableValue getIdleTimeoutObserveValue = serverConfigObservable.observePropertyValue("idleTimeout");
		IObservableValue idleTimeoutTextObserveWidget = WidgetProperties.text(SWT.Modify).observe(idleTimoutField);
		IObservableValue getMaxPreparedStatementsIdleObserveValue = serverConfigObservable.observePropertyValue("maxPreparedStatementsIdle");
		IObservableValue maxPreparedStatementsIdleTextObserveWidget = WidgetProperties.text(SWT.Modify).observe(maxPreparedStatementsIdleField);
		IObservableValue getValidationTypeObserveValue = new AbstractObservableValue()
		{
			public Object getValueType()
			{
				return null;
			}

			@Override
			protected Object doGetValue()
			{
				return getConnectionValidationTypeAsString(serverConfigObservable.getObject().getConnectionValidationType());
			}

			@Override
			protected void doSetValue(Object value)
			{
				String validationType = value.toString();
				int type = ServerConfig.VALIDATION_TYPE_DEFAULT;
				if (getConnectionValidationTypeAsString(CONNECTION_EXCEPTION_VALIDATION).equals(validationType))
				{
					type = CONNECTION_EXCEPTION_VALIDATION;
					validationQueryField.setEnabled(false);
				}
				else if (getConnectionValidationTypeAsString(CONNECTION_METADATA_VALIDATION).equals(validationType))
				{
					type = CONNECTION_METADATA_VALIDATION;
					validationQueryField.setEnabled(false);
				}
				else if (getConnectionValidationTypeAsString(CONNECTION_QUERY_VALIDATION).equals(validationType))
				{
					type = CONNECTION_QUERY_VALIDATION;
					validationQueryField.setEnabled(true);
				}
				else if (getConnectionValidationTypeAsString(CONNECTION_DRIVER_VALIDATION).equals(validationType))
				{
					type = CONNECTION_DRIVER_VALIDATION;
					validationQueryField.setEnabled(false);
				}
				serverConfigObservable.setPropertyValue("connectionValidationType", new Integer(type));
			}
		};

		IObservableValue getSortingNullprecedenceObserveValue = new AbstractObservableValue()
		{
			public Object getValueType()
			{
				return null;
			}

			@Override
			protected Object doGetValue()
			{
				return serverSettingsObservable.getObject().getSortingNullprecedence().display();
			}

			@Override
			protected void doSetValue(Object value)
			{
				SortingNullprecedence.fromDisplay(value.toString()).ifPresent(snp -> serverSettingsObservable.setPropertyValue("sortingNullprecedence", snp));
			}
		};

		IObservableValue validationTypeSelectionObserveWidget = WidgetProperties.widgetSelection().observe(validationTypeField);
		IObservableValue getValidationQueryObserveValue = serverConfigObservable.observePropertyValue("validationQuery");
		IObservableValue validationQueryTextObserveWidget = WidgetProperties.text(SWT.Modify).observe(validationQueryField);
		IObservableValue getEnabledObserveValue = serverConfigObservable.observePropertyValue("enabled");
		IObservableValue enabledSelectionObserveWidget = WidgetProperties.widgetSelection().observe(enabledButton);
		IObservableValue getSkipSysTablesObserveValue = serverConfigObservable.observePropertyValue("skipSysTables");
		IObservableValue skipSysTablesSelectionObserveWidget = WidgetProperties.widgetSelection().observe(skipSysTablesButton);
		IObservableValue proceduresButtonObserveValue = serverSettingsObservable.observePropertyValue("queryProcedures");
		IObservableValue proceduresButtonSelectionObserveWidget = WidgetProperties.widgetSelection().observe(proceduresButton);
		IObservableValue clientOnlyConnectionsButtonObserveValue = serverSettingsObservable.observePropertyValue("clientOnlyConnections");
		IObservableValue clientOnlyConnectionsSelectionObserveWidget = WidgetProperties.widgetSelection().observe(clientOnlyConnectionsButton);
		IObservableValue prefixTablesButtonObserveValue = serverConfigObservable.observePropertyValue("prefixTables");
		IObservableValue prefixTablesButtonSelectionObserveWidget = WidgetProperties.widgetSelection().observe(prefixTablesButton);

		IObservableValue sortIgnoreCaseButtonObserveValue = serverSettingsObservable.observePropertyValue("sortIgnorecase");
		IObservableValue sortIgnoreCaseButtonSelectionObserveWidget = WidgetProperties.widgetSelection().observe(sortIgnoreCaseButton);
		IObservableValue sortNullprecedenceSelectionObserveWidget = WidgetProperties.widgetSelection().observe(sortNullprecedenceField);

		m_bindingContext = new DataBindingContext();
		m_bindingContext.bindValue(serverNameTextObserveWidget, getServerNameObserveValue, null, null);
		m_bindingContext.bindValue(userNameTextObserveWidget, getUserNameObserveValue, null, null);
		m_bindingContext.bindValue(passwordTextObserveWidget, getPasswordObserveValue, null, null);
		m_bindingContext.bindValue(urlTextObserveWidget, getUrlObserveValue, null, null);
		m_bindingContext.bindValue(driverTextObserveWidget, getDriverObserveValue, null, null);
		m_bindingContext.bindValue(catalogSelectionObserveWidget, getCatalogObserveValue, null, null);
		m_bindingContext.bindValue(schemaSelectionObserveWidget, getSchemaObserveValue, null, null);
		m_bindingContext.bindValue(maxActiveTextObserveWidget, getMaxActiveObserveValue, null, null);
		m_bindingContext.bindValue(idleTimeoutTextObserveWidget, getIdleTimeoutObserveValue, null, null);
		m_bindingContext.bindValue(maxIdleTextObserveWidget, getMaxIdleObserveValue, null, null);
		m_bindingContext.bindValue(maxPreparedStatementsIdleTextObserveWidget, getMaxPreparedStatementsIdleObserveValue, null, null);
		m_bindingContext.bindValue(validationTypeSelectionObserveWidget, getValidationTypeObserveValue, null, null);
		m_bindingContext.bindValue(validationQueryTextObserveWidget, getValidationQueryObserveValue, null, null);
		m_bindingContext.bindValue(dataModel_cloneFromTextObserveWidget, getDataModel_cloneFromObserveValue, null, null);
		m_bindingContext.bindValue(enabledSelectionObserveWidget, getEnabledObserveValue, null, null);
		m_bindingContext.bindValue(skipSysTablesSelectionObserveWidget, getSkipSysTablesObserveValue, null, null);
		m_bindingContext.bindValue(proceduresButtonSelectionObserveWidget, proceduresButtonObserveValue, null, null);
		m_bindingContext.bindValue(clientOnlyConnectionsSelectionObserveWidget, clientOnlyConnectionsButtonObserveValue, null, null);
		m_bindingContext.bindValue(prefixTablesButtonSelectionObserveWidget, prefixTablesButtonObserveValue, null, null);

		m_bindingContext.bindValue(sortIgnoreCaseButtonSelectionObserveWidget, sortIgnoreCaseButtonObserveValue, null, null);
		m_bindingContext.bindValue(sortNullprecedenceSelectionObserveWidget, getSortingNullprecedenceObserveValue, null, null);

		BindingHelper.addGlobalChangeListener(m_bindingContext, new IChangeListener()
		{
			public void handleChange(ChangeEvent event)
			{
				flagModified();
			}
		});
		validationQueryField.setEnabled(serverConfigObservable.getObject().getConnectionValidationType() == CONNECTION_QUERY_VALIDATION);

		logServerButton
			.setSelection(serverConfigObservable.getObject().getServerName().equals(ApplicationServerRegistry.get().getServerManager().getLogServerName()));

		logTableName.setText(ApplicationServerRegistry.get().getServerManager().getLogTableName());

		updateTestConnectionButton();
	}

	public void flagModified()
	{
		modified = true;
		this.getSite().getShell().getDisplay().asyncExec(() -> firePropertyChange(IEditorPart.PROP_DIRTY));
	}

	@Override
	protected void firePropertyChange(int propertyId)
	{
		super.firePropertyChange(propertyId);

		if (propertyId == IEditorPart.PROP_DIRTY)
		{
			updateSaveButton();
		}
	}

	@Override
	public Object getAdapter(Class adapter)
	{
		if (ServerConfig.class.equals(adapter))
		{
			return serverConfigObservable.getObject();
		}
		if (adapter.equals(IContextProvider.class))
		{
			return new ViewPartHelpContextProvider("com.servoy.eclipse.ui.server_editor");
		}
		return super.getAdapter(adapter);
	}

	@Override
	public void setFocus()
	{
		serverNameField.setFocus();
		//in case log table is deleted but the current log server remains the same
		enableButtons();
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

	private boolean isExistingDriver(String driver)
	{
		boolean existing = false;
		for (String name : ApplicationServerRegistry.get().getServerManager().getAllDriverClassNames())
		{
			if (name.equals(driver))
			{
				existing = true;
				break;
			}
		}
		return existing;
	}

	protected void updateSaveButton()
	{
		saveButton.setEnabled(isDirty());
	}

	protected void updateTestConnectionButton()
	{
		final String testConnectionTextEnabled = "Test connection";
		final String testConnectionTextDisabled = "Test connection (n/a for disabled servers)";

		if (enabledButton.getSelection())
		{
			testConnectionButton.setText(testConnectionTextEnabled);
			testConnectionButton.setEnabled(true);
		}
		else
		{
			testConnectionButton.setText(testConnectionTextDisabled);
			testConnectionButton.setEnabled(false);
		}
	}

	private void enableButtons()
	{

		if (serverConfigObservable.getObject().getServerName().equals(ApplicationServerRegistry.get().getServerManager().getLogServerName()))
		{
			logTableName.setEnabled(true);
			if (ApplicationServerRegistry.get().getServerManager().logTableExists())
			{
				createLogTableButton.setEnabled(false);
				// FIXME: show tooltips for disabled button
				createLogTableButton
					.setToolTipText("Log table already exists in '" + ApplicationServerRegistry.get().getServerManager().getLogServerName() + "'.");
			}
			else
			{
				createLogTableButton.setEnabled(true);
				createLogTableButton.setToolTipText(
					"Create a log table for tracking; " + "the creation of such a table is possible only if the current database server is the log server " +
						"and if it does not already contain a log table.");
			}
			if (ApplicationServerRegistry.get().getServerManager().clientStatsTableExists())
			{
				createClientstatsTableButton.setEnabled(false);
				// FIXME: show tooltips for disabled button
				createClientstatsTableButton.setToolTipText(
					"Client statistics table already exists in '" + ApplicationServerRegistry.get().getServerManager().getLogServerName() + "'.");
			}
			else
			{
				createClientstatsTableButton.setEnabled(true);
				createClientstatsTableButton.setToolTipText("Create a client statistics table for logging (un)registering of clients; " +
					"the creation of such a table is possible only if the current database server is the log server " +
					"and if it does not already contain a client_stats table.");
			}
		}
		else
		{
			createLogTableButton.setEnabled(false);
			createClientstatsTableButton.setEnabled(false);
			logTableName.setEnabled(false);
		}
	}

	class EnableServerListener implements IServerConfigListener
	{
		public void serverConfigurationChanged(ServerConfig oldServerConfig, ServerConfig newServerConfig)
		{
			ServerEditorInput currentServerEditorInput = (ServerEditorInput)getEditorInput();
			boolean isUpdateForDifferenServer = oldServerConfig != null && currentServerEditorInput != null &&
				oldServerConfig.getServerName() != currentServerEditorInput.getServerConfig().getServerName();

			// if it is an update for a diff server or it is a delete (newServerConfig == null) or a new one (oldServerConfig == null), skip ui update
			if (isUpdateForDifferenServer || newServerConfig == null || oldServerConfig == null) return;

			setInput(new ServerEditorInput(newServerConfig, currentServerEditorInput.getServerSettings()));
			initDataBindings();
			relayout();
			if (serverConfigObservable.getObject().getServerName().equals(ApplicationServerRegistry.get().getServerManager().getLogServerName()))
			{
				logServerButton.setSelection(true);
			}
			else
			{
				logServerButton.setSelection(false);
			}
		}
	}

	@Override
	public ShowInContext getShowInContext()
	{
		return new ShowInContext(getEditorInput(), null);
	}
}
