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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.dialogs.IInputValidator;
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

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.OptionDialog;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.RepositoryHelper;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ILogLevel;
import com.servoy.j2db.util.LogUtils;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.docvalidator.IdentDocumentValidator;
import com.servoy.j2db.util.xmlxport.IXMLImportUserChannel;

public class EclipseImportUserChannel implements IXMLImportUserChannel
{
	private int groupsAction = SKIP_ACTION;
	private Boolean allowSQLKeywords = null;
	private Boolean allowDataModelChanges = null;
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

	public EclipseImportUserChannel(boolean allowDataModelChanges, boolean displayDataModelChanges, Shell shell)
	{
		this.shell = shell;
		allImportantMSGes = new StringBuffer();
		this.allowDataModelChanges = new Boolean(allowDataModelChanges);
		this.displayDataModelChanges = new Boolean(displayDataModelChanges);
	}

	public int askAllowSQLKeywords()
	{
		if (allowSQLKeywords == null)
		{
			allowSQLKeywords = Boolean.valueOf(UIUtils.askConfirmation(
				shell,
				"SQL Keywords",
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
			final MessageDialog dialog = new MessageDialog(shell, "User groups used by imported solution already exist", null,
				"Do you want to configure security rights on existing groups?\nIf you choose no then no security rights will be imported for existing groups.",
				MessageDialog.WARNING, new String[] { "Yes to all", "No to all" }, 0);
			Display.getDefault().syncExec(new Runnable()
			{
				public void run()
				{
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
			final I18NDialog dialog = new I18NDialog(shell, "Import I18N data", null,
				"Do you wish to import the I18N data contained in the import(updates and inserts)?", MessageDialog.NONE, new String[] { "Yes", "No" }, 0);
			Display.getDefault().syncExec(new Runnable()
			{
				public void run()
				{
					retval = dialog.open();
				}
			});
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
		return importI18NPolicy.intValue();
	}

	@Override
	public boolean getSkipDatabaseViewsUpdate()
	{
		if (skipDatabaseViewsUpdate == null)
		{
			skipDatabaseViewsUpdate = !Boolean.valueOf(UIUtils.askConfirmation(
				shell,
				"Database View Import",
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
		final InputDialog nameDialog = new InputDialog(shell, objectType + " exists", objectType + " with name '" + name +
			"' already exists(or you choose clean import), specify new name:", "", new IInputValidator()
		{
			public String isValid(String newText)
			{
				if (!IdentDocumentValidator.isJavaIdentifier(newText)) return "Invalid name.";
				if (newText != null && newText.length() > IRepository.MAX_ROOT_OBJECT_NAME_LENGTH) return "Name too long.";
				return null;
			}
		});

		Display.getDefault().syncExec(new Runnable()
		{
			public void run()
			{
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
		String[] serverNames = ServoyModel.getServerManager().getServerNames(true, true, true, false);
		int selectedServerIdx = 0;
		for (int i = 0; i < serverNames.length; i++)
		{
			if (serverNames[i].equals(importServerName))
			{
				selectedServerIdx = i;
				break;
			}
		}
		final OptionDialog optionDialog = new OptionDialog(shell, "Select server for import user data", null,
			"Please select server to be used for importing user data that were exported from a server with name : " + importServerName +
				"\n\nIf you want to import into a new server, please press skip, create the new server and restart the import.", MessageDialog.QUESTION,
			new String[] { "OK", "Skip" }, 0, serverNames, selectedServerIdx);
		Display.getDefault().syncExec(new Runnable()
		{
			public void run()
			{
				retval = optionDialog.open();
			}
		});

		return retval == Window.OK ? serverNames[optionDialog.getSelectedOption()] : null;
	}

	public String askServerForRepositoryUserData()
	{
		try
		{
			if (serverNameForRepositoryUserData == null)
			{
				ServoyModelManager.getServoyModelManager().getServoyModel();
				String[] serverNames = ServoyModel.getServerManager().getServerNames(true, true, true, false);
				final OptionDialog optionDialog = new OptionDialog(shell, "User data in Repository Server", null,
					"The solution contains user data in the repository server, this is not recommended. Choose another server for the user data if you wish.",
					MessageDialog.WARNING, new String[] { "OK", "Skip" }, 0, serverNames, 0);
				Display.getDefault().syncExec(new Runnable()
				{
					public void run()
					{
						retval = optionDialog.open();
					}
				});
				if (retval == Window.OK)
				{
					serverNameForRepositoryUserData = serverNames[optionDialog.getSelectedOption()];
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
		final MessageDialog dialog = new MessageDialog(shell, "Style/Solution '" + name + "' already exists", null, "Style/Solution '" + name +
			"' exists in the workspace. Do you want to overwrite it or skip its import?", MessageDialog.WARNING,
			new String[] { "Overwrite", "Skip", "Overwrite all", "Skip all" }, 0);
		Display.getDefault().syncExec(new Runnable()
		{
			public void run()
			{
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

	public int askUnknownServerAction(String name)
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
				String[] serverNames = ServoyModel.getServerManager().getServerNames(true, true, true, false);
				final OptionDialog optionDialog = new OptionDialog(shell, "Server not found", null, "Server with name '" + name +
					"' is not found, but used by the import solution, select another server to use or press cancel to define the server first",
					MessageDialog.WARNING, new String[] { "OK", "Cancel" }, 0, serverNames, 0);
				Display.getDefault().syncExec(new Runnable()
				{
					public void run()
					{
						retval = optionDialog.open();
					}
				});
				if (retval == Window.OK)
				{
					s = serverNames[optionDialog.getSelectedOption()];
				}
				unknownServerNameMap.put(name, s);
			}
			if (s != null)
			{
				lastName = s;
				return RENAME_ACTION;
			}
			else
			{
				return CANCEL_ACTION;
			}
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
			final UserImportDialog dialog = new UserImportDialog(shell, "User information", null,
				"Do you wish to import the user information contained in the import?", MessageDialog.NONE, new String[] { "Yes", "No" }, 0);
			Display.getDefault().syncExec(new Runnable()
			{
				public void run()
				{
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

	public String getAllImportantMSGes()
	{
		return allImportantMSGes.toString();
	}

	class I18NDialog extends MessageDialog
	{
		private boolean insertNewKeys;
		private Button insertNewKeysButton;

		public I18NDialog(Shell parentShell, String dialogTitle, Image dialogTitleImage, String dialogMessage, int dialogImageType,
			String[] dialogButtonLabels, int defaultIndex)
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
			userImportPolicy = new Integer(updateUsers.getSelection() ? IXMLImportUserChannel.IMPORT_USER_POLICY_CREATE_U_UPDATE_G
				: (overwriteUsers.getSelection() ? IXMLImportUserChannel.IMPORT_USER_POLICY_OVERWRITE_COMPLETELY
					: IXMLImportUserChannel.IMPORT_USER_POLICY_DONT));
		}

		@Override
		protected Control createCustomArea(Composite parent)
		{

			updateUsers = new Button(parent, SWT.RADIO);
			updateUsers.setText("Create nonexisting users and add existing users to groups specified in import");
			updateUsers.setSelection(true);
			updateUsers.addSelectionListener(this);

			overwriteUsers = new Button(parent, SWT.RADIO);
			overwriteUsers.setText("Overwrite existing users completely (USE WITH CARE)");
			overwriteUsers.addSelectionListener(this);
			addUsersToAdminGroupButton = new Button(parent, SWT.CHECK);
			addUsersToAdminGroupButton.setText("Allow users to be added to the " + IRepository.ADMIN_GROUP + " group");
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

	public int getAllowDataModelChange()
	{
		if (allowDataModelChanges == null)
		{
			allowDataModelChanges = Boolean.valueOf(UIUtils.askConfirmation(shell, "Allow Database Change",
				"Do you want to change database structure as in import file?"));
		}
		return allowDataModelChanges.booleanValue() ? OK_ACTION : CANCEL_ACTION;
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
		return UIUtils.showPasswordDialog(shell, "Solution '" + solutionName + "' is password protected", "Please enter protection password for solution : '" +
			solutionName + '\'', "", null);

	}

	public int askUpdateSequences()
	{
		return CANCEL_ACTION;
	}

	public String getImporterUsername()
	{
		return null;
	}
}
