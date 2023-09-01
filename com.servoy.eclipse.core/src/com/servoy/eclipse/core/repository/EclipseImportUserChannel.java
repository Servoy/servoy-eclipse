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
package com.servoy.eclipse.core.repository;

import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.EclipseDatabaseUtils;
import com.servoy.eclipse.core.util.OptionDialog;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.IServerManagerInternal;
import com.servoy.j2db.persistence.RepositoryHelper;
import com.servoy.j2db.persistence.ServerConfig;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ILogLevel;
import com.servoy.j2db.util.ITransactionConnection;
import com.servoy.j2db.util.LogUtils;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.docvalidator.IdentDocumentValidator;
import com.servoy.j2db.util.xmlxport.IXMLImportUserChannel;

public class EclipseImportUserChannel implements IXMLImportUserChannel
{
	private int groupsAction = SKIP_ACTION;
	private Boolean allowSQLKeywords = null;
	private Boolean allowDataModelChangesGlobal = null;
	private final Map<String, Boolean> allowDataModelChangesForServer = new HashMap<String, Boolean>();
	private Boolean displayDataModelChanges = null;
	private Integer importI18NPolicy = null;
	private Boolean skipDatabaseViewsUpdate = null;
	private boolean insertNewI18NKeysOnly = true;
	private boolean overwriteAllStyles = false;
	private boolean skipAllStyles = false;
	private String lastName;
	private int retval;
	private final Map<String, Integer> serverSequenceTypesMap = new HashMap<String, Integer>();
	private final Map<String, Integer> serverDefaultValuesMap = new HashMap<String, Integer>();
	private String serverNameForRepositoryUserData;
	private final HashMap<String, String> unknownServerNameMap = new HashMap<String, String>();
	private Integer userImportPolicy;
	private boolean addUsersToAdminGroup = false;
	private final StringBuffer allImportantMSGes;
	private final Shell shell;
	private final HashMap<String, Integer> rootObjectsMap = new HashMap();

	public EclipseImportUserChannel(boolean displayDataModelChanges, Shell shell)
	{
		this.shell = shell;
		allImportantMSGes = new StringBuffer();
		this.displayDataModelChanges = new Boolean(displayDataModelChanges);
	}

	public int askAllowSQLKeywords()
	{
		if (allowSQLKeywords == null)
		{
			allowSQLKeywords = Boolean.valueOf(UIUtils.askConfirmation(shell, "SQL Keywords",
				"SQL keywords are used in this solution as table or column names.\nDo you want to try and import the solution anyway?\n(This will fail unless supported by the backend database)"));
		}
		return allowSQLKeywords.booleanValue() ? OK_ACTION : CANCEL_ACTION;

	}

	public int askForImportPolicy(String rootObjectName, int rootObjectType, boolean hasRevisions)
	{
		return OK_ACTION;
	}

	public int askGroupAlreadyExistsAction(String name)
	{
		if (groupsAction != OVERWRITE_ACTION && groupsAction != CANCEL_ACTION)
		{
			Display.getDefault().syncExec(new Runnable()
			{
				public void run()
				{
					final MessageDialog dialog = new MessageDialog(shell, "User groups used by imported solution already exist", null,
						"Do you want to configure security rights on existing groups?\nIf you choose no then no security rights will be imported for existing groups.",
						MessageDialog.WARNING, new String[] { "Yes", "No" }, 0);
					retval = dialog.open();
				}
			});
			if (retval == Window.OK)
			{

				groupsAction = OVERWRITE_ACTION;
			}
			else
			{
				groupsAction = CANCEL_ACTION;
			}
		}
		return groupsAction;
	}

	public int askImportI18NPolicy()
	{
		if (importI18NPolicy == null)
		{
			Display.getDefault().syncExec(new Runnable()
			{
				public void run()
				{
					final I18NDialog dialog = new I18NDialog(shell, "Import I18N data", null,
						"Do you wish to import the I18N data contained in the import(updates and inserts)?", MessageDialog.NONE, new String[] { "Yes", "No" },
						0);
					retval = dialog.open();
					if (retval == Window.OK)
					{
						insertNewI18NKeysOnly = dialog.insertNewKeys();
						importI18NPolicy = new Integer(OK_ACTION);
					}
					else
					{
						importI18NPolicy = new Integer(CANCEL_ACTION);
					}
				}
			});
		}
		return importI18NPolicy.intValue();
	}

	@Override
	public boolean getSkipDatabaseViewsUpdate()
	{
		if (skipDatabaseViewsUpdate == null)
		{
			skipDatabaseViewsUpdate = !Boolean.valueOf(UIUtils.askConfirmation(shell, "Database View Import",
				"Database View was encountered during import. Servoy cannot create/update database views. Do you want to create/update views as database tables? (Table Info will be imported no matter what you choose)"));
		}
		return skipDatabaseViewsUpdate.booleanValue();
	}

	public boolean getInsertNewI18NKeysOnly()
	{
		return insertNewI18NKeysOnly;
	}

	public int askImportRootObjectAsLocalName(String name, String localName, int objectTypeId)
	{
		info(Utils.stringInitCap(RepositoryHelper.getObjectTypeName(objectTypeId)) + " name mismatch, import name '" + name + "', workspace name '" +
			localName + "'. Import failed.", ERROR);
		return CANCEL_ACTION;
	}

	public int askImportMetaData()
	{
		return UIUtils.askQuestion(shell, "Meta Data", "Do you want to import the meta data contained in the import?") ? OK_ACTION : CANCEL_ACTION;
	}

	public int askImportSampleData()
	{
		return UIUtils.askQuestion(shell, "Sample Data", "Do you want to import the sample data contained in the import?") ? OK_ACTION : CANCEL_ACTION;
	}

	@Override
	public int askImportDatasources()
	{
		return UIUtils.askQuestion(shell, "Datasources", "Do you want to overwrite the DBI files in the workspace with those contained in the import?")
			? OK_ACTION : CANCEL_ACTION;
	}


	public int askMediaChangedAction(String name)
	{
		return CANCEL_ACTION;
	}

	public int askMediaNameCollisionAction(String name)
	{
		return CANCEL_ACTION;
	}

	public int askOverrideSequenceTypes(final String serverName)
	{
		Integer action = serverSequenceTypesMap.get(serverName);
		if (action == null)
		{
			action = new Integer(UIUtils.askQuestion(shell, "Sequence Types",
				"The sequence types in the import are different from the sequence types on existing tables for server '" + serverName +
					"'.\nDo you wish to override the existing sequence types?") ? OK_ACTION : CANCEL_ACTION);
			serverSequenceTypesMap.put(serverName, action);
		}
		return action.intValue();
	}

	public int askOverrideDefaultValues(final String serverName)
	{
		Integer action = serverDefaultValuesMap.get(serverName);
		if (action == null)
		{
			action = new Integer(UIUtils.askQuestion(shell, "Auto Enter",
				"The default values in the import are different from the sequence types on existing tables for server '" + serverName +
					"'.\nDo you wish to override the existing default values?") ? OK_ACTION : CANCEL_ACTION);
			serverDefaultValuesMap.put(serverName, action);
		}
		return action.intValue();
	}

	public int askRenameRootObjectAction(final String name, final int objectTypeId)
	{
		String objectType = Utils.stringInitCap(RepositoryHelper.getObjectTypeName(objectTypeId));

		Display.getDefault().syncExec(new Runnable()
		{
			public void run()
			{
				final InputDialog nameDialog = new InputDialog(shell, objectType + " exists",
					objectType + " with name '" + name + "' already exists(or you choose clean import), specify new name:", "", new IInputValidator()
					{
						public String isValid(String newText)
						{
							if (!IdentDocumentValidator.isJavaIdentifier(newText)) return "Invalid name.";
							if (newText != null && newText.length() > IRepository.MAX_ROOT_OBJECT_NAME_LENGTH) return "Name too long.";
							return null;
						}
					});
				int res = nameDialog.open();
				if (res == Window.OK)
				{
					lastName = nameDialog.getValue();
				}
				else
				{
					lastName = null;
				}
			}
		});

		if (lastName != null)
		{
			return RENAME_ACTION;
		}
		return UIUtils.askConfirmation(shell, "No new name specified for " + RepositoryHelper.getObjectTypeName(objectTypeId) + " '" + name + "'.",
			"Do you wish to skip it and continue the import?") ? SKIP_ACTION : CANCEL_ACTION;

	}

	public String askServerForImportUserData(String importServerName)
	{
		ServoyModelManager.getServoyModelManager().getServoyModel();
		String[] serverNames = ApplicationServerRegistry.get().getServerManager().getServerNames(true, true, true, false);
		int selectedServerIdx = 0;
		for (int i = 0; i < serverNames.length; i++)
		{
			if (serverNames[i].equals(importServerName))
			{
				selectedServerIdx = i;
				break;
			}
		}
		final int severIndex = selectedServerIdx;
		final int[] selectedOption = new int[0];
		Display.getDefault().syncExec(new Runnable()
		{
			public void run()
			{
				final OptionDialog optionDialog = new OptionDialog(shell, "Select server for import user data", null,
					"Please select server to be used for importing user data that were exported from a server with name : " + importServerName +
						"\n\nIf you want to import into a new server, please press skip, create the new server and restart the import.",
					MessageDialog.QUESTION, new String[] { "OK", "Skip" }, 0, serverNames, severIndex);
				retval = optionDialog.open();
				selectedOption[0] = optionDialog.getSelectedOption();
			}
		});

		return retval == Window.OK ? serverNames[selectedOption[0]] : null;
	}

	public String askServerForRepositoryUserData()
	{
		try
		{
			if (serverNameForRepositoryUserData == null)
			{
				int[] selectedOption = new int[1];
				ServoyModelManager.getServoyModelManager().getServoyModel();
				String[] serverNames = ApplicationServerRegistry.get().getServerManager().getServerNames(true, true, true, false);
				Display.getDefault().syncExec(new Runnable()
				{
					public void run()
					{
						final OptionDialog optionDialog = new OptionDialog(shell, "User data in Repository Server", null,
							"The solution contains user data in the repository server, this is not recommended. Choose another server for the user data if you wish.",
							MessageDialog.WARNING, new String[] { "OK", "Skip" }, 0, serverNames, 0);
						retval = optionDialog.open();
						selectedOption[0] = optionDialog.getSelectedOption();
					}
				});
				if (retval == Window.OK)
				{
					serverNameForRepositoryUserData = serverNames[selectedOption[0]];
				}
			}
			return serverNameForRepositoryUserData;
		}
		catch (Exception e)
		{
			return null;
		}
	}

	public int askStyleAlreadyExistsAction(String name)
	{
		if (overwriteAllStyles) return OVERWRITE_ACTION;
		if (skipAllStyles) return SKIP_ACTION;
		if (rootObjectsMap.containsKey(name))
		{
			return rootObjectsMap.get(name);
		}
		Display.getDefault().syncExec(new Runnable()
		{
			public void run()
			{
				final MessageDialog dialog = new MessageDialog(shell, "Style/Solution '" + name + "' already exists", null,
					"Style/Solution '" + name + "' exists in the workspace. Do you want to overwrite it or skip its import?", MessageDialog.WARNING,
					new String[] { "Overwrite", "Skip", "Overwrite all", "Skip all" }, 0);
				retval = dialog.open();
			}
		});
		if (retval == 0)
		{
			rootObjectsMap.put(name, OVERWRITE_ACTION);
			return OVERWRITE_ACTION;
		}
		else if (retval == 1)
		{
			rootObjectsMap.put(name, SKIP_ACTION);
			return SKIP_ACTION;
		}
		else if (retval == 2)
		{
			overwriteAllStyles = true;
			return OVERWRITE_ACTION;
		}
		else
		{
			skipAllStyles = true;
			return SKIP_ACTION;
		}
	}

	public int askUnknownServerAction(final String name)
	{
		try
		{
			String s = null;
			if (unknownServerNameMap.containsKey(name))
			{
				s = unknownServerNameMap.get(name);
			}
			else
			{
				ServoyModelManager.getServoyModelManager().getServoyModel();
				IServerManagerInternal serverManager = ApplicationServerRegistry.get().getServerManager();
				String[] serverNames = serverManager.getServerNames(false, true, true, false);
				ServerConfig serverConfig = serverManager.getServerConfig(name);
				if (serverConfig != null)
				{
					if (!serverConfig.isEnabled())
					{
						boolean[] enable = new boolean[] { true };
						Display.getDefault().syncExec(() -> {
							enable[0] = MessageDialog.openConfirm(shell, "Server found",
								"The database server '" + name + "' was found, but it is disabled. Do you wish to enable it?");
						});
						if (!enable[0])
						{
							return CANCEL_ACTION;
						}
						try
						{
							serverConfig = serverConfig.getEnabledCopy(true);
							serverManager.testServerConfigConnection(serverConfig, 0);
							serverManager.saveServerConfig(name, serverConfig);
							// return retry so importer picks up the enabled server
							return RETRY_ACTION;
						}
						catch (Exception e)
						{
							ServoyLog.logError(e);
							MessageDialog.openError(shell, "Cannot enable server", e.getMessage());
							return CANCEL_ACTION;
						}
					}
				}
				else
				{
					IServerInternal serverPrototype = null;
					ServerConfig[] serverConfigs = serverManager.getServerConfigs();
					for (ServerConfig sc : serverConfigs)
					{
						if (sc.isEnabled() && sc.isPostgresDriver())
						{
							serverPrototype = (IServerInternal)serverManager.getServer(sc.getServerName());
							if (serverPrototype != null && serverPrototype.isValid())
							{
								serverConfig = new ServerConfig(name, sc.getUserName(), sc.getPassword(), EclipseDatabaseUtils.getPostgresServerUrl(sc, name),
									sc.getConnectionProperties(), sc.getDriver(), sc.getCatalog(), null, sc.getMaxActive(), sc.getMaxIdle(),
									sc.getMaxPreparedStatementsIdle(), sc.getConnectionValidationType(), sc.getValidationQuery(), null, true, false,
									sc.getPrefixTables(), sc.getQueryProcedures(), -1, sc.getSelectINValueCountLimit(), sc.getDialectClass(),
									sc.getQuoteList(), sc.isClientOnlyConnections());
								if (serverManager.validateServerConfig(null, serverConfig) != null)
								{
									// something is wrong
									serverConfig = null;
								}
							}
						}
					}

					final int[] selectedOption = new int[1];
					String[] buttons = serverConfig != null ? new String[] { "Replace Server", "Create Server", "Cancel" }
						: new String[] { "Replace Server", "Cancel" };
					int defaultOption = serverConfig != null ? 1 : 0;
					Display.getDefault().syncExec(new Runnable()
					{
						public void run()
						{
							final OptionDialog optionDialog = new OptionDialog(shell, "Server '" + name + "'not found", null, "Server with name '" + name +
								"' is not found, but used by the import solution, select another server to use, try to create a new server or press cancel to cancel import and define the server first.",
								MessageDialog.WARNING, buttons, defaultOption, serverNames, defaultOption);
							retval = optionDialog.open();
							selectedOption[0] = optionDialog.getSelectedOption();
						}
					});
					if (serverConfig != null && retval == 1)
					{
						// create server option
						ITransactionConnection connection = null;
						PreparedStatement ps = null;
						try
						{
							connection = serverPrototype.getUnmanagedConnection();
							ps = connection.prepareStatement("CREATE DATABASE \"" + name + "\" WITH ENCODING 'UNICODE';");
							ps.execute();
							ps.close();
							ps = null;
						}
						catch (Exception e)
						{
							ServoyLog.logError(e);
						}
						finally
						{
							Utils.closeConnection(connection);
							Utils.closeStatement(ps);
						}
						try
						{
							serverManager.testServerConfigConnection(serverConfig, 0);
							serverManager.saveServerConfig(null, serverConfig);
						}
						catch (Exception ex)
						{
							ServoyLog.logError(ex);
							Display.getDefault().syncExec(new Runnable()
							{
								public void run()
								{
									MessageDialog.openError(shell, "Cannot create server '" + name + "'",
										"An unexpected error occured while creating new server, please select an existing server or create server manually.");
								}
							});
						}
						// return retry so that the importer will pick up the new database server or show the choices again if the server was not created successfully
						return RETRY_ACTION;
					}
					// replace server option
					if (retval == 0)
					{
						s = serverNames[selectedOption[0]];
					}

				}
				unknownServerNameMap.put(name, s);
			}
			if (s != null)
			{
				lastName = s;
				return RENAME_ACTION;
			}
			// rename or create was not selected, so user choose to cancel
			return CANCEL_ACTION;
		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
			return CANCEL_ACTION;
		}

	}

	public int askUserImportPolicy()
	{
		if (userImportPolicy == null)
		{
			Display.getDefault().syncExec(new Runnable()
			{
				public void run()
				{
					final UserImportDialog dialog = new UserImportDialog(shell, "User information", null,
						"Do you wish to import the user information contained in the import?", MessageDialog.NONE, new String[] { "Yes", "No" }, 0);
					dialog.open();
				}
			});
		}

		return userImportPolicy.intValue();
	}

	public boolean getAddUsersToAdministratorGroup()
	{
		return addUsersToAdminGroup;
	}

	public boolean getDeleteEnabled()
	{
		return false;
	}


	public boolean getMergeEnabled()
	{
		return false;
	}

	public boolean getMustAuthenticate(boolean importMustAuthenticate)
	{
		return importMustAuthenticate;
	}

	public String getNewName()
	{
		return lastName;
	}

	public boolean getOverwriteEnabled()
	{
		return false;
	}

	public boolean getUseLocalOnMergeConflict()
	{
		return false;
	}

	public void notifyWorkDone(float amount)
	{
		// do nothing

	}

	public void info(String message, int priority)
	{
		if (priority > ILogLevel.DEBUG)
		{
			allImportantMSGes.append(LogUtils.getLogLevelString(priority)).append(": ").append(message).append('\n');
		}
		Debug.trace(message);
	}

	@Override
	public void clientInfo(String message, int priority)
	{
		this.info(message, priority);
	}

	public String getAllImportantMSGes()
	{
		return allImportantMSGes.toString();
	}

	class I18NDialog extends MessageDialog
	{
		private boolean insertNewKeys;
		private Button insertNewKeysButton;

		public I18NDialog(Shell parentShell, String dialogTitle, Image dialogTitleImage, String dialogMessage, int dialogImageType, String[] dialogButtonLabels,
			int defaultIndex)
		{
			super(parentShell, dialogTitle, dialogTitleImage, dialogMessage, dialogImageType, dialogButtonLabels, defaultIndex);
			setBlockOnOpen(true);
		}

		@Override
		public boolean close()
		{
			insertNewKeys = insertNewKeysButton.getSelection();
			return super.close();
		}

		@Override
		protected Control createCustomArea(Composite parent)
		{
			insertNewKeysButton = new Button(parent, SWT.CHECK);
			insertNewKeysButton.setText("Insert new keys only(inserts only, no updates)");
			GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
			insertNewKeysButton.setLayoutData(gridData);

			return insertNewKeysButton;
		}

		public boolean insertNewKeys()
		{
			return insertNewKeys;
		}
	}
	class UserImportDialog extends MessageDialog implements SelectionListener
	{
		private Button addUsersToAdminGroupButton;
		private Button updateUsers;
		private Button overwriteUsers;

		public UserImportDialog(Shell parentShell, String dialogTitle, Image dialogTitleImage, String dialogMessage, int dialogImageType,
			String[] dialogButtonLabels, int defaultIndex)
		{
			super(parentShell, dialogTitle, dialogTitleImage, dialogMessage, dialogImageType, dialogButtonLabels, defaultIndex);
			setBlockOnOpen(true);
		}

		@Override
		public boolean close()
		{
			return super.close();
		}

		@Override
		public int open()
		{
			int ret = super.open();
			if (ret != Window.OK)
			{
				userImportPolicy = new Integer(IXMLImportUserChannel.IMPORT_USER_POLICY_DONT);
			}
			return ret;
		}

		private void fillValues()
		{
			addUsersToAdminGroup = addUsersToAdminGroupButton.getSelection();
			userImportPolicy = new Integer(
				updateUsers.getSelection() ? IXMLImportUserChannel.IMPORT_USER_POLICY_CREATE_U_UPDATE_G : (overwriteUsers.getSelection()
					? IXMLImportUserChannel.IMPORT_USER_POLICY_OVERWRITE_COMPLETELY : IXMLImportUserChannel.IMPORT_USER_POLICY_DONT));
		}

		@Override
		protected Control createCustomArea(Composite parent)
		{

			updateUsers = new Button(parent, SWT.RADIO);
			updateUsers.setText("Create nonexisting users and give existing users the permissions specified in import");
			updateUsers.setSelection(true);
			updateUsers.addSelectionListener(this);

			overwriteUsers = new Button(parent, SWT.RADIO);
			overwriteUsers.setText("Overwrite existing users completely (USE WITH CARE)");
			overwriteUsers.addSelectionListener(this);
			addUsersToAdminGroupButton = new Button(parent, SWT.CHECK);
			addUsersToAdminGroupButton.setText("Allow users to be added to the " + IRepository.ADMIN_GROUP + " permission");
			addUsersToAdminGroupButton.addSelectionListener(this);

			GridData gridData = new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false);
			updateUsers.setLayoutData(gridData);

			gridData = new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false);
			overwriteUsers.setLayoutData(gridData);

			gridData = new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false);
			//gridData.horizontalIndent = 20;
			addUsersToAdminGroupButton.setLayoutData(gridData);

			fillValues();
			return super.createCustomArea(parent);
		}

		public void widgetDefaultSelected(SelectionEvent e)
		{

		}

		public void widgetSelected(SelectionEvent e)
		{
			fillValues();
		}
	}

	public int getAllowDataModelChange(String serverName)
	{
		if (allowDataModelChangesGlobal != null)
		{
			return allowDataModelChangesGlobal.booleanValue() ? OK_ACTION : CANCEL_ACTION;
		}
		if (allowDataModelChangesForServer.containsKey(serverName))
		{
			return allowDataModelChangesForServer.get(serverName).booleanValue() ? OK_ACTION : CANCEL_ACTION;
		}

		final int[] returnValue = new int[1];
		Display.getDefault().syncExec(new Runnable()
		{
			public void run()
			{
				MessageDialog dialog = new MessageDialog(shell, "Allow Database Change for Server: " + serverName, null,
					"Do you want to change database structure as in import file for server: " + serverName + "?", MessageDialog.QUESTION,
					new String[] { "Yes for all servers", IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL, "No for all servers" }, 0);
				dialog.setBlockOnOpen(true);
				returnValue[0] = dialog.open();
			}
		});
		int action = OK_ACTION;
		if (returnValue[0] == 0)
		{
			allowDataModelChangesGlobal = Boolean.TRUE;
		}
		else if (returnValue[0] == 1)
		{
			allowDataModelChangesForServer.put(serverName, Boolean.TRUE);
		}
		else if (returnValue[0] == 2)
		{
			allowDataModelChangesForServer.put(serverName, Boolean.FALSE);
			action = CANCEL_ACTION;
		}
		else if (returnValue[0] == 3)
		{
			allowDataModelChangesGlobal = Boolean.FALSE;
			action = CANCEL_ACTION;
		}
		return action;
	}

	public boolean getDisplayDataModelChange()
	{
		if (displayDataModelChanges == null)
		{
			return false;
		}
		return displayDataModelChanges.booleanValue();
	}

	public String askProtectionPassword(String solutionName)
	{
		return UIUtils.showPasswordDialog(shell, "Solution '" + solutionName + "' is password protected",
			"Please enter protection password for solution : '" + solutionName + '\'', "", null);

	}

	public int askUpdateSequences()
	{
		return CANCEL_ACTION;
	}

	public String getImporterUsername()
	{
		return null;
	}

	@Override
	public boolean compactSolutions()
	{
		return false;
	}

	@Override
	public boolean allowImportEmptySolution()
	{
		return true;
	}

	@Override
	public void displayWarningMessage(String title, String message, boolean scrollableDialog)
	{
		Display.getDefault().syncExec(new Runnable()
		{
			public void run()
			{
				if (scrollableDialog)
				{
					UIUtils.showScrollableDialog(UIUtils.getActiveShell(), IMessageProvider.WARNING, "War export", title,
						message);
				}
				else
				{
					MessageDialog.openWarning(UIUtils.getActiveShell(), title,
						message);
				}
			}
		});
	}
}
