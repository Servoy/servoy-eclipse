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
import java.util.Map.Entry;

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.json.JSONObject;
import org.sablo.websocket.IServerService;

import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.developersolution.DeveloperBridge;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.WebComponent;
import com.servoy.j2db.scripting.FunctionDefinition;
import com.servoy.j2db.scripting.solutionmodel.developer.JSDeveloperMenu;
import com.servoy.j2db.util.UUID;

/**
 * @author gabi
 *
 */
public class ExecuteDeveloperMenu implements IServerService
{

	private final BaseVisualFormEditor editorPart;
	private final UUID formUUID;
	private final ISelectionProvider selectionProvider;

	public ExecuteDeveloperMenu(BaseVisualFormEditor editorPart, UUID formUUID, ISelectionProvider selectionProvider)
	{
		this.editorPart = editorPart;
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
			Form form = null;
			ArrayList<BaseComponent> wc = new ArrayList<>();
			boolean allSelectedComponentsAreWebComponents = true;

			if (args.getBoolean("isForm"))
			{
				form = (Form)ServoyModelFinder.getServoyModel().getActiveProject().getEditingPersist(formUUID);
			}
			else if (selectionProvider != null)
			{
				PersistContext[] selection = null;
				selection = (PersistContext[])((IStructuredSelection)selectionProvider.getSelection()).toList().toArray(new PersistContext[0]);
				if (selection.length > 0 && selection[0].getContext() instanceof Form && selection[0].getPersist() instanceof BaseComponent)
				{
					form = (Form)selection[0].getContext();
					for (PersistContext element : selection)
					{
						if (element.getPersist() instanceof BaseComponent)
						{
							wc.add((BaseComponent)element.getPersist());
							if (allSelectedComponentsAreWebComponents) allSelectedComponentsAreWebComponents = element.getPersist() instanceof WebComponent;
						}
					}
				}
			}

			String menuName = args.getString("name");
			for (Entry<JSDeveloperMenu, FunctionDefinition> entry : DeveloperBridge.menus.entrySet())
			{
				if (menuName.equals(entry.getKey().getText()))
				{
					String[] componentNames = entry.getKey().getComponentNames();

					if (args.getBoolean("isForm") || componentNames == null || componentNames.length == 0 || !allSelectedComponentsAreWebComponents ||
						(wc.size() > 0 && Arrays.asList(componentNames).contains(((WebComponent)wc.get(0)).getTypeName())))
					{
						editorPart.executeDeveloperMenuCommand(entry.getValue(), new Form[] { form }, wc.toArray(new BaseComponent[wc.size()]));
						break;
					}
				}
			}
		}
		return null;
	}
}