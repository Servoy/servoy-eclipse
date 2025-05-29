/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2025 Servoy BV

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

package com.servoy.eclipse.developersolution;

import java.awt.Dimension;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.widgets.Display;
import org.mozilla.javascript.Function;

import com.servoy.eclipse.ui.dialogs.BrowserDialog;
import com.servoy.j2db.IDebugClient;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.scripting.solutionmodel.developer.IJSDeveloperBridge;
import com.servoy.j2db.scripting.solutionmodel.developer.JSDeveloperMenu;
import com.servoy.j2db.scripting.solutionmodel.developer.Location;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.UUID;

/**
 * @author jcompagner
 *
 * @since 2025.09
 *
 */
public class DeveloperBridge implements IJSDeveloperBridge
{
	public static Map<JSDeveloperMenu, Function> menus = new HashMap<>();


	private final IDebugClient client;
	private final Map<UUID, Integer> foreignElementUUIDs = new HashMap<UUID, Integer>();

	public DeveloperBridge(IDebugClient client)
	{
		this.client = client;
	}


	@Override
	public void showForm(String formName)
	{
		Display display = Display.getDefault();
		display.asyncExec(() -> {
			BrowserDialog dialog = new BrowserDialog(Display.getDefault().getActiveShell(),
				"http://localhost:" + ApplicationServerRegistry.get().getWebServerPort() + "/solution/" + client.getSolutionName() +
					"/index.html?svy_developer=true",
				false, false, true);
			client.getFormManager().showFormInCurrentContainer(formName);
			Form form = client.getFlattenedSolution().getForm(formName);
			Dimension size = form.getSize();

			float systemDPI = 96f;
			size.height = Math.round(size.height * Display.getDefault().getDPI().y / systemDPI);
			size.width = Math.round(size.width * Display.getDefault().getDPI().x / systemDPI);
			dialog.open(size);
		});
	}

	@Override
	public JSDeveloperMenu createMenu(String text, int location)
	{
		return new JSDeveloperMenu(text, location);
	}

	@Override
	public void registerMenuItem(JSDeveloperMenu menu, Function callback)
	{
		menus.put(menu, callback);
	}

	@Override
	public void registerMenuItem(JSDeveloperMenu menu, Function callback, Function enabler)
	{
	}

	@Override
	public Location getLOCATION()
	{
		return LOCATION;
	}
}
