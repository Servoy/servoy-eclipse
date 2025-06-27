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

package com.servoy.eclipse.designer.editor.rfb.actions.handlers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.json.JSONObject;
import org.mozilla.javascript.Function;
import org.sablo.websocket.IServerService;

import com.servoy.eclipse.developersolution.DeveloperBridge;
import com.servoy.eclipse.developersolution.DeveloperNGClient;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.DeveloperSolutionAction;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.WebComponent;
import com.servoy.j2db.scripting.solutionmodel.JSForm;
import com.servoy.j2db.scripting.solutionmodel.JSWebComponent;
import com.servoy.j2db.scripting.solutionmodel.developer.JSDeveloperMenu;
import com.servoy.j2db.util.UUID;

/**
 * @author gabi
 *
 */
public class ExecuteDeveloperMenu implements IServerService
{

	private final UUID formUUID;
	private final ISelectionProvider selectionProvider;

	public ExecuteDeveloperMenu(UUID formUUID, ISelectionProvider selectionProvider)
	{
		this.formUUID = formUUID;
		this.selectionProvider = selectionProvider;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.sablo.websocket.IServerService#executeMethod(java.lang.String, org.json.JSONObject)
	 */
	@Override
	public Object executeMethod(String methodName, JSONObject args) throws Exception
	{
		if (args.has("isForm") && args.has("name"))
		{
			JSForm form = null;
			ArrayList<JSWebComponent> wc = new ArrayList<>();

			if (args.getBoolean("isForm"))
			{
				form = new JSForm(DeveloperNGClient.INSTANCE, (Form)ServoyModelFinder.getServoyModel().getActiveProject().getEditingPersist(formUUID), true);
			}
			else if (selectionProvider != null)
			{
				PersistContext[] selection = null;
				selection = (PersistContext[])((IStructuredSelection)selectionProvider.getSelection()).toList().toArray(new PersistContext[0]);
				if (selection.length > 0 && selection[0].getContext() instanceof Form && selection[0].getPersist() instanceof WebComponent)
				{
					form = new JSForm(DeveloperNGClient.INSTANCE, (Form)selection[0].getContext(), true);
					wc.add(new JSWebComponent(form, (WebComponent)selection[0].getPersist(), DeveloperNGClient.INSTANCE, true));

					for (int i = 1; i < selection.length; i++)
					{
						if (selection[i].getPersist() instanceof WebComponent)
						{
							wc.add(new JSWebComponent(form, (WebComponent)selection[i].getPersist(), DeveloperNGClient.INSTANCE, true));
						}
					}
				}
			}

			String menuName = args.getString("name");
			for (Map.Entry<JSDeveloperMenu, Function> entry : DeveloperBridge.menus.entrySet())
			{
				if (menuName.equals(entry.getKey().getText()))
				{
					String[] componentNames = entry.getKey().getComponentNames();

					if (args.getBoolean("isForm") || componentNames == null || componentNames.length == 0 ||
						(wc.size() > 0 && Arrays.asList(componentNames).contains(wc.get(0).getTypeName())))
					{
						DeveloperSolutionAction devSolAction = new DeveloperSolutionAction(entry.getKey(), entry.getValue(),
							new Object[] { form, wc.toArray(new JSWebComponent[wc.size()]) });
						devSolAction.run();
						break;
					}
				}
			}
		}
		return null;
	}

}
