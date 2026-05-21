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

package com.servoy.eclipse.ngclient.startup;

import com.servoy.j2db.IBasicFormManager;
import com.servoy.j2db.IDesignerCallback;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.server.ngclient.INGClientWebsocketSession;
import com.servoy.j2db.server.ngclient.NGClient;
import com.servoy.j2db.server.ngclient.NGFormManager;

/**
 * A lightweight NG client for form preview/testing purposes.
 * <p>
 * This client:
 * <ul>
 *   <li>Skips authentication (bypasses mustAuthenticate/login form)</li>
 *   <li>Shows a specific form directly instead of the solution's first form</li>
 *   <li>Does not attach the debugger</li>
 * </ul>
 * <p>
 * Started via the URL parameter {@code ?formpreview=formName}.
 * Authentication is also bypassed at the HTTP filter level in {@code IndexPageFilter}.
 *
 * @since 2026.6
 */
public class FormPreviewNGClient extends NGClient
{
	private static volatile String pendingTargetFormName;
	private final String targetFormName;

	public FormPreviewNGClient(INGClientWebsocketSession wsSession, IDesignerCallback designerCallback, String targetFormName) throws Exception
	{
		super(wsSession, designerCallback);
		this.targetFormName = targetFormName;
	}

	public static void setPendingTargetFormName(String formName)
	{
		pendingTargetFormName = formName;
	}

	public String getTargetFormName()
	{
		return targetFormName;
	}

	@Override
	protected IBasicFormManager createFormManager()
	{
		final String formToShow = pendingTargetFormName;

		return new NGFormManager(this)
		{
			@Override
			public void makeSolutionSettings(Solution s)
			{
		
				java.util.Iterator<com.servoy.j2db.persistence.Form> e = application.getFlattenedSolution().getForms(true);
				while (e.hasNext())
				{
					com.servoy.j2db.persistence.Form form = e.next();
					if (application.getFlattenedSolution().formCanBeInstantiated(form))
					{
						addForm(form, form.getName().equals(formToShow));
					}
					else
					{
						addForm(form, false);
					}
				}

				application.getModeManager().setMode(com.servoy.j2db.IModeManager.EDIT_MODE);

				if (getCurrentForm() == null)
				{
					showFormInMainPanel(formToShow);
				}
			}
		};
	}

	@Override
	protected void showInfoPanel()
	{
		// skip info panel for preview client
	}

	@Override
	public void showDefaultLogin() throws com.servoy.j2db.util.ServoyException
	{
		// skip authentication for form preview - set a fake user so solution loading continues

		getClientInfo().setUserUid("formpreview_user");
		getClientInfo().setUserName("FormPreview");
	}
}
