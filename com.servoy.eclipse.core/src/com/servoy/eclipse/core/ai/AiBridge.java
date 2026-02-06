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

import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.ScriptMethod;

/**
 * This is a helper clas that is implemented in the various releases to set stuff that are different between the releases, like the event method ids on the form.
 *
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
		ScriptMethod scriptMethod = ServoyModelFinder.getServoyModel().getFlattenedSolution().getScriptMethod(methodUUID);
		if (scriptMethod == null)
		{
			ServoyLog.logInfo("[FormService] Could not find method with UUID: " + methodUUID);
			return;
		}
		switch (eventName)
		{
			case "onLoad" :
				form.setOnLoadMethodID(scriptMethod.getID());
				break;
			case "onUnLoad" :
				form.setOnUnLoadMethodID(scriptMethod.getID());
				break;
			case "onShow" :
				form.setOnShowMethodID(scriptMethod.getID());
				break;
			case "onHide" :
				form.setOnHideMethodID(scriptMethod.getID());
				break;
			case "onBeforeHide" :
				form.setOnBeforeHideMethodID(scriptMethod.getID());
				break;
			case "onRecordSelection" :
				form.setOnRecordSelectionMethodID(scriptMethod.getID());
				break;
			case "onRecordEditStart" :
				form.setOnRecordEditStartMethodID(scriptMethod.getID());
				break;
			case "onRecordEditStop" :
				form.setOnRecordEditStopMethodID(scriptMethod.getID());
				break;
			case "onElementDataChange" :
				form.setOnElementDataChangeMethodID(scriptMethod.getID());
				break;
			case "onElementFocusGained" :
				form.setOnElementFocusGainedMethodID(scriptMethod.getID());
				break;
			case "onElementFocusLost" :
				form.setOnElementFocusLostMethodID(scriptMethod.getID());
				break;
			case "onResize" :
				form.setOnResizeMethodID(scriptMethod.getID());
				break;
			case "onSort" :
				form.setOnSortCmdMethodID(scriptMethod.getID());
				break;
			default :
				ServoyLog.logInfo("[FormService] Unknown event: " + eventName);
				break;
		}
	}

	/**
	 * @param form
	 * @param parentForm
	 */
	public static void setFormExtendsID(Form form, Form parentForm)
	{
		form.setExtendsForm(parentForm);
		form.setExtendsID(parentForm.getID());
	}
}
