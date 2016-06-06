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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.databinding.observable.value.AbstractObservableValue;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ExpandEvent;
import org.eclipse.swt.events.ExpandListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.ExpandBar;
import org.eclipse.swt.widgets.ExpandItem;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.EditorPart;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.resource.ServerEditorInput;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.repository.DataModelManager;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.util.BindingHelper;
import com.servoy.eclipse.ui.util.DocumentValidatorVerifyListener;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.eclipse.ui.util.ExpandBarWidthAware;
import com.servoy.eclipse.ui.util.ImmutableObjectObservable;
import com.servoy.j2db.persistence.IServerConfigListener;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.IServerManagerInternal;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ServerConfig;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.serverconfigtemplates.ServerTemplateDefinition;
import com.servoy.j2db.util.Settings;

public class ServerEditor extends EditorPart
{
	public ServerEditor()
	{
	}

	private DataBindingContext m_bindingContext;
	private boolean modified = false;
	private ImmutableObjectObservable<ServerConfig> serverConfigObservable;

	private Button enabledButton;
	private Button logServerButton;
	private Button createLogTableButton;
	private Button createClientstatsTableButton;
	private Button skipSysTablesButton;
	private Text validationQueryField;
	private Combo validationTypeField;
	private Text maxPreparedStatementsIdleField;
	private Text maxIdleField;
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
	private Text noDriverMessage;
	private Button addDriverButton;

	private String oldServerName = null;

	private IServerConfigListener logServerListener = null;

	private Composite mainComposite;
	private ExpandItem collapsableItem;
	private ScrolledComposite myScrolledComposite;
	private Composite advancedSettingsComposite;

	@Override
	public void createPartControl(final Composite parent)
	{
		myScrolledComposite = new ScrolledComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL);
		myScrolledComposite.setShowFocusedControl(true);
		myScrolledComposite.setExpandHorizontal(true);
		myScrolledComposite.setExpandVertical(true);

		mainComposite = new Composite(myScrolledComposite, SWT.NONE);
		myScrolledComposite.setContent(mainComposite);

		Label serverNameLabel;
		serverNameLabel = new Label(mainComposite, SWT.LEFT);
		serverNameLabel.setText("Server name");

		serverNameField = new Text(mainComposite, SWT.BORDER);

		if (serverTemplateDefinition != null)
		{
			String[] urlKeys = serverTemplateDefinition.getUrlKeys();
			if (urlKeys != null)
			{
				ModifyListener ml = new ModifyListener()
				{
					@Override
					public void modifyText(ModifyEvent e)
					{
						String[] values = new String[urlPropertiesFields.size()];
						for (int z = 0; z < urlPropertiesFields.size(); z++)
						{
							values[z] = urlPropertiesFields.get(z).getText();
						}
						String newUrl = serverTemplateDefinition.getUrlForValues(values);
						if (newUrl != null)
						{
							urlField.setText(newUrl);
						}
					}
				};
				String[] urlValues = serverTemplateDefinition.getUrlValues(serverConfigObservable.getObject().getServerUrl());

				for (int z = 0; z < urlKeys.length; z++)
				{
					Label templateLabel;
					templateLabel = new Label(mainComposite, SWT.LEFT);
					templateLabel.setText(urlKeys[z]);
					urlPropertiesLabels.add(templateLabel);

					Text templateField = new Text(mainComposite, SWT.BORDER);
					if (urlValues != null && z < urlValues.length)
					{
						templateField.setText(urlValues[z]);
					}
					templateField.addModifyListener(ml);

					urlPropertiesFields.add(templateField);
				}
			}
		}

		Label userNameLabel;
		userNameLabel = new Label(mainComposite, SWT.LEFT);
		userNameLabel.setText("User name");

		userNameField = new Text(mainComposite, SWT.BORDER);

		Label passwordLabel;
		passwordLabel = new Label(mainComposite, SWT.LEFT);
		passwordLabel.setText("Password");

		passwordField = new Text(mainComposite, SWT.BORDER | SWT.PASSWORD);

		if (!isExistingDriver(((ServerEditorInput)getEditorInput()).getServerConfig().getDriver()))
		{
			noDriverWarning = new Label(mainComposite, SWT.WRAP)
			{
				@Override
				public Point computeSize(int wHint, int hHint, boolean changed)
				{
					// this label doesn't care to take up any width space when asked what it wants
					Point superComputedSize = super.computeSize(wHint, hHint, changed);
					Point adjustedComputedSize;
					if (wHint == SWT.DEFAULT || hHint == SWT.DEFAULT)
					{
						adjustedComputedSize = new Point(0, superComputedSize.y); // workaround to make a label inside a ScrolledComposite with setExpandHorizontal(true) be able to wrap to the width of the other components in that composite
					}
					else adjustedComputedSize = superComputedSize;
					return adjustedComputedSize;
				}

				@Override
				protected void checkSubclass()
				{
					// remove subclass protection; TODO use a wrapper composite instead of subclassing
				}
			};
			noDriverWarning.setText(
				"No driver installed for this database typeNo driver installed for this database typeNo driver installed for this database typeNo driver installed for this database typeNo driver installed for this database typeNo driver installed for this database typeNo driver installed for this database typeNo driver installed for this database typeNo driver installed for this database type");
			noDriverWarning.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));

			noDriverMessage = new Text(mainComposite, SWT.LEFT | SWT.MULTI | SWT.WRAP);
//			noDriverMessage.setBackground(mainComposite.getBackground());
//			StringBuffer msg = new StringBuffer("Please download a driver for this type of database (\"");
//			msg.append(((ServerEditorInput)getEditorInput()).getServerConfig().getDriver()).append(
//				"\") and use the \"Install (downloaded) driver\" button bellow to install it. You can also install the driver manually by copying it to the Servoy application server's \"drivers\" directory and then restarting Servoy Developer.");
//
//			if (serverTemplateDefinition.getDriverDownloadURL() != null)
//			{
//				msg.append("\n\nYou can download a driver from: ").append(serverTemplateDefinition.getDriverDownloadURL());
//			}
//
//			noDriverMessage.setText(msg.toString());

			addDriverButton = new Button(mainComposite, SWT.PUSH);
			addDriverButton.setText("Install (downloaded) driver");
			addDriverButton.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(SelectionEvent e)
				{
					FileDialog fileOpenDlg = new FileDialog(Display.getDefault().getActiveShell(), SWT.OPEN | SWT.MULTI);
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

							ServoyModel.getServerManager().loadInstalledDrivers();
							ServerEditor.this.getEditorSite().getPage().closeEditor(ServerEditor.this, false);
							EditorUtil.openServerEditor(serverConfigObservable.getObject(), true);
						}
						catch (IOException ex)
						{
							MessageDialog.openError(Display.getDefault().getActiveShell(), "Install (downloaded) driver",
								"Error during copy of database driver files to Servoy");
							ServoyLog.logError(ex);
						}
					}
				}
			});
		}

		Composite advancedSettingsCollapserComposite = new Composite(mainComposite, SWT.NONE);

		Label separator1 = new Label(advancedSettingsCollapserComposite, SWT.SEPARATOR | SWT.HORIZONTAL);
		ExpandBar advancedSettingsCollapser = new ExpandBarWidthAware(advancedSettingsCollapserComposite, SWT.V_SCROLL);
		Label separator2 = new Label(advancedSettingsCollapserComposite, SWT.SEPARATOR | SWT.HORIZONTAL);

		collapsableItem = new ExpandItem(advancedSettingsCollapser, SWT.NONE, 0);
		collapsableItem.setText("Show advanced server settings");

		advancedSettingsCollapser.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
		advancedSettingsCollapser.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_DARK_BLUE));

		advancedSettingsComposite = new Composite(advancedSettingsCollapser, SWT.NONE);
		collapsableItem.setImage(Activator.getDefault().loadImageFromBundle("outline_co.gif"));

		advancedSettingsCollapser.addExpandListener(new ExpandListener()
		{
			public void itemExpanded(ExpandEvent e)
			{
				collapsableItem.setText("Hide advanced server settings");
				relayout();
			}

			public void itemCollapsed(ExpandEvent e)
			{
				collapsableItem.setText("Show advanced server settings");
				relayout();
			}

		});

		// partial fix for a Linux (Ubuntu) bug (that I think has to do with ExpandBar's implementation)
		advancedSettingsComposite.addControlListener(new ControlAdapter()
		{
			@Override
			public void controlResized(ControlEvent e)
			{
				relayout();
			}
		});

		Label urlLabel;
		urlLabel = new Label(advancedSettingsComposite, SWT.LEFT);
		urlLabel.setText("URL");

		urlField = new Text(advancedSettingsComposite, SWT.BORDER);

		Label driverLabel;
		driverLabel = new Label(advancedSettingsComposite, SWT.LEFT);
		driverLabel.setText("Driver");

		driverField = new Combo(advancedSettingsComposite, SWT.BORDER);
		UIUtils.setDefaultVisibleItemCount(driverField);
		driverField.addModifyListener(new ModifyListener()
		{
			public void modifyText(ModifyEvent e)
			{
				driverField.setForeground(Display.getCurrent().getSystemColor(isExistingDriver(driverField.getText()) ? SWT.COLOR_BLACK : SWT.COLOR_RED));
			}
		});
		driverField.setForeground(Display.getCurrent().getSystemColor(
			isExistingDriver(((ServerEditorInput)getEditorInput()).getServerConfig().getDriver()) ? SWT.COLOR_BLACK : SWT.COLOR_RED));

		Label catalogLabel;
		catalogLabel = new Label(advancedSettingsComposite, SWT.LEFT);
		catalogLabel.setText("Catalog");

		catalogField = new Combo(advancedSettingsComposite, SWT.BORDER);
		UIUtils.setDefaultVisibleItemCount(catalogField);

		Label schemaLabel;
		schemaLabel = new Label(advancedSettingsComposite, SWT.LEFT);
		schemaLabel.setText("Schema");

		schemaField = new Combo(advancedSettingsComposite, SWT.BORDER);
		UIUtils.setDefaultVisibleItemCount(schemaField);

		Label maxActiveLabel;
		maxActiveLabel = new Label(advancedSettingsComposite, SWT.LEFT);
		maxActiveLabel.setText("Max Active Connections");

		maxActiveField = new Text(advancedSettingsComposite, SWT.BORDER);

		Label maxIdleLabel;
		maxIdleLabel = new Label(advancedSettingsComposite, SWT.LEFT);
		maxIdleLabel.setText("Max Idle Connections");

		maxIdleField = new Text(advancedSettingsComposite, SWT.BORDER);

		Label idleTimoutLabel;
		idleTimoutLabel = new Label(advancedSettingsComposite, SWT.LEFT);
		idleTimoutLabel.setText("Connection Idle Timeout");

		idleTimoutField = new Text(advancedSettingsComposite, SWT.BORDER);

		Label maxPreparedStatementsIdleLabel;
		maxPreparedStatementsIdleLabel = new Label(advancedSettingsComposite, SWT.LEFT);
		maxPreparedStatementsIdleLabel.setText("Max Idle Prepared Statements");

		maxPreparedStatementsIdleField = new Text(advancedSettingsComposite, SWT.BORDER);

		Label separator4 = new Label(advancedSettingsComposite, SWT.SEPARATOR | SWT.HORIZONTAL);

		Label validationTypeLabel;
		validationTypeLabel = new Label(advancedSettingsComposite, SWT.LEFT);
		validationTypeLabel.setText("Connection Validation Type");

		validationTypeField = new Combo(advancedSettingsComposite, SWT.BORDER | SWT.READ_ONLY);
		UIUtils.setDefaultVisibleItemCount(validationTypeField);

		Label validationQueryLabel;
		validationQueryLabel = new Label(advancedSettingsComposite, SWT.LEFT);
		validationQueryLabel.setText("Connection Validation Query");

		validationQueryField = new Text(advancedSettingsComposite, SWT.BORDER);

		Label dataModel_cloneFromLabel;
		dataModel_cloneFromLabel = new Label(advancedSettingsComposite, SWT.LEFT);
		dataModel_cloneFromLabel.setText("Data model clone from");

		dataModel_cloneFromField = new Combo(advancedSettingsComposite, SWT.BORDER | SWT.READ_ONLY);
		UIUtils.setDefaultVisibleItemCount(dataModel_cloneFromField);

		Label enabledLabel;
		enabledLabel = new Label(advancedSettingsComposite, SWT.LEFT);
		enabledLabel.setText("Enabled");

		enabledButton = new Button(advancedSettingsComposite, SWT.CHECK);

		Label skipSysTablesLabel;
		skipSysTablesLabel = new Label(advancedSettingsComposite, SWT.LEFT);
		skipSysTablesLabel.setText("Skip System Tables");

		skipSysTablesButton = new Button(advancedSettingsComposite, SWT.CHECK);

		Label logServerLabel;
		logServerLabel = new Label(advancedSettingsComposite, SWT.LEFT);
		logServerLabel.setText("Log Server");

		logServerButton = new Button(advancedSettingsComposite, SWT.CHECK);
		logServerButton.addListener(SWT.Selection, new Listener()
		{
			public void handleEvent(Event event)
			{
				flagModified();
			}
		});

		ServoyModel.getServerManager().addServerConfigListener(logServerListener = new LogServerListener());

		Composite buttonsComposite = new Composite(advancedSettingsComposite, SWT.NONE);

		createLogTableButton = new Button(buttonsComposite, SWT.PUSH);
		createLogTableButton.setText("Create Log Table");
		createLogTableButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				IServerInternal logServer = (IServerInternal)ServoyModel.getServerManager().getLogServer();
				if (logServer == null)
				{
					MessageDialog.openError(Display.getDefault().getActiveShell(), "Log server not found",
						"Required server '" + ServoyModel.getServerManager().getLogServerName() + "' not found or cannot be reached.");
					return;
				}

				try
				{
					Table logTable = logServer.getLogTable();
					if (logTable == null)
					{
						logTable = logServer.createLogTable();
						MessageDialog.openInformation(Display.getDefault().getActiveShell(), "Table log created",
							"Table log successfully created in '" + ServoyModel.getServerManager().getLogServerName() + "'.");
					}
					else
					{
						MessageDialog.openInformation(Display.getDefault().getActiveShell(), "Table already exists",
							"Log table already exists in '" + ServoyModel.getServerManager().getLogServerName() + "'.");
					}
					createLogTableButton.setEnabled(logTable != null);
				}
				catch (RepositoryException re)
				{
					ServoyLog.logError(re);
					MessageDialog.openError(Display.getDefault().getActiveShell(), "Error creating table", "Could not create log table: " + re.getMessage());
				}
				catch (Exception err)
				{
					ServoyLog.logError(err);
					MessageDialog.openError(Display.getDefault().getActiveShell(), "Error creating table",
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
				IServerInternal logServer = (IServerInternal)ServoyModel.getServerManager().getLogServer();
				if (logServer == null)
				{
					MessageDialog.openError(Display.getDefault().getActiveShell(), "Log server not found",
						"Required server '" + ServoyModel.getServerManager().getLogServerName() + "' not found or cannot be reached.");
					return;
				}

				try
				{
					Table statsTable = logServer.getClientStatsTable();
					if (statsTable == null)
					{
						statsTable = logServer.createClientStatsTable();
						MessageDialog.openInformation(Display.getDefault().getActiveShell(), "Table client_stats created",
							"Table client_stats successfully created in '" + ServoyModel.getServerManager().getLogServerName() + "'.");
					}
					else
					{
						MessageDialog.openInformation(Display.getDefault().getActiveShell(), "Table already exists",
							"Client statistics table already exists in '" + ServoyModel.getServerManager().getLogServerName() + "'.");
					}
					createClientstatsTableButton.setEnabled(statsTable != null);
				}
				catch (RepositoryException re)
				{
					ServoyLog.logError(re);
					MessageDialog.openError(Display.getDefault().getActiveShell(), "Error creating table",
						"Could not create client statistics table: " + re.getMessage());
				}
				catch (Exception err)
				{
					ServoyLog.logError(err);
					MessageDialog.openError(Display.getDefault().getActiveShell(), "Error creating table",
						"Unexpected error while creating client statistics table. Check the log for more details.");
				}
			}
		});
		enableButtons();

		// now do the main composite layout
		GridLayout gridLayout = new GridLayout(4, false);
		gridLayout.marginWidth = gridLayout.marginHeight = 10;
		gridLayout.verticalSpacing = 6;
		gridLayout.horizontalSpacing = 10;
		mainComposite.setLayout(gridLayout);

		// simple part of editor layout setup follows
		serverNameLabel.setLayoutData(col1GD());
		serverNameField.setLayoutData(col234GD());

		for (int i = 0; i < urlPropertiesLabels.size(); i += 2)
		{
			urlPropertiesLabels.get(i).setLayoutData(col1GD());
			urlPropertiesFields.get(i).setLayoutData(col2GD());
			if (i + 1 < urlPropertiesLabels.size())
			{
				urlPropertiesLabels.get(i + 1).setLayoutData(col3GD());
				urlPropertiesFields.get(i + 1).setLayoutData(col4GD());
			}
			else
			{
				urlPropertiesFields.get(i).setLayoutData(col234GD());
			}
		}

		userNameLabel.setLayoutData(col1GD());
		userNameField.setLayoutData(col2GD());

		passwordLabel.setLayoutData(col3GD());
		passwordField.setLayoutData(col4GD());

		// layout missing driver if necessary
		if (noDriverMessage != null)
		{
			noDriverWarning.setLayoutData(col1234GD());
			noDriverMessage.setLayoutData(col1234GD());
			addDriverButton.setLayoutData(col1234GD());
		}

		// layout advanced settings

		gridLayout = new GridLayout(1, false);
		gridLayout.marginWidth = gridLayout.marginHeight = 0;
		gridLayout.verticalSpacing = 0;
		gridLayout.horizontalSpacing = 0;
		advancedSettingsCollapserComposite.setLayout(gridLayout);

		advancedSettingsCollapserComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 4, 1));
		GridData tmpGD = new GridData(SWT.FILL, SWT.BOTTOM, true, false);
		tmpGD.verticalIndent = 30;
		separator1.setLayoutData(tmpGD);
		advancedSettingsCollapser.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		separator2.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		gridLayout = new GridLayout(4, false);
		gridLayout.marginRight = 0;
		gridLayout.marginLeft = 20;
		gridLayout.marginTop = 10;
		gridLayout.marginBottom = 0;
		gridLayout.verticalSpacing = 6;
		gridLayout.horizontalSpacing = 10;
		advancedSettingsComposite.setLayout(gridLayout);

		urlLabel.setLayoutData(col1GD());
		urlField.setLayoutData(col234GD());

		driverLabel.setLayoutData(col1GD());
		driverField.setLayoutData(col234GD());

		catalogLabel.setLayoutData(col1GD());
		catalogField.setLayoutData(col2GD());

		schemaLabel.setLayoutData(col3GD());
		schemaField.setLayoutData(col4GD());

		maxActiveLabel.setLayoutData(col1GD());
		maxActiveField.setLayoutData(col2GD());

		maxIdleLabel.setLayoutData(col3GD());
		maxIdleField.setLayoutData(col4GD());

		idleTimoutLabel.setLayoutData(col1GD());
		idleTimoutField.setLayoutData(col2GD());

		maxPreparedStatementsIdleLabel.setLayoutData(col3GD());
		maxPreparedStatementsIdleField.setLayoutData(col4GD());

		tmpGD = new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1);
		tmpGD.verticalIndent = 10;
		separator4.setLayoutData(tmpGD);

		tmpGD = col1GD();
		tmpGD.verticalIndent = 10;
		validationTypeLabel.setLayoutData(tmpGD);

		tmpGD = col2GD();
		tmpGD.verticalIndent = 10;
		validationTypeField.setLayoutData(tmpGD);

		tmpGD = col3GD();
		tmpGD.verticalIndent = 10;
		validationQueryLabel.setLayoutData(tmpGD);

		tmpGD = col4GD();
		tmpGD.verticalIndent = 10;
		validationQueryField.setLayoutData(tmpGD);

		dataModel_cloneFromLabel.setLayoutData(col1GD());
		dataModel_cloneFromField.setLayoutData(col234GD());

		enabledLabel.setLayoutData(col1GD());
		enabledButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));

		skipSysTablesLabel.setLayoutData(col3GD());
		skipSysTablesButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));

		logServerLabel.setLayoutData(col1GD());
		logServerButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));

		buttonsComposite.setLayoutData(col34GD());

		gridLayout = new GridLayout(2, false);
		gridLayout.marginWidth = gridLayout.marginHeight = 0;
		gridLayout.horizontalSpacing = 10;
		buttonsComposite.setLayout(gridLayout);

		createLogTableButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		createClientstatsTableButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		collapsableItem.setHeight(advancedSettingsComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
		collapsableItem.setControl(advancedSettingsComposite);

		myScrolledComposite.setMinSize(mainComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT));

		initComboData();
		initDataBindings();
	}

	protected void relayout()
	{
		getSite().getShell().getDisplay().asyncExec(new Runnable()
		{
			public void run()
			{
				collapsableItem.setHeight(advancedSettingsComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
				myScrolledComposite.setMinSize(mainComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
				mainComposite.layout(true, true);
			}
		});
	}

	// declare some grid-datas creators to be reused per column for easier tuning
	protected GridData col1GD()
	{
		return new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
	}

	protected GridData col2GD()
	{
		GridData col2GD = new GridData(SWT.FILL, SWT.CENTER, true, false);
		col2GD.minimumWidth = 100;
		return col2GD;
	}

	protected GridData col3GD()
	{
		return new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
	}

	protected GridData col4GD()
	{
		GridData col4GD = new GridData(SWT.FILL, SWT.CENTER, true, false);
		col4GD.minimumWidth = 100;
		return col4GD;
	}

	protected GridData col234GD()
	{
		GridData col234GD = new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1);
		col234GD.minimumWidth = 100;
		return col234GD;
	}

	protected GridData col34GD()
	{
		GridData col1234GD = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
		col1234GD.minimumWidth = 100;
		return col1234GD;
	}

	protected GridData col1234GD()
	{
		GridData col1234GD = new GridData(SWT.FILL, SWT.TOP, true, false, 4, 1);
//		col1234GD.minimumWidth = 100;
		return col1234GD;
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
		ServerConfig serverConfig = inputConfig == null ? ServerConfig.TEMPLATES.get(ServerConfig.EMPTY_TEMPLATE_NAME).getTemplate() : inputConfig;
		oldServerName = inputConfig == null ? null : inputConfig.getServerName();


		for (ServerTemplateDefinition templateDefinition : ServerConfig.TEMPLATES.values())
		{
			if (templateDefinition.getTemplate().getDriver().equals(serverConfig.getDriver()))
			{
				serverTemplateDefinition = templateDefinition;
				break;
			}
		}

		serverConfigObservable = new ImmutableObjectObservable<ServerConfig>(serverConfig,
			new Class[] { String.class, String.class, String.class, String.class, Map.class, String.class, String.class, String.class, int.class, int.class, int.class, int.class, String.class, String.class, boolean.class, boolean.class, int.class, String.class },
			new String[] { "serverName", "userName", "password", "serverUrl", "connectionProperties", "driver", "catalog", "schema", "maxActive", "maxIdle", "maxPreparedStatementsIdle", "connectionValidationType", "validationQuery", "dataModelCloneFrom", "enabled", "skipSysTables", "idleTimeout", "dialectClass" });

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
			IServerManagerInternal serverManager = ServoyModel.getServerManager();
			ServerConfig serverConfig = serverConfigObservable.getObject();
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
					if (serverManager.getLogServerName().equals(currentServerName)) serverManager.setLogServerName("");
				}
				settings.save();
			}

			serverManager.saveServerConfig(oldServerName, serverConfig);

			//"refresh" the log table creation button
			enableButtons();

			modified = false;
			firePropertyChange(IEditorPart.PROP_DIRTY);
			updateTitle();
			setInput(new ServerEditorInput(serverConfig));
			initDataBindings();
			if (serverConfig.isOracleDriver() && (serverConfig.getSchema() == null || serverConfig.getSchema().trim().length() == 0))
			{
				// if you do not specify the schema in oracle you see thousands of non-useful system tables/views in that server
				MessageDialog.openInformation(getSite().getShell(), "Oracle server",
					"You should add a 'schema' for Oracle servers = the Oracle user name.\n\nNot specifying a schema will probably result in seing lots of system tables/views in this server, not just user tables/views.");
			}
			if (serverConfig.getDataModelCloneFrom() != null && !Utils.equalObject(dataModelCloneFrom, serverConfig.getDataModelCloneFrom()))
			{
				DataModelManager dataModelManager = ServoyModelManager.getServoyModelManager().getServoyModel().getDataModelManager();
				if (dataModelManager != null &&
					MessageDialog.openQuestion(getSite().getShell(), "Copy files", "Server '" + currentServerName + "' was marked as clone of '" +
						serverConfig.getDataModelCloneFrom() + "'. Do you want to copy(overwrite) all table related files from parent server?"))
				{
					IFolder sourceFolder = dataModelManager.getDBIFileContainer(serverConfig.getDataModelCloneFrom());
					IFolder cloneFolder = dataModelManager.getDBIFileContainer(currentServerName);
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
			}
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

		driverField.add("sun.jdbc.odbc.JdbcOdbcDriver");

		catalogField.removeAll();
		catalogField.add(ServerConfig.NONE);
		catalogField.add(ServerConfig.EMPTY);

		schemaField.removeAll();
		schemaField.add(ServerConfig.NONE);
		schemaField.add(ServerConfig.EMPTY);

		maxActiveField.addVerifyListener(DocumentValidatorVerifyListener.NUMBER_VERIFIER);
		maxIdleField.addVerifyListener(DocumentValidatorVerifyListener.NUMBER_VERIFIER);
		idleTimoutField.addVerifyListener(DocumentValidatorVerifyListener.NUMBER_VERIFIER);
		maxPreparedStatementsIdleField.addVerifyListener(DocumentValidatorVerifyListener.NUMBER_VERIFIER);

		validationTypeField.removeAll();
		validationTypeField.add(ServerConfig.getConnectionValidationTypeAsString(ServerConfig.CONNECTION_EXCEPTION_VALIDATION));
		validationTypeField.add(ServerConfig.getConnectionValidationTypeAsString(ServerConfig.CONNECTION_METADATA_VALIDATION));
		validationTypeField.add(ServerConfig.getConnectionValidationTypeAsString(ServerConfig.CONNECTION_QUERY_VALIDATION));

		dataModel_cloneFromField.removeAll();
		dataModel_cloneFromField.add(ServerConfig.NONE);
		String serverName = serverConfigObservable.getObject().getServerName();
		String cloneServerName;
		for (ServerConfig sc : ServoyModel.getServerManager().getServerConfigs())
		{
			cloneServerName = sc.getServerName();
			if (cloneServerName != null && !cloneServerName.equals(serverName)) dataModel_cloneFromField.add(cloneServerName);
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

		IObservableValue getMaxActiveObserveValue = serverConfigObservable.observePropertyValue("maxActive");
		IObservableValue maxActiveTextObserveWidget = SWTObservables.observeText(maxActiveField, SWT.Modify);
		IObservableValue getMaxIdleObserveValue = serverConfigObservable.observePropertyValue("maxIdle");
		IObservableValue maxIdleTextObserveWidget = SWTObservables.observeText(maxIdleField, SWT.Modify);
		IObservableValue getIdleTimeoutObserveValue = serverConfigObservable.observePropertyValue("idleTimeout");
		IObservableValue idleTimeoutTextObserveWidget = SWTObservables.observeText(idleTimoutField, SWT.Modify);
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
		m_bindingContext.bindValue(idleTimeoutTextObserveWidget, getIdleTimeoutObserveValue, null, null);
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
		for (String name : ServoyModel.getServerManager().getAllDriverClassNames())
		{
			if (name.equals(driver))
			{
				existing = true;
				break;
			}
		}
		return existing;
	}

	private void enableButtons()
	{

		if (serverConfigObservable.getObject().getServerName().equals(ServoyModel.getServerManager().getLogServerName()))
		{
			if (ServoyModel.getServerManager().logTableExists())
			{
				createLogTableButton.setEnabled(false);
				// FIXME: show tooltips for disabled button
				createLogTableButton.setToolTipText("Log table already exists in '" + ServoyModel.getServerManager().getLogServerName() + "'.");
			}
			else
			{
				createLogTableButton.setEnabled(true);
				createLogTableButton.setToolTipText(
					"Create a log table for tracking; " + "the creation of such a table is possible only if the current database server is the log server " +
						"and if it does not already contain a log table.");
			}
			if (ServoyModel.getServerManager().clientStatsTableExists())
			{
				createClientstatsTableButton.setEnabled(false);
				// FIXME: show tooltips for disabled button
				createClientstatsTableButton.setToolTipText(
					"Client statistics table already exists in '" + ServoyModel.getServerManager().getLogServerName() + "'.");
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
