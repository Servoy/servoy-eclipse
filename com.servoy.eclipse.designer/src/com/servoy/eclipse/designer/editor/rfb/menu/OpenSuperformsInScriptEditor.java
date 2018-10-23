/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2018 Servoy BV

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

package com.servoy.eclipse.designer.editor.rfb.menu;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;

import com.servoy.eclipse.designer.editor.rfb.actions.handlers.OpenScriptHandler;
import com.servoy.eclipse.designer.util.DesignerUtil;
import com.servoy.eclipse.ui.Activator;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.util.PersistHelper;

/**
 * Adds menu items for opening super forms in the script editor.
 * @author emera
 */
public class OpenSuperformsInScriptEditor extends CompoundContributionItem
{
	public static final String OPEN_SUPER_SCRIPT_MENU_LABEL = "Open Superform in Script Editor";
	public static final String OPEN_SUPER_SCRIPT_MENU_ID = "opensuperforminscript";

	public OpenSuperformsInScriptEditor()
	{
	}

	public OpenSuperformsInScriptEditor(String id)
	{
		super(id);
	}

	@Override
	protected IContributionItem[] getContributionItems()
	{
		Form form = DesignerUtil.getActiveEditor().getForm();
		if (form == null) return new IContributionItem[0];
		List<AbstractBase> overrideHierarchy = PersistHelper.getOverrideHierarchy(form);
		IContributionItem[] items = new IContributionItem[overrideHierarchy.size()];
		int len = 0;
		ImageDescriptor icon = Activator.loadImageDescriptorFromBundle("js.png");
		for (AbstractBase ab : overrideHierarchy)
		{
			Form f = (Form)ab;
			final CommandContributionItemParameter commandContributionItemParameter = new CommandContributionItemParameter(
				PlatformUI.getWorkbench().getActiveWorkbenchWindow(), null, OpenScriptHandler.OPEN_SUPER_SCRIPT_ID, CommandContributionItem.STYLE_PUSH);
			Map<String, String> parameters = new HashMap<String, String>();
			parameters.put(OpenScriptHandler.FORM_PARAMETER_NAME, f.getName());
			commandContributionItemParameter.parameters = parameters;
			commandContributionItemParameter.label = f.getName() + ".js";
			commandContributionItemParameter.icon = icon;
			commandContributionItemParameter.visibleEnabled = true;
			CommandContributionItem item = new CommandContributionItem(commandContributionItemParameter);
			items[len++] = item;
		}
		return items;
	}
}
