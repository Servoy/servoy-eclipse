/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2026 Servoy BV

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

package com.servoy.eclipse.core.ai;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.Form;

/**
 * @author jcompagner
 *
 * @since 2024.03.10 2026.03
 *
 */
public class AiBridge
{
	/**
	 * Applies a specific event method to the form.
	 *
	 * @param form The form to update
	 * @param eventName The event name
	 * @param methodUUID The method UUID
	 */
	public static void applyEventMethod(Form form, String eventName, String methodUUID)
	{
		switch (eventName)
		{
			case "onLoad" :
				form.setOnLoadMethodID(methodUUID);
				break;
			case "onUnLoad" :
				form.setOnUnLoadMethodID(methodUUID);
				break;
			case "onShow" :
				form.setOnShowMethodID(methodUUID);
				break;
			case "onHide" :
				form.setOnHideMethodID(methodUUID);
				break;
			case "onBeforeHide" :
				form.setOnBeforeHideMethodID(methodUUID);
				break;
			case "onRecordSelection" :
				form.setOnRecordSelectionMethodID(methodUUID);
				break;
			case "onBeforeRecordSelection" :
				form.setOnBeforeRecordSelectionMethodID(methodUUID);
				break;
			case "onRecordEditStart" :
				form.setOnRecordEditStartMethodID(methodUUID);
				break;
			case "onRecordEditStop" :
				form.setOnRecordEditStopMethodID(methodUUID);
				break;
			case "onElementDataChange" :
				form.setOnElementDataChangeMethodID(methodUUID);
				break;
			case "onElementFocusGained" :
				form.setOnElementFocusGainedMethodID(methodUUID);
				break;
			case "onElementFocusLost" :
				form.setOnElementFocusLostMethodID(methodUUID);
				break;
			case "onResize" :
				form.setOnResizeMethodID(methodUUID);
				break;
			case "onSort" :
				form.setOnSortCmdMethodID(methodUUID);
				break;
			default :
				ServoyLog.logInfo("[FormService] Unknown event: " + eventName);
				break;
		}
	}
}
