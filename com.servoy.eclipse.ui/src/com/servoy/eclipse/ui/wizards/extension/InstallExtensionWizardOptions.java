/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

package com.servoy.eclipse.ui.wizards.extension;

import org.eclipse.jface.dialogs.IDialogSettings;

/**
 * This class handles the persisted options of the the install extension wizard. 
 * @author acostescu
 */
public class InstallExtensionWizardOptions
{

	protected final static String FILE_IMPORT_FOLDER = "file import folder";
	protected final static String USE_LOCAL_FOLDER = "use local folder";
	protected final static String USE_MARKETPLACE = "use marketplace";
	protected final static String ALLOW_UPGRADES = "allow upgrades";
	protected final static String ALLOW_DOWNGRADES = "allow downgrades";
	protected final static String ALLOW_ONLY_FINAL = "allow only final";
	protected final static String ALLOW_LIB_CONFLICTS = "allow lib conflicts";


	public String fileImportParent;
	/** Used when importing from local .exp file */
	public boolean useLocalFolderForDependencies = true;
	/** Used when importing from local .exp file */
	public boolean useMarketplaceForDependencies = false;

	public boolean allowUpgrades = true;
	public boolean allowDowngrades = false;
	public boolean allowOnlyFinalVersions = true;
	public boolean allowLibConflicts = false;

	/**
	 * Default options.
	 */
	public InstallExtensionWizardOptions()
	{
		// just defaults
	}

	/**
	 * Initialized the object by reading the persisted wizard options.
	 */
	public InstallExtensionWizardOptions(IDialogSettings dialogSettings)
	{
		fileImportParent = dialogSettings.get(FILE_IMPORT_FOLDER);
		if (dialogSettings.get(USE_LOCAL_FOLDER) != null) useLocalFolderForDependencies = dialogSettings.getBoolean(USE_LOCAL_FOLDER);
		if (dialogSettings.get(USE_MARKETPLACE) != null) useMarketplaceForDependencies = dialogSettings.getBoolean(USE_MARKETPLACE);
		if (dialogSettings.get(ALLOW_UPGRADES) != null) allowUpgrades = dialogSettings.getBoolean(ALLOW_UPGRADES);
		if (dialogSettings.get(ALLOW_DOWNGRADES) != null) allowDowngrades = dialogSettings.getBoolean(ALLOW_DOWNGRADES);
		if (dialogSettings.get(ALLOW_ONLY_FINAL) != null) allowOnlyFinalVersions = dialogSettings.getBoolean(ALLOW_ONLY_FINAL);
		if (dialogSettings.get(ALLOW_LIB_CONFLICTS) != null) allowLibConflicts = dialogSettings.getBoolean(ALLOW_LIB_CONFLICTS);
	}

	/**
	 * Saves/persists the current wizard options.
	 */
	public void saveOptions(IDialogSettings dialogSettings)
	{
		dialogSettings.put(FILE_IMPORT_FOLDER, fileImportParent);
		dialogSettings.put(USE_LOCAL_FOLDER, useLocalFolderForDependencies);
		dialogSettings.put(USE_MARKETPLACE, useMarketplaceForDependencies);
		dialogSettings.put(ALLOW_UPGRADES, allowUpgrades);
		dialogSettings.put(ALLOW_DOWNGRADES, allowDowngrades);
		dialogSettings.put(ALLOW_ONLY_FINAL, allowOnlyFinalVersions);
		dialogSettings.put(ALLOW_LIB_CONFLICTS, allowLibConflicts);
	}

}
