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

import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.dialogs.MessageDialog;

import com.servoy.eclipse.core.Activator;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.core.util.UIUtils.MessageAndCheckBoxDialog;

/**
 * Moved from EclipseMessages.
 * @author acostescu
 */
public class I18NMessagesUtil
{

	private static final String DO_NOT_WARN_ON_I18N_DATASOURCE = "DO_NOT_WARN_ON_I18N_DATASOURCE"; //$NON-NLS-1$

	public static void showDatasourceWarning()
	{
		Preferences pluginPreferences = Activator.getDefault().getPluginPreferences();
		if (!pluginPreferences.getBoolean(DO_NOT_WARN_ON_I18N_DATASOURCE))
		{
			MessageAndCheckBoxDialog dialog = new MessageAndCheckBoxDialog(
				UIUtils.getActiveShell(),
				"I18N",
				null,
				"Changes made to the i18n entries will be saved to the workspace.\nThe table name you have set will be used when the solution is imported into the application server.",
				"Do not show this warning in the future.", false, MessageDialog.WARNING, new String[] { "OK", }, 0);
			dialog.open();
			if (dialog.isChecked())
			{
				pluginPreferences.setValue(DO_NOT_WARN_ON_I18N_DATASOURCE, true);
				Activator.getDefault().savePluginPreferences();
			}
		}
	}

}
